package uk.co.real_logic.artio.acceptance_tests.steps;

import uk.co.real_logic.artio.acceptance_tests.Environment;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InitiateDisconnect implements TestStep
{
    private static final Pattern DISCONNECT_PATTERN = Pattern.compile("i(\\d+)*,?DISCONNECT");
    private final String line;

    public InitiateDisconnect(final String line)
    {
        this.line = line;
    }

    public void run(final Environment environment) throws Exception
    {
        final Matcher matcher = DISCONNECT_PATTERN.matcher(line);
        final int clientId = getClientId(matcher, line);
        environment.expectDisconnect(clientId);
    }

    public String toString()
    {
        return line;
    }
}
