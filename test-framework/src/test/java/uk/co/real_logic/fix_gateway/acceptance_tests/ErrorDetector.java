package uk.co.real_logic.fix_gateway.acceptance_tests;

import uk.co.real_logic.fix_gateway.library.GatewayErrorHandler;
import uk.co.real_logic.fix_gateway.messages.GatewayError;

public class ErrorDetector implements GatewayErrorHandler
{
    private int lastDuplicateConnection = 0;

    public void onError(final GatewayError errorType, final int libraryId, final String message)
    {
        if (errorType == GatewayError.DUPLICATE_SESSION)
        {
            lastDuplicateConnection = Integer.parseInt(message.split(":")[0]);
        }
    }

    public int lastDuplicateConnection()
    {
        return lastDuplicateConnection;
    }
}
