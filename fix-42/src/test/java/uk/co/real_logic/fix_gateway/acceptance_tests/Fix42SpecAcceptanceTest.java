package uk.co.real_logic.fix_gateway.acceptance_tests;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import uk.co.real_logic.aeron.driver.MediaDriver;
import uk.co.real_logic.agrona.LangUtil;
import uk.co.real_logic.fix_gateway.DebugLogger;
import uk.co.real_logic.fix_gateway.acceptance_tests.steps.TestStep;
import uk.co.real_logic.fix_gateway.decoder.Constants;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;
import static uk.co.real_logic.agrona.CloseHelper.quietClose;
import static uk.co.real_logic.fix_gateway.TestFixtures.launchMediaDriver;

@Ignore
@RunWith(Parameterized.class)
public class Fix42SpecAcceptanceTest
{
    private static final String FIX_TEST_TIMEOUT_PROP = "fix.test.timeout";
    private static final int FIX_TEST_TIMEOUT_DEFAULT = 25_000;

    private static final String QUICKFIX_DEFINITIONS = "src/test/resources/quickfixj_definitions";
    private static final String QUICKFIX_4_2_ROOT_PATH = QUICKFIX_DEFINITIONS + "/fix42";
    private static final String CUSTOM_ROOT_PATH = "src/test/resources/custom_definitions/fix42";

    @Rule
    public Timeout timeout = Timeout.millis(Long.getLong(FIX_TEST_TIMEOUT_PROP, FIX_TEST_TIMEOUT_DEFAULT));

    static
    {
        // Fake additional field in order to correctly test validation.
        Constants.ALL_FIELDS.add(55);
    }

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

    // Low
    // "8_AdminAndApplicationMessages.def"
    // "8_OnlyAdminMessages.def"
    // "8_OnlyApplicationMessages.def"

    private static final List<String> QUICKFIX_WHITELIST = Arrays.asList(
        "1a_ValidLogonWithCorrectMsgSeqNum.def",
        "1b_DuplicateIdentity.def",
        "1c_InvalidTargetCompID.def",
        "1c_InvalidSenderCompID.def",
        "1d_InvalidLogonBadSendingTime.def",
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
        "1e_NotLogonMessage.def", // also has wrong target comp id
        // Edited logon at the end, sequence number looks invalid:
        "2b_MsgSeqNumTooHigh.def",
        "1a_ValidLogonMsgSeqNumTooHigh.def",
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
        try
        {
            final List<Object[]> tests = new ArrayList<>();
            tests.addAll(fix42Tests());
            tests.addAll(fix42CustomisedTests());
            return tests;
        }
        catch (Exception e)
        {
            LangUtil.rethrowUnchecked(e);
            return null;
        }
    }

    private static List<Object[]> fix42CustomisedTests()
    {
        return testsFor(CUSTOM_ROOT_PATH, CUSTOM_WHITELIST, Environment::fix44);
    }

    private static List<Object[]> fix42Tests()
    {
        return testsFor(QUICKFIX_4_2_ROOT_PATH, QUICKFIX_WHITELIST, Environment::fix42);
    }

    private static List<Object[]> testsFor(
        final String rootPath, final List<String> files, final Supplier<Environment> environment)
    {
        return files.stream()
                    .map(file -> Paths.get(rootPath, file))
                    .map(path -> new Object[]{path, path.getFileName(), environment})
                    .collect(toList());
    }

    private final List<TestStep> steps;
    private final Environment environment;
    private final MediaDriver mediaDriver;

    public Fix42SpecAcceptanceTest(
        final Path path, final Path filename, final Supplier<Environment> environment)
    {
        steps = TestStep.load(path);
        mediaDriver = launchMediaDriver();
        this.environment = environment.get();
    }

    @Test
    public void shouldPassAcceptanceCriteria() throws Exception
    {
        steps.forEach(step ->
        {
            DebugLogger.log("Starting %s at %s\n", step, LocalTime.now());
            step.perform(environment);
        });
    }

    @After
    public void shutdown()
    {
        quietClose(environment);
        quietClose(mediaDriver);
    }

}
