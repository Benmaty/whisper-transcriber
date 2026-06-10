package com.benmaty.whispertranscriber.service;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import androidx.core.app.NotificationCompat;
import com.benmaty.whispertranscriber.R;
import com.benmaty.whispertranscriber.WhisperJNI;
import com.benmaty.whispertranscriber.model.*;
import com.benmaty.whispertranscriber.utils.AudioExtractor;
import java.io.File;
import java.util.concurrent.*;
public class TranscriptionService extends Service {
    public interface TranscriptionListener {
        void onProgress(int percent, String stage);
        void onResult(String text);
        void onError(String message);
    }
    private static final String CHANNEL_ID = "transcription_channel";
    private static final int NOTIF_ID = 1;
    private final IBinder binder = new LocalBinder();
    private TranscriptionListener listener;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private WhisperJNI whisperJNI;
    private long ctxPtr = 0;
    private volatile boolean cancelled = false;
    public class LocalBinder extends Binder {
        public TranscriptionService getService() { return TranscriptionService.this; }
    }
    @Override public IBinder onBind(Intent intent) { return binder; }
    @Override public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        whisperJNI = new WhisperJNI();
    }
    public void setListener(TranscriptionListener l) { this.listener = l; }
    public void transcribe(Uri fileUri, WhisperModel model, WhisperLanguage language, boolean translate) {
        cancelled = false;
        startForeground(NOTIF_ID, buildNotification("Démarrage…", 0));
        executor.submit(() -> {
            try {
                notifyProgress(5, "Extraction audio…");
                float[] pcm = AudioExtractor.extractPcm(this, fileUri, pct ->
                    notifyProgress(5 + pct * 40 / 100, "Extraction audio… " + pct + "%"));
                if (cancelled) { notifyError("Annulé"); return; }
                notifyProgress(50, "Chargement du modèle…");
                File modelFile = ModelManager.getInstance(this).getModelFile(model);
                if (!modelFile.exists()) { notifyError("Modèle introuvable."); return; }
                if (ctxPtr != 0) { whisperJNI.freeContext(ctxPtr); ctxPtr = 0; }
                ctxPtr = whisperJNI.initContext(modelFile.getAbsolutePath());
                if (ctxPtr == 0) { notifyError("Impossible de charger le modèle"); return; }
                if (cancelled) { notifyError("Annulé"); return; }
                notifyProgress(55, "Transcription en cours…");
                int langIdx = language == WhisperLanguage.AUTO ? -1 : language.ordinal();
                String result = whisperJNI.transcribeAudio(ctxPtr, pcm, langIdx, translate);
                if (cancelled) { notifyError("Annulé"); return; }
                notifyProgress(100, "Terminé");
                if (listener != null) listener.onResult(result != null ? result : "");
            } catch (Exception e) {
                notifyError("Erreur : " + e.getMessage());
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE);
            }
        });
    }
    public void cancel() {
        cancelled = true;
        if (ctxPtr != 0) whisperJNI.cancelTranscription(ctxPtr);
    }
    private void notifyProgress(int pct, String stage) {
        if (listener != null) listener.onProgress(pct, stage);
    }
    private void notifyError(String msg) {
        if (listener != null) listener.onError(msg);
        stopForeground(STOP_FOREGROUND_REMOVE);
    }
    private Notification buildNotification(String text, int progress) {
        Intent intent = new Intent(this, com.benmaty.whispertranscriber.ui.MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mic)
            .setContentTitle("Whisper Transcription")
            .setContentText(text)
            .setProgress(100, progress, progress == 0)
            .setContentIntent(pi)
            .setOngoing(true)
            .build();
    }
    private void createNotificationChannel() {
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Transcription", NotificationManager.IMPORTANCE_LOW);
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
    }
    @Override public void onDestroy() {
        super.onDestroy();
        if (ctxPtr != 0) { whisperJNI.freeContext(ctxPtr); ctxPtr = 0; }
        executor.shutdown();
    }
}
