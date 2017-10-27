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
package uk.co.real_logic.artio.acceptance_tests.steps;

import uk.co.real_logic.artio.acceptance_tests.Environment;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExpectDisconnectStep implements TestStep
{
    private static final Pattern DISCONNECT_PATTERN = Pattern.compile("e(\\d+)*,?DISCONNECT");
    private final String line;

    public ExpectDisconnectStep(final String line)
    {
        this.line = line;
    }

    public void run(final Environment environment) throws Exception
    {
        final Matcher matcher = DISCONNECT_PATTERN.matcher(line);
        final int clientId = getClientId(matcher, line);
        environment.expectDisconnect(clientId);
    }

    public String toString()
    {
        return line;
    }
}
