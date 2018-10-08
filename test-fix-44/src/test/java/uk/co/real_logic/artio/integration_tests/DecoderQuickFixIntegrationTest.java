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
import uk.co.real_logic.artio.DebugLogger;
import uk.co.real_logic.artio.builder.Decoder;
import uk.co.real_logic.artio.decoder.HeaderDecoder;
import uk.co.real_logic.artio.decoder.LogonDecoder;
import uk.co.real_logic.artio.decoder.TestRequestDecoder;
import uk.co.real_logic.artio.fields.UtcTimestampDecoder;
import uk.co.real_logic.artio.util.MutableAsciiBuffer;

import static org.junit.Assert.assertEquals;
import static uk.co.real_logic.artio.LogTag.FIX_TEST;
import static uk.co.real_logic.artio.acceptance_tests.CustomMatchers.assertCharsEquals;

public class DecoderQuickFixIntegrationTest
{
    private final MutableAsciiBuffer buffer = new MutableAsciiBuffer(new byte[16 * 1024]);

    @Test
    public void decodesTestRequest()
    {
        final TestRequestDecoder decoder = new TestRequestDecoder();
        decode(QuickFixMessageUtil.testRequest(), decoder);

        assertCharsEquals("abc", decoder.testReqID(), decoder.testReqIDLength());
    }

    @Test
    public void decodesLogon()
    {
        final LogonDecoder decoder = new LogonDecoder();
        decode(QuickFixMessageUtil.logon(), decoder);

        DebugLogger.log(FIX_TEST, "Decoder: %s\n", decoder);

        assertEquals(0, decoder.encryptMethod());
        assertEquals(10, decoder.heartBtInt());

        final UtcTimestampDecoder sendingTimeDecoder = new UtcTimestampDecoder();
        final HeaderDecoder header = decoder.header();
        assertCharsEquals("LEH_LZJ02", header.senderCompID(), header.senderCompIDLength());
        assertCharsEquals("CCG", header.targetCompID(), header.targetCompIDLength());
        assertEquals(1, header.msgSeqNum());
        assertEquals(0, sendingTimeDecoder.decode(header.sendingTime()));
    }

    private void decode(final Object encoder, final Decoder decoder)
    {
        final String message = encoder.toString();
        DebugLogger.log(FIX_TEST, message);
        buffer.putAscii(0, message);
        decoder.decode(buffer, 0, message.length());
    }
}
