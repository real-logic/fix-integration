package uk.co.real_logic.artio.acceptance_tests;

import io.aeron.archive.ArchivingMediaDriver;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import uk.co.real_logic.artio.DebugLogger;
import uk.co.real_logic.artio.TestFixtures;
import uk.co.real_logic.artio.acceptance_tests.steps.TestStep;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;
import static org.agrona.CloseHelper.quietClose;
import static uk.co.real_logic.artio.LogTag.FIX_TEST;
import static uk.co.real_logic.artio.TestFixtures.launchMediaDriver;

public abstract class AbstractFixSpecAcceptanceTest
{
    static
    {
        // Ensure all the validation is switched on so these tests run consistently in an IDE
        System.setProperty("fix.codecs.reject_unknown_field", "true");
    }

    private static final String FIX_TEST_TIMEOUT_PROP = "fix.test.timeout";
    private static final int FIX_TEST_TIMEOUT_DEFAULT = 30_000;

    protected static final String QUICKFIX_DEFINITIONS =
        "../../quickfixj/quickfixj-core/src/test/resources/quickfix/test/acceptance/definitions";
    protected static final String QUICKFIX_SERVER_DEFINITIONS = QUICKFIX_DEFINITIONS + "/server";
    protected static final String QUICKFIX_RESEND_REQUEST_CHUNK_SIZE_DEFINITIONS =
        QUICKFIX_DEFINITIONS + "/resendRequestChunkSize";
    protected static final String CUSTOM_ROOT_PATH = "src/test/resources/custom_definitions";

    protected static final int RESEND_REQUEST_CHUNK_SIZE = 5;
    protected static final List<String> QUICKFIX_RESEND_CHUNK_WHITELIST = Arrays.asList(
        "SequenceGapFollowedBySequenceResetWithGapFill.def",
        "SequenceGapFollowedByMessageResent.def"
    );

    @Rule
    public Timeout timeout = Timeout.millis(Long.getLong(FIX_TEST_TIMEOUT_PROP, FIX_TEST_TIMEOUT_DEFAULT));

    protected static List<Object[]> testsFor(
        final String rootDirPath, final List<String> files, final Supplier<Environment> environment)
    {
        final String rootDir = getCorrectDirectory(rootDirPath);

        return files
            .stream()
            .map((file) -> Paths.get(rootDir, file))
            .map((path) -> new Object[]{ path, path.getFileName(), environment })
            .collect(toList());
    }

    private static String getCorrectDirectory(final String rootDirPath)
    {
        final File rootDirFile = new File(rootDirPath);
        if (rootDirFile.exists())
        {
            return rootDirPath;
        }

        // Remove ../ if run from the checkout directory
        return rootDirPath.substring(3);
    }

    private final List<TestStep> steps;
    private final Environment environment;
    private final ArchivingMediaDriver mediaDriver;

    public AbstractFixSpecAcceptanceTest(
        final Path path, final Path filename, final Supplier<Environment> environment)
    {
        steps = TestStep.load(path);
        mediaDriver = launchMediaDriver();
        this.environment = environment.get();
    }

    @Test
    public void shouldPassAcceptanceCriteria()
    {
        steps.forEach(
            (step) ->
            {
                DebugLogger.log(FIX_TEST, "Starting %s at %s\n", step, LocalTime.now());
                step.perform(environment);

                sleep();
            });
    }

    private void sleep()
    {
        try
        {
            Thread.sleep(10);
        }
        catch (final InterruptedException ex)
        {
            ex.printStackTrace();
        }
    }

    @After
    public void shutdown()
    {
        quietClose(environment);
        TestFixtures.cleanupMediaDriver(mediaDriver);
    }
}
