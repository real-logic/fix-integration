package uk.co.real_logic.fix_gateway;

import org.junit.Assert;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExpectDisconnectStep implements TestStep
{
    private static final Pattern DISCONNECT_PATTERN = Pattern.compile("e(\\d+)*,?DISCONNECT");
    private final String line;
    private int clientId = 0;

    public ExpectDisconnectStep(final String line)
    {
        this.line = line;
    }

    public void run()
    {
        final Matcher matcher = DISCONNECT_PATTERN.matcher(line);
        if (matcher.lookingAt())
        {
            if (matcher.group(1) != null)
            {
                clientId = Integer.parseInt(matcher.group(1));
            }
            else
            {
                clientId = 1;
            }
        }
        else
        {
            Assert.fail("incorrect disconnect command: " + line);
        }
    }
}
