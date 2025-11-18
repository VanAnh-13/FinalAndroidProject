package com.example.healthylifehub.utils.report;

import android.content.Context;
import android.os.Environment;

import com.example.healthylifehub.data.model.HealthMetric;
import com.example.healthylifehub.data.model.Reminder;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Generator for Excel reports
 */
public class ExcelReportGenerator {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
    
    /**
     * Generate health metrics report in Excel format
     */
    public static File generateHealthMetricsReport(Context context, List<HealthMetric> metrics, Date startDate, Date endDate) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Health Metrics");
        
        // Create header style
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dateStyle = createDateStyle(workbook);
        
        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"STT", "Loại chỉ số", "Giá trị", "Đơn vị", "Thời gian đo", "Ghi chú"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Fill data
        int rowNum = 1;
        for (HealthMetric metric : metrics) {
            Row row = sheet.createRow(rowNum++);
            
            row.createCell(0).setCellValue(rowNum - 1);
            row.createCell(1).setCellValue(getMetricTypeName(metric.getType()));
            row.createCell(2).setCellValue(getMetricValue(metric));
            row.createCell(3).setCellValue(getMetricUnit(metric.getType()));
            
            Cell dateCell = row.createCell(4);
            dateCell.setCellValue(DATE_FORMAT.format(metric.getMeasuredAt()));
            dateCell.setCellStyle(dateStyle);
            
            row.createCell(5).setCellValue(metric.getNotes() != null ? metric.getNotes() : "");
        }
        
        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
        
        // Save file
        File file = createReportFile(context, "HealthMetrics");
        FileOutputStream outputStream = new FileOutputStream(file);
        workbook.write(outputStream);
        workbook.close();
        outputStream.close();
        
        return file;
    }
    
    /**
     * Generate reminders report in Excel format
     */
    public static File generateRemindersReport(Context context, List<Reminder> reminders) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Reminders");
        
        // Create styles
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dateStyle = createDateStyle(workbook);
        
        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"STT", "Tiêu đề", "Mô tả", "Tần suất", "Thời gian", "Trạng thái", "Tiến độ"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Fill data
        int rowNum = 1;
        for (Reminder reminder : reminders) {
            Row row = sheet.createRow(rowNum++);
            
            row.createCell(0).setCellValue(rowNum - 1);
            row.createCell(1).setCellValue(reminder.getTitle());
            row.createCell(2).setCellValue(reminder.getDescription() != null ? reminder.getDescription() : "");
            row.createCell(3).setCellValue(getFrequencyName(reminder.getFrequency()));
            
            Cell dateCell = row.createCell(4);
            dateCell.setCellValue(DATE_FORMAT.format(new Date(reminder.getReminderTime())));
            dateCell.setCellStyle(dateStyle);
            
            row.createCell(5).setCellValue(reminder.isActive() ? "Đang hoạt động" : "Tạm dừng");
            row.createCell(6).setCellValue(String.format("%d/%d (%d%%)", 
                reminder.getCompletedCount(), 
                reminder.getTotalExpected(),
                reminder.getProgressPercentage()));
        }
        
        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
        
        // Save file
        File file = createReportFile(context, "Reminders");
        FileOutputStream outputStream = new FileOutputStream(file);
        workbook.write(outputStream);
        workbook.close();
        outputStream.close();
        
        return file;
    }
    
    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
    
    private static CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("dd/mm/yyyy hh:mm"));
        return style;
    }
    
    private static File createReportFile(Context context, String prefix) {
        File dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "HealthReports");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String fileName = prefix + "_" + FILE_DATE_FORMAT.format(new Date()) + ".xlsx";
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
