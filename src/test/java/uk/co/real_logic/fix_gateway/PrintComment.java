package uk.co.real_logic.fix_gateway;

public class PrintComment implements TestStep
{
    private final String line;

    public PrintComment(final String line)
    {
        this.line = line;
    }

    public void run(final Environment environment)
    {
        DebugLogger.log(line);
    }
}
