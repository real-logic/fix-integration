package uk.co.real_logic.fix_gateway.acceptance_tests.environments;

import uk.co.real_logic.agrona.collections.Int2ObjectHashMap;
import uk.co.real_logic.fix_gateway.acceptance_tests.quickfix.TestConnection;
import uk.co.real_logic.fix_gateway.engine.FixEngine;
import uk.co.real_logic.fix_gateway.library.FixLibrary;
import uk.co.real_logic.fix_gateway.library.session.Session;
import uk.co.real_logic.fix_gateway.system_tests.FakeOtfAcceptor;
import uk.co.real_logic.fix_gateway.system_tests.FakeSessionHandler;
import uk.co.real_logic.fix_gateway.system_tests.SystemTestUtil;

import java.io.IOException;
import java.util.concurrent.locks.LockSupport;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static uk.co.real_logic.agrona.CloseHelper.quietClose;
import static uk.co.real_logic.fix_gateway.TestFixtures.unusedPort;
import static uk.co.real_logic.fix_gateway.system_tests.SystemTestUtil.*;

public class QuickFixToGatewayEnvironment implements Environment
{
    private final Int2ObjectHashMap<TestConnection> connections = new Int2ObjectHashMap<>();
    private final Int2ObjectHashMap<Session> acceptors = new Int2ObjectHashMap<>();

    private final FakeOtfAcceptor acceptingOtfAcceptor = new FakeOtfAcceptor();
    private final FakeSessionHandler acceptingSessionHandler = new FakeSessionHandler(acceptingOtfAcceptor);

    private final FixEngine acceptingEngine;
    private final FixLibrary acceptingLibrary;
    private final int port;

    public QuickFixToGatewayEnvironment()
    {
        port = unusedPort();
        final int aeronPort = unusedPort();
        acceptingEngine = launchAcceptingGateway(port);
        acceptingLibrary = new FixLibrary(
            acceptingLibraryConfig(acceptingSessionHandler, ACCEPTOR_ID, INITIATOR_ID, aeronPort, "acceptingLibrary"));
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
        final Session session = SystemTestUtil.acceptSession(acceptingSessionHandler, acceptingLibrary);
        acceptors.put(clientId, session);
    }

    public void initiateMessage(final int clientId, final String message) throws IOException
    {
        acceptingLibrary.poll(1);
        connections.get(clientId).sendMessage(clientId, message);
    }

    public void initiateDisconnect(final int clientId) throws Exception
    {
        acceptingLibrary.poll(1);
        connections.get(clientId).disconnect(clientId);
    }

    public void expectDisconnect(final int clientId) throws Exception
    {
        final Session session = acceptors.get(clientId);
        assertSessionDisconnected(acceptingLibrary, session);

        connections.get(clientId).waitForClientDisconnect(clientId);
    }

    public CharSequence readMessage(final int clientId, final long timeoutInMs) throws Exception
    {
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

    private void park()
    {
        LockSupport.parkNanos(MICROSECONDS.toNanos(10));
    }
}
