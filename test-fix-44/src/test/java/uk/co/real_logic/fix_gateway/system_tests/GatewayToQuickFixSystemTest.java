/*
 * Copyright 2015 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.real_logic.fix_gateway.system_tests;

import io.aeron.driver.MediaDriver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import quickfix.ConfigError;
import quickfix.SocketAcceptor;
import uk.co.real_logic.fix_gateway.Reply;
import uk.co.real_logic.fix_gateway.engine.FixEngine;
import uk.co.real_logic.fix_gateway.library.FixLibrary;
import uk.co.real_logic.fix_gateway.messages.SessionState;
import uk.co.real_logic.fix_gateway.session.Session;

import static org.agrona.CloseHelper.quietClose;
import static org.junit.Assert.*;
import static uk.co.real_logic.fix_gateway.TestFixtures.launchMediaDriver;
import static uk.co.real_logic.fix_gateway.TestFixtures.unusedPort;
import static uk.co.real_logic.fix_gateway.Timing.DEFAULT_TIMEOUT_IN_MS;
import static uk.co.real_logic.fix_gateway.Timing.assertEventuallyTrue;
import static uk.co.real_logic.fix_gateway.acceptance_tests.CustomMatchers.containsInitiator;
import static uk.co.real_logic.fix_gateway.messages.SessionState.DISCONNECTED;
import static uk.co.real_logic.fix_gateway.system_tests.QuickFixUtil.*;
import static uk.co.real_logic.fix_gateway.system_tests.SystemTestUtil.*;

public class GatewayToQuickFixSystemTest
{
    private MediaDriver mediaDriver;
    private FixEngine initiatingEngine;
    private FixLibrary initiatingLibrary;
    private TestSystem testSystem;
    private Session initiatedSession;

    private FakeOtfAcceptor initiatingOtfAcceptor = new FakeOtfAcceptor();
    private FakeHandler initiatingSessionHandler = new FakeHandler(initiatingOtfAcceptor);

    private SocketAcceptor acceptor;
    private FakeQuickFixApplication acceptorApplication = new FakeQuickFixApplication();

    @Before
    public void launch() throws ConfigError
    {
        final int port = unusedPort();
        final int initAeronPort = unusedPort();
        mediaDriver = launchMediaDriver();
        acceptor = launchQuickFixAcceptor(port, acceptorApplication);
        initiatingEngine = launchInitiatingEngine(initAeronPort);
        initiatingLibrary = newInitiatingLibrary(initAeronPort, initiatingSessionHandler);
        testSystem = new TestSystem(initiatingLibrary);
        final Reply<Session> reply = initiate(initiatingLibrary, port, INITIATOR_ID, ACCEPTOR_ID);
        awaitLibraryReply(testSystem, reply);
        initiatedSession = reply.resultIfPresent();
        assertNotNull(initiatedSession);

        sessionLogsOn(testSystem, initiatedSession, DEFAULT_TIMEOUT_IN_MS);
    }

    @Test
    public void sessionHasBeenInitiated()
    {
        assertTrue("Session has failed to connect", initiatedSession.isConnected());
        assertTrue("Session has failed to logon", initiatedSession.state() == SessionState.ACTIVE);
        assertThat(acceptorApplication.logons(), containsInitiator());
    }

    @Test
    public void messagesCanBeSentFromInitiatorToAcceptor()
    {
        sendTestRequest(initiatedSession);

        assertQuickFixReceivedMessage(acceptorApplication);
    }

    @Test
    public void messagesCanBeSentFromAcceptorToInitiator()
    {
        sendTestRequestTo(onlySessionId(acceptor));

        assertReceivedTestRequest(testSystem, initiatingOtfAcceptor);
    }

    @Test
    public void initiatorSessionCanBeDisconnected()
    {
        initiatedSession.startLogout();

        assertQuickFixDisconnected(acceptorApplication, containsInitiator());
    }

    @Test
    public void acceptorSessionCanBeDisconnected()
    {
        logout(acceptor);

        assertSessionDisconnected(initiatedSession);
    }

    private void assertSessionDisconnected(final Session session)
    {
        assertEventuallyTrue("Session is still connected",
            () ->
            {
                testSystem.poll();
                return session.state() == DISCONNECTED;
            });
    }

    @After
    public void close() throws Exception
    {
        if (acceptor != null)
        {
            acceptor.stop();
        }

        quietClose(initiatingLibrary);
        quietClose(initiatingEngine);
        quietClose(mediaDriver);
    }

}
