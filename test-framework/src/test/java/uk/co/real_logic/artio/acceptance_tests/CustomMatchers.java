package uk.co.real_logic.artio.acceptance_tests;

import org.hamcrest.Matcher;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
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

    public static void assertCharsEquals(final String expectedValue, final char[] chars, final int length)
    {
        assertEquals("length wasn't equal", expectedValue.length(), length);
        final String value = new String(chars, 0, length);
        assertEquals(expectedValue, value);
    }


}
