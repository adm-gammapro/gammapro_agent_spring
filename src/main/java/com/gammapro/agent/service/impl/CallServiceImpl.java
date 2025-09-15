package com.gammapro.agent.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gammapro.agent.service.CallService;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.GenerateContentConfig;
import lombok.RequiredArgsConstructor;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CallServiceImpl implements CallService {
    private final OkHttpClient http = new OkHttpClient();
    private final ObjectMapper om = new ObjectMapper();

    @Autowired
    @Qualifier("okhttpFlash")
    private OkHttpClient httpFlash;

    @Autowired
    @Qualifier("okhttpPro")
    private OkHttpClient httpPro;

    @Value("${app.gemini.model:gemini-2.5-flash}")
    private String model;

    @Value("${app.gemini.apiKey}")
    private String apiKey;

    public String callGeminiWithContext(String question,
                                        List<Map<String, String>> retrieved) throws IOException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Falta GEMINI_API_KEY (apiKey) en configuración");
        }

        List<Map<String, String>> trimmed = retrieved.stream()
                .map(r -> Map.of(
                        "filename", r.get("filename"),
                        "snippet", trim1200(r.get("snippet"))
                ))
                .collect(Collectors.toList());

        // Construir contexto "Fuente i (filename):\nsnippet"
        String context = buildContext(trimmed);

        String system = """
                Eres un asistente que responde usando exclusivamente el contexto proporcionado.
                - Si la respuesta no está en el contexto, di claramente que no está disponible.
                - Cuando cites una evidencia o afirmación, incluye la referencia exacta en formato (Fuente N), donde N corresponde al índice adjunto al fragmento de contexto.
                - Sé conciso y claro.
                """.trim();

        String user = "Contexto:\n" + context + "\n\nPregunta:\n" + question;

        Map<String, Object> genConfig = new HashMap<>();
        genConfig.put("temperature", 0.2);

        boolean isPro = model != null && model.toLowerCase().contains("pro");
        if (isPro) {
            genConfig.put("maxOutputTokens", 512); // opcional pero recomendado para pro
        }

        // Body igual al de Node
        Map<String, Object> bodyObj = Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", system + "\n\n" + user))
                )),
                "generationConfig", Map.of("temperature", 0.2)
        );

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + HttpUrl.parse("https://dummy/").newBuilder().addPathSegment(model).build().encodedPath().substring(1)
                + ":generateContent";

        RequestBody body = RequestBody.create(om.writeValueAsBytes(bodyObj), MediaType.parse("application/json"));
        Request req = new Request.Builder()
                .url(url)
                .post(body)
                // Igual que en Node: X-goog-api-key header (en lugar de ?key=)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-goog-api-key", apiKey)
                .build();

        OkHttpClient client = clientForModel(model);
        long t0 = System.nanoTime();

        try (Response resp = client.newCall(req).execute()) {
            long ms = (System.nanoTime() - t0) / 1_000_000;
            if (!resp.isSuccessful()) {
                String txt = resp.body() != null ? resp.body().string() : "";
                throw new IOException("Gemini error " + resp.code() + " (" + ms + " ms): " + txt);
            }
            JsonNode data = om.readTree(resp.body().bytes());
            JsonNode candidates = data.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && parts.size() > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (JsonNode p : parts) {
                        String t = p.path("text").asText("");
                        sb.append(t);
                    }
                    String text = sb.toString().trim();
                    return text.isEmpty() ? "No hubo respuesta del modelo." : text;
                }
            }
            return "No hubo respuesta del modelo.";
        } catch (java.net.SocketTimeoutException e) {
            long ms = (System.nanoTime() - t0) / 1_000_000;
            // CHANGED: mensaje más claro para diagnosticar
            throw new IOException("Timeout llamando a Gemini (" + model + ") tras " + ms + " ms. " +
                    "Considera aumentar readTimeout/callTimeout o limitar tokens/contexto.", e);
        }
    }

    private static String trim1200(String s) {
        if (s == null) return "";
        return s.length() > 1200 ? s.substring(0, 1200) + "…" : s;
        // Nota: usa el mismo carácter “…” que en tu Node
    }

    private static String buildContext(List<Map<String, String>> trimmed) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < trimmed.size(); i++) {
            Map<String, String> r = trimmed.get(i);
            int idx = i + 1;
            sb.append("Fuente ").append(idx)
                    .append(" (").append(r.get("filename")).append("):\n")
                    .append(r.get("snippet"));
            if (i < trimmed.size() - 1) sb.append("\n\n");
        }
        return sb.toString();
    }

    private OkHttpClient clientForModel(String model) {
        if (model != null && model.toLowerCase().contains("pro")) {
            return httpPro;
        }
        return httpFlash;
    }
}