package com.fitnesstraining.shared.export;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * CSV pensado para Excel en Windows (español):
 * <ul>
 *   <li>Charset Windows-1252 (lo que Excel usa al abrir por doble clic)</li>
 *   <li>Separador {@code ;} + línea {@code sep=;}</li>
 *   <li>Fechas como texto {@code dd/MM/yyyy} con apóstrofe para que no se corrompan</li>
 * </ul>
 */
public final class CsvExporter {

    /** Excel (es-AR / es-ES) interpreta CSV por doble clic con este charset, no UTF-8. */
    static final Charset EXCEL_CHARSET = Charset.forName("windows-1252");

    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
    private static final DateTimeFormatter EXCEL_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private CsvExporter() {
    }

    public static String suggestedFileName(String reportTitle) {
        String base = sanitizeFileName(reportTitle == null || reportTitle.isBlank() ? "reporte" : reportTitle);
        return base + "_" + LocalDateTime.now().format(FILE_STAMP) + ".csv";
    }

    /**
     * Fecha forzada a texto en Excel ({@code 'dd/MM/yyyy}) para evitar {@code ########}
     * cuando el locale interpreta mal día/mes.
     */
    public static String formatExcelDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        return "'" + EXCEL_DATE.format(date);
    }

    public static void write(Path path, List<String> headers, List<List<String>> rows) throws IOException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(headers, "headers");
        Objects.requireNonNull(rows, "rows");
        StringBuilder sb = new StringBuilder();
        sb.append("sep=;\r\n");
        sb.append(String.join(";", headers.stream().map(CsvExporter::escape).toList()));
        sb.append("\r\n");
        for (List<String> row : rows) {
            sb.append(String.join(";", row.stream().map(CsvExporter::escape).toList()));
            sb.append("\r\n");
        }
        Files.writeString(path, sb.toString(), EXCEL_CHARSET);
    }

    private static String escape(String value) {
        String raw = value == null ? "" : value;
        if (raw.contains(";") || raw.contains("\"") || raw.contains("\n") || raw.contains("\r")) {
            return "\"" + raw.replace("\"", "\"\"") + "\"";
        }
        return raw;
    }

    private static String sanitizeFileName(String name) {
        String cleaned = name.trim().replace(' ', '_');
        StringBuilder sb = new StringBuilder(cleaned.length());
        for (char c : cleaned.toCharArray()) {
            sb.append(isInvalidFileChar(c) ? '_' : c);
        }
        return sb.isEmpty() ? "reporte" : sb.toString();
    }

    private static boolean isInvalidFileChar(char c) {
        return c < 32 || "<>:\"/\\|?*".indexOf(c) >= 0;
    }
}
