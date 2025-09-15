package com.gammapro.agent.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestRequest {
    private MultipartFile excel;

    private String host;
    private Integer port;
    private String user;
    private String password;
    private String privateKey;
    private Boolean secure;
}
