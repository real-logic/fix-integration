package uk.co.real_logic.fix_gateway.environments;

public interface Environment extends AutoCloseable
{
    void connect(final int clientId) throws Exception;

    void initiateMessage(final int clientId, final String message) throws Exception;

    void expectDisconnect(final int clientId) throws Exception;
}
