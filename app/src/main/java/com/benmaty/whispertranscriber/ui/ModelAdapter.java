package com.benmaty.whispertranscriber.ui;
import android.content.Context;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.benmaty.whispertranscriber.R;
import com.benmaty.whispertranscriber.model.*;
import java.util.HashMap;
import java.util.Map;
public class ModelAdapter extends RecyclerView.Adapter<ModelAdapter.ModelViewHolder> {
    public interface ActionListener { void onAction(WhisperModel model, String action); }
    private final Context context;
    private final ModelManager modelManager;
    private final ActionListener actionListener;
    private final WhisperModel[] models = WhisperModel.values();
    private final Map<WhisperModel, Integer> downloadProgress = new HashMap<>();
    private final Map<WhisperModel, Boolean> downloading = new HashMap<>();
    public ModelAdapter(Context ctx, ModelManager mgr, ActionListener l) { context=ctx; modelManager=mgr; actionListener=l; }
    public void setDownloading(WhisperModel model, boolean active, int progress) {
        downloading.put(model, active); downloadProgress.put(model, progress);
        int idx = indexOf(model); if (idx >= 0) notifyItemChanged(idx);
    }
    private int indexOf(WhisperModel m) { for (int i=0;i<models.length;i++) if(models[i]==m) return i; return -1; }
    @NonNull @Override public ModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ModelViewHolder(LayoutInflater.from(context).inflate(R.layout.item_model, parent, false));
    }
    @Override public void onBindViewHolder(@NonNull ModelViewHolder h, int position) {
        WhisperModel model = models[position];
        boolean downloaded = modelManager.isModelDownloaded(model);
        boolean isActive = modelManager.getActiveModel() == model;
        boolean isDownloading = Boolean.TRUE.equals(downloading.get(model));
        int progress = downloadProgress.getOrDefault(model, 0);
        h.tvName.setText(model.displayName); h.tvDescription.setText(model.description); h.tvSize.setText(model.getFormattedSize());
        h.tvActiveBadge.setVisibility(isActive && downloaded ? View.VISIBLE : View.GONE);
        h.progressBar.setVisibility(isDownloading ? View.VISIBLE : View.GONE);
        h.progressBar.setProgress(progress);
        h.tvProgressText.setVisibility(isDownloading ? View.VISIBLE : View.GONE);
        h.tvProgressText.setText(progress + "%");
        if (isDownloading) { h.btnDownload.setVisibility(View.GONE); h.btnSelect.setVisibility(View.GONE); h.btnDelete.setVisibility(View.GONE); }
        else if (downloaded) { h.btnDownload.setVisibility(View.GONE); h.btnSelect.setVisibility(isActive ? View.GONE : View.VISIBLE); h.btnDelete.setVisibility(View.VISIBLE); }
        else { h.btnDownload.setVisibility(View.VISIBLE); h.btnSelect.setVisibility(View.GONE); h.btnDelete.setVisibility(View.GONE); }
        h.btnDownload.setOnClickListener(v -> actionListener.onAction(model, "download"));
        h.btnSelect.setOnClickListener(v -> actionListener.onAction(model, "select"));
        h.btnDelete.setOnClickListener(v -> actionListener.onAction(model, "delete"));
        h.itemView.setBackgroundResource(isActive && downloaded ? R.drawable.bg_model_active : R.drawable.bg_model_normal);
    }
    @Override public int getItemCount() { return models.length; }
    static class ModelViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDescription, tvSize, tvActiveBadge, tvProgressText;
        Button btnDownload, btnSelect, btnDelete;
        ProgressBar progressBar;
        ModelViewHolder(@NonNull View v) {
            super(v);
            tvName=v.findViewById(R.id.tv_model_name); tvDescription=v.findViewById(R.id.tv_model_description);
            tvSize=v.findViewById(R.id.tv_model_size); tvActiveBadge=v.findViewById(R.id.tv_active_badge);
            tvProgressText=v.findViewById(R.id.tv_progress_text); btnDownload=v.findViewById(R.id.btn_model_download);
            btnSelect=v.findViewById(R.id.btn_model_select); btnDelete=v.findViewById(R.id.btn_model_delete);
            progressBar=v.findViewById(R.id.progress_model);
        }
    }
}
