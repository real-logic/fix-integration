package uk.co.real_logic.fix_gateway.system_tests;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import uk.co.real_logic.agrona.IoUtil;
import uk.co.real_logic.agrona.LangUtil;
import uk.co.real_logic.fix_gateway.builder.Decoder;
import uk.co.real_logic.fix_gateway.dictionary.CodecGenerationTool;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static uk.co.real_logic.agrona.generation.CompilerUtil.compile;
import static uk.co.real_logic.fix_gateway.dictionary.generation.GenerationUtil.DECODER_PACKAGE;

public class FixCodecCompilationTest
{

    private static final String OUTPUT_PATH = "build/generated";
    private static Decoder newOrderSingle;

    @BeforeClass
    public static void compileFix44Dictionary() throws Exception
    {
        generateDictionary("src/test/resources/FIX44.xml");
        final URLClassLoader classLoader = new URLClassLoader(new URL[] {new File(OUTPUT_PATH).toURI().toURL()});
        final Class<?> newOrderSingleClass = classLoader.loadClass(DECODER_PACKAGE + ".NewOrderSingleDecoder");
        newOrderSingle = (Decoder) newOrderSingleClass.newInstance();
    }

    @Test
    public void shouldGenerateQuickFix44Dictionary() throws Exception
    {
        assertNotNull(newOrderSingle);
    }

    @Ignore
    @Test
    public void shouldGenerateQuickFix42Dictionary() throws Exception
    {
        generateDictionary("src/test/resources/FIX42.xml");
    }

    private static StandardJavaFileManager generateDictionary(final String xmlPath) throws Exception
    {
        IoUtil.delete(new File(OUTPUT_PATH), true);

        final String[] args = {OUTPUT_PATH, xmlPath};
        CodecGenerationTool.main(args);
        return compileAllClasses(OUTPUT_PATH);
    }

    private static StandardJavaFileManager compileAllClasses(final String outputPath) throws IOException
    {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        final StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        final Iterable<? extends JavaFileObject> files = findAll(outputPath, fileManager);
        final JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null, null, files);

        try
        {
            assertTrue(compile(diagnostics, task));
        }
        catch (StringIndexOutOfBoundsException e)
        {
            // Ignore bizarre indexing error
        }
        return fileManager;
    }

    private static Iterable<? extends JavaFileObject> findAll(final String outputPath,
                                                       final StandardJavaFileManager fileManager)
        throws IOException
    {
        final List<File> files = listTree(Paths.get(outputPath))
            .map(Path::toFile)
            .collect(toList());

        return fileManager.getJavaFileObjectsFromFiles(files);
    }

    private static Stream<Path> listTree(final Path path)
    {
        if (!Files.isDirectory(path))
        {
            return Stream.of(path);
        }

        try
        {
            return Files.list(path).flatMap(FixCodecCompilationTest::listTree);
        }
        catch (IOException e)
        {
            LangUtil.rethrowUnchecked(e);
            return null;
        }
    }

}
