package com.gammapro.agent.service;

import com.gammapro.agent.domain.model.IngestRequest;
import com.gammapro.agent.domain.model.IngestResponse;

public interface IngestService {
    IngestResponse ingestFromExcelAndSftp(IngestRequest request) throws Exception;
}
