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

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import quickfix.ConfigError;
import quickfix.FieldNotFound;
import quickfix.SessionID;
import quickfix.SocketAcceptor;
import uk.co.real_logic.aeron.driver.MediaDriver;
import uk.co.real_logic.fix_gateway.engine.FixEngine;
import uk.co.real_logic.fix_gateway.library.FixLibrary;
import uk.co.real_logic.fix_gateway.library.session.Session;
import uk.co.real_logic.fix_gateway.library.session.SessionState;

import java.io.IOException;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.co.real_logic.agrona.CloseHelper.quietClose;
import static uk.co.real_logic.fix_gateway.TestFixtures.launchMediaDriver;
import static uk.co.real_logic.fix_gateway.TestFixtures.unusedPort;
import static uk.co.real_logic.fix_gateway.Timing.assertEventuallyTrue;
import static uk.co.real_logic.fix_gateway.system_tests.QuickFixUtil.*;
import static uk.co.real_logic.fix_gateway.system_tests.SystemTestUtil.*;

public class GatewayToQuickFixSystemTest
{
    private MediaDriver mediaDriver;
    private FixEngine initiatingEngine;
    private FixLibrary initiatingLibrary;
    private Session initiatedSession;

    private FakeOtfAcceptor initiatingOtfAcceptor = new FakeOtfAcceptor();
    private FakeSessionHandler initiatingSessionHandler = new FakeSessionHandler(initiatingOtfAcceptor);

    private SocketAcceptor acceptor;
    private FakeQuickFixApplication acceptorApplication = new FakeQuickFixApplication();

    @Before
    public void launch() throws ConfigError
    {
        final int port = unusedPort();
        final int initAeronPort = unusedPort();
        mediaDriver = launchMediaDriver();
        acceptor = launchQuickFixAcceptor(port, acceptorApplication);
        initiatingEngine = launchInitiatingGateway(initAeronPort);
        initiatingLibrary = newInitiatingLibrary(initAeronPort, initiatingSessionHandler);
        initiatedSession = initiate(initiatingLibrary, port, INITIATOR_ID, ACCEPTOR_ID);

        sessionLogsOn(initiatingLibrary, null, initiatedSession);
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

        assertReceivedTestRequest(initiatingLibrary, initiatingOtfAcceptor);
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

        assertSessionDisconnected(initiatingLibrary, initiatedSession);
    }

    @Ignore
    @Test
    public void gatewayProcessesResendRequests() throws IOException, InterruptedException, FieldNotFound
    {
        clearMessages();

        final SessionID sessionID = onlySessionId(acceptor);
        sendTestRequestTo(sessionID);

        awaitMessages();

        /*final Session session = onlySession(acceptor);
        final int msgSeqNum = session.getExpectedSenderNum() - 1;

        clearMessages();

        sendResendRequest(sessionID, msgSeqNum, msgSeqNum);

        awaitMessages();

        final Message message = acceptorApplication.messages().get(0);
        assertNotNull(message);

        final String sender = message.getHeader().getString(SenderCompID.FIELD);
        assertEquals(INITIATOR_ID, sender);*/
    }

    private void awaitMessages()
    {
        assertEventuallyTrue("Failed to receive a reply", () -> acceptorApplication.messages().size() >= 2);
    }

    private void clearMessages()
    {
        acceptorApplication.messages().clear();
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
