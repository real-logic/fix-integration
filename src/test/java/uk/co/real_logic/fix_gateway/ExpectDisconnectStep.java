package uk.co.real_logic.fix_gateway;

import uk.co.real_logic.fix_gateway.system_tests.FakeSessionHandler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static uk.co.real_logic.fix_gateway.Timing.assertEventuallyTrue;

public class ExpectDisconnectStep implements TestStep
{
    private static final Pattern DISCONNECT_PATTERN = Pattern.compile("e(\\d+)*,?DISCONNECT");
    private final String line;

    public ExpectDisconnectStep(final String line)
    {
        this.line = line;
    }

    public void run(final Environment environment) throws InterruptedException
    {
        final Matcher matcher = DISCONNECT_PATTERN.matcher(line);
        final int clientId = getClientId(matcher, line);
        final FakeSessionHandler sessionHandler = environment.acceptingSessionHandler();

        assertEventuallyTrue("Failed to disconnect",
            () ->
            {
                sessionHandler.subscription().poll(1);
                assertEquals(0L, sessionHandler.connectionId());
            });
    }
}
