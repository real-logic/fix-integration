package uk.co.real_logic.artio.acceptance_tests.steps;

import uk.co.real_logic.artio.DebugLogger;
import uk.co.real_logic.artio.acceptance_tests.Environment;

import static uk.co.real_logic.artio.LogTag.FIX_TEST;

public class PrintCommentStep implements TestStep
{
    private final String line;

    public PrintCommentStep(final String line)
    {
        this.line = line;
    }

    public void run(final Environment environment)
    {
        DebugLogger.log(FIX_TEST, line);
    }

    public String toString()
    {
        return line;
    }
}
