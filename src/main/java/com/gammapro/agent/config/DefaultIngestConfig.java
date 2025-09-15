package com.gammapro.agent.config;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Component
public class DefaultIngestConfig {
    private final String ftpBasePath = "/data/90 Servicredit - Entrega SBS";
    private final List<String> defaultPaths = List.of(
            "03-2 Manual de Crédito Convenio/MPP-PRD-002 Manual de Creditos Convenio V 0.3.docx",
            "03-1 Manual de Crédito Hipotecario/MPP-PRD-001 Manual de Creditos Hipotecarios V 0.4.docx",
            "05-1 Manual de Gestión Integral de Riesgos/MPO-RIE-002 Manual de Políticas de Gestión Integral de Riesgos V0.3.docx",
            "captaciones/MANUAL DE DEPOSITOS.pdf"
    );

    /*private final List<String> defaultPaths = List.of(
            "CAPTACIONES/MANUAL DE DEPOSITOS.pdf",
            "CAPTACIONES/REGLAMENTO DEL COMITE DE ACTIVOS Y PASIVOS FINAL.pdf"
    );*/
}
