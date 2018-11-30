package com.example.francois.indoornav.util;

public abstract class BytesUtils {

    public static int byteArray4ToInt(byte[] bytes){
        return (bytes[0] & 0xFF) |
                (bytes[1] & 0xFF) << 8 |
                (bytes[2] & 0xFF) << 16 |
                (bytes[3] & 0xFF) << 24;
    }

    public static long byteArray5ToLong(byte[] bytes) {
        return (long)(bytes[0] & 0xFF) |
                (long)(bytes[1] & 0xFF) << 8 |
                (long)(bytes[2] & 0xFF) << 16 |
                (long)(bytes[3] & 0xFF) << 24 |
                (long)(bytes[4] & 0xFF) << 32;

    }
}
