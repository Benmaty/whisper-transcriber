package com.benmaty.whispertranscriber;
public class WhisperJNI {
    static { System.loadLibrary("whisper_android"); }
    public native long initContext(String modelPath);
    public native String transcribeAudio(long ctxPtr, float[] samples, int language, boolean translate);
    public native void freeContext(long ctxPtr);
    public native int getProgress(long ctxPtr);
    public native void cancelTranscription(long ctxPtr);
    public static boolean isAvailable() {
        try { System.loadLibrary("whisper_android"); return true; }
        catch (UnsatisfiedLinkError e) { return false; }
    }
}
