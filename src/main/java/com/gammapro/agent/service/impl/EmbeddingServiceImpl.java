package com.gammapro.agent.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gammapro.agent.service.EmbeddingService;
import com.gammapro.agent.utils.TextUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.MediaType;
import okhttp3.HttpUrl;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmbeddingServiceImpl implements EmbeddingService {
    private final OkHttpClient http = new OkHttpClient.Builder()
            .callTimeout(Duration.ofSeconds(60))
            .build();
    private final ObjectMapper om = new ObjectMapper();

    @Value("${app.gemini.apiKey:}")
    private String apiKey;

    private final Map<String, double[]> cache = new ConcurrentHashMap<>();
    private int perSec = 3, perMin = 60;
    private int secTokens = perSec, minTokens = perMin;

    public EmbeddingServiceImpl() {
        new Timer(true).schedule(new TimerTask() { public void run(){ secTokens = perSec; }}, 0, 1000);
        new Timer(true).schedule(new TimerTask() { public void run(){ minTokens = perMin; }}, 0, 60_000);
    }

    private void rateGate() {
        while (secTokens <= 0 || minTokens <= 0) {
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        }
        secTokens--; minTokens--;
    }

    public double[] embed(String text) throws Exception {
        String id = TextUtils.sha256(text);
        if (cache.containsKey(id)) return cache.get(id);

        rateGate();

        HttpUrl url = HttpUrl.parse("https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:embedContent");
        RequestBody body = RequestBody.create(
                om.writeValueAsBytes(Map.of(
                        "model","text-embedding-004",
                        "content", Map.of("parts", List.of(Map.of("text", text)))
                )),
                MediaType.parse("application/json")
        );
        Request req = new Request.Builder()
                .url(url)
                .addHeader("X-goog-api-key", apiKey)
                .post(body).build();

        try (Response resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful()) throw new RuntimeException("Gemini embed error " + resp.code() + ": " + (resp.body()!=null?resp.body().string():""));
            JsonNode node = om.readTree(resp.body().bytes());
            JsonNode vals = node.path("embedding").path("values");
            double[] arr = new double[vals.size()];
            for (int i=0;i<vals.size();i++) arr[i] = vals.get(i).asDouble();
            cache.put(id, arr);
            return arr;
        }
    }

    public Map<String, double[]> embedBatch(List<Map.Entry<String, String>> inputs) throws Exception {
        if (inputs.isEmpty()) return Map.of();

        Map<String, double[]> out = new HashMap<>();
        List<Map.Entry<String, String>> toSend = new ArrayList<>();

        // Paso 1: Filtrar elementos no cacheados
        for (var e : inputs) {
            String id = TextUtils.sha256(e.getValue());
            if (cache.containsKey(id)) {
                out.put(e.getKey(), cache.get(id));
            } else {
                toSend.add(e);
            }
        }
        if (toSend.isEmpty()) return out;

        // Paso 2: Dividir en batches de máximo 100 elementos
        int BATCH_SIZE = 100; // Límite de Gemini
        for (int i = 0; i < toSend.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, toSend.size());
            List<Map.Entry<String, String>> batch = toSend.subList(i, end);

            if (batch.isEmpty()) continue;

            rateGate(); // Control de tasa por batch

            // Paso 3: Construir solicitud con formato correcto de modelo
            HttpUrl url = HttpUrl.parse("https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:batchEmbedContents")
                    .newBuilder().addQueryParameter("key", apiKey).build();

            var requests = new ArrayList<Map<String, Object>>();
            for (var e : batch) {
                requests.add(Map.of(
                        "model", "models/text-embedding-004", // ¡Prefijo "models/" agregado!
                        "content", Map.of("parts", List.of(Map.of("text", e.getValue())))
                ));
            }

            RequestBody body = RequestBody.create(
                    new ObjectMapper().writeValueAsBytes(Map.of("requests", requests)),
                    MediaType.parse("application/json")
            );

            // Paso 4: Ejecutar y procesar respuesta
            Request req = new Request.Builder().url(url).post(body).build();
            try (Response resp = http.newCall(req).execute()) {
                if (!resp.isSuccessful()) {
                    String errorBody = resp.body() != null ? resp.body().string() : "";
                    throw new RuntimeException("Gemini batch error " + resp.code() + ": " + errorBody);
                }

                JsonNode node = om.readTree(resp.body().bytes());
                JsonNode embeddings = node.path("embeddings");

                if (!embeddings.isArray()) {
                    throw new IllegalStateException("Respuesta inesperada de Gemini: " + node.toString());
                }

                for (int j = 0; j < embeddings.size(); j++) {
                    JsonNode vals = embeddings.get(j).path("values");
                    double[] arr = new double[vals.size()];
                    for (int k = 0; k < vals.size(); k++) {
                        arr[k] = vals.get(k).asDouble();
                    }
                    Map.Entry<String, String> entry = batch.get(j);
                    out.put(entry.getKey(), arr);
                    cache.put(TextUtils.sha256(entry.getValue()), arr);
                }
            }
        }
        return out;
    }

    public double cosine(double[] a, double[] b) {
        double dot=0, na=0, nb=0;
        int n = Math.min(a.length, b.length);
        for(int i=0;i<n;i++){ dot += a[i]*b[i]; na+=a[i]*a[i]; nb+=b[i]*b[i]; }
        double denom = Math.sqrt(na) * Math.sqrt(nb);
        return denom==0?0: dot/denom;
    }
}
