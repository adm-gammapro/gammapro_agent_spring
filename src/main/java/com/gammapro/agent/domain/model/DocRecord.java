package com.gammapro.agent.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DocRecord {
    private String filename;
    private String mimeType;
    private String text;
    private long size;
}
