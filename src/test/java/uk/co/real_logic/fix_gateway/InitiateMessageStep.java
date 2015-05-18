package uk.co.real_logic.fix_gateway;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InitiateMessageStep implements TestStep
{
    private static final Pattern HEADER_PATTERN = Pattern.compile("^I(\\d+),.*");
    private final String line;
    private int clientId;

    public InitiateMessageStep(final String line)
    {
        this.line = line;
    }

    public void run()
    {
        final Matcher headerMatcher = HEADER_PATTERN.matcher(line);
        if (headerMatcher.matches())
        {
            clientId = Integer.parseInt(headerMatcher.group(1));
        }
        else
        {
            clientId = 1;
        }
    }
}
