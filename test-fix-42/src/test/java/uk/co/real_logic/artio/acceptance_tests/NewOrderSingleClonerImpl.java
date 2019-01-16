package uk.co.real_logic.artio.acceptance_tests;

import org.agrona.DirectBuffer;
import uk.co.real_logic.artio.builder.Encoder;
import uk.co.real_logic.artio.builder.NewOrderSingleEncoder;
import uk.co.real_logic.artio.decoder.NewOrderSingleDecoder;
import uk.co.real_logic.artio.util.AsciiBuffer;
import uk.co.real_logic.artio.util.MutableAsciiBuffer;

public class NewOrderSingleClonerImpl implements NewOrderSingleCloner
{
    private final AsciiBuffer asciiBuffer = new MutableAsciiBuffer();
    private final NewOrderSingleDecoder decoder = new NewOrderSingleDecoder();
    private final NewOrderSingleEncoder encoder = new NewOrderSingleEncoder();

    @Override
    public Encoder clone(final DirectBuffer buffer, final int offset, final int length)
    {
        asciiBuffer.wrap(buffer);
        decoder.decode(asciiBuffer, offset, length);

        encoder
            .clOrdID(decoder.clOrdID(), decoder.clOrdIDLength())
            .handlInst(decoder.handlInst())
            .ordType(decoder.ordType())
            .side(decoder.side())
            .transactTime(decoder.transactTime(), decoder.transactTimeLength())
            .symbol(decoder.symbol(), decoder.symbolLength());

        if (decoder.hasOrderQty())
        {
            encoder.orderQty(decoder.orderQty());
        }

        return encoder;
    }
}
