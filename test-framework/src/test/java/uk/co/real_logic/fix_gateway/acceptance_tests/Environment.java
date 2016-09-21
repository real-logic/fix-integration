package uk.co.real_logic.fix_gateway.acceptance_tests;

import org.agrona.collections.Int2ObjectHashMap;
import uk.co.real_logic.fix_gateway.engine.EngineConfiguration;
import uk.co.real_logic.fix_gateway.engine.FixEngine;
import uk.co.real_logic.fix_gateway.library.FixLibrary;
import uk.co.real_logic.fix_gateway.library.LibraryConfiguration;
import uk.co.real_logic.fix_gateway.session.Session;
import uk.co.real_logic.fix_gateway.system_tests.FakeHandler;
import uk.co.real_logic.fix_gateway.system_tests.FakeOtfAcceptor;

import java.io.IOException;

import static java.lang.System.currentTimeMillis;
import static org.agrona.CloseHelper.quietClose;
import static uk.co.real_logic.fix_gateway.TestFixtures.unusedPort;
import static uk.co.real_logic.fix_gateway.system_tests.SystemTestUtil.*;

public final class Environment implements AutoCloseable
{
    public static final String ACCEPTOR_ID = "ISLD";
    public static final String INITIATOR_ID = "TW";

    private final Int2ObjectHashMap<TestConnection> clientIdToConnection = new Int2ObjectHashMap<>();
    private final Int2ObjectHashMap<Session> acceptors = new Int2ObjectHashMap<>();

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
        delete(ACCEPTOR_LOGS);
        final EngineConfiguration config = acceptingConfig(port, "engineCounters", ACCEPTOR_ID, INITIATOR_ID);
        acceptingEngine = FixEngine.launch(config);
        final LibraryConfiguration acceptingLibrary =
            acceptingLibraryConfig(acceptingHandler, ACCEPTOR_ID, INITIATOR_ID, "aeron:ipc");
        this.acceptingLibrary = FixLibrary.connect(acceptingLibrary);
    }

    public void close() throws Exception
    {
        quietClose(acceptingLibrary);
        quietClose(acceptingEngine);
    }

    // NB: assumes clientids arrive in the order, holds true for FIX acceptance tests
    public void connect(final int clientId) throws IOException
    {
        final TestConnection connection = new TestConnection();
        connection.connect(clientId, port);
        clientIdToConnection.put(clientId, connection);
    }

    public void initiateMessage(final int clientId, final String message) throws IOException
    {
        acceptingLibrary.poll(1);
        clientIdToConnection.get(clientId).sendMessage(clientId, message);
    }

    public void initiatorDisconnect(final int clientId) throws Exception
    {
        acceptingLibrary.poll(1);
        clientIdToConnection.get(clientId).disconnect(clientId);
    }

    public void expectDisconnect(final int clientId) throws Exception
    {
        clientIdToConnection.get(clientId).waitForClientDisconnect(clientId, acceptingLibrary);
    }

    public CharSequence readMessage(final int clientId, final long timeoutInMs) throws Exception
    {
        final long timeout = currentTimeMillis() + timeoutInMs;
        final TestConnection.TestIoHandler handler = clientIdToConnection.get(clientId).getIoHandler(clientId);
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
