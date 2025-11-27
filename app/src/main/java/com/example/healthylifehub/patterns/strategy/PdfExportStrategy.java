package com.example.healthylifehub.patterns.strategy;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.UnitValue;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Strategy Pattern - PDF Export Implementation
 */
public class PdfExportStrategy implements ExportStrategy {

    @Override
    public void export(ExportData data, OutputStream output) throws Exception {
        PdfWriter writer = new PdfWriter(output);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Title
        document.add(new Paragraph(data.getTitle())
                .setFontSize(20)
                .setBold());

        // Date range
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        document.add(new Paragraph("Từ: " + sdf.format(new Date(data.getStartDate())) +
                " - Đến: " + sdf.format(new Date(data.getEndDate()))));

        // Health Metrics Table
        if (!data.getHealthMetrics().isEmpty()) {
            document.add(new Paragraph("Chỉ số sức khỏe").setBold().setFontSize(14));
            Table table = new Table(UnitValue.createPercentArray(new float[]{2, 2, 2, 3}))
                    .setWidth(UnitValue.createPercentValue(100));
            
            table.addHeaderCell(new Cell().add(new Paragraph("Loại")));
            table.addHeaderCell(new Cell().add(new Paragraph("Giá trị")));
            table.addHeaderCell(new Cell().add(new Paragraph("Đơn vị")));
            table.addHeaderCell(new Cell().add(new Paragraph("Ngày")));

            for (var metric : data.getHealthMetrics()) {
                table.addCell(new Cell().add(new Paragraph(metric.getType())));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(metric.getValue()))));
                table.addCell(new Cell().add(new Paragraph(getUnitForType(metric.getType()))));
                table.addCell(new Cell().add(new Paragraph(metric.getMeasuredAt() != null ? 
                        sdf.format(metric.getMeasuredAt()) : "")));
            }
            document.add(table);
        }

        document.close();
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
        return ".pdf";
    }

    @Override
    public String getMimeType() {
        return "application/pdf";
    }
}
