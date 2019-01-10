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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import quickfix.ConfigError;
import quickfix.SocketInitiator;
import uk.co.real_logic.artio.engine.DefaultEngineScheduler;
import uk.co.real_logic.artio.engine.EngineConfiguration;
import uk.co.real_logic.artio.engine.FixEngine;
import uk.co.real_logic.artio.library.FixLibrary;
import uk.co.real_logic.artio.session.Session;
import uk.co.real_logic.artio.validation.PersistenceLevel;

import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.agrona.CloseHelper.quietClose;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.co.real_logic.artio.TestFixtures.launchMediaDriver;
import static uk.co.real_logic.artio.TestFixtures.unusedPort;
import static uk.co.real_logic.artio.acceptance_tests.CustomMatchers.containsAcceptor;
import static uk.co.real_logic.artio.messages.SessionState.ACTIVE;
import static uk.co.real_logic.artio.system_tests.QuickFixUtil.assertQuickFixDisconnected;
import static uk.co.real_logic.artio.system_tests.QuickFixUtil.assertQuickFixReceivedMessage;
import static uk.co.real_logic.artio.system_tests.SystemTestUtil.*;

@RunWith(Parameterized.class)
public class QuickFixToGatewaySystemTest
{
    private ArchivingMediaDriver mediaDriver;
    private FixEngine acceptingEngine;
    private FixLibrary acceptingLibrary;
    private TestSystem testSystem;
    private Session acceptedSession;

    private FakeOtfAcceptor acceptingOtfAcceptor = new FakeOtfAcceptor();
    private FakeHandler acceptingSessionHandler = new FakeHandler(acceptingOtfAcceptor);

    private SocketInitiator socketInitiator;
    private FakeQuickFixApplication initiator = new FakeQuickFixApplication();

    private PersistenceLevel level;

    @Parameterized.Parameters
    public static List<Object[]> data()
    {
        return Stream.of(PersistenceLevel.values())
                     .map(level -> new Object[] {level})
                     .collect(toList());
    }

    public QuickFixToGatewaySystemTest(final PersistenceLevel level)
    {
        this.level = level;
    }

    @Before
    public void launch() throws ConfigError
    {
        final int port = unusedPort();
        mediaDriver = launchMediaDriver();

        final EngineConfiguration config = acceptingConfig(port, ACCEPTOR_ID, INITIATOR_ID);
        config.scheduler(new DefaultEngineScheduler());
        config.sessionPersistenceStrategy(logon -> level);
        SystemTestUtil.delete(config.logFileDir());

        acceptingEngine = FixEngine.launch(config);
        testSystem = new TestSystem();
        acceptingLibrary = testSystem.connect(acceptingLibraryConfig(acceptingSessionHandler));
        socketInitiator = QuickFixUtil.launchQuickFixInitiator(port, initiator);
        awaitQuickFixLogon();
        acceptedSession = acquireSession(acceptingSessionHandler, acceptingLibrary, 1, testSystem);
    }

    @Test
    public void sessionHasBeenInitiated()
    {
        assertThat(initiator.logons(), containsAcceptor());

        assertTrue("Session has failed to connect", acceptedSession.isConnected());
        assertTrue("Session has failed to logon", acceptedSession.state() == ACTIVE);
    }

    @Test
    public void messagesCanBeSentFromAcceptorToInitiator()
    {
        final String testReqID = testReqId();
        sendTestRequest(acceptedSession, testReqID);

        assertQuickFixReceivedMessage(initiator);
    }

    @Test
    public void acceptorSessionCanBeDisconnected()
    {
        acceptedSession.startLogout();

        assertQuickFixDisconnected(initiator, containsAcceptor());
    }

    @After
    public void close()
    {
        if (socketInitiator != null)
        {
            socketInitiator.stop();
        }

        quietClose(acceptingLibrary);
        quietClose(acceptingEngine);
        quietClose(mediaDriver);
    }

    private void awaitQuickFixLogon()
    {
        while (!socketInitiator.isLoggedOn())
        {
            testSystem.poll();
            Thread.yield();
        }
    }

}
