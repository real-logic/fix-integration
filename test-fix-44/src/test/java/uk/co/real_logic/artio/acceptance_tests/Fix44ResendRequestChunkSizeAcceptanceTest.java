package uk.co.real_logic.artio.acceptance_tests;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Supplier;

@RunWith(Parameterized.class)
public class Fix44ResendRequestChunkSizeAcceptanceTest extends AbstractFixSpecAcceptanceTest
{
    private static final String QUICKFIX_4_4_ROOT_PATH = QUICKFIX_RESEND_REQUEST_CHUNK_SIZE_DEFINITIONS + "/fix44";

    @Parameterized.Parameters(name = "Acceptance: {1}")
    public static Collection<Object[]> data()
    {
        return testsFor(
            QUICKFIX_4_4_ROOT_PATH, QUICKFIX_RESEND_CHUNK_WHITELIST, () -> Environment.fix44(
                new NewOrderSingleClonerImpl(), RESEND_REQUEST_CHUNK_SIZE));
    }

    public Fix44ResendRequestChunkSizeAcceptanceTest(
        final Path path, final Path filename, final Supplier<Environment> environment)
    {
        super(path, filename, environment);
    }

}
