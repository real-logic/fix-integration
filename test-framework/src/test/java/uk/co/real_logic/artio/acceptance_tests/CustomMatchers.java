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

import org.hamcrest.Matcher;

import static org.hamcrest.Matchers.*;
import static uk.co.real_logic.artio.system_tests.SystemTestUtil.ACCEPTOR_ID;
import static uk.co.real_logic.artio.system_tests.SystemTestUtil.INITIATOR_ID;

public final class CustomMatchers
{
    public static <T> Matcher<Iterable<? super T>> containsInitiator()
    {
        return containsLogon(ACCEPTOR_ID, INITIATOR_ID);
    }

    public static <T> Matcher<Iterable<? super T>> containsAcceptor()
    {
        return containsLogon(INITIATOR_ID, ACCEPTOR_ID);
    }

    private static <T> Matcher<Iterable<? super T>> containsLogon(final String senderCompId, final String targetCompId)
    {
        return hasItem(
            allOf(hasSenderCompId(senderCompId),
                hasTargetCompId(targetCompId)));
    }

    private static <T> Matcher<T> hasTargetCompId(final String targetCompId)
    {
        return hasProperty("targetCompID", equalTo(targetCompId));
    }

    private static <T> Matcher<T> hasSenderCompId(final String senderCompId)
    {
        return hasProperty("senderCompID", equalTo(senderCompId));
    }


}
