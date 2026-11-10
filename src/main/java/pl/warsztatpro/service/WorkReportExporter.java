package pl.warsztatpro.service;

import pl.warsztatpro.model.WorkReportRow;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Exports the same detailed work report to UTF-8 CSV or a lightweight PDF. */
public final class WorkReportExporter {
    private static final String[] HEADERS = {"Numer zlecenia", "Data przyjęcia", "Termin wizyty", "Planowane zakończenie",
            "Klient", "Rejestracja", "Pojazd", "Status", "Stanowisko", "Opis usterki", "Wykonane prace", "Wartość (zł)"};

    private WorkReportExporter() { }

    public static void writeCsv(Path path, List<WorkReportRow> rows) throws IOException {
        StringBuilder csv = new StringBuilder("\uFEFF");
        appendCsvRow(csv, HEADERS);
        for (WorkReportRow row : rows) appendCsvRow(csv, values(row));
        Files.writeString(path, csv, StandardCharsets.UTF_8);
    }

    private static void appendCsvRow(StringBuilder csv, String[] values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) csv.append(';');
            csv.append('"').append(nullSafe(values[i]).replace("\"", "\"\"")).append('"');
        }
        csv.append("\r\n");
    }

    private static String[] values(WorkReportRow row) {
        return new String[]{row.orderNumber(), text(row.receivedDate()), text(row.scheduledAt()), text(row.plannedCompletionDate()),
                row.client(), row.registrationNumber(), row.vehicle(), row.status(), row.workshop(), row.faultDescription(),
                row.workDescription(), String.format(java.util.Locale.ROOT, "%.2f", row.totalCostGrosze() / 100.0)};
    }

    public static void writePdf(Path path, List<WorkReportRow> rows, LocalDate from, LocalDate to) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("RAPORT PRAC WARSZTATU");
        lines.add("Okres: " + from + " - " + to + "    Liczba zlecen: " + rows.size());
        lines.add("");
        for (WorkReportRow row : rows) {
            String[] v = values(row);
            lines.add(v[0] + " | " + v[1] + " | " + v[4] + " | " + v[5] + " | " + v[6] + " | " + v[7]);
            lines.add("Wizyta: " + v[2] + " | Planowane zakonczenie: " + v[3] + " | Stanowisko: " + v[8] + " | Wartosc: " + v[11] + " zl");
            lines.add("Usterka: " + v[9]);
            lines.add("Prace: " + v[10]);
            lines.add("");
        }
        if (rows.isEmpty()) lines.add("Brak zlecen w wybranym okresie.");
        writeSimplePdf(path, lines);
    }

    private static void writeSimplePdf(Path path, List<String> sourceLines) throws IOException {
        List<List<String>> pages = new ArrayList<>();
        List<String> page = new ArrayList<>();
        for (String source : sourceLines) {
            String line = ascii(source);
            if (line.isEmpty()) { page.add(""); continue; }
            while (line.length() > 105) {
                int split = line.lastIndexOf(' ', 105);
                if (split < 30) split = 105;
                page.add(line.substring(0, split));
                line = line.substring(split).stripLeading();
                if (page.size() >= 58) { pages.add(page); page = new ArrayList<>(); }
            }
            page.add(line);
            if (page.size() >= 58) { pages.add(page); page = new ArrayList<>(); }
        }
        if (!page.isEmpty() || pages.isEmpty()) pages.add(page);

        int fontId = 3 + pages.size() * 2;
        List<byte[]> objects = new ArrayList<>();
        objects.add(bytes("<< /Type /Catalog /Pages 2 0 R >>"));
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) kids.append(3 + i * 2).append(" 0 R ");
        objects.add(bytes("<< /Type /Pages /Kids [" + kids + "] /Count " + pages.size() + " >>"));
        for (int i = 0; i < pages.size(); i++) {
            int pageId = 3 + i * 2, contentId = pageId + 1;
            objects.add(bytes("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 " + fontId + " 0 R >> >> /Contents " + contentId + " 0 R >>"));
            StringBuilder content = new StringBuilder("BT\n/F1 8 Tf\n40 805 Td\n11 TL\n");
            for (String line : pages.get(i)) content.append('(').append(pdfEscape(line)).append(") Tj\nT*\n");
            content.append("ET");
            byte[] stream = bytes(content.toString());
            objects.add(concat(bytes("<< /Length " + stream.length + " >>\nstream\n"), stream, bytes("\nendstream")));
        }
        objects.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Courier /Encoding /WinAnsiEncoding >>"));

        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        output.write(bytes("%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n"));
        List<Integer> offsets = new ArrayList<>(); offsets.add(0);
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(output.size());
            output.write(bytes((i + 1) + " 0 obj\n")); output.write(objects.get(i)); output.write(bytes("\nendobj\n"));
        }
        int xref = output.size();
        output.write(bytes("xref\n0 " + (objects.size() + 1) + "\n0000000000 65535 f \n"));
        for (int i = 1; i < offsets.size(); i++) output.write(bytes(String.format(java.util.Locale.ROOT, "%010d 00000 n \n", offsets.get(i))));
        output.write(bytes("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF"));
        Files.write(path, output.toByteArray());
    }

    private static String ascii(String value) {
        String normalized = Normalizer.normalize(nullSafe(value), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return normalized.replace("ł", "l").replace("Ł", "L").replaceAll("[^\\x20-\\x7E]", "?");
    }
    private static String pdfEscape(String value) { return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)"); }
    private static String text(Object value) { return value == null ? "" : value.toString(); }
    private static String nullSafe(String value) { return value == null ? "" : value; }
    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.ISO_8859_1); }
    private static byte[] concat(byte[]... chunks) throws IOException {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        for (byte[] chunk : chunks) output.write(chunk);
        return output.toByteArray();
    }
}
