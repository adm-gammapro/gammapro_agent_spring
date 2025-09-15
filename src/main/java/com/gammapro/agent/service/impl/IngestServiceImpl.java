package com.gammapro.agent.service.impl;

import com.gammapro.agent.domain.model.Chunk;
import com.gammapro.agent.domain.model.DocRecord;
import com.gammapro.agent.domain.model.IngestRequest;
import com.gammapro.agent.domain.model.IngestResponse;
import com.gammapro.agent.service.EmbeddingService;
import com.gammapro.agent.service.ExcelRoutes;
import com.gammapro.agent.service.IngestService;
import com.gammapro.agent.service.ParseService;
import com.gammapro.agent.service.RagService;
import com.gammapro.agent.service.SftpService;
import com.gammapro.agent.utils.TextUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class IngestServiceImpl implements IngestService {
    private final ExcelRoutes excelRoutes;
    private final SftpService sftp;
    private final ParseService parse;
    private final EmbeddingService emb;
    private final RagService rag;

    @Override
    public IngestResponse ingestFromExcelAndSftp(IngestRequest request) throws Exception {
        validateExcel(request.getExcel());

        List<String> paths = excelRoutes.readRoutes(request.getExcel().getBytes());
        if (paths == null || paths.isEmpty()) {
            throw new IllegalArgumentException("No se encontraron rutas en el Excel");
        }

        // 2) Descargar archivos vía SFTP
        var files = sftp.fetchFiles(paths);
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No se pudieron descargar archivos del SFTP");
        }

        // 3) Parsear archivos a texto y construir DocRecord
        List<DocRecord> docs = parseFilesToDocs(files);

        // 4) Crear sesión y chunkear
        String sessionId = rag.newSession(docs);
        List<Chunk> chunks = rag.toChunks(docs, 1800, 250);

        // 5) Embeddings por lotes
        embedChunksInPlace(chunks);

        // 6) Persistir chunks en el RAG store
        rag.putChunks(sessionId, chunks);

        // 7) Resumen de archivos para respuesta
        List<Map<String, Object>> fileSumm = summarizeDocs(docs);

        return IngestResponse.builder()
                .sessionId(sessionId)
                .files(fileSumm)
                .chunks(chunks.size())
                .build();
    }

    public IngestResponse ingestFromSftpPaths(java.util.List<String> paths,
                                              String host, int port, String user,
                                              String password, String privateKey) throws Exception {
        var files = sftp.fetchFiles(paths);
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No se pudieron descargar archivos del SFTP");
        }

        List<DocRecord> docs = parseFilesToDocs(files);

        String sessionId = rag.newSession(docs);
        List<Chunk> chunks = rag.toChunks(docs, 1800, 250);

        embedChunksInPlace(chunks);
        rag.putChunks(sessionId, chunks);

        var fileSumm = summarizeDocs(docs);

        return IngestResponse.builder()
                .sessionId(sessionId)
                .files(fileSumm)
                .chunks(chunks.size())
                .build();
    }

    private void validateExcel(MultipartFile excel) {
        if (excel == null || excel.isEmpty()) {
            throw new IllegalArgumentException("Falta archivo Excel (campo 'excel')");
        }
    }

    private List<DocRecord> parseFilesToDocs(List<SftpServiceImpl.FileData> files) {
        List<DocRecord> docs = new ArrayList<>();
        for (var f : files) {
            String nameLower = f.filename().toLowerCase();
            try {
                String text = parseByExtension(nameLower, f.bytes());
                docs.add(DocRecord.builder()
                        .filename(f.filename())
                        .mimeType(mimeOf(nameLower))
                        .text(text)
                        .size(f.bytes().length)
                        .build());
            } catch (Exception e) {
                docs.add(DocRecord.builder()
                        .filename(f.filename())
                        .mimeType("application/octet-stream")
                        .text("[[ERROR AL PARSEAR: " + e.getMessage() + "]]")
                        .size(f.bytes().length)
                        .build());
            }
        }
        return docs;
    }

    private String parseByExtension(String nameLower, byte[] content) throws Exception {
        if (nameLower.endsWith(".txt")) return parse.parseTxt(content);
        if (nameLower.endsWith(".docx")) return parse.parseDocx(content);
        if (nameLower.endsWith(".pdf")) return parse.parsePdf(content);
        throw new IllegalArgumentException("Formato no soportado: " + nameLower);
    }

    private String mimeOf(String lower) {
        if (lower.endsWith(".txt")) return "text/plain";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".pdf")) return "application/pdf";
        return "application/octet-stream";
    }

    private void embedChunksInPlace(List<Chunk> chunks) throws Exception {
        // preparar lote
        List<Map.Entry<String, String>> batch = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            String sanitized = TextUtils.sanitizeForEmbedding(chunks.get(i).getText(), 2000);
            batch.add(Map.entry(String.valueOf(i), sanitized));
        }
        // solicitar embeddings
        Map<String, double[]> vecs = emb.embedBatch(batch);
        // asignar embedding
        for (int i = 0; i < chunks.size(); i++) {
            chunks.get(i).setEmbedding(vecs.get(String.valueOf(i)));
        }
    }

    private List<Map<String, Object>> summarizeDocs(List<DocRecord> docs) {
        List<Map<String, Object>> fileSumm = new ArrayList<>(docs.size());
        for (DocRecord d : docs) {
            String preview = d.getText() == null ? "" : d.getText().substring(0, Math.min(200, d.getText().length()));
            fileSumm.add(Map.of(
                    "filename", d.getFilename(),
                    "size", d.getSize(),
                    "mimeType", d.getMimeType(),
                    "preview", preview
            ));
        }
        return fileSumm;
    }
}
