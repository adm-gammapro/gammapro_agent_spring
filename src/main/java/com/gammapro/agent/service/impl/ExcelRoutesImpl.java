package com.gammapro.agent.service.impl;

import com.gammapro.agent.service.ExcelRoutes;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ExcelRoutesImpl implements ExcelRoutes {
    public List<String> readRoutes(byte[] excelBytes) throws Exception {
        try (var wb = WorkbookFactory.create(new java.io.ByteArrayInputStream(excelBytes))) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) return List.of();

            Iterator<Row> it = sheet.iterator();
            if (!it.hasNext()) return List.of();

            Row headerRow = it.next();
            Map<String,Integer> cols = new HashMap<>();
            for (Cell c : headerRow) {
                String key = getString(c).trim().toLowerCase(Locale.ROOT);
                if (!key.isEmpty()) cols.put(key, c.getColumnIndex());
            }

            int idxTipo = cols.getOrDefault("tipo", -1);
            int idxRuta = cols.getOrDefault("ruta", -1);

            List<String> out = new ArrayList<>();
            while (it.hasNext()) {
                Row r = it.next();

                String tipo = idxTipo >= 0 ? getString(r.getCell(idxTipo)).toUpperCase(Locale.ROOT).trim() : "";
                if (!"D".equals(tipo)) continue;

                String raw = idxRuta >= 0 ? getString(r.getCell(idxRuta)).trim() : "";
                if (raw.isEmpty()) continue;

                String normalized = raw.replace('\u00A0',' ')
                        .replace("\\","/")
                        .replaceAll("/+","/")
                        .trim();
                out.add(normalized);
            }
            return out;
        }
    }

    private static String getString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toString();
                } else {
                    double v = cell.getNumericCellValue();
                    // Evitar “1.0” cuando era “1”
                    if (Math.rint(v) == v) yield String.valueOf((long) v);
                    yield String.valueOf(v);
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                // Evaluar como string si es posible; si no, fallback al valor calculado
                try {
                    yield cell.getStringCellValue();
                } catch (IllegalStateException e) {
                    yield getString(evaluateFormulaResult(cell));
                }
            }
            case BLANK, _NONE, ERROR -> "";
        };
    }

    private static Cell evaluateFormulaResult(Cell cell) {
        var wb = cell.getSheet().getWorkbook();
        var evaluator = wb.getCreationHelper().createFormulaEvaluator();
        var result = evaluator.evaluateInCell(cell); // reemplaza la celda por el resultado
        return result;
    }
}
