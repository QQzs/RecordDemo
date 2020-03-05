package com.czt.mp3recorder.util;

public class LameUtil {
    public LameUtil() {
    }

    public static native void init(int var0, int var1, int var2, int var3, int var4);

    public static native int encode(short[] var0, short[] var1, int var2, byte[] var3);

    public static native int flush(byte[] var0);

    public static native void close();

    static {
        System.loadLibrary("mp3lame");
    }
}
