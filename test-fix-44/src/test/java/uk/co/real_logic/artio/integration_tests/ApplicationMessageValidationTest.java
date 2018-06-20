package uk.co.real_logic.artio.integration_tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import uk.co.real_logic.artio.SessionRejectReason;
import uk.co.real_logic.artio.builder.Decoder;
import uk.co.real_logic.artio.decoder.NewOrderSingleDecoder;
import uk.co.real_logic.artio.util.MutableAsciiBuffer;

import java.util.Arrays;
import java.util.Collection;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static uk.co.real_logic.artio.SessionRejectReason.*;

@RunWith(Parameterized.class)
public class ApplicationMessageValidationTest
{

    private static final Object[][] TEST_CASES = {
        {
            "14b_RequiredFieldMissing.def",
            "8=FIX.4.4^A9=65^A35=D^A49=TW^A34=3^A56=ISLD^A52=<TIME>^A40=1^A60=<TIME>^A54=1^A21=3^A11=id^A10=0^A",
            55,
            REQUIRED_TAG_MISSING
        },

        {
            "14e_IncorrectEnumValue.def",
            "8=FIX.4.4^A9=1^A35=D^A34=2^A49=TW^A52=<TIME>^A56=ISLD^A11=ID^A21=4^A40=1^A54=1^A" +
            "38=002000.00^A55=INTC^A60=<TIME>^A10=1^A",
            21,
            VALUE_IS_INCORRECT
        },

        {
            "14e_IncorrectEnumValue.def",
            "8=FIX.4.4^A9=1^A35=D^A34=3^A49=TW^A52=<TIME>^A56=ISLD^A11=ID^A21=1^A40=1^A54=1^A" +
            "38=002000.00^A55=INTC^A60=<TIME>^A167=BOO^A10=1^A",
            167,
            VALUE_IS_INCORRECT
        },

        /*{
            "14f_IncorrectDataFormat.def",
            "8=FIX.4.4^A9=1^A35=D^A34=2^A49=TW^A52=<TIME>^A56=ISLD^A11=ID^A21=1^A40=1^A54=1^A" +
            "38=+200.00^A55=INTC^A60=<TIME>^A10=1^A",
            38,
            SessionRejectReason.INCORRECT_DATA_FORMAT_FOR_VALUE
        }*/

        {
            "14h_RepeatedTag.def",
            "8=FIX.4.4^A9=1^A35=D^A34=2^A49=TW^A52=<TIME>^A56=ISLD^A11=ID^A21=1^A40=1^A54=1^A" +
            "40=2^A38=200.00^A55=INTC^A60=<TIME>^A10=1^A",
            40,
            TAG_APPEARS_MORE_THAN_ONCE
        }

    };

    @Parameterized.Parameters(name = "{0}: {1}")
    public static Collection<Object[]> data()
    {
        return Arrays.asList(TEST_CASES);
    }

    private static Decoder newOrderSingle = new NewOrderSingleDecoder();

    private final String message;
    private final int refTagId;
    private final SessionRejectReason rejectReason;
    private final MutableAsciiBuffer buffer = new MutableAsciiBuffer(new byte[16 * 1024]);

    public ApplicationMessageValidationTest(
        final String description,
        final String message,
        final int refTagId,
        final SessionRejectReason rejectReason
    )
    {
        this.message = message;
        this.refTagId = refTagId;
        this.rejectReason = rejectReason;
    }

    @Test
    public void shouldValidateMessage() throws Exception
    {
        newOrderSingle.reset();

        final String correctedMessage = MessageStringUtil.correct(message);
        final byte[] bytes = correctedMessage.getBytes(US_ASCII);
        buffer.putBytes(0, bytes);
        newOrderSingle.decode(buffer, 0, bytes.length);

        assertFalse("Invalid message was validated", newOrderSingle.validate());

        final SessionRejectReason reason = SessionRejectReason.decode(newOrderSingle.rejectReason());
        final int invalidTagId = newOrderSingle.invalidTagId();

        assertEquals("Rejected for the wrong reason, tag Id = " + invalidTagId, rejectReason, reason);
        assertEquals("Wrong tag id", refTagId, invalidTagId);
    }
}
