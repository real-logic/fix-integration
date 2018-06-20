package uk.co.real_logic.artio.acceptance_tests;

import org.agrona.collections.Int2ObjectHashMap;
import uk.co.real_logic.artio.CommonConfiguration;
import uk.co.real_logic.artio.engine.EngineConfiguration;
import uk.co.real_logic.artio.engine.FixEngine;
import uk.co.real_logic.artio.engine.LowResourceEngineScheduler;
import uk.co.real_logic.artio.library.FixLibrary;
import uk.co.real_logic.artio.library.LibraryConfiguration;
import uk.co.real_logic.artio.session.SessionCustomisationStrategy;
import uk.co.real_logic.artio.system_tests.FakeHandler;
import uk.co.real_logic.artio.system_tests.FakeOtfAcceptor;
import uk.co.real_logic.artio.validation.AuthenticationStrategy;
import uk.co.real_logic.artio.validation.MessageValidationStrategy;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static java.lang.System.currentTimeMillis;
import static java.util.Collections.singletonList;
import static org.agrona.CloseHelper.quietClose;
import static uk.co.real_logic.artio.TestFixtures.unusedPort;
import static uk.co.real_logic.artio.system_tests.SystemTestUtil.ACCEPTOR_LOGS;
import static uk.co.real_logic.artio.system_tests.SystemTestUtil.delete;

public final class Environment implements AutoCloseable
{
    private static final String ACCEPTOR_ID = "ISLD";
    private static final String INITIATOR_ID = "TW";

    private final Int2ObjectHashMap<TestConnection> clientIdToConnection = new Int2ObjectHashMap<>();

    private final FakeOtfAcceptor acceptingOtfAcceptor = new FakeOtfAcceptor();
    private final FakeHandler acceptingHandler = new FakeHandler(acceptingOtfAcceptor);

    private final FixEngine acceptingEngine;
    private final FixLibrary acceptingLibrary;
    private final int port;

    public static Environment fix44()
    {
        return new Environment(null);
    }


    public static Environment fix42()
    {
        return new Environment(null);
    }

    public static Environment fix50(final SessionCustomisationStrategy sessionCustomisationStrategy)
    {
        return new Environment(sessionCustomisationStrategy);
    }

    private Environment(final SessionCustomisationStrategy sessionCustomisationStrategy)
    {
        port = unusedPort();
        delete(ACCEPTOR_LOGS);
        delete("engineCounters");
        final EngineConfiguration config = acceptingConfig(port, ACCEPTOR_ID, INITIATOR_ID);
        if (sessionCustomisationStrategy != null)
        {
            config.sessionCustomisationStrategy(sessionCustomisationStrategy);
        }
        acceptingEngine = FixEngine.launch(config);
        final LibraryConfiguration acceptingLibrary = new LibraryConfiguration();
        acceptingLibrary.sessionCustomisationStrategy(sessionCustomisationStrategy);
        setupCommonConfig(ACCEPTOR_ID, INITIATOR_ID, acceptingLibrary);
        acceptingLibrary
            .sessionExistsHandler(acceptingHandler)
            .sessionAcquireHandler(acceptingHandler)
            .sentPositionHandler(acceptingHandler)
            .libraryAeronChannels(singletonList("aeron:ipc"));

        this.acceptingLibrary = FixLibrary.connect(acceptingLibrary);
    }

    private static void setupCommonConfig(
        final String acceptorId, final String initiatorId, final CommonConfiguration configuration)
    {
        final MessageValidationStrategy validationStrategy = MessageValidationStrategy
            .targetCompId(acceptorId)
            .and(MessageValidationStrategy.senderCompId(Arrays.asList(initiatorId, "initiator2")));
        final AuthenticationStrategy authenticationStrategy = AuthenticationStrategy.of(validationStrategy);
        configuration.authenticationStrategy(authenticationStrategy).messageValidationStrategy(validationStrategy);
    }

    private static EngineConfiguration acceptingConfig(
        final int port, final String acceptorId, final String initiatorId)
    {
        final EngineConfiguration configuration = new EngineConfiguration();
        setupCommonConfig(acceptorId, initiatorId, configuration);
        return configuration
            .bindTo("localhost", port)
            .libraryAeronChannel("aeron:ipc")
            .monitoringFile(acceptorMonitoringFile())
            .logFileDir("engineCounters")
            .scheduler(new LowResourceEngineScheduler());
    }

    private static String acceptorMonitoringFile()
    {
        return CommonConfiguration.optimalTmpDirName() +
            File.separator + "fix-acceptor" + File.separator + "engineCounters";
    }

    public void close()
    {
        quietClose(acceptingLibrary);
        quietClose(acceptingEngine);
    }

    // NB: assumes client ids arrive in the order, holds true for FIX acceptance tests
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
