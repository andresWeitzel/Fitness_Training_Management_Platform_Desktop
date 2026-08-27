package com.fitnesstraining.shared.export;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvExporterTest {

    @TempDir
    Path tempDir;

    @Test
    void writesWindows1252WithSpanishAccentsReadableByExcel() throws Exception {
        Path file = tempDir.resolve("reporte.csv");
        CsvExporter.write(
                file,
                List.of("Urgencia", "Días"),
                List.of(List.of("Crítico", "3")));

        byte[] bytes = Files.readAllBytes(file);
        // windows-1252: í = 0xED (no es secuencia UTF-8 C3 AD)
        String as1252 = new String(bytes, CsvExporter.EXCEL_CHARSET);
        assertTrue(as1252.startsWith("sep=;\r\n"));
        assertTrue(as1252.contains("Crítico"));
        assertTrue(as1252.contains("Días"));
        assertFalse(as1252.contains("\uFEFF"));

        // El byte de "í" en Crítico / Días debe ser 0xED en 1252
        assertTrue(containsByte(bytes, (byte) 0xED));
    }

    @Test
    void formatsExcelDateAsTextWithLeadingApostrophe() {
        assertEquals("'28/08/2026", CsvExporter.formatExcelDate(LocalDate.of(2026, 8, 28)));
        assertEquals("", CsvExporter.formatExcelDate(null));
    }

    private static boolean containsByte(byte[] bytes, byte target) {
        for (byte b : bytes) {
            if (b == target) {
                return true;
            }
        }
        return false;
    }
}
