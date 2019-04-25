package uk.co.real_logic.artio.integration_tests;

import org.agrona.ErrorHandler;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import uk.co.real_logic.artio.SessionRejectReason;
import uk.co.real_logic.artio.session.InternalSession;
import uk.co.real_logic.artio.session.Session;
import uk.co.real_logic.artio.session.SessionIdStrategy;
import uk.co.real_logic.artio.session.SessionParser;
import uk.co.real_logic.artio.validation.AuthenticationStrategy;
import uk.co.real_logic.artio.validation.MessageValidationStrategy;

import java.util.Arrays;
import java.util.Collection;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.co.real_logic.artio.SessionRejectReason.INVALID_TAG_NUMBER;
import static uk.co.real_logic.artio.SessionRejectReason.REQUIRED_TAG_MISSING;

/**
 * Tests message validation against quickfix message scenarios.
 */
@RunWith(Parameterized.class)
public class SessionMessageValidationTest
{

    private static final char[] HEARTBEAT = {'0'};
    private static final Object[][] TEST_CASES = {
        { "14a_BadField", "8=FIX.4.4^A35=0^A34=2^A49=TW^A52=<TIME>^A56=ISLD^A999=HI^A",
            2, 999, HEARTBEAT, '0', INVALID_TAG_NUMBER},

        { "14a_BadField", "8=FIX.4.4^A35=0^A34=3^A49=TW^A52=<TIME>^A56=ISLD^A0=HI^A",
            3, 0, HEARTBEAT, '0', INVALID_TAG_NUMBER},

        { "14a_BadField", "8=FIX.4.4^A35=0^A34=4^A49=TW^A52=<TIME>^A56=ISLD^A-1=HI^A",
            4, -1, HEARTBEAT, '0', INVALID_TAG_NUMBER},

        { "14a_BadField", "8=FIX.4.4^A35=0^A34=5^A49=TW^A52=<TIME>^A56=ISLD^A5000=HI^A",
            5, 5000, HEARTBEAT, '0', INVALID_TAG_NUMBER},

        { "14b_RequiredFieldMissing.def", "8=FIX.4.4^A9=0032^A35=0^A34=2^A49=TW^A52=<TIME>^A",
            2, 56, HEARTBEAT, '0', REQUIRED_TAG_MISSING},
    };

    private final String message;
    private final int refSeqNum;
    private final int refTagId;
    private final char[] refMsgType;
    private final int msgType;
    private final SessionRejectReason rejectReason;

    private InternalSession session = mock(InternalSession.class);
    private SessionIdStrategy sessionIdStrategy = mock(SessionIdStrategy.class);
    private AuthenticationStrategy authenticationStrategy = mock(AuthenticationStrategy.class);
    private MessageValidationStrategy validationStrategy = mock(MessageValidationStrategy.class);
    private ErrorHandler errorHandler = mock(ErrorHandler.class);
    private SessionParser parser = new SessionParser(
        session, validationStrategy, errorHandler);
    private UnsafeBuffer buffer = new UnsafeBuffer(new byte[16 * 1024]);

    @Parameterized.Parameters(name = "{0}: {1}")
    public static Collection<Object[]> data()
    {
        return Arrays.asList(TEST_CASES);
    }

    public SessionMessageValidationTest(
        final String description,
        final String message,
        final int refSeqNum,
        final int refTagId,
        final char[] refMsgType,
        final int msgType,
        final SessionRejectReason rejectReason
    )
    {
        this.message = message;
        this.refSeqNum = refSeqNum;
        this.refTagId = refTagId;
        this.refMsgType = refMsgType;
        this.msgType = msgType;
        this.rejectReason = rejectReason;
    }

    @Test
    public void shouldValidateMessage()
    {
        final String correctedMessage = MessageStringUtil.correct(message);
        final byte[] bytes = correctedMessage.getBytes(US_ASCII);
        buffer.putBytes(0, bytes);
        parser.onMessage(buffer, 0, bytes.length, msgType, 0L);

        verify(session).onInvalidMessage(
            refSeqNum,
            refTagId,
            refMsgType,
            refMsgType.length,
            rejectReason.representation()
        );
    }

}
