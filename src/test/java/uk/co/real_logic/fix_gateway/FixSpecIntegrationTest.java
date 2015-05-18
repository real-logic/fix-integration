package uk.co.real_logic.fix_gateway;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import uk.co.real_logic.agrona.LangUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;

@RunWith(Parameterized.class)
public class FixSpecIntegrationTest
{
    private static final String ROOT_PATH = "src/test/resources/quickfixj_definitions/fix42";
    private final Path path;
    private final List<TestStep> steps;

    @Parameterized.Parameters(name = "Acceptance: {1}")
    public static Collection<Object[]> data()
    {
        try
        {
            return Files
                .list(Paths.get(ROOT_PATH))
                .map(path -> new Object[]{path, path.getFileName()})
                .collect(toList());
        }
        catch (IOException e)
        {
            LangUtil.rethrowUnchecked(e);
            return null;
        }
    }

    public FixSpecIntegrationTest(final Path path, final Path filename)
    {
        this.path = path;
        steps = TestStep.load(path);
    }

    @Test
    public void shouldPassAcceptanceCriteria()
    {
        steps.forEach(TestStep::run);
    }

    @After
    public void shutdown()
    {

    }

}
