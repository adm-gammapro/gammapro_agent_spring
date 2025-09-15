package com.gammapro.agent.service.impl;

import com.gammapro.agent.config.DefaultIngestConfig;
import com.gammapro.agent.domain.model.IngestResponse;
import com.gammapro.agent.service.AutoIngestService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AutoIngestServiceImpl implements AutoIngestService {
    private final DefaultIngestConfig defaultIngestConfig;
    private final IngestServiceImpl ingestServiceImpl; // usamos la implementación concreta para acceder al nuevo método

    @Value("${app.sftp.host}") private String defaultHost;
    @Value("${app.sftp.port}") private int defaultPort;
    @Value("${app.sftp.username}") private String defaultUser;
    @Value("${app.sftp.password:}") private String defaultPass;
    @Value("${app.sftp.privateKey:}") private String defaultKey;

    @Override
    public IngestResponse runAutoIngest() throws Exception {
        String base = defaultIngestConfig.getFtpBasePath();
        List<String> relPaths = defaultIngestConfig.getDefaultPaths();

        // Convierte rutas relativas a absolutas (opcional si tu SftpService admite rel + base)
        List<String> absolutePaths = relPaths.stream()
                .map(p -> normalize(base, p))
                .toList();

        List<IngestResponse.TreeNodeDto> routesTree = buildRoutesTree(absolutePaths);

        IngestResponse response = ingestServiceImpl.ingestFromSftpPaths(absolutePaths,
                                                                        defaultHost,
                                                                        defaultPort,
                                                                        defaultUser,
                                                                        defaultPass,
                                                                        defaultKey);

        response.setRoutesTree(routesTree);

        return response;
    }

    private String normalize(String base, String rel) {
        String b = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return b + (rel.startsWith("/") ? rel : "/" + rel);
    }

    private List<IngestResponse.TreeNodeDto> buildRoutesTree(List<String> paths) {
        // Normalizar separadores y quitar duplicados/espacios
        List<String> clean = paths.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(p -> p.replace("\\", "/"))
                .distinct()
                .toList();

        // Usamos un trie básico con mapas anidados
        Node root = new Node("");

        for (String path : clean) {
            // quitar leading "/" para uniformidad
            String normalized = path.startsWith("/") ? path.substring(1) : path;
            // si termina con "/", lo tratamos como directorio
            boolean endsWithSlash = normalized.endsWith("/");
            if (endsWithSlash) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }
            if (normalized.isEmpty()) continue;

            String[] parts = normalized.split("/");
            Node current = root;
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                boolean isLast = (i == parts.length - 1);

                current.children.putIfAbsent(part, new Node(part));
                current = current.children.get(part);
                if (isLast) {
                    // si ruta apuntaba a archivo (no terminaba en '/'), marcar hoja
                    if (!endsWithSlash && !part.isEmpty()) {
                        current.leaf = true;
                    }
                }
            }
        }

        // Convertir a DTOs
        List<IngestResponse.TreeNodeDto> dto = new ArrayList<>();
        for (Node child : root.children.values()) {
            dto.add(toDto(child, ""));
        }
        // ordenar opcional: carpetas primero, luego archivos, y por label
        dto.sort(nodeComparator());
        return dto;
    }

    private static class Node {
        String name;
        boolean leaf = false;
        Map<String, Node> children = new LinkedHashMap<>();
        Node(String name) { this.name = name; }
    }

    private IngestResponse.TreeNodeDto toDto(Node node, String parentKey) {
        String key = parentKey.isEmpty() ? node.name : parentKey + "/" + node.name;
        List<IngestResponse.TreeNodeDto> kids = new ArrayList<>();
        for (Node c : node.children.values()) {
            kids.add(toDto(c, key));
        }
        // ordenar hijos: carpetas primero
        kids.sort(nodeComparator());
        return IngestResponse.TreeNodeDto.builder()
                .key(key)
                .label(node.name)
                .leaf(node.leaf || node.children.isEmpty())
                .children(kids.isEmpty() ? null : kids)
                .build();
    }

    private Comparator<IngestResponse.TreeNodeDto> nodeComparator() {
        return Comparator
                .comparing((IngestResponse.TreeNodeDto n) -> n.isLeaf()) // false (carpeta) antes que true (archivo)
                .thenComparing(IngestResponse.TreeNodeDto::getLabel, String.CASE_INSENSITIVE_ORDER);
    }
}
