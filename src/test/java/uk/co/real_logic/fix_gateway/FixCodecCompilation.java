package uk.co.real_logic.fix_gateway;

import uk.co.real_logic.agrona.IoUtil;
import uk.co.real_logic.agrona.LangUtil;
import uk.co.real_logic.fix_gateway.builder.Decoder;
import uk.co.real_logic.fix_gateway.dictionary.CodecGenerationTool;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertTrue;
import static uk.co.real_logic.agrona.generation.CompilerUtil.compile;
import static uk.co.real_logic.fix_gateway.dictionary.generation.GenerationUtil.DECODER_PACKAGE;

public class FixCodecCompilation
{
    private static final String DEFINITION_PREFIX = "src/test/resources/";

    public static final String OUTPUT_PREFIX = "build/generated";
    public static final String FIX44_OUTPUT = OUTPUT_PREFIX + "/fix44";
    public static final String FIX42_OUTPUT = OUTPUT_PREFIX + "/fix42";

    public static void generateQuickFix44Dictionary()
    {
        generateDictionary("FIX44.xml", FIX44_OUTPUT);
    }

    public static void generateQuickFix42Dictionary()
    {
        generateDictionary("FIX42.xml", FIX42_OUTPUT);
    }

    public static void ensureDictionariesGenerated()
    {
        if (!exists(FIX42_OUTPUT))
        {
            generateQuickFix42Dictionary();
        }

        if (!exists(FIX44_OUTPUT))
        {
            generateQuickFix44Dictionary();
        }
    }

    private static boolean exists(final String dirPath)
    {
        final File dir = new File(dirPath);
        return dir.exists() && dir.isDirectory();
    }

    public static Decoder newOrderSingleDecoder(final String outputDirectory) throws Exception
    {
        final URLClassLoader classLoader = classLoader(outputDirectory);
        final Class<?> newOrderSingleClass = classLoader.loadClass(DECODER_PACKAGE + ".NewOrderSingleDecoder");
        return (Decoder) newOrderSingleClass.newInstance();
    }

    public static URLClassLoader classLoader(final String outputDirectory) throws MalformedURLException
    {
        return new URLClassLoader(new URL[] {new File(outputDirectory).toURI().toURL()});
    }

    public static StandardJavaFileManager generateDictionary(final String xmlFile, final String outputDirectory)
    {
        IoUtil.delete(new File(outputDirectory), true);
        final String[] args = {outputDirectory, DEFINITION_PREFIX + xmlFile};

        try
        {
            CodecGenerationTool.main(args);
            return compileAllClasses(outputDirectory);
        }
        catch (Exception e)
        {
            LangUtil.rethrowUnchecked(e);
            return null;
        }
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
            return Files.list(path).flatMap(FixCodecCompilation::listTree);
        }
        catch (IOException e)
        {
            LangUtil.rethrowUnchecked(e);
            return null;
        }
    }

}
