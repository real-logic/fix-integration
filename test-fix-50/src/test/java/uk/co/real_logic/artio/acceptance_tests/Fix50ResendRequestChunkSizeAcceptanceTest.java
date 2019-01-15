package uk.co.real_logic.artio.acceptance_tests;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Supplier;

@RunWith(Parameterized.class)
public class Fix50ResendRequestChunkSizeAcceptanceTest extends AbstractFixSpecAcceptanceTest
{
    private static final String QUICKFIX_5_0_ROOT_PATH = QUICKFIX_RESEND_REQUEST_CHUNK_SIZE_DEFINITIONS + "/fix50";

    @Parameterized.Parameters(name = "Acceptance: {1}")
    public static Collection<Object[]> data()
    {
        return testsFor(
            QUICKFIX_5_0_ROOT_PATH, QUICKFIX_RESEND_CHUNK_WHITELIST, () -> Environment.fix50(
                new Fix50SessionCustomizationStrategy(),
                new NewOrderSingleClonerImpl(),
                RESEND_REQUEST_CHUNK_SIZE));
    }

    public Fix50ResendRequestChunkSizeAcceptanceTest(
        final Path path, final Path filename, final Supplier<Environment> environment)
    {
        super(path, filename, environment);
    }

}
