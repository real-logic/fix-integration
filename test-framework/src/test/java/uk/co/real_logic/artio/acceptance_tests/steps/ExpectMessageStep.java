package uk.co.real_logic.artio.acceptance_tests.steps;

import org.junit.Assert;
import uk.co.real_logic.artio.DebugLogger;
import uk.co.real_logic.artio.acceptance_tests.Environment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.junit.Assert.*;
import static uk.co.real_logic.artio.LogTag.FIX_TEST;

public class ExpectMessageStep implements TestStep
{
    private static final String CHECKSUM = String.valueOf(10);
    private static final long TIMEOUT_IN_MS = 10000;
    private static final HashSet<String> IGNORE_FIELDS = new HashSet<>();
    private static final Pattern HEADER_PATTERN = Pattern.compile("^E(\\d+),.*");
    private static final Pattern FIELD_PATTERN = Pattern.compile("(\\d+)=([^\\001]+)\\001");

    static
    {
        IGNORE_FIELDS.add("52");
        IGNORE_FIELDS.add("60");
        IGNORE_FIELDS.add("122");
        IGNORE_FIELDS.add("9");
    }

    private final String line;

    public ExpectMessageStep(final String line)
    {
        this.line = line;
    }

    public void run(final Environment environment) throws Exception
    {
        final Matcher headerMatcher = HEADER_PATTERN.matcher(line);
        final int clientId = getClientId(headerMatcher);
        final Map<String, String> expected = parse(line);
        try
        {
            final CharSequence message = environment.readMessage(clientId, TIMEOUT_IN_MS);
            DebugLogger.log(FIX_TEST, "Expected: %s\nReceived: %s\n", expectedMessage(), message);
            assertNotNull("Missing message returned", message);
            final Map<String, String> actual = parse(message);

            // Check message type first
            assertFieldEqual(actual, "35", expected.get("35"));
            expected.forEach((key, expectedValue) -> assertFieldEqual(actual, key, expectedValue));
        }
        catch (final InterruptedException ex)
        {
            ex.printStackTrace();
            Assert.fail("Timed out whilst expecting: " + expectedMessage());
        }
    }

    private String expectedMessage()
    {
        return line.substring(1);
    }

    private void assertFieldEqual(final Map<String, String> actualMessage, final String key, final String expectedValue)
    {
        final String actualValue = actualMessage.get(key);
        if (actualValue == null)
        {
            Assert.fail("Missing field: " + key + " should be " + expectedValue);
        }

        try
        {
            // Allow leading zeros, etc. for numbers
            final int expectedNum = Integer.parseInt(expectedValue);
            final int actualNum = Integer.parseInt(actualValue);
            assertEquals("Different values for field " + key, expectedNum, actualNum);
        }
        catch (final NumberFormatException ignore)
        {
            // Just ignore this inconsistent suffix.
            final String valueToCheck = expectedValue.replace(", field=52", "");

            assertThat("Different values for field " + key + " within " + actualMessage,
                actualValue,
                anyOf(
                    equalToIgnoringCase(valueToCheck),
                    equalToIgnoringCase(expectedValue)));
        }
    }

    private Map<String, String> parse(final CharSequence data)
    {
        final HashMap<String, String> fields = new HashMap<>();
        final Matcher fieldMatcher = FIELD_PATTERN.matcher(data);
        while (fieldMatcher.find())
        {
            final String key = fieldMatcher.group(1);
            if (!IGNORE_FIELDS.contains(key) && !CHECKSUM.equals(key))
            {
                fields.put(key, fieldMatcher.group(2));
            }
        }

        return fields;
    }

    public String toString()
    {
        return line;
    }
}
