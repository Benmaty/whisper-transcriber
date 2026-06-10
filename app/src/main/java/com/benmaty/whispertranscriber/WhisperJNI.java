package com.benmaty.whispertranscriber;

public class WhisperJNI {

    private static boolean loaded = false;

    static {
        try {
            System.loadLibrary("whisper_android");
            loaded = true;
        } catch (UnsatisfiedLinkError e) {
            loaded = false;
        }
    }

    public native long initContext(String modelPath);
    public native String transcribeAudio(long ctxPtr, float[] samples, int language, boolean translate);
    public native void freeContext(long ctxPtr);
    public native int getProgress(long ctxPtr);
    public native void cancelTranscription(long ctxPtr);

    public static boolean isAvailable() { return loaded; }
}
