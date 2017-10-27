package uk.co.real_logic.fix_gateway.acceptance_tests.steps;

import quickfix.FixVersions;
import quickfix.MessageUtils;
import quickfix.field.converter.UtcTimestampConverter;
import uk.co.real_logic.fix_gateway.DebugLogger;
import uk.co.real_logic.fix_gateway.acceptance_tests.Environment;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static uk.co.real_logic.fix_gateway.LogTag.FIX_TEST;

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
            if (messageStructureMatcher.group(1) != null
                && !messageStructureMatcher.group(1).equals(""))
            {
                clientId = Integer.parseInt(messageStructureMatcher.group(1).replaceAll(",", ""));
            }
            else
            {
                clientId = 1;
            }
            final String version = messageStructureMatcher.group(2);
            String messageTail = insertTimes(messageStructureMatcher.group(3));
            messageTail = modifyHeartbeat(messageTail);
            String checksum = messageStructureMatcher.group(4);
            if ("10=0\001".equals(checksum))
            {
                checksum = "10=000\001";
            }
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

    private String insertTimes(String message)
    {
        Matcher matcher = TIME_PATTERN.matcher(message);
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
            message = matcher.replaceFirst(UtcTimestampConverter.convert(new Date(System
                .currentTimeMillis() + offset), includeMillis));
            matcher = TIME_PATTERN.matcher(message);
        }
        return message;
    }

    private String modifyHeartbeat(String messageTail)
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
