package com.benmaty.whispertranscriber.model;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class ModelManager {
    public interface DownloadCallback {
        void onProgress(int percent, long downloadedBytes, long totalBytes);
        void onSuccess(File modelFile);
        void onError(String message);
        void onCancelled();
    }
    private static ModelManager instance;
    private final Context context;
    private final SharedPreferences prefs;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile boolean cancelDownload = false;
    private static final String PREFS_NAME = "whisper_models";
    private static final String KEY_ACTIVE_MODEL = "active_model";
    private ModelManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    public static synchronized ModelManager getInstance(Context context) {
        if (instance == null) instance = new ModelManager(context);
        return instance;
    }
    public File getModelsDir() {
        File dir = new File(context.getFilesDir(), "models");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }
    public File getModelFile(WhisperModel model) { return new File(getModelsDir(), model.filename); }
    public boolean isModelDownloaded(WhisperModel model) {
        File f = getModelFile(model);
        return f.exists() && f.length() > 1024 * 1024;
    }
    public void downloadModel(WhisperModel model, DownloadCallback callback) {
        cancelDownload = false;
        executor.submit(() -> {
            File outFile = getModelFile(model);
            File tmpFile = new File(outFile.getParent(), outFile.getName() + ".tmp");
            try {
                URL url = new URL(model.downloadUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(30_000);
                conn.setReadTimeout(60_000);
                conn.setRequestProperty("User-Agent", "WhisperTranscriber/1.0");
                long existing = tmpFile.exists() ? tmpFile.length() : 0;
                if (existing > 0) conn.setRequestProperty("Range", "bytes=" + existing + "-");
                int responseCode = conn.getResponseCode();
                boolean isResume = responseCode == 206;
                long total = conn.getContentLengthLong();
                if (isResume) total += existing;
                try (InputStream in = conn.getInputStream();
                     FileOutputStream out = new FileOutputStream(tmpFile, isResume)) {
                    byte[] buffer = new byte[32 * 1024];
                    long downloaded = existing;
                    int read;
                    long lastNotify = 0;
                    while ((read = in.read(buffer)) != -1) {
                        if (cancelDownload) { mainHandler.post(callback::onCancelled); return; }
                        out.write(buffer, 0, read);
                        downloaded += read;
                        long now = System.currentTimeMillis();
                        if (now - lastNotify > 300) {
                            lastNotify = now;
                            int pct = total > 0 ? (int)(downloaded * 100 / total) : -1;
                            long dl = downloaded, tot = total;
                            mainHandler.post(() -> callback.onProgress(pct, dl, tot));
                        }
                    }
                }
                if (outFile.exists()) outFile.delete();
                tmpFile.renameTo(outFile);
                mainHandler.post(() -> callback.onSuccess(outFile));
            } catch (Exception e) { mainHandler.post(() -> callback.onError(e.getMessage())); }
        });
    }
    public void cancelDownload() { cancelDownload = true; }
    public void deleteModel(WhisperModel model) {
        File f = getModelFile(model);
        if (f.exists()) f.delete();
        File tmp = new File(f.getParent(), f.getName() + ".tmp");
        if (tmp.exists()) tmp.delete();
    }
    public void setActiveModel(WhisperModel model) { prefs.edit().putString(KEY_ACTIVE_MODEL, model.id).apply(); }
    public WhisperModel getActiveModel() {
        String id = prefs.getString(KEY_ACTIVE_MODEL, WhisperModel.TINY.id);
        for (WhisperModel m : WhisperModel.values()) if (m.id.equals(id)) return m;
        return WhisperModel.TINY;
    }
    public long getTotalUsedSpace() {
        long total = 0;
        for (WhisperModel m : WhisperModel.values()) { File f = getModelFile(m); if (f.exists()) total += f.length(); }
        return total;
    }
}
