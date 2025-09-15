package com.gammapro.agent.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface CallService {
    String callGeminiWithContext(String question,
                                 List<Map<String, String>> retrieved) throws IOException;
}
