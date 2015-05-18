package uk.co.real_logic.fix_gateway;

import uk.co.real_logic.agrona.LangUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
                        return new PrintComment(line);
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

    void run();
}
