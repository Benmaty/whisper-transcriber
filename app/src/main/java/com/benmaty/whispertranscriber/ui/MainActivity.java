package com.benmaty.whispertranscriber.ui;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import android.view.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.benmaty.whispertranscriber.R;
import com.benmaty.whispertranscriber.model.*;
import com.benmaty.whispertranscriber.service.TranscriptionService;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity implements TranscriptionService.TranscriptionListener {

    private Uri selectedFileUri;
    private TranscriptionService transcriptionService;
    private boolean serviceBound = false;

    private TextView tvFileName, tvModelStatus, tvProgress, tvResult;
    private Button btnSelectFile, btnTranscribe, btnCopy, btnSave, btnCancel;
    private ProgressBar progressBar;
    private Spinner spinnerLanguage;
    private CheckBox checkboxTranslate;
    private ScrollView scrollResult;

    private WhisperLanguage selectedLanguage = WhisperLanguage.AUTO;
    private ModelManager modelManager;

    private final ActivityResultLauncher<Intent> filePickerLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) {
                    selectedFileUri = uri;
                    tvFileName.setText(getFileName(uri));
                    btnTranscribe.setEnabled(true);
                }
            }
        });

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            transcriptionService = ((TranscriptionService.LocalBinder) binder).getService();
            transcriptionService.setListener(MainActivity.this);
            serviceBound = true;
        }
        @Override public void onServiceDisconnected(ComponentName name) { serviceBound = false; }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        modelManager = ModelManager.getInstance(this);
        initViews();
        setupLanguageSpinner();
        updateModelStatusBadge();
        requestPermissions();
        bindTranscriptionService();

        Intent intent = getIntent();
        if (intent != null) {
            Uri sharedUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (sharedUri == null) sharedUri = intent.getData();
            if (sharedUri != null) {
                selectedFileUri = sharedUri;
                tvFileName.setText(getFileName(sharedUri));
                btnTranscribe.setEnabled(true);
            }
        }
    }

    private void initViews() {
        tvFileName = findViewById(R.id.tv_file_name);
        tvModelStatus = findViewById(R.id.tv_model_status);
        tvProgress = findViewById(R.id.tv_progress);
        tvResult = findViewById(R.id.tv_result);
        btnSelectFile = findViewById(R.id.btn_select_file);
        btnTranscribe = findViewById(R.id.btn_transcribe);
        btnCopy = findViewById(R.id.btn_copy);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);
        progressBar = findViewById(R.id.progress_bar);
        spinnerLanguage = findViewById(R.id.spinner_language);
        checkboxTranslate = findViewById(R.id.checkbox_translate);
        scrollResult = findViewById(R.id.scroll_result);

        btnSelectFile.setOnClickListener(v -> openFilePicker());
        btnTranscribe.setOnClickListener(v -> startTranscription());
        btnCopy.setOnClickListener(v -> copyToClipboard());
        btnSave.setOnClickListener(v -> saveToFile());
        btnCancel.setOnClickListener(v -> cancelTranscription());
        tvModelStatus.setOnClickListener(v -> openModelManager());

        btnTranscribe.setEnabled(false);
        btnCopy.setEnabled(false);
        btnSave.setEnabled(false);
        btnCancel.setVisibility(View.GONE);
    }

    private void setupLanguageSpinner() {
        WhisperLanguage[] langs = WhisperLanguage.values();
        String[] labels = new String[langs.length];
        for (int i = 0; i < langs.length; i++) labels[i] = langs[i].getLabel();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(adapter);
        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { selectedLanguage = langs[pos]; }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void updateModelStatusBadge() {
        WhisperModel active = modelManager.getActiveModel();
        boolean downloaded = modelManager.isModelDownloaded(active);
        if (downloaded) {
            tvModelStatus.setText("✅ " + active.displayName);
            tvModelStatus.setBackgroundResource(R.drawable.badge_green);
        } else {
            tvModelStatus.setText("⬇ Aucun modèle — Appuyez pour télécharger");
            tvModelStatus.setBackgroundResource(R.drawable.badge_orange);
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"video/*", "audio/*"});
        filePickerLauncher.launch(intent);
    }

    private void openModelManager() {
        startActivity(new Intent(this, ModelManagerActivity.class));
    }

    private void startTranscription() {
        if (selectedFileUri == null) { Toast.makeText(this, "Sélectionnez un fichier d'abord", Toast.LENGTH_SHORT).show(); return; }
        WhisperModel model = modelManager.getActiveModel();
        if (!modelManager.isModelDownloaded(model)) {
            Toast.makeText(this, "Téléchargez d'abord un modèle", Toast.LENGTH_LONG).show();
            openModelManager(); return;
        }
        if (!serviceBound) { Toast.makeText(this, "Service non disponible", Toast.LENGTH_SHORT).show(); return; }
        setTranscribing(true);
        tvResult.setText("");
        tvProgress.setText("Démarrage…");
        progressBar.setProgress(0);
        transcriptionService.transcribe(selectedFileUri, model, selectedLanguage, checkboxTranslate.isChecked());
    }

    private void cancelTranscription() {
        if (serviceBound) transcriptionService.cancel();
        setTranscribing(false);
        tvProgress.setText("Annulé");
    }

    @Override public void onProgress(int percent, String stage) {
        runOnUiThread(() -> { progressBar.setProgress(percent); tvProgress.setText(stage); });
    }

    @Override public void onResult(String text) {
        runOnUiThread(() -> {
            setTranscribing(false);
            tvResult.setText(text.isEmpty() ? "(Aucun texte détecté)" : text);
            btnCopy.setEnabled(!text.isEmpty());
            btnSave.setEnabled(!text.isEmpty());
            tvProgress.setText("✅ Transcription terminée");
        });
    }

    @Override public void onError(String message) {
        runOnUiThread(() -> {
            setTranscribing(false);
            tvProgress.setText("❌ " + message);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }

    private void setTranscribing(boolean active) {
        btnTranscribe.setEnabled(!active);
        btnSelectFile.setEnabled(!active);
        btnCancel.setVisibility(active ? View.VISIBLE : View.GONE);
        progressBar.setVisibility(active ? View.VISIBLE : View.GONE);
        if (!active) progressBar.setProgress(0);
    }

    private void copyToClipboard() {
        String text = tvResult.getText().toString();
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("Transcription", text));
        Toast.makeText(this, "Copié !", Toast.LENGTH_SHORT).show();
    }

    private void saveToFile() {
        String text = tvResult.getText().toString();
        if (text.isEmpty()) return;
        try {
            String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File dir = new File(getExternalFilesDir(null), "transcriptions");
            dir.mkdirs();
            File out = new File(dir, "transcription_" + ts + ".txt");
            new FileWriter(out).write(text);
            Toast.makeText(this, "Sauvegardé : " + out.getName(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String getFileName(Uri uri) {
        String name = uri.getLastPathSegment();
        try (android.database.Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = c.getString(idx);
            }
        } catch (Exception ignored) {}
        return name != null ? name : "Fichier sélectionné";
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS}, 1);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
    }

    private void bindTranscriptionService() {
        bindService(new Intent(this, TranscriptionService.class), serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu); return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_models) { openModelManager(); return true; }
        return super.onOptionsItemSelected(item);
    }

    @Override protected void onResume() {
        super.onResume();
        updateModelStatusBadge();
        if (serviceBound && transcriptionService != null) transcriptionService.setListener(this);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) { unbindService(serviceConnection); serviceBound = false; }
    }
}
