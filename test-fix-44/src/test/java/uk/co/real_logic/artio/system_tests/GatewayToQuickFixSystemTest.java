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
package uk.co.real_logic.artio.system_tests;

import io.aeron.archive.ArchivingMediaDriver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import quickfix.ConfigError;
import quickfix.SocketAcceptor;
import uk.co.real_logic.artio.Reply;
import uk.co.real_logic.artio.Timing;
import uk.co.real_logic.artio.engine.FixEngine;
import uk.co.real_logic.artio.library.FixLibrary;
import uk.co.real_logic.artio.session.Session;

import static org.agrona.CloseHelper.quietClose;
import static org.junit.Assert.assertThat;
import static uk.co.real_logic.artio.TestFixtures.launchMediaDriver;
import static uk.co.real_logic.artio.TestFixtures.unusedPort;
import static uk.co.real_logic.artio.Timing.assertEventuallyTrue;
import static uk.co.real_logic.artio.acceptance_tests.CustomMatchers.containsInitiator;
import static uk.co.real_logic.artio.messages.SessionState.DISCONNECTED;
import static uk.co.real_logic.artio.system_tests.QuickFixUtil.*;
import static uk.co.real_logic.artio.system_tests.SystemTestUtil.*;

public class GatewayToQuickFixSystemTest
{
    private ArchivingMediaDriver mediaDriver;
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
        testSystem.awaitReply(reply);
        initiatedSession = reply.resultIfPresent();
        assertConnected(initiatedSession);
        assertThat(acceptorApplication.logons(), containsInitiator());
    }

    @Test
    public void messagesCanBeSentFromInitiatorToAcceptor()
    {
        final String testReqID = testReqId();
        sendTestRequest(initiatedSession, testReqID);

        assertQuickFixReceivedMessage(acceptorApplication);
    }

    @Test
    public void messagesCanBeSentFromAcceptorToInitiator()
    {
        final String testReqId = "hi";
        sendTestRequestTo(onlySessionId(acceptor), testReqId);

        assertReceivedTestRequest(testSystem, initiatingOtfAcceptor, testReqId);
    }

    private static void assertReceivedTestRequest(
        final TestSystem testSystem, final FakeOtfAcceptor acceptor, final String testReqId)
    {
        Timing.assertEventuallyTrue("Failed to receive a test request message", () ->
        {
            testSystem.poll();
            return acceptor
                .hasReceivedMessage("1")
                .filter((msg) -> testReqId.equals(msg.testReqId())).count() > 0L;
        });
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
    public void close()
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
