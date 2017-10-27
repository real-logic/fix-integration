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

import quickfix.FixVersions;
import quickfix.MessageUtils;
import quickfix.field.converter.UtcTimestampConverter;
import uk.co.real_logic.artio.DebugLogger;
import uk.co.real_logic.artio.acceptance_tests.Environment;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static uk.co.real_logic.artio.LogTag.FIX_TEST;

public class InitiateMessageStep implements TestStep
{
    // Matches FIX.X.X or FIXT.X.X style begin string
    private static final Pattern MESSAGE_PATTERN = Pattern.compile(
        "I(\\d,)*(8=FIXT?\\.\\d\\.\\d\\001)(.*?\\001)(10=.*|)$");

    private static final Pattern TIME_PATTERN = Pattern.compile("<TIME([+-](\\d+))*>");

    private static final Pattern HEARTBEAT_PATTERN = Pattern.compile("108=\\d+\001");

    private static final DecimalFormat CHECKSUM_FORMAT = new DecimalFormat("000");

    private static final int HEART_BEAT_OVERRIDE;

    static
    {
        final String hbi = System.getProperty("atest.heartbeat");
        HEART_BEAT_OVERRIDE = hbi != null ? Integer.parseInt(hbi) : -1;
    }

    private final String line;
    private int clientId;

    public InitiateMessageStep(final String line)
    {
        this.line = line;
    }

    public void run(final Environment environment) throws Exception
    {
        final String message = parseMessage();
        DebugLogger.log(FIX_TEST, "sending to client " + clientId + ": " + message);
        environment.initiateMessage(clientId, message);
    }

    private String parseMessage()
    {
        final Matcher messageStructureMatcher = MESSAGE_PATTERN.matcher(line);
        String message;
        if (messageStructureMatcher.matches())
        {
            if (messageStructureMatcher.group(1) != null && !messageStructureMatcher.group(1).equals(""))
            {
                clientId = Integer.parseInt(messageStructureMatcher.group(1).replaceAll(",", ""));
            }
            else
            {
                clientId = 1;
            }

            final String version = messageStructureMatcher.group(2);
            final String messageTail = modifyHeartbeat(insertTimes(messageStructureMatcher.group(3)));

            final String matchedChecksum = messageStructureMatcher.group(4);
            final String checksum = "10=0\001".equals(matchedChecksum) ? "10=000\001" : matchedChecksum;

            message = version +
                (!messageTail.startsWith("9=") ? "9=" + messageTail.length() + "\001" : "") +
                messageTail + checksum;
        }
        else
        {
            //log.info("garbled message being sent");
            clientId = 1;
            message = line.substring(1);
        }

        if (!message.contains("\00110="))
        {
            message += "10=" + CHECKSUM_FORMAT.format(MessageUtils.checksum(message)) + '\001';
        }

        return message;
    }

    private String insertTimes(final String message)
    {
        Matcher matcher = TIME_PATTERN.matcher(message);
        String replacedString = message;

        while (matcher.find())
        {
            long offset = 0;
            if (matcher.group(2) != null)
            {
                offset = Long.parseLong(matcher.group(2)) * 1100L;
                if (matcher.group(1).startsWith("-"))
                {
                    offset *= -1;
                }
            }

            final String beginString = message.substring(2, 9);
            final boolean includeMillis = beginString.compareTo(FixVersions.BEGINSTRING_FIX42) >= 0;

            replacedString = matcher.replaceFirst(
                UtcTimestampConverter.convert(new Date(System.currentTimeMillis() + offset), includeMillis));

            matcher = TIME_PATTERN.matcher(replacedString);
        }

        return replacedString;
    }

    private String modifyHeartbeat(final String messageTail)
    {
        if (HEART_BEAT_OVERRIDE > 0 && messageTail.contains("35=A\001"))
        {
            final Matcher matcher = HEARTBEAT_PATTERN.matcher(messageTail);
            if (matcher.find())
            {
                return matcher.replaceFirst("108=" + HEART_BEAT_OVERRIDE + "\001");
            }
        }

        return messageTail;
    }

    public String toString()
    {
        return line;
    }
}
