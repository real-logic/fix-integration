package uk.co.real_logic.fix_gateway.environments;

import uk.co.real_logic.agrona.collections.Int2ObjectHashMap;
import uk.co.real_logic.fix_gateway.FixGateway;
import uk.co.real_logic.fix_gateway.session.InitiatorSession;
import uk.co.real_logic.fix_gateway.system_tests.FakeOtfAcceptor;
import uk.co.real_logic.fix_gateway.system_tests.FakeSessionHandler;

import static uk.co.real_logic.fix_gateway.TestFixtures.unusedPort;
import static uk.co.real_logic.fix_gateway.system_tests.SystemTestUtil.initiate;
import static uk.co.real_logic.fix_gateway.system_tests.SystemTestUtil.launchInitiatingGateway;

public class GatewayToQuickFixEnvironment implements Environment
{
    private final Int2ObjectHashMap<InitiatorSession> initiators = new Int2ObjectHashMap<>();

    private final FakeOtfAcceptor initiatingOtfAcceptor = new FakeOtfAcceptor();

    private final FakeSessionHandler initiatingSessionHandler = new FakeSessionHandler(initiatingOtfAcceptor);

    private final int port;
    private final FixGateway initiatingGateway;

    public GatewayToQuickFixEnvironment()
    {
        port = unusedPort();
        initiatingGateway = launchInitiatingGateway(initiatingSessionHandler);
    }

    public void close() throws Exception
    {
        if (initiatingGateway != null)
        {
            initiatingGateway.close();
        }
    }

    public void connect(final int clientId)
    {
        initiators.put(clientId, initiate(initiatingGateway, port));
    }

    public void initiateMessage(final int clientId, final String message)
    {

    }

    public void expectDisconnect(final int clientId)
    {

    }

    public InitiatorSession initiatorSession(final int clientId)
    {
        return initiators.get(clientId);
    }
}
