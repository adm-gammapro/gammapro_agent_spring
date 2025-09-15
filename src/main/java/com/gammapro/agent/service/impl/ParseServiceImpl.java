package com.gammapro.agent.service.impl;

import com.gammapro.agent.service.ParseService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Service
public class ParseServiceImpl implements ParseService {
    public String parseTxt(byte[] bytes) {
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    public String parseDocx(byte[] bytes) throws Exception {
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            WordprocessingMLPackage pkg = WordprocessingMLPackage.load(in);
            MainDocumentPart doc = pkg.getMainDocumentPart();
            return doc.getContent().toString().replaceAll("<[^>]+>", " ").replaceAll("\\s+"," ").trim();
        }
    }



    public String parsePdf(byte[] bytes) throws Exception {
        try (PDDocument doc = Loader.loadPDF(bytes)) { // Usa Loader.loadPDF()
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    public String parseByName(String filename, byte[] bytes) throws Exception {
        String name = filename.toLowerCase();
        if (name.endsWith(".txt")) return parseTxt(bytes);
        if (name.endsWith(".docx")) return parseDocx(bytes);
        if (name.endsWith(".pdf")) return parsePdf(bytes);
        throw new IllegalArgumentException("Formato no soportado: " + filename);
    }
}
