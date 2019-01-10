package uk.co.real_logic.artio.acceptance_tests;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

@RunWith(Parameterized.class)
public class Fix42ResendRequestChunkSizeAcceptanceTest extends AbstractFixSpecAcceptanceTest
{
    private static final String QUICKFIX_4_2_ROOT_PATH = QUICKFIX_RESEND_REQUEST_CHUNK_SIZE_DEFINITIONS + "/fix42";
    private static final int RESEND_REQUEST_CHUNK_SIZE = 5;

    private static final List<String> QUICKFIX_WHITELIST = Arrays.asList(
        "SequenceGapFollowedByMessageResent.def" //,
        //"SequenceGapFollowedBySequenceResetWithGapFill.def"
    );

    @Parameterized.Parameters(name = "Acceptance: {1}")
    public static Collection<Object[]> data()
    {
        return testsFor(
            QUICKFIX_4_2_ROOT_PATH, QUICKFIX_WHITELIST, () -> Environment.fix42(RESEND_REQUEST_CHUNK_SIZE,
                new NewOrderSingleClonerImpl()));
    }

    public Fix42ResendRequestChunkSizeAcceptanceTest(
        final Path path, final Path filename, final Supplier<Environment> environment)
    {
        super(path, filename, environment);
    }

}
