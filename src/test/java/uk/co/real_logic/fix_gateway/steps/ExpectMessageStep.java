package uk.co.real_logic.fix_gateway.steps;

import org.junit.Assert;
import uk.co.real_logic.fix_gateway.DebugLogger;
import uk.co.real_logic.fix_gateway.decoder.Constants;
import uk.co.real_logic.fix_gateway.environments.Environment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ExpectMessageStep implements TestStep
{
    private static final String CHECKSUM = String.valueOf(Constants.CHECK_SUM);
    private static final long TIMEOUT_IN_MS = 10000;
    private static final HashSet<String> TIME_FIELDS = new HashSet<String>();
    private static final Pattern HEADER_PATTERN = Pattern.compile("^E(\\d+),.*");
    private static final Pattern FIELD_PATTERN = Pattern.compile("(\\d+)=([^\\001]+)\\001");

    static
    {
        TIME_FIELDS.add("52");
        TIME_FIELDS.add("60");
        TIME_FIELDS.add("122");
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
        final CharSequence message = environment.readMessage(clientId, TIMEOUT_IN_MS);
        DebugLogger.log("Expected: %s\nReceived: %s\n", line.substring(1), message);
        assertNotNull("Missing message returned", message);
        final Map<String, String> actual = parse(message);
        expected.forEach((key, expectedValue) ->
        {
            final String actualValue = actual.get(key);
            if (actualValue == null)
            {
                Assert.fail("Missing field: " + key);
            }

            try
            {
                // Allow leading zeros, etc. for numbers
                final int expectedNum = Integer.parseInt(expectedValue);
                final int actualNum = Integer.parseInt(actualValue);
                assertEquals("Different values for field " + key, expectedNum, actualNum);
            }
            catch (final NumberFormatException e)
            {
                assertEquals("Different values for field " + key, expectedValue, actualValue);
            }
        });
    }

    private Map<String, String> parse(final CharSequence data)
    {
        final HashMap<String, String> fields = new HashMap<String, String>();
        final Matcher fieldMatcher = FIELD_PATTERN.matcher(data);
        while (fieldMatcher.find())
        {
            final String key = fieldMatcher.group(1);
            if (!TIME_FIELDS.contains(key) && !CHECKSUM.equals(key))
            {
                fields.put(key, fieldMatcher.group(2));
            }
        }
        return fields;
    }
}
