package uk.co.real_logic.fix_gateway;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import uk.co.real_logic.aeron.driver.MediaDriver;
import uk.co.real_logic.agrona.LangUtil;
import uk.co.real_logic.fix_gateway.environments.Environment;
import uk.co.real_logic.fix_gateway.environments.QuickFixToGatewayEnvironment;
import uk.co.real_logic.fix_gateway.steps.TestStep;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static uk.co.real_logic.fix_gateway.system_tests.SystemTestUtil.launchMediaDriver;

@RunWith(Parameterized.class)
public class FixSpecAcceptanceTest
{
    private static final String ROOT_PATH = "src/test/resources/quickfixj_definitions/fix42";

    private Path path;
    private List<TestStep> steps;
    private MediaDriver mediaDriver;

    @Parameterized.Parameters(name = "Acceptance: {1}")
    public static Collection<Object[]> data()
    {
        try
        {
            return Files
                .list(Paths.get(ROOT_PATH))
                .filter(path -> path.endsWith("1e_NotLogonMessage.def"))
                .filter(path -> false)
                .map(path -> new Object[]{path, path.getFileName()})
                .collect(toList());
        }
        catch (IOException e)
        {
            LangUtil.rethrowUnchecked(e);
            return null;
        }
    }

    public FixSpecAcceptanceTest(final Path path, final Path filename)
    {
        this.path = path;
        steps = TestStep.load(path);
        mediaDriver = launchMediaDriver();
    }

    @Test
    public void shouldPassAcceptanceCriteria() throws Exception
    {
        try (final Environment environment = new QuickFixToGatewayEnvironment())
        {
            steps.forEach(step -> step.perform(environment));
        }
    }

    @After
    public void shutdown()
    {
        if (mediaDriver != null)
        {
            mediaDriver.close();
        }
    }

}
