package com.example.healthylifehub.patterns.strategy;

import java.io.OutputStream;

/**
 * Strategy Pattern - Export Strategy Interface
 * Defines contract for different export formats (PDF, Excel, CSV)
 */
public interface ExportStrategy {
    void export(ExportData data, OutputStream output) throws Exception;
    String getFileExtension();
    String getMimeType();
}
