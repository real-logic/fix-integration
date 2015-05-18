package uk.co.real_logic.fix_gateway;


import org.junit.Assert;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConnectToServerStep implements TestStep
{
    private static final Pattern CONNECT_PATTERN = Pattern.compile("i(\\d+)*,?CONNECT");
    private final String line;
    private int clientId;

    public ConnectToServerStep(final String line)
    {
        this.line = line;
    }

    public void run()
    {
        final Matcher matcher = CONNECT_PATTERN.matcher(line);
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
            Assert.fail("incorrect connect command: " + line);
        }
    }
}
