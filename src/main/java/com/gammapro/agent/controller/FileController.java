package com.gammapro.agent.controller;

import com.gammapro.agent.service.SftpService;
import com.gammapro.agent.service.impl.SftpServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileController {
    private final SftpService sftpService;

    @GetMapping("/view")
    public ResponseEntity<byte[]> view(@RequestParam("path") String path) throws Exception {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("El parámetro 'path' es requerido");
        }

        SftpServiceImpl.FileData fd = fetchSingle(path);
        MediaType mediaType = mediaTypeOf(fd.filename());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.noCache())
                .body(fd.bytes());
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> download(@RequestParam("path") String path) throws Exception {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("El parámetro 'path' es requerido");
        }

        SftpServiceImpl.FileData fd = fetchSingle(path);
        MediaType mediaType = mediaTypeOf(fd.filename());

        String rawFilename = fd.filename();
        String safeFilename = rawFilename.replace("\"", "");
        String encoded = URLEncoder.encode(safeFilename, StandardCharsets.UTF_8).replace("+", "%20");
        String contentDisposition = "attachment; filename=\"" + fd.filename() + "\"; filename*=UTF-8''" + encoded;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .cacheControl(CacheControl.noCache())
                .body(fd.bytes());
    }

    private SftpServiceImpl.FileData fetchSingle(String path) throws Exception {
        var list = sftpService.fetchFiles(List.of(path));
        if (list == null || list.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo no encontrado: " + path);
        }
        return list.get(0);
    }

    private MediaType mediaTypeOf(String filename) {
        String lower = filename == null ? "" : filename.toLowerCase();
        if (lower.endsWith(".pdf")) return MediaType.APPLICATION_PDF;
        if (lower.endsWith(".txt")) return MediaType.TEXT_PLAIN;
        if (lower.endsWith(".csv")) return MediaType.valueOf("text/csv");
        if (lower.endsWith(".json")) return MediaType.APPLICATION_JSON;
        if (lower.endsWith(".xml")) return MediaType.APPLICATION_XML;

        // Office
        if (lower.endsWith(".docx")) return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        if (lower.endsWith(".xlsx")) return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        if (lower.endsWith(".pptx")) return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.presentationml.presentation");

        // Imágenes comunes
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (lower.endsWith(".gif")) return MediaType.IMAGE_GIF;
        if (lower.endsWith(".svg")) return MediaType.valueOf("image/svg+xml");
        if (lower.endsWith(".webp")) return MediaType.valueOf("image/webp");

        // Por defecto, binario
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
