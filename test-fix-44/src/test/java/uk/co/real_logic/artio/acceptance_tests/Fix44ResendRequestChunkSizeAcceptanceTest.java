package uk.co.real_logic.artio.acceptance_tests;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Supplier;

@RunWith(Parameterized.class)
public class Fix44ResendRequestChunkSizeAcceptanceTest extends AbstractFixSpecAcceptanceTest
{
    @Parameterized.Parameters(name = "Acceptance: {1}")
    public static Collection<Object[]> data()
    {
        return testsFor(
            CUSTOM_ROOT_PATH + "/fix44",
            QUICKFIX_RESEND_CHUNK_INCLUDE_LIST,
            () -> Environment.fix44(new NewOrderSingleClonerImpl(), RESEND_REQUEST_CHUNK_SIZE));
    }

    public Fix44ResendRequestChunkSizeAcceptanceTest(
        final Path path, final Path filename, final Supplier<Environment> environment)
    {
        super(path, filename, environment);
    }
}
