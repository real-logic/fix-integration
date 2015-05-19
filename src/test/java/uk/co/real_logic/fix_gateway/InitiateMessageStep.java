package uk.co.real_logic.fix_gateway;

import quickfix.FixVersions;
import quickfix.field.converter.UtcTimestampConverter;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;
import uk.co.real_logic.fix_gateway.session.InitiatorSession;

import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.US_ASCII;

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

    public void run(final Environment environment)
    {
        final String message = parseMessage();
        DebugLogger.log("sending to client " + clientId + ": " + message);
        final InitiatorSession session = environment.initiatorSession(clientId);
        send(session, message);
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
            message += "10=" + CHECKSUM_FORMAT.format(checksum(message)) + '\001';
        }
        return message;
    }

    private void send(final InitiatorSession session, final String message)
    {
        final int length = message.length();
        final UnsafeBuffer buffer = new UnsafeBuffer(new byte[length]);
        buffer.putBytes(0, message.getBytes(US_ASCII));
        session.send(buffer, 0, length, '0'); // TODO
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

    public static int checksum(final Charset charset, final String data, final boolean isEntireMessage)
    {
        int sum = 0;
        final byte[] bytes = data.getBytes(charset);
        int len = bytes.length;
        if (isEntireMessage && bytes[len - 8] == '\001' && bytes[len - 7] == '1'
            && bytes[len - 6] == '0' && bytes[len - 5] == '=')
        {
            len = len - 7;
        }
        for (int i = 0; i < len; i++)
        {
            sum += (bytes[i] & 0xFF);
        }
        return sum & 0xFF; // better than sum % 256 since it avoids overflow issues
    }

    public static int checksum(final String message)
    {
        return checksum(US_ASCII, message, true);
    }

}
