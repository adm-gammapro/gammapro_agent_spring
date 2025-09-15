package com.gammapro.agent.service;

import java.util.List;
import java.util.Map;

public interface EmbeddingService {
    double[] embed(String text) throws Exception;

    Map<String, double[]> embedBatch(List<Map.Entry<String,String>> inputs) throws Exception;

    double cosine(double[] a, double[] b);
}
