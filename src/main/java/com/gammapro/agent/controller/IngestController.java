package com.gammapro.agent.controller;

import com.gammapro.agent.domain.model.IngestRequest;
import com.gammapro.agent.domain.model.IngestResponse;
import com.gammapro.agent.service.IngestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class IngestController {

    private final IngestService ingestService;

    @PostMapping(value="/ingest-ftp-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<IngestResponse> ingest(
            @RequestParam("excel") MultipartFile excel,
            @RequestParam(value="host", required=false) String host,
            @RequestParam(value="port", required=false) Integer port,
            @RequestParam(value="user", required=false) String user,
            @RequestParam(value="username", required=false) String username,
            @RequestParam(value="password", required=false) String password,
            @RequestParam(value="privateKey", required=false) String privateKey,
            @RequestParam(value = "secure", required = false) Boolean secure
    ) throws Exception {
        IngestRequest req = IngestRequest.builder()
                .excel(excel)
                .host(host)
                .port(port)
                .user(user != null ? user : username)
                .password(password)
                .privateKey(privateKey)
                .secure(secure)
                .build();

        IngestResponse res = ingestService.ingestFromExcelAndSftp(req);
        return ResponseEntity.ok(res);
    }
}
