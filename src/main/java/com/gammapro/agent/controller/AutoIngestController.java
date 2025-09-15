package com.gammapro.agent.controller;

import com.gammapro.agent.domain.model.IngestResponse;
import com.gammapro.agent.service.AutoIngestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ingest")
public class AutoIngestController {
    private final AutoIngestService autoIngestService;

    @PostMapping("/auto")
    public ResponseEntity<IngestResponse> autoIngest() throws Exception {
        IngestResponse res = autoIngestService.runAutoIngest();
        return ResponseEntity.ok(res);
    }
}