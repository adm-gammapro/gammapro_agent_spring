package com.gammapro.agent.service;

import com.gammapro.agent.domain.model.IngestResponse;

public interface AutoIngestService {
    IngestResponse runAutoIngest() throws Exception;
}
