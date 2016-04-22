package uk.co.real_logic.fix_gateway.acceptance_tests;

import org.agrona.collections.Int2ObjectHashMap;
import org.junit.Assert;
import uk.co.real_logic.fix_gateway.engine.FixEngine;
import uk.co.real_logic.fix_gateway.library.FixLibrary;
import uk.co.real_logic.fix_gateway.library.LibraryConfiguration;
import uk.co.real_logic.fix_gateway.messages.SessionReplyStatus;
import uk.co.real_logic.fix_gateway.session.Session;
import uk.co.real_logic.fix_gateway.system_tests.FakeOtfAcceptor;
import uk.co.real_logic.fix_gateway.system_tests.FakeHandler;
import uk.co.real_logic.fix_gateway.system_tests.SystemTestUtil;

import java.io.IOException;

import static java.lang.System.currentTimeMillis;
import static org.agrona.CloseHelper.quietClose;
import static uk.co.real_logic.fix_gateway.TestFixtures.unusedPort;
import static uk.co.real_logic.fix_gateway.system_tests.SystemTestUtil.acceptingLibraryConfig;
import static uk.co.real_logic.fix_gateway.system_tests.SystemTestUtil.assertSessionDisconnected;

public final class Environment implements AutoCloseable
{
    public static final String ACCEPTOR_ID = "ISLD";
    public static final String INITIATOR_ID = "TW";

    private final Int2ObjectHashMap<TestConnection> connections = new Int2ObjectHashMap<>();
    private final Int2ObjectHashMap<Session> acceptors = new Int2ObjectHashMap<>();

    private final ErrorDetector errorDetector = new ErrorDetector();
    private final FakeOtfAcceptor acceptingOtfAcceptor = new FakeOtfAcceptor();
    private final FakeHandler acceptingHandler = new FakeHandler(acceptingOtfAcceptor);

    private final FixEngine acceptingEngine;
    private final FixLibrary acceptingLibrary;
    private final int port;

    public static Environment fix44()
    {
        return new Environment();
    }

    public static Environment fix42()
    {
        return new Environment();
    }

    private Environment()
    {
        port = unusedPort();
        acceptingEngine = SystemTestUtil.launchAcceptingEngine(port, ACCEPTOR_ID, INITIATOR_ID);
        final LibraryConfiguration acceptingLibrary =
            acceptingLibraryConfig(acceptingHandler, ACCEPTOR_ID, INITIATOR_ID, "acceptingLibrary")
                .gatewayErrorHandler(errorDetector);
        this.acceptingLibrary = FixLibrary.connect(acceptingLibrary);
    }

    public void close() throws Exception
    {
        quietClose(acceptingLibrary);
        quietClose(acceptingEngine);
    }

    public void connect(final int clientId) throws IOException
    {
        final TestConnection connection = new TestConnection();
        connection.connect(clientId, port);
        connections.put(clientId, connection);
    }

    private void ensureSession(final int clientId)
    {
        if (!acceptors.containsKey(clientId))
        {
            while (!acceptingHandler.hasSession())
            {
                acceptingLibrary.poll(1);
            }

            final long sessionId = acceptingHandler.latestSessionId();
            acceptingHandler.clearConnections();
            final SessionReplyStatus reply = acceptingLibrary.acquireSession(sessionId);
            Assert.assertEquals(SessionReplyStatus.OK, reply);
            final Session session = acceptingHandler.latestSession();
            acceptingHandler.resetSession();
            acceptors.put(clientId, session);
        }
    }

    public void initiateMessage(final int clientId, final String message) throws IOException
    {
        acceptingLibrary.poll(1);
        connections.get(clientId).sendMessage(clientId, message);
    }

    public void initiatorDisconnect(final int clientId) throws Exception
    {
        acceptingLibrary.poll(1);
        connections.get(clientId).disconnect(clientId);
    }

    public void expectDisconnect(final int clientId) throws Exception
    {
        ensureSession(clientId);
        final Session session = acceptors.get(clientId);
        assertSessionDisconnected(acceptingLibrary, session);

        connections.get(clientId).waitForClientDisconnect(clientId);
    }

    public CharSequence readMessage(final int clientId, final long timeoutInMs) throws Exception
    {
        ensureSession(clientId);
        final long timeout = currentTimeMillis() + timeoutInMs;
        final TestConnection.TestIoHandler handler = connections.get(clientId).getIoHandler(clientId);
        String message;
        while ((message = handler.pollMessage()) == null)
        {
            acceptingLibrary.poll(1);

            if (timeout < currentTimeMillis())
            {
                throw new InterruptedException("Timed out reading message");
            }
        }
        return message;
    }
}
