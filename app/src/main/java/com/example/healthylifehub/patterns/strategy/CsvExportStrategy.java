package com.example.healthylifehub.patterns.strategy;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Strategy Pattern - CSV Export Implementation
 */
public class CsvExportStrategy implements ExportStrategy {

    @Override
    public void export(ExportData data, OutputStream output) throws Exception {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        // Health Metrics
        if (!data.getHealthMetrics().isEmpty()) {
            writer.println("=== CHỈ SỐ SỨC KHỎE ===");
            writer.println("Loại,Giá trị,Đơn vị,Ngày");
            
            for (var metric : data.getHealthMetrics()) {
                writer.printf("%s,%s,%s,%s%n",
                        escapeCsv(metric.getType()),
                        metric.getValue(),
                        escapeCsv(getUnitForType(metric.getType())),
                        metric.getMeasuredAt() != null ? sdf.format(metric.getMeasuredAt()) : "");
            }
            writer.println();
        }

        // Medical Records
        if (!data.getMedicalRecords().isEmpty()) {
            writer.println("=== HỒ SƠ Y TẾ ===");
            writer.println("Tiêu đề,Bệnh viện,Chẩn đoán,Ngày");
            
            for (var record : data.getMedicalRecords()) {
                writer.printf("%s,%s,%s,%s%n",
                        escapeCsv(record.getTitle()),
                        escapeCsv(record.getHospital()),
                        escapeCsv(record.getDiagnosis()),
                        record.getDate() != null ? record.getDate() : "");
            }
        }

        writer.flush();
    }

    private String getUnitForType(String type) {
        if (type == null) return "";
        switch (type.toLowerCase()) {
            case "blood_pressure": return "mmHg";
            case "blood_sugar": return "mg/dL";
            case "heart_rate": return "bpm";
            case "weight": return "kg";
            case "bmi": return "kg/m²";
            case "temperature": return "°C";
            default: return "";
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    @Override
    public String getFileExtension() {
        return ".csv";
    }

    @Override
    public String getMimeType() {
        return "text/csv";
    }
}
