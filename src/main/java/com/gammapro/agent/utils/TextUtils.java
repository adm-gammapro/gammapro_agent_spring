package com.gammapro.agent.utils;

import java.security.MessageDigest;

public class TextUtils {
    public static String sha256(String s){
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for(byte b: d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e){ throw new RuntimeException(e); }
    }

    public static String sanitizeForEmbedding(String s, int maxLen) {
        if (s == null || s.isEmpty()) return "";

        // Paso 1: Procesar la cadena (esto puede acortarla)
        String processed = s.replace('\u00A0', ' ')
                .replaceAll("[\\u0000-\\u001F\\u007F-\\u009F]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (processed.isEmpty()) {
            return processed; // Evitar substring(0,0) en cadena vacía
        }

        return processed.substring(0, Math.min(maxLen, processed.length()));
    }
}
