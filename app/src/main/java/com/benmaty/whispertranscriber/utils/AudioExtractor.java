package com.benmaty.whispertranscriber.utils;
import android.content.Context;
import android.media.*;
import android.net.Uri;
import java.io.*;
import java.nio.*;
public class AudioExtractor {
    public interface ProgressCallback { void onProgress(int percent); }
    private static final int TARGET_SAMPLE_RATE = 16000;
    public static float[] extractPcm(Context ctx, Uri uri, ProgressCallback progress) throws IOException {
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(ctx, uri, null);
        int audioTrack = -1;
        MediaFormat audioFormat = null;
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat fmt = extractor.getTrackFormat(i);
            String mime = fmt.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith("audio/")) { audioTrack = i; audioFormat = fmt; break; }
        }
        if (audioTrack == -1) throw new IOException("Aucune piste audio trouvée");
        extractor.selectTrack(audioTrack);
        String mime = audioFormat.getString(MediaFormat.KEY_MIME);
        int srcSampleRate = audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channelCount = audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        MediaCodec codec = MediaCodec.createDecoderByType(mime);
        codec.configure(audioFormat, null, null, 0);
        codec.start();
        ByteArrayOutputStream rawPcm = new ByteArrayOutputStream();
        boolean sawEOS = false, inputDone = false;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        while (!sawEOS) {
            if (!inputDone) {
                int inIdx = codec.dequeueInputBuffer(10_000);
                if (inIdx >= 0) {
                    ByteBuffer inBuf = codec.getInputBuffer(inIdx);
                    inBuf.clear();
                    int sampleSize = extractor.readSampleData(inBuf, 0);
                    if (sampleSize < 0) { codec.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM); inputDone = true; }
                    else { codec.queueInputBuffer(inIdx, 0, sampleSize, extractor.getSampleTime(), 0); extractor.advance(); }
                }
            }
            int outIdx = codec.dequeueOutputBuffer(info, 10_000);
            if (outIdx >= 0) {
                ByteBuffer outBuf = codec.getOutputBuffer(outIdx);
                if (outBuf != null && info.size > 0) { byte[] chunk = new byte[info.size]; outBuf.get(chunk); rawPcm.write(chunk); }
                codec.releaseOutputBuffer(outIdx, false);
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) sawEOS = true;
            }
        }
        codec.stop(); codec.release(); extractor.release();
        byte[] pcmBytes = rawPcm.toByteArray();
        ShortBuffer shortBuf = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        short[] shorts = new short[shortBuf.limit()];
        shortBuf.get(shorts);
        short[] mono;
        if (channelCount > 1) {
            mono = new short[shorts.length / channelCount];
            for (int i = 0; i < mono.length; i++) { long sum = 0; for (int c = 0; c < channelCount; c++) sum += shorts[i*channelCount+c]; mono[i] = (short)(sum/channelCount); }
        } else mono = shorts;
        if (progress != null) progress.onProgress(60);
        float[] out = new float[mono.length];
        if (srcSampleRate != TARGET_SAMPLE_RATE) {
            double ratio = (double) TARGET_SAMPLE_RATE / srcSampleRate;
            int outLen = (int)(mono.length * ratio);
            out = new float[outLen];
            for (int i = 0; i < outLen; i++) {
                double pos = i / ratio; int idx = (int)pos; double frac = pos - idx;
                out[i] = (idx+1 < mono.length ? (float)(mono[idx]*(1-frac)+mono[idx+1]*frac) : mono[Math.min(idx,mono.length-1)]) / 32768.0f;
            }
        } else { for (int i = 0; i < mono.length; i++) out[i] = mono[i] / 32768.0f; }
        if (progress != null) progress.onProgress(80);
        return out;
    }
}
