package uk.co.real_logic.fix_gateway.acceptance_tests.steps;

import uk.co.real_logic.fix_gateway.DebugLogger;
import uk.co.real_logic.fix_gateway.acceptance_tests.environments.Environment;

public class PrintCommentStep implements TestStep
{
    private final String line;

    public PrintCommentStep(final String line)
    {
        this.line = line;
    }

    public void run(final Environment environment)
    {
        DebugLogger.log(line);
    }

    public String toString()
    {
        return line;
    }
}
