package uk.co.real_logic.fix_gateway.acceptance_tests;

import uk.co.real_logic.fix_gateway.library.GatewayErrorHandler;
import uk.co.real_logic.fix_gateway.messages.GatewayError;

public class ErrorDetector implements GatewayErrorHandler
{
    private static final int NO_CONNECTION = -1;

    private long lastDuplicateConnectionId = NO_CONNECTION;

    public void onError(final GatewayError errorType, final int libraryId, final String message)
    {
        if (errorType == GatewayError.DUPLICATE_SESSION)
        {
            final String connectionIdField = message.split(":")[0];
            lastDuplicateConnectionId = Long.parseLong(connectionIdField);
        }
    }

    public long lastDuplicateConnectionId()
    {
        return lastDuplicateConnectionId;
    }

    public boolean hasConnection()
    {
        return lastDuplicateConnectionId != NO_CONNECTION;
    }

    public void reset()
    {
        lastDuplicateConnectionId = NO_CONNECTION;
    }
}
