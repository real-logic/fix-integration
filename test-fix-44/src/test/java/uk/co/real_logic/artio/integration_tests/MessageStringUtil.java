package uk.co.real_logic.artio.integration_tests;

import quickfix.field.converter.UtcTimestampConverter;

import java.util.Date;

public class MessageStringUtil
{

    private static final String CURRENT_TIMESTAMP = UtcTimestampConverter.convert(new Date(), true);

    public static String correct(final String message)
    {
        return message.replace("^A", "\001").replace("<TIME>", CURRENT_TIMESTAMP);
    }

}
