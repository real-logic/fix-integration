package uk.co.real_logic.fix_gateway.acceptance_tests.steps;

import org.junit.Assert;
import org.agrona.LangUtil;
import uk.co.real_logic.fix_gateway.DebugLogger;
import uk.co.real_logic.fix_gateway.acceptance_tests.Environment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;

import static java.util.stream.Collectors.toList;

public interface TestStep
{
    static List<TestStep> load(final Path path)
    {
        try
        {
            return Files
                .lines(path, StandardCharsets.ISO_8859_1)
                .filter(line -> line.length() > 0)
                .map(line ->
                {
                    if (line.matches("^[ \t]*#.*"))
                    {
                        return new PrintCommentStep(line);
                    }
                    else if (line.startsWith("I"))
                    {
                        return new InitiateMessageStep(line);
                    }
                    else if (line.startsWith("E"))
                    {
                        return new ExpectMessageStep(line);
                    }
                    else if (line.matches("^i\\d*,?CONNECT"))
                    {
                        return new ConnectToServerStep(line);
                    }
                    else if (line.matches("^iSET_SESSION.*"))
                    {
                        return new ConfigureSessionStep(line);
                    }
                    else if (line.matches("^e\\d*,?DISCONNECT"))
                    {
                        return new ExpectDisconnectStep(line);
                    }

                    DebugLogger.log("Unknown line: " + line);
                    return null;
                })
                .filter(line -> line != null)
                .collect(toList());
        }
        catch (IOException e)
        {
            LangUtil.rethrowUnchecked(e);
            return null;
        }
    }

    void run(final Environment environment) throws Exception;

    default void perform(final Environment environment)
    {
        try
        {
            run(environment);
        }
        catch (final Exception e)
        {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    default int getClientId(final Matcher matcher, final String line)
    {
        if (matcher.matches())
        {
            if (matcher.group(1) != null)
            {
                return Integer.parseInt(matcher.group(1));
            }
        }
        else
        {
            Assert.fail("incorrect command: " + line);
        }

        return 1;
    }


    default int getClientId(final Matcher headerMatcher)
    {
        if (headerMatcher.matches())
        {
            return Integer.parseInt(headerMatcher.group(1));
        }
        else
        {
            return 1;
        }
    }
}
