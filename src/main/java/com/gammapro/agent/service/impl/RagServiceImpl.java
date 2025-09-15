package com.gammapro.agent.service.impl;

import com.gammapro.agent.domain.model.Chunk;
import com.gammapro.agent.domain.model.DocRecord;
import com.gammapro.agent.service.RagService;
import com.gammapro.agent.utils.Chunker;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RagServiceImpl implements RagService {
    private final Map<String, List<DocRecord>> sessions = new ConcurrentHashMap<>();
    private final Map<String, List<Chunk>> sessionChunks = new ConcurrentHashMap<>();
    private final SecureRandom rnd = new SecureRandom();

    public String newSession(List<DocRecord> docs){
        String id = randomId();
        sessions.put(id, docs);
        return id;
    }

    public void putChunks(String sessionId, List<Chunk> chunks){
        sessionChunks.put(sessionId, chunks);
    }

    public List<Chunk> getChunks(String sessionId){
        return sessionChunks.getOrDefault(sessionId, List.of());
    }

    public List<DocRecord> getDocs(String sessionId){
        return sessions.getOrDefault(sessionId, List.of());
    }

    public List<Chunk> toChunks(List<DocRecord> docs, int size, int overlap) {
        List<Chunk> out = new ArrayList<>();
        for (DocRecord d : docs) {
            if (d.getText()==null || d.getText().startsWith("[[ERROR")) continue;
            var parts = Chunker.chunk(d.getText(), size, overlap);
            for (int i=0;i<parts.size();i++){
                out.add(Chunk.builder().filename(d.getFilename()).index(i).text(parts.get(i)).build());
            }
        }
        return out;
    }

    private String randomId(){
        byte[] b = new byte[16];
        rnd.nextBytes(b);
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }
}
