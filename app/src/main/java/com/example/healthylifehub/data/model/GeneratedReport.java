package com.example.healthylifehub.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedReport {
    private String title;
    private String info;
    private String date;
    private ReportType type;

    public enum ReportType {
        PDF,
        EXCEL
    }

}
