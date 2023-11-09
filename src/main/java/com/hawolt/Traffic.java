package com.hawolt;

import java.util.Arrays;

public class Traffic {
    private byte[] bytes = new byte[128];
    private int position = 0;

    // reset our internal buffers to re-use object
    private void reset() {
        this.bytes = new byte[128];
        this.position = 0;
    }

    // push byte to the "stack" and grow it when needed
    public void add(byte b) {
        this.bytes[position++] = b;
        if (position >= bytes.length) {
            byte[] replacement = new byte[bytes.length << 1];
            System.arraycopy(
                    bytes,
                    0,
                    replacement,
                    0,
                    bytes.length);
            this.bytes = replacement;
        }
    }

    // return current stack data and reset
    public byte[] get() {
        byte[] b = Arrays.copyOfRange(bytes, 0, position);
        this.reset();
        return b;
    }
}
