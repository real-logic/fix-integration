package uk.co.real_logic.fix_gateway;

import uk.co.real_logic.agrona.collections.Int2ObjectHashMap;
import uk.co.real_logic.fix_gateway.session.InitiatorSession;
import uk.co.real_logic.fix_gateway.session.Session;
import uk.co.real_logic.fix_gateway.system_tests.FakeOtfAcceptor;
import uk.co.real_logic.fix_gateway.system_tests.FakeSessionHandler;

import static uk.co.real_logic.fix_gateway.TestFixtures.unusedPort;
import static uk.co.real_logic.fix_gateway.system_tests.SystemTestUtil.*;

public class Environment implements AutoCloseable
{
    private final Int2ObjectHashMap<InitiatorSession> initiators = new Int2ObjectHashMap<>();
    private final Int2ObjectHashMap<Session> acceptors = new Int2ObjectHashMap<>();

    private final FakeOtfAcceptor initiatingOtfAcceptor = new FakeOtfAcceptor();
    private final FakeSessionHandler initiatingSessionHandler = new FakeSessionHandler(initiatingOtfAcceptor);

    private final FakeOtfAcceptor acceptingOtfAcceptor = new FakeOtfAcceptor();
    private final FakeSessionHandler acceptingSessionHandler = new FakeSessionHandler(acceptingOtfAcceptor);

    private final FixGateway acceptingGateway;
    private final FixGateway initiatingGateway;
    private final int port;

    public Environment()
    {
        port = unusedPort();
        acceptingGateway = launchAcceptingGateway(port, acceptingSessionHandler);
        initiatingGateway = launchInitiatingGateway(initiatingSessionHandler);
    }

    public void close() throws Exception
    {
        if (acceptingGateway != null)
        {
            acceptingGateway.close();
        }

        if (initiatingGateway != null)
        {
            initiatingGateway.close();
        }
    }

    public void connect(final int clientId)
    {
        initiators.put(clientId, initiate(initiatingGateway, port));
        acceptors.put(clientId, acceptingSessionHandler.session());
    }

    public InitiatorSession initiatorSession(final int clientId)
    {
        return initiators.get(clientId);
    }

    public Session acceptorSession(final int clientId)
    {
        return acceptors.get(clientId);
    }

    public FakeSessionHandler acceptingSessionHandler()
    {
        return acceptingSessionHandler;
    }
}
