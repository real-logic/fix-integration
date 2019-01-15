package uk.co.real_logic.artio.system_tests;

import org.agrona.IoUtil;
import org.agrona.LangUtil;
import org.hamcrest.Matcher;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.TestRequest;
import uk.co.real_logic.artio.DebugLogger;

import java.io.File;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static quickfix.field.MsgType.TEST_REQUEST;
import static uk.co.real_logic.artio.LogTag.FIX_TEST;
import static uk.co.real_logic.artio.Timing.assertEventuallyTrue;
import static uk.co.real_logic.artio.system_tests.SystemTestUtil.ACCEPTOR_ID;
import static uk.co.real_logic.artio.system_tests.SystemTestUtil.INITIATOR_ID;

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
        final LogFactory logFactory = null; // new ScreenLogFactory(settings);
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
        final LogFactory logFactory = null; // new ScreenLogFactory(settings);
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
        assertEventuallyTrue("Failed to receive a logout", () -> acceptor.logouts().size() >= 1);
        final List<SessionID> logouts = acceptor.logouts();
        DebugLogger.log(FIX_TEST, "\nLogouts: %s\n", logouts);
        assertThat(logouts, sessionMatcher);
    }

    public static void sendTestRequestTo(final SessionID sessionID, final String testReqId)
    {
        final TestRequest message = new TestRequest(
            new TestReqID(testReqId));
        sendMessage(sessionID, message);
    }

    private static void sendMessage(final SessionID sessionID, final Message message)
    {
        try
        {
            final boolean hasSent = Session.sendToTarget(message, sessionID);
            assertTrue("Failed to send message to: ", hasSent);
        }
        catch (final SessionNotFound ex)
        {
            LangUtil.rethrowUnchecked(ex);
        }
    }

    public static SessionID onlySessionId(final SocketAcceptor socketAcceptor)
    {
        final List<SessionID> sessions = socketAcceptor.getSessions();
        assertThat(sessions, hasSize(1));
        return sessions.get(0);
    }

    public static void logout(final SocketAcceptor socketAcceptor)
    {
        final Session session = onlySession(socketAcceptor);
        session.logout("Its only a test!");
    }

    public static Session onlySession(final SocketAcceptor socketAcceptor)
    {
        return Session.lookupSession(onlySessionId(socketAcceptor));
    }
}
