package com.example.healthylifehub.patterns.strategy;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Strategy Pattern - Excel Export Implementation
 */
public class ExcelExportStrategy implements ExportStrategy {

    @Override
    public void export(ExportData data, OutputStream output) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        // Health Metrics Sheet
        if (!data.getHealthMetrics().isEmpty()) {
            Sheet sheet = workbook.createSheet("Chỉ số sức khỏe");
            
            // Header
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Loại");
            headerRow.createCell(1).setCellValue("Giá trị");
            headerRow.createCell(2).setCellValue("Đơn vị");
            headerRow.createCell(3).setCellValue("Ngày");

            // Data
            int rowNum = 1;
            for (var metric : data.getHealthMetrics()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(metric.getType());
                row.createCell(1).setCellValue(metric.getValue());
                row.createCell(2).setCellValue(getUnitForType(metric.getType()));
                row.createCell(3).setCellValue(metric.getMeasuredAt() != null ? 
                        sdf.format(metric.getMeasuredAt()) : "");
            }
        }

        // Medical Records Sheet
        if (!data.getMedicalRecords().isEmpty()) {
            Sheet sheet = workbook.createSheet("Hồ sơ y tế");
            
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Tiêu đề");
            headerRow.createCell(1).setCellValue("Bệnh viện");
            headerRow.createCell(2).setCellValue("Chẩn đoán");
            headerRow.createCell(3).setCellValue("Ngày");

            int rowNum = 1;
            for (var record : data.getMedicalRecords()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(record.getTitle());
                row.createCell(1).setCellValue(record.getHospital());
                row.createCell(2).setCellValue(record.getDiagnosis());
                row.createCell(3).setCellValue(record.getDate() != null ? record.getDate() : "");
            }
        }

        workbook.write(output);
        workbook.close();
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

    @Override
    public String getFileExtension() {
        return ".xlsx";
    }

    @Override
    public String getMimeType() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }
}
