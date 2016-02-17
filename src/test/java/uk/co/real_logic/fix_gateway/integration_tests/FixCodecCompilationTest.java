package uk.co.real_logic.fix_gateway.integration_tests;

import org.junit.Test;
import uk.co.real_logic.fix_gateway.FixCodecCompilation;

public class FixCodecCompilationTest
{

    @Test
    public void shouldGenerateQuickFix44Dictionary()
    {
        FixCodecCompilation.generateQuickFix44Dictionary();
    }

    @Test
    public void shouldGenerateQuickFix42Dictionary() throws Exception
    {
        FixCodecCompilation.generateQuickFix42Dictionary();
    }

}
