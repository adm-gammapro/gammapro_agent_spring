package com.gammapro.agent.utils;

import java.util.ArrayList;
import java.util.List;

public class Chunker {
    public static List<String> chunk(String text, int chunkSize, int overlap){
        List<String> out = new ArrayList<>();
        int i=0;
        while(i < text.length()){
            int end = Math.min(text.length(), i + chunkSize);
            String c = text.substring(i, end);
            if (!c.trim().isEmpty()) out.add(c);
            if (end == text.length()) break;
            i = Math.max(0, end - overlap);
        }
        return out;
    }
}
