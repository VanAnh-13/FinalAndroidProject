package com.example.healthylifehub.utils.report;

import android.content.Context;
import android.os.Environment;

import com.example.healthylifehub.data.model.HealthMetric;
import com.example.healthylifehub.data.model.Reminder;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Generator for PDF reports
 */
public class PDFReportGenerator {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
    
    /**
     * Generate health metrics report in PDF format
     */
    public static File generateHealthMetricsReport(Context context, List<HealthMetric> metrics, Date startDate, Date endDate) throws Exception {
        File file = createReportFile(context, "HealthMetrics");
        PdfWriter writer = new PdfWriter(new FileOutputStream(file));
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        
        // Title
        Paragraph title = new Paragraph("BÁO CÁO CHỈ SỐ SỨC KHỎE")
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER);
        document.add(title);
        
        // Date range
        Paragraph dateRange = new Paragraph(String.format("Từ ngày: %s - Đến ngày: %s", 
                DATE_FORMAT.format(startDate), DATE_FORMAT.format(endDate)))
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(dateRange);
        
        document.add(new Paragraph("\n"));
        
        // Create table
        float[] columnWidths = {1, 3, 2, 2, 3, 4};
        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(100));
        
        // Add headers
        addTableHeader(table, new String[]{"STT", "Loại chỉ số", "Giá trị", "Đơn vị", "Thời gian đo", "Ghi chú"});
        
        // Add data
        int index = 1;
        for (HealthMetric metric : metrics) {
            table.addCell(new Cell().add(new Paragraph(String.valueOf(index++))));
            table.addCell(new Cell().add(new Paragraph(getMetricTypeName(metric.getType()))));
            table.addCell(new Cell().add(new Paragraph(getMetricValue(metric))));
            table.addCell(new Cell().add(new Paragraph(getMetricUnit(metric.getType()))));
            table.addCell(new Cell().add(new Paragraph(DATE_FORMAT.format(metric.getMeasuredAt()))));
            table.addCell(new Cell().add(new Paragraph(metric.getNotes() != null ? metric.getNotes() : "")));
        }
        
        document.add(table);
        
        // Footer
        document.add(new Paragraph("\n"));
        Paragraph footer = new Paragraph(String.format("Tổng số: %d chỉ số", metrics.size()))
                .setFontSize(10)
                .setTextAlignment(TextAlignment.RIGHT);
        document.add(footer);
        
        Paragraph generated = new Paragraph(String.format("Được tạo lúc: %s", DATE_FORMAT.format(new Date())))
                .setFontSize(8)
                .setTextAlignment(TextAlignment.RIGHT);
        document.add(generated);
        
        document.close();
        return file;
    }
    
    /**
     * Generate reminders report in PDF format
     */
    public static File generateRemindersReport(Context context, List<Reminder> reminders) throws Exception {
        File file = createReportFile(context, "Reminders");
        PdfWriter writer = new PdfWriter(new FileOutputStream(file));
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        
        // Title
        Paragraph title = new Paragraph("BÁO CÁO NHẮC NHỞ")
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER);
        document.add(title);
        
        document.add(new Paragraph("\n"));
        
        // Create table
        float[] columnWidths = {1, 3, 4, 2, 3, 2, 2};
        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(100));
        
        // Add headers
        addTableHeader(table, new String[]{"STT", "Tiêu đề", "Mô tả", "Tần suất", "Thời gian", "Trạng thái", "Tiến độ"});
        
        // Add data
        int index = 1;
        for (Reminder reminder : reminders) {
            table.addCell(new Cell().add(new Paragraph(String.valueOf(index++))));
            table.addCell(new Cell().add(new Paragraph(reminder.getTitle())));
            table.addCell(new Cell().add(new Paragraph(reminder.getDescription() != null ? reminder.getDescription() : "")));
            table.addCell(new Cell().add(new Paragraph(getFrequencyName(reminder.getFrequency()))));
            table.addCell(new Cell().add(new Paragraph(DATE_FORMAT.format(new Date(reminder.getReminderTime())))));
            table.addCell(new Cell().add(new Paragraph(reminder.isActive() ? "Hoạt động" : "Tạm dừng")));
            table.addCell(new Cell().add(new Paragraph(String.format("%d%%", reminder.getProgressPercentage()))));
        }
        
        document.add(table);
        
        // Statistics
        document.add(new Paragraph("\n"));
        int activeCount = 0;
        int completedCount = 0;
        for (Reminder reminder : reminders) {
            if (reminder.isActive()) activeCount++;
            if (reminder.getProgressPercentage() >= 100) completedCount++;
        }
        
        Paragraph stats = new Paragraph(String.format("Tổng số: %d nhắc nhở | Đang hoạt động: %d | Hoàn thành: %d", 
                reminders.size(), activeCount, completedCount))
                .setFontSize(10)
                .setTextAlignment(TextAlignment.RIGHT);
        document.add(stats);
        
        Paragraph generated = new Paragraph(String.format("Được tạo lúc: %s", DATE_FORMAT.format(new Date())))
                .setFontSize(8)
                .setTextAlignment(TextAlignment.RIGHT);
        document.add(generated);
        
        document.close();
        return file;
    }
    
    private static void addTableHeader(Table table, String[] headers) {
        for (String header : headers) {
            Cell cell = new Cell().add(new Paragraph(header).setBold());
            cell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
            cell.setTextAlignment(TextAlignment.CENTER);
            table.addHeaderCell(cell);
        }
    }
    
    private static File createReportFile(Context context, String prefix) {
        File dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "HealthReports");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String fileName = prefix + "_" + FILE_DATE_FORMAT.format(new Date()) + ".pdf";
        return new File(dir, fileName);
    }
    
    private static String getMetricTypeName(String type) {
        switch (type) {
            case "blood_pressure": return "Huyết áp";
            case "heart_rate": return "Nhịp tim";
            case "blood_sugar": return "Đường huyết";
            case "weight": return "Cân nặng";
            case "temperature": return "Nhiệt độ";
            default: return type;
        }
    }
    
    private static String getMetricValue(HealthMetric metric) {
        if ("blood_pressure".equals(metric.getType())) {
            return metric.getSystolic() + "/" + metric.getDiastolic();
        }
        return String.valueOf(metric.getValue());
    }
    
    private static String getMetricUnit(String type) {
        switch (type) {
            case "blood_pressure": return "mmHg";
            case "heart_rate": return "bpm";
            case "blood_sugar": return "mg/dL";
            case "weight": return "kg";
            case "temperature": return "°C";
            default: return "";
        }
    }
    
    private static String getFrequencyName(String frequency) {
        switch (frequency) {
            case "daily": return "Hàng ngày";
            case "weekly": return "Hàng tuần";
            case "monthly": return "Hàng tháng";
            default: return frequency;
        }
    }
}
