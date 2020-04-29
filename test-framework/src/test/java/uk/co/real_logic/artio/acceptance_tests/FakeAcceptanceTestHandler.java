package uk.co.real_logic.artio.acceptance_tests;

import io.aeron.logbuffer.ControlledFragmentHandler.Action;
import org.agrona.DirectBuffer;
import uk.co.real_logic.artio.Pressure;
import uk.co.real_logic.artio.builder.Encoder;
import uk.co.real_logic.artio.library.OnMessageInfo;
import uk.co.real_logic.artio.session.Session;
import uk.co.real_logic.artio.system_tests.FakeHandler;
import uk.co.real_logic.artio.system_tests.FakeOtfAcceptor;
import uk.co.real_logic.artio.system_tests.FixMessage;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static io.aeron.logbuffer.ControlledFragmentHandler.Action.CONTINUE;

public class FakeAcceptanceTestHandler extends FakeHandler
{
    private static final String NEW_ORDER_SINGLE_TYPE = "D";
    private static final int MESSAGE_TYPE_TAG = 35;
    private static final int POSS_RESEND_TAG = 97;
    private static final int CL_ORD_ID_TAG = 11;

    private final NewOrderSingleCloner newOrderSingleCloner;
    private final FakeOtfAcceptor acceptor;
    private final Set<OrderIdPair> orderIdPairs = new HashSet<>();

    public FakeAcceptanceTestHandler(final NewOrderSingleCloner newOrderSingleCloner, final FakeOtfAcceptor acceptor)
    {
        super(acceptor);
        this.newOrderSingleCloner = newOrderSingleCloner;
        this.acceptor = acceptor;
    }

    public Action onMessage(
        final DirectBuffer buffer,
        final int offset,
        final int length,
        final int libraryId,
        final Session session,
        final int sequenceIndex,
        final long messageType,
        final long timestampInNs,
        final long position,
        final OnMessageInfo messageInfo)
    {
        final Action action = super.onMessage(
            buffer,
            offset,
            length,
            libraryId,
            session,
            sequenceIndex,
            messageType,
            timestampInNs,
            position,
            messageInfo);

        if (action == CONTINUE)
        {
            return onMessage(buffer, offset, length, session);
        }

        return action;
    }

    private Action onMessage(final DirectBuffer buffer, final int offset, final int length, final Session session)
    {
        final FixMessage fixMessage = acceptor.lastReceivedMessage();
        if (fixMessage != null)
        {
            if (NEW_ORDER_SINGLE_TYPE.equals(fixMessage.get(MESSAGE_TYPE_TAG)))
            {
                final boolean possResend = "Y".equalsIgnoreCase(fixMessage.get(POSS_RESEND_TAG));
                final String clOrdId = fixMessage.get(CL_ORD_ID_TAG);
                final OrderIdPair orderIdPair = new OrderIdPair(session.id(), clOrdId);

                if (possResend && orderIdPairs.contains(orderIdPair))
                {
                    return null;
                }

                orderIdPairs.add(orderIdPair);

                final Encoder encoder = newOrderSingleCloner.clone(buffer, offset, length);
                return Pressure.apply(session.trySend(encoder));
            }
        }
        else
        {
            System.err.printf("Invalid fix message for %S%n", buffer.getStringAscii(offset, length));
        }

        return CONTINUE;
    }

    static class OrderIdPair
    {
        private final long sessionId;
        private final String clOrdId;

        OrderIdPair(final long sessionId, final String clOrdId)
        {
            this.sessionId = sessionId;
            this.clOrdId = clOrdId;
        }

        public boolean equals(final Object o)
        {
            if (this == o)
            {
                return true;
            }

            if (o == null || getClass() != o.getClass())
            {
                return false;
            }

            final OrderIdPair that = (OrderIdPair)o;
            return sessionId == that.sessionId && Objects.equals(clOrdId, that.clOrdId);
        }

        public int hashCode()
        {
            return Objects.hash(sessionId, clOrdId);
        }
    }
}
