/*
 * Copyright 2015-2017 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.real_logic.artio.acceptance_tests;

import io.aeron.driver.MediaDriver;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import uk.co.real_logic.artio.DebugLogger;
import uk.co.real_logic.artio.TestFixtures;
import uk.co.real_logic.artio.acceptance_tests.steps.TestStep;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;
import static org.agrona.CloseHelper.quietClose;
import static uk.co.real_logic.artio.LogTag.FIX_TEST;
import static uk.co.real_logic.artio.TestFixtures.launchMediaDriver;

public abstract class AbstractFixSpecAcceptanceTest
{
    private static final String FIX_TEST_TIMEOUT_PROP = "fix.test.timeout";
    private static final int FIX_TEST_TIMEOUT_DEFAULT = 30_000;

    protected static final String QUICKFIX_DEFINITIONS =
        "../../quickfixj/quickfixj-core/src/test/resources/quickfix/test/acceptance/definitions/server";
    protected static final String CUSTOM_ROOT_PATH = "src/test/resources/custom_definitions";

    @Rule
    public Timeout timeout = Timeout.millis(Long.getLong(FIX_TEST_TIMEOUT_PROP, FIX_TEST_TIMEOUT_DEFAULT));

    protected static List<Object[]> testsFor(
        final String rootPath, final List<String> files, final Supplier<Environment> environment)
    {
        return files
            .stream()
            .map((file) -> Paths.get(rootPath, file))
            .map((path) -> new Object[]{ path, path.getFileName(), environment })
            .collect(toList());
    }

    private final List<TestStep> steps;
    private final Environment environment;
    private final MediaDriver mediaDriver;

    public AbstractFixSpecAcceptanceTest(
        final Path path, final Path filename, final Supplier<Environment> environment)
    {
        steps = TestStep.load(path);
        mediaDriver = launchMediaDriver();
        this.environment = environment.get();
    }

    @Test
    public void shouldPassAcceptanceCriteria() throws Exception
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
