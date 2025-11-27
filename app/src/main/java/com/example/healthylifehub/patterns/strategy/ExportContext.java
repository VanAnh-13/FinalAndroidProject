package com.example.healthylifehub.patterns.strategy;

import java.io.OutputStream;

/**
 * Strategy Pattern - Context class
 * Uses different export strategies interchangeably
 */
public class ExportContext {
    private ExportStrategy strategy;

    public ExportContext() {
        // Default to PDF
        this.strategy = new PdfExportStrategy();
    }

    public ExportContext(ExportStrategy strategy) {
        this.strategy = strategy;
    }

    public void setStrategy(ExportStrategy strategy) {
        this.strategy = strategy;
    }

    public void executeExport(ExportData data, OutputStream output) throws Exception {
        strategy.export(data, output);
    }

    public String getFileExtension() {
        return strategy.getFileExtension();
    }

    public String getMimeType() {
        return strategy.getMimeType();
    }
}
