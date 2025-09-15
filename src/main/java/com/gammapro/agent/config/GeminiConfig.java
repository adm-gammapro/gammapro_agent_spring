package com.gammapro.agent.config;

import com.google.genai.Client;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {
    @Bean
    public Client geminiClient(@Value("${app.gemini.apiKey}") String apiKey) {
        return Client.builder()
                .apiKey(apiKey)
                .build();
    }

    @Bean("okhttpFlash")
    public OkHttpClient okhttpFlash() {
        return new OkHttpClient.Builder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .readTimeout(java.time.Duration.ofSeconds(60))
                .writeTimeout(java.time.Duration.ofSeconds(60))
                .pingInterval(30, java.util.concurrent.TimeUnit.SECONDS) // HTTP/2 keepalive
                .retryOnConnectionFailure(true)
                .build();
    }

    @Bean("okhttpPro")
    public OkHttpClient okhttpPro() {
        return new OkHttpClient.Builder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .readTimeout(java.time.Duration.ofSeconds(150))
                .writeTimeout(java.time.Duration.ofSeconds(60))
                .callTimeout(java.time.Duration.ofSeconds(150)) // límite global
                .pingInterval(30, java.util.concurrent.TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }
}