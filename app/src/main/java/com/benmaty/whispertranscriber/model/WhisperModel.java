package com.benmaty.whispertranscriber.model;
public enum WhisperModel {
    TINY("tiny","ggml-tiny.bin","https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin",75L*1024*1024,"Tiny","Le plus rapide, moins précis"),
    TINY_EN("tiny.en","ggml-tiny.en.bin","https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.en.bin",75L*1024*1024,"Tiny (EN)","Tiny optimisé anglais"),
    BASE("base","ggml-base.bin","https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin",142L*1024*1024,"Base","Bon équilibre vitesse/précision"),
    BASE_EN("base.en","ggml-base.en.bin","https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.en.bin",142L*1024*1024,"Base (EN)","Base optimisé anglais"),
    SMALL("small","ggml-small.bin","https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin",466L*1024*1024,"Small","Précision correcte"),
    SMALL_EN("small.en","ggml-small.en.bin","https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.en.bin",466L*1024*1024,"Small (EN)","Small optimisé anglais"),
    MEDIUM("medium","ggml-medium.bin","https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-medium.bin",1500L*1024*1024,"Medium","Très bonne précision multilingue"),
    MEDIUM_EN("medium.en","ggml-medium.en.bin","https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-medium.en.bin",1500L*1024*1024,"Medium (EN)","Medium optimisé anglais"),
    LARGE_V1("large-v1","ggml-large-v1.bin","https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-large-v1.bin",2900L*1024*1024,"Large v1","Haute précision"),
    LARGE_V2("large-v2","ggml-large-v2.bin","https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-large-v2.bin",2900L*1024*1024,"Large v2","Meilleure précision multilingue"),
    LARGE_V3("large-v3","ggml-large-v3.bin","https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-large-v3.bin",2900L*1024*1024,"Large v3","Dernière version"),
    LARGE_V3_TURBO("large-v3-turbo","ggml-large-v3-turbo.bin","https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-large-v3-turbo.bin",1600L*1024*1024,"Large v3 Turbo","8x plus rapide que Large v3");
    public final String id,filename,downloadUrl,displayName,description;
    public final long sizeBytes;
    WhisperModel(String id,String filename,String downloadUrl,long sizeBytes,String displayName,String description){
        this.id=id;this.filename=filename;this.downloadUrl=downloadUrl;
        this.sizeBytes=sizeBytes;this.displayName=displayName;this.description=description;
    }
    public String getFormattedSize(){
        if(sizeBytes>=1024L*1024*1024) return String.format("%.1f Go",sizeBytes/(1024.0*1024*1024));
        return String.format("%d Mo",sizeBytes/(1024*1024));
    }
}
