package uk.co.real_logic.fix_gateway.system_tests;

import org.junit.Test;
import uk.co.real_logic.fix_gateway.dictionary.CodecGenerationTool;

public class FixCompilationTest
{

    @Test
    public void shouldGenerateQuickFix44Dictionary() throws Exception
    {
        generateDictionary("src/test/resources/FIX44.xml");
    }

    @Test
    public void shouldGenerateQuickFix42Dictionary() throws Exception
    {
        generateDictionary("src/test/resources/FIX42.xml");
    }

    private void generateDictionary(final String xmlPath) throws Exception
    {
        final String outputPath = "build/generated";
        final String[] args = {outputPath, xmlPath};
        CodecGenerationTool.main(args);
    }

}
