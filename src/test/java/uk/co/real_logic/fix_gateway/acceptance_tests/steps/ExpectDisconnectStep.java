package uk.co.real_logic.fix_gateway.acceptance_tests.steps;

import uk.co.real_logic.fix_gateway.acceptance_tests.environments.Environment;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExpectDisconnectStep implements TestStep
{
    private static final Pattern DISCONNECT_PATTERN = Pattern.compile("e(\\d+)*,?DISCONNECT");
    private final String line;

    public ExpectDisconnectStep(final String line)
    {
        this.line = line;
    }

    public void run(final Environment environment) throws Exception
    {
        final Matcher matcher = DISCONNECT_PATTERN.matcher(line);
        final int clientId = getClientId(matcher, line);
        environment.expectDisconnect(clientId);
    }
}
