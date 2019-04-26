/*******************************************************************************
 * Copyright (c) quickfixengine.org  All rights reserved.
 *
 * This file is part of the QuickFIX FIX Engine
 *
 * This file may be distributed under the terms of the quickfixengine.org
 * license as defined by quickfixengine.org and appearing in the file
 * LICENSE included in the packaging of this file.
 *
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING
 * THE WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE.
 *
 * See http://www.quickfixengine.org/LICENSE for licensing information.
 *
 * Contact ask@quickfixengine.org if any conditions of this licensing
 * are not clear to you.
 ******************************************************************************/

package uk.co.real_logic.artio.acceptance_tests.quickfix;

import quickfix.*;
import quickfix.field.ClOrdID;
import quickfix.field.PossResend;

import java.util.HashSet;

class AcceptanceTestMessageCracker extends quickfix.MessageCracker
{
    private final HashSet<Pair> orderIDs = new HashSet<Pair>();

    public void reset()
    {
        orderIDs.clear();
    }

    public void process(final Message message, final SessionID sessionID) throws FieldNotFound
    {
        final quickfix.Message echo = (quickfix.Message)message.clone();
        final PossResend possResend = new PossResend(false);
        if (message.getHeader().isSetField(possResend))
        {
            message.getHeader().getField(possResend);
        }

        final ClOrdID clOrdID = new ClOrdID();
        message.getField(clOrdID);

        final Pair pair = new Pair(clOrdID, sessionID);

        if (possResend.getValue() && orderIDs.contains(pair))
        {
            return;
        }

        orderIDs.add(pair);
        try
        {
            Session.sendToTarget(echo, sessionID);
        }
        catch (final SessionNotFound snf)
        {
        }
    }

    public void onMessage(final quickfix.fix44.NewOrderSingle message, final SessionID sessionID)
        throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue
    {
        process(message, sessionID);
    }

    public void onMessage(final quickfix.fix44.SecurityDefinition message, final SessionID sessionID)
        throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue
    {
        try
        {
            Session.sendToTarget(message, sessionID);
        }
        catch (final SessionNotFound snf)
        {
            snf.printStackTrace();
        }
    }

    public void onMessage(final quickfix.fix42.NewOrderSingle message, final SessionID sessionID)
        throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue
    {
        process(message, sessionID);
    }

    public void onMessage(final quickfix.fix42.SecurityDefinition message, final SessionID sessionID)
        throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue
    {
        try
        {
            Session.sendToTarget(message, sessionID);
        }
        catch (final SessionNotFound snf)
        {
            snf.printStackTrace();
        }
    }

    static class Pair
    {
        private final ClOrdID clOrdID;
        private final SessionID sessionID;
        private final int hashCode;

        Pair(final ClOrdID clOrdID, final SessionID sessionID)
        {
            this.clOrdID = clOrdID;
            this.sessionID = sessionID;
            hashCode = ("C:" + clOrdID.toString() + "S:" + sessionID.toString()).hashCode();
        }

        public boolean equals(final Object object)
        {
            if (object == null)
            {
                return false;
            }
            if (!(object instanceof Pair))
            {
                return false;
            }
            final Pair pair = (Pair)object;

            return clOrdID.equals(pair.clOrdID) && sessionID.equals(pair.sessionID);
        }

        public int hashCode()
        {
            return hashCode;
        }
    }
}
