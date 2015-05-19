package uk.co.real_logic.fix_gateway.steps;

import uk.co.real_logic.fix_gateway.environments.Environment;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExpectMessageStep implements TestStep
{
    private static final Pattern HEADER_PATTERN = Pattern.compile("^E(\\d+),.*");
    private final String line;
    private int clientId;

    public ExpectMessageStep(final String line)
    {
        this.line = line;
    }

    public void run(final Environment environment)
    {
        final Matcher headerMatcher = HEADER_PATTERN.matcher(line);
        clientId = getClientId(headerMatcher);
    }
}
