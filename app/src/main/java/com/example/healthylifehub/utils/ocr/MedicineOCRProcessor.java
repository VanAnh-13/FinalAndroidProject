package com.example.healthylifehub.utils.ocr;

import android.graphics.Bitmap;

import com.example.healthylifehub.data.model.Medicine;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MedicineOCRProcessor handles OCR processing of prescription images using ML Kit.
 * Uses a single-thread executor for image preprocessing and ML Kit for text recognition.
 */
public class MedicineOCRProcessor {
    
    private final TextRecognizer recognizer;
    private final ExecutorService executor;
    
    public MedicineOCRProcessor() {
        // Initialize ML Kit Text Recognizer with default Latin options
        this.recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        
        // Create single-thread executor for preprocessing
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Process a prescription image to extract medicine information.
     * 
     * @param image The prescription image bitmap
     * @return CompletableFuture containing list of extracted Medicine objects
     */
    public CompletableFuture<List<Medicine>> processPrescriptionImage(Bitmap image) {
        // Stage 1: Preprocess image on executor
        return CompletableFuture.supplyAsync(() -> preprocessImage(image), executor)
            
            // Stage 2: Run ML Kit OCR (convert Task to CompletableFuture)
            .thenCompose(processedImage -> {
                CompletableFuture<Text> ocrFuture = new CompletableFuture<>();
                
                com.google.mlkit.vision.common.InputImage inputImage = 
                    com.google.mlkit.vision.common.InputImage.fromBitmap(processedImage, 0);
                
                recognizer.process(inputImage)
                    .addOnSuccessListener(ocrFuture::complete)
                    .addOnFailureListener(ocrFuture::completeExceptionally);
                
                return ocrFuture;
            })
            
            // Stage 3: Parse medicine info with regex on executor
            .thenApplyAsync(text -> parseMedicineInfo(text), executor);
    }
    
    /**
     * Preprocess the image to improve OCR accuracy.
     * Converts to grayscale and enhances contrast.
     * 
     * @param image Original bitmap image
     * @return Processed bitmap image
     */
    private Bitmap preprocessImage(Bitmap image) {
        // Create a mutable copy of the bitmap
        Bitmap processedImage = image.copy(Bitmap.Config.ARGB_8888, true);
        
        int width = processedImage.getWidth();
        int height = processedImage.getHeight();
        
        // Convert to grayscale and enhance contrast
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixel = processedImage.getPixel(x, y);
                
                // Extract RGB components
                int red = (pixel >> 16) & 0xff;
                int green = (pixel >> 8) & 0xff;
                int blue = pixel & 0xff;
                
                // Convert to grayscale using luminosity method
                int gray = (int) (0.299 * red + 0.587 * green + 0.114 * blue);
                
                // Enhance contrast (simple linear stretch)
                // This increases the difference between light and dark pixels
                gray = Math.min(255, Math.max(0, (int) ((gray - 128) * 1.5 + 128)));
                
                // Set the new pixel value (grayscale)
                int newPixel = (0xff << 24) | (gray << 16) | (gray << 8) | gray;
                processedImage.setPixel(x, y, newPixel);
            }
        }
        
        return processedImage;
    }
    
    /**
     * Parse medicine information from OCR text using regex patterns.
     * Extracts medicine names and dosages.
     * 
     * @param text ML Kit Text object containing recognized text
     * @return List of Medicine objects extracted from the text
     */
    private List<Medicine> parseMedicineInfo(Text text) {
        java.util.List<Medicine> medicines = new java.util.ArrayList<>();
        
        // Regex pattern to match medicine name followed by dosage
        // Pattern: word(s) followed by number with mg or IU
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "([A-Za-z]+)\\s+(\\d+\\s*mg|\\d+\\s*IU)",
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        
        // Process each text block from OCR result
        for (Text.TextBlock block : text.getTextBlocks()) {
            String blockText = block.getText();
            java.util.regex.Matcher matcher = pattern.matcher(blockText);
            
            while (matcher.find()) {
                String medicineName = matcher.group(1);
                String dosage = matcher.group(2);
                
                // Create Medicine object with extracted information
                // Set status to ACTIVE by default for newly scanned medicines
                Medicine medicine = new Medicine(
                    medicineName,
                    dosage,
                    null,  // frequency - to be filled by user
                    null,  // instructions - to be filled by user
                    null,  // startDate - to be filled by user
                    Medicine.MedicineStatus.ACTIVE
                );
                
                medicines.add(medicine);
            }
        }
        
        return medicines;
    }
    
    /**
     * Cleanup method to shutdown the executor service.
     * Should be called when the processor is no longer needed.
     */
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
