package com.gammapro.agent.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestResponse {
    private String sessionId;
    private List<Map<String,Object>> files;
    private int chunks;

    private List<TreeNodeDto> routesTree;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TreeNodeDto {
        private String key;               // ruta absoluta o acumulada
        private String label;             // nombre del nodo (carpeta/archivo)
        private boolean leaf;             // true si es archivo (hoja)
        private List<TreeNodeDto> children;
    }
}