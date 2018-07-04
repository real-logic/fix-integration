package uk.co.real_logic.artio.acceptance_tests;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

@RunWith(Parameterized.class)
public class Fix44SpecAcceptanceTest extends AbstractFixSpecAcceptanceTest
{
    private static final String QUICKFIX_4_4_ROOT_PATH = QUICKFIX_DEFINITIONS + "/fix44";
    private static final String CUSTOM_4_4_ROOT_PATH = CUSTOM_ROOT_PATH + "/fix44";

    /**
     * banned acceptance tests - not part of the spec we're aiming to support
     */
    private static final Set<String> BLACKLIST = new HashSet<>(Arrays.asList(
        // TODO: ask for feedback on the following
        // ignore if garbled, should we allow this, or just disconnect?
        "2d_GarbledMessage.def",
        "3c_GarbledMessage.def",

        "2r_UnregisteredMsgType.def", // how do we validate/configure this?

        "14i_RepeatingGroupCountNotEqual.def", // Is this required?
        "14j_OutOfOrderRepeatingGroupMembers.def", // Is this required?

        // Permanent Blacklist:

        // These tests are all run as integration tests using validation
        "14b_RequiredFieldMissing.def", // reject messages with required field missing
        "14e_IncorrectEnumValue.def",
        "14f_IncorrectDataFormat.def",
        "14h_RepeatedTag.def",

        // Refer to New Order Single, thus business domain validation.
        "19a_PossResendMessageThatHAsAlreadyBeenSent.def",
        "19b_PossResendMessageThatHasNotBeenSent.def",
        // These tests make new order single behaviour assumptions, we have equivalent unit tests to these that don't
        "2f_PossDupOrigSendingTimeTooHigh.def",
        "2g_PossDupNoOrigSendingTime.def",

        // Customers have asked for the opposite
        "15_HeaderAndBodyFieldsOrderedDifferently.def",
        "14g_HeaderBodyTrailerFieldsOutOfOrder.def"
    ));

    // Medium:
    // "2m_BodyLengthValueNotCorrect.def" - length too short

    private static final List<String> QUICKFIX_WHITELIST = Arrays.asList(
        "1a_ValidLogonWithCorrectMsgSeqNum.def",
        "1b_DuplicateIdentity.def",
        "1c_InvalidTargetCompID.def",
        "1c_InvalidSenderCompID.def",
        // TODO: move this to a custom definition, for the modified error message
        // "1d_InvalidLogonBadSendingTime.def",
        "1d_InvalidLogonWrongBeginString.def",
        "1d_InvalidLogonLengthInvalid.def",
        "2a_MsgSeqNumCorrect.def",
        "2c_MsgSeqNumTooLow.def",
        "2e_PossDupAlreadyReceived.def",
        "2e_PossDupNotReceived.def",
        "2i_BeginStringValueUnexpected.def",
        "2k_CompIDDoesNotMatchProfile.def",
        "2o_SendingTimeValueOutOfRange.def",
        "4a_NoDataSentDuringHeartBtInt.def",
        "4b_ReceivedTestRequest.def",
        "7_ReceiveRejectMessage.def",
        //"8_AdminAndApplicationMessages.def",
        "8_OnlyAdminMessages.def",
        //"8_OnlyApplicationMessages.def",
        "10_MsgSeqNumEqual.def",
        "10_MsgSeqNumLess.def",
        "11c_NewSeqNoLess.def",
        "11a_NewSeqNoGreater.def",
        "11b_NewSeqNoEqual.def",
        "13b_UnsolicitedLogoutMessage.def",
        "14a_BadField.def", // reject messages with invalid field numbers
        "14c_TagNotDefinedForMsgType.def", // Tag not defined for this message type - add to set
        "14d_TagSpecifiedWithoutValue.def", // Tag specified without a value - needs a check, second set
        "QFJ648_NegativeHeartBtInt.def",
        "QFJ650_MissingMsgSeqNum.def"
    );

    private static final List<String> CUSTOM_WHITELIST = Arrays.asList(
        "1a_ValidLogonMsgSeqNumTooHigh.def",
        "1e_NotLogonMessage.def", // also has wrong target comp id
        // Edited logon at the end, sequence number looks invalid:
        "2b_MsgSeqNumTooHigh.def",

        "2q_MsgTypeNotValid.def",

        // Edited to make messages valid apart from first three fields
        "2t_FirstThreeFieldsOutOfOrder.def",

        "10_MsgSeqNumGreater.def",  // Added reply to test request that looks valid
        "6_SendTestRequest.def",    // Failing to pickup disconnect
        "3b_InvalidChecksum.def"    // Modified to account for resend request with no NewOrderSingle
    );

    @Parameterized.Parameters(name = "Acceptance: {1}")
    public static Collection<Object[]> data()
    {
        final List<Object[]> tests = new ArrayList<>();
        tests.addAll(fix44Tests());
        tests.addAll(fix44CustomisedTests());
        return tests;
    }

    private static List<Object[]> fix44CustomisedTests()
    {
        return testsFor(CUSTOM_4_4_ROOT_PATH, CUSTOM_WHITELIST, Environment::fix44);
    }

    private static List<Object[]> fix44Tests()
    {
        return testsFor(QUICKFIX_4_4_ROOT_PATH, QUICKFIX_WHITELIST, Environment::fix44);
    }

    public Fix44SpecAcceptanceTest(
        final Path path, final Path filename, final Supplier<Environment> environment)
    {
        super(path, filename, environment);
    }

}
