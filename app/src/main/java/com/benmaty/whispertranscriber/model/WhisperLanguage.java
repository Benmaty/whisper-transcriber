package com.benmaty.whispertranscriber.model;
public enum WhisperLanguage {
    AUTO("auto","Détection automatique","🌐"),
    ARABIC("ar","Arabe","🇸🇦"),
    CHINESE("zh","Chinois","🇨🇳"),
    ENGLISH("en","Anglais","🇬🇧"),
    FRENCH("fr","Français","🇫🇷"),
    GERMAN("de","Allemand","🇩🇪"),
    HINDI("hi","Hindi","🇮🇳"),
    ITALIAN("it","Italien","🇮🇹"),
    JAPANESE("ja","Japonais","🇯🇵"),
    KOREAN("ko","Coréen","🇰🇷"),
    PORTUGUESE("pt","Portugais","🇵🇹"),
    RUSSIAN("ru","Russe","🇷🇺"),
    SPANISH("es","Espagnol","🇪🇸"),
    TURKISH("tr","Turc","🇹🇷"),
    UKRAINIAN("uk","Ukrainien","🇺🇦"),
    VIETNAMESE("vi","Vietnamien","🇻🇳");
    public final String code, displayName, flag;
    WhisperLanguage(String code, String displayName, String flag) {
        this.code = code; this.displayName = displayName; this.flag = flag;
    }
    public String getLabel() { return flag + " " + displayName; }
    public static WhisperLanguage fromCode(String code) {
        for (WhisperLanguage l : values()) if (l.code.equals(code)) return l;
        return AUTO;
    }
}
