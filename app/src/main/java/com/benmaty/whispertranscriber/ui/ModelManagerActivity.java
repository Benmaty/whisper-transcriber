package com.benmaty.whispertranscriber.ui;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.benmaty.whispertranscriber.R;
import com.benmaty.whispertranscriber.model.*;
public class ModelManagerActivity extends AppCompatActivity {
    private ModelManager modelManager;
    private ModelAdapter adapter;
    private TextView tvStorageUsed;
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model_manager);
        if (getSupportActionBar() != null) { getSupportActionBar().setDisplayHomeAsUpEnabled(true); getSupportActionBar().setTitle("Gestion des modèles"); }
        modelManager = ModelManager.getInstance(this);
        tvStorageUsed = findViewById(R.id.tv_storage_used);
        RecyclerView rv = findViewById(R.id.rv_models);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ModelAdapter(this, modelManager, this::onModelAction);
        rv.setAdapter(adapter);
        updateStorageInfo();
    }
    private void onModelAction(WhisperModel model, String action) {
        switch (action) {
            case "download": startDownload(model); break;
            case "select": modelManager.setActiveModel(model); adapter.notifyDataSetChanged(); Toast.makeText(this, model.displayName + " sélectionné", Toast.LENGTH_SHORT).show(); break;
            case "delete": confirmDelete(model); break;
        }
    }
    private void startDownload(WhisperModel model) {
        adapter.setDownloading(model, true, 0);
        modelManager.downloadModel(model, new ModelManager.DownloadCallback() {
            @Override public void onProgress(int p, long d, long t) { runOnUiThread(() -> adapter.setDownloading(model, true, p)); }
            @Override public void onSuccess(java.io.File f) { runOnUiThread(() -> { adapter.setDownloading(model, false, 100); adapter.notifyDataSetChanged(); updateStorageInfo(); modelManager.setActiveModel(model); Toast.makeText(ModelManagerActivity.this, model.displayName + " téléchargé ✅", Toast.LENGTH_SHORT).show(); }); }
            @Override public void onError(String msg) { runOnUiThread(() -> { adapter.setDownloading(model, false, 0); Toast.makeText(ModelManagerActivity.this, "Erreur : " + msg, Toast.LENGTH_LONG).show(); }); }
            @Override public void onCancelled() { runOnUiThread(() -> adapter.setDownloading(model, false, 0)); }
        });
    }
    private void confirmDelete(WhisperModel model) {
        new AlertDialog.Builder(this).setTitle("Supprimer " + model.displayName + " ?")
            .setPositiveButton("Supprimer", (d, w) -> { modelManager.deleteModel(model); adapter.notifyDataSetChanged(); updateStorageInfo(); })
            .setNegativeButton("Annuler", null).show();
    }
    private void updateStorageInfo() {
        long used = modelManager.getTotalUsedSpace();
        tvStorageUsed.setText("Espace utilisé : " + (used >= 1024L*1024*1024 ? String.format("%.1f Go", used/(1024.0*1024*1024)) : String.format("%d Mo", used/(1024*1024))));
    }
    @Override public boolean onOptionsItemSelected(MenuItem item) { if (item.getItemId() == android.R.id.home) { finish(); return true; } return super.onOptionsItemSelected(item); }
}
