package uk.co.real_logic.artio.acceptance_tests;

import org.agrona.DirectBuffer;
import uk.co.real_logic.artio.builder.Encoder;

public interface NewOrderSingleCloner
{
    Encoder clone(DirectBuffer buffer, int offset, int length);
}
