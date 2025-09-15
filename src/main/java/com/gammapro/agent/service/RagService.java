package com.gammapro.agent.service;

import com.gammapro.agent.domain.model.Chunk;
import com.gammapro.agent.domain.model.DocRecord;

import java.util.List;

public interface RagService {
    String newSession(List<DocRecord> docs);

    void putChunks(String sessionId, List<Chunk> chunks);

    List<Chunk> getChunks(String sessionId);

    List<DocRecord> getDocs(String sessionId);

    List<Chunk> toChunks(List<DocRecord> docs, int size, int overlap);
}
