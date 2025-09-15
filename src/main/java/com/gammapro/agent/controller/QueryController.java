package com.gammapro.agent.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gammapro.agent.domain.model.Chunk;
import com.gammapro.agent.service.CallService;
import com.gammapro.agent.service.EmbeddingService;
import com.gammapro.agent.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class QueryController {
    private final RagService rag;
    private final EmbeddingService emb;
    private final CallService call;

    public record QueryReq(@JsonProperty("sessionId") String sessionId,
                           @JsonProperty("query") String query) {}

    @PostMapping("/query")
    public Map<String,Object> query(@RequestBody QueryReq req) throws Exception {
        if (req.sessionId()==null || req.sessionId().isBlank() || req.query()==null || req.query().isBlank())
            return Map.of("error","Faltan sessionId o query");

        List<Chunk> chunks = rag.getChunks(req.sessionId());
        List<Map<String,Object>> topSnippets = new ArrayList<>();

        if (!chunks.isEmpty()){
            double[] qvec = emb.embed(req.query());
            record Sc(String filename, String snippet, double score) {}
            List<Sc> scored = new ArrayList<>();
            for (Chunk c: chunks){
                if (c.getEmbedding()==null) continue;
                String snip = c.getText().length()>1200 ? c.getText().substring(0,1200) + "…" : c.getText();
                scored.add(new Sc(c.getFilename(), snip, emb.cosine(qvec, c.getEmbedding())));
            }
            scored.sort((a,b)->Double.compare(b.score, a.score));
            int maxPerFile = 2;  // máximo chunks por archivo
            int totalMax = 8;    // máximo total de chunks a usar

            Map<String, Integer> perFileCount = new HashMap<>();
            List<Sc> diversified = new ArrayList<>();

            for (Sc s : scored) {
                int count = perFileCount.getOrDefault(s.filename, 0);
                if (count < maxPerFile) {
                    diversified.add(s);
                    perFileCount.put(s.filename, count + 1);
                    if (diversified.size() >= totalMax) break;
                }
            }

            // Usar la lista diversificada en lugar de limit(5)
            int i = 1;
            for (Sc s : diversified) {
                String label = "Fuente " + i;
                topSnippets.add(Map.of(
                        "index", i,
                        "filename", s.filename,
                        "snippet", s.snippet,
                        "label", label
                ));
                i++;
            }
            /*var top = scored.stream().limit(5).toList();
            int i=1;
            for (var s: top){
                topSnippets.add(Map.of("index", i, "filename", s.filename, "snippet", s.snippet, "label", "Fuente "+i));
                i++;
            }*/
        }

        // Fallback naive
        if (topSnippets.isEmpty()){
            var docs = rag.getDocs(req.sessionId());
            String q = req.query().toLowerCase(Locale.ROOT);
            int i=1;
            for (var d: docs){
                int idx = d.getText().toLowerCase(Locale.ROOT).indexOf(q);
                if (idx>=0){
                    int start = Math.max(0, idx-200), end = Math.min(d.getText().length(), idx+q.length()+200);
                    String sn = d.getText().substring(start, end);
                    topSnippets.add(Map.of("index", i, "filename", d.getFilename(), "snippet", sn, "label", "Fuente "+i));
                    if (i++>=5) break;
                }
            }
        }

        String answer;
        if (topSnippets.isEmpty()) {
            answer = "No se encontraron fragmentos relevantes en los documentos para responder la pregunta.";
        } else {
            List<Map<String, String>> retrieved = topSnippets.stream()
                    .map(m -> Map.of(
                            "filename", String.valueOf(m.get("filename")),
                            "snippet", String.valueOf(m.get("snippet"))
                    ))
                    .collect(Collectors.toList());

            answer = call.callGeminiWithContext(req.query(), retrieved);
        }

        return Map.of(
                "query", req.query(),
                "foundIn", topSnippets.size(),
                "snippets", topSnippets,
                "answer", answer
        );
    }


}
