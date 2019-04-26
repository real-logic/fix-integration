/*
 * Copyright 2015 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.real_logic.artio.integration_tests;

import org.junit.Test;
import quickfix.DataDictionary;
import quickfix.Message;
import quickfix.field.*;
import quickfix.fix44.Logon;
import quickfix.fix44.TestRequest;
import uk.co.real_logic.artio.builder.Encoder;
import uk.co.real_logic.artio.builder.LogonEncoder;
import uk.co.real_logic.artio.builder.TestRequestEncoder;
import uk.co.real_logic.artio.fields.UtcTimestampEncoder;
import uk.co.real_logic.artio.util.MutableAsciiBuffer;

import static org.junit.Assert.assertEquals;

public class EncoderQuickFixIntegrationTest
{
    public static final String TEST_REQ_ID = "abc";

    private final MutableAsciiBuffer buffer = new MutableAsciiBuffer(new byte[16 * 1024]);

    @Test
    public void encodesTestRequest() throws Exception
    {
        final UtcTimestampEncoder timestampEncoder = new UtcTimestampEncoder();
        timestampEncoder.encode(0);

        final TestRequestEncoder encoder = new TestRequestEncoder()
            .testReqID(TEST_REQ_ID);

        encoder
            .header()
            .senderCompID("LEH_LZJ02")
            .targetCompID("CCG")
            .sendingTime(timestampEncoder.buffer());

        final TestRequest decoder = new TestRequest();
        encode(encoder, decoder);

        QuickFixMessageUtil.assertFieldEquals(TEST_REQ_ID, decoder, new TestReqID());
    }

    @Test
    public void encodesLogon() throws Exception
    {
        final UtcTimestampEncoder timestampEncoder = new UtcTimestampEncoder();

        final LogonEncoder encoder = new LogonEncoder()
            .heartBtInt(10)
            .encryptMethod(0);

        encoder
            .header()
            .senderCompID("LEH_LZJ02")
            .targetCompID("CCG")
            .msgSeqNum(1)
            .sendingTime(timestampEncoder.buffer(), timestampEncoder.encode(0));

        final Logon decoder = new Logon();
        encode(encoder, decoder);

        QuickFixMessageUtil.assertFieldEquals(0, decoder, new EncryptMethod());
        QuickFixMessageUtil.assertFieldEquals(10, decoder, new HeartBtInt());

        final Message.Header header = decoder.getHeader();
        QuickFixMessageUtil.assertFieldEquals("LEH_LZJ02", header, new SenderCompID());
        QuickFixMessageUtil.assertFieldEquals("CCG", header, new TargetCompID());
        QuickFixMessageUtil.assertFieldEquals(1, header, new MsgSeqNum());

        final SendingTime sendingTime = new SendingTime();
        header.getField(sendingTime);
        assertEquals(0, sendingTime.getValue().getTime());
    }

    private void encode(final Encoder encoder, final Message decoder) throws Exception
    {
        final long result = encoder.encode(buffer, 0);
        final int length = Encoder.length(result);
        final String message = buffer.getAscii(Encoder.offset(result), length);
        decoder.fromString(message, new DataDictionary("FIX44.xml"), true);
    }
}
