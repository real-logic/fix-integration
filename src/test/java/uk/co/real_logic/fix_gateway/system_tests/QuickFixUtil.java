package uk.co.real_logic.fix_gateway.system_tests;

import org.hamcrest.Matcher;
import quickfix.*;
import quickfix.field.BeginString;
import quickfix.field.MsgType;
import quickfix.field.SenderCompID;
import quickfix.field.TargetCompID;
import uk.co.real_logic.agrona.IoUtil;
import uk.co.real_logic.fix_gateway.DebugLogger;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertThat;
import static quickfix.field.MsgType.TEST_REQUEST;
import static uk.co.real_logic.fix_gateway.Timing.assertEventuallyEquals;
import static uk.co.real_logic.fix_gateway.Timing.assertEventuallyTrue;
import static uk.co.real_logic.fix_gateway.system_tests.SystemTestUtil.ACCEPTOR_ID;
import static uk.co.real_logic.fix_gateway.system_tests.SystemTestUtil.INITIATOR_ID;

public final class QuickFixUtil
{

    public static SocketAcceptor launchQuickFixAcceptor(final int port, final FakeQuickFixApplication application)
        throws ConfigError
    {
        final SessionID sessionID = sessionID(ACCEPTOR_ID, INITIATOR_ID);
        final SessionSettings settings = sessionSettings(sessionID);

        settings.setString("SocketAcceptPort", String.valueOf(port));
        settings.setString(sessionID, "ConnectionType", "acceptor");

        final FileStoreFactory storeFactory = new FileStoreFactory(settings);
        final LogFactory logFactory = new ScreenLogFactory(settings);
        final SocketAcceptor socketAcceptor = new SocketAcceptor(
            application, storeFactory, settings, logFactory, new DefaultMessageFactory());
        socketAcceptor.start();

        return socketAcceptor;
    }

    public static SocketInitiator launchQuickFixInitiator(
        final int port, final FakeQuickFixApplication application) throws ConfigError
    {
        final SessionID sessionID = sessionID(INITIATOR_ID, ACCEPTOR_ID);
        final SessionSettings settings = sessionSettings(sessionID);

        settings.setString("HeartBtInt", "30");

        settings.setString(sessionID, "ConnectionType", "initiator");
        settings.setString(sessionID, "SocketConnectPort", String.valueOf(port));
        settings.setString(sessionID, "SocketConnectHost", "localhost");

        final FileStoreFactory storeFactory = new FileStoreFactory(settings);
        final LogFactory logFactory = new ScreenLogFactory(settings);
        final SocketInitiator socketInitiator = new SocketInitiator(
            application, storeFactory, settings, logFactory, new DefaultMessageFactory());
        socketInitiator.start();
        return socketInitiator;
    }

    private static SessionSettings sessionSettings(final SessionID sessionID)
    {
        final SessionSettings settings = new SessionSettings();
        final String path = "build/tmp/quickfix";
        IoUtil.delete(new File(path), true);
        settings.setString("FileStorePath", path);
        settings.setString("DataDictionary", "FIX44.xml");
        settings.setString("BeginString", "FIX.4.4");
        settings.setString(sessionID, "StartTime", "00:00:00");
        settings.setString(sessionID, "EndTime", "00:00:00");
        return settings;
    }

    private static SessionID sessionID(final String senderCompId, final String targetCompId)
    {
        return new SessionID(
            new BeginString("FIX.4.4"),
            new SenderCompID(senderCompId),
            new TargetCompID(targetCompId)
        );
    }

    public static void assertQuickFixReceivedMessage(final FakeQuickFixApplication acceptor)
    {
        assertEventuallyTrue("Unable to fnd test request", () ->
        {
            final List<Message> messages = acceptor.messages();
            for (final Message message : messages)
            {
                if (TEST_REQUEST.equals(getMsgType(message)))
                {
                    return true;
                }
            }

            return false;
        });
    }

    private static String getMsgType(final Message message)
    {
        try
        {
            return message.getHeader().getField(new MsgType()).getValue();
        }
        catch (final FieldNotFound ex)
        {
            ex.printStackTrace();
            return null;
        }
    }

    public static void assertQuickFixDisconnected(
        final FakeQuickFixApplication acceptor,
        final Matcher<Iterable<? super SessionID>> sessionMatcher)
    {
        assertEventuallyEquals("Failed to receive a logout", 1, () -> acceptor.logouts().size());
        final List<SessionID> logouts = acceptor.logouts();
        DebugLogger.log("\nLogouts: %s\n", logouts);
        assertThat(logouts, sessionMatcher);
    }

}
