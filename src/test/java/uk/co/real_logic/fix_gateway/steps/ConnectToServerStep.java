package uk.co.real_logic.fix_gateway.steps;


import uk.co.real_logic.fix_gateway.environments.Environment;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ConnectToServerStep implements TestStep
{
    private static final Pattern CONNECT_PATTERN = Pattern.compile("i(\\d+)*,?CONNECT");
    private final String line;

    public ConnectToServerStep(final String line)
    {
        this.line = line;
    }

    public void run(final Environment environment) throws Exception
    {
        final Matcher matcher = CONNECT_PATTERN.matcher(line);
        final int clientId = getClientId(matcher, line);
        environment.connect(clientId);
    }
}
