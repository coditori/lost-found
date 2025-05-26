package com.example.lostfound.service.parser;

import com.example.lostfound.entity.LostItem;
import com.example.lostfound.exception.FileParsingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class PdfParsingStrategy extends AbstractFileParsingStrategy {

    // Structured patterns for key-value format
    private static final Pattern ITEM_NAME_PATTERN = Pattern.compile(
            "^\\s*Item\\s*Name\\s*:\\s*(.+?)\\s*$", Pattern.CASE_INSENSITIVE
    );
    private static final Pattern QUANTITY_PATTERN = Pattern.compile(
            "^\\s*Quantity\\s*:\\s*(\\d+)\\s*$", Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PLACE_PATTERN = Pattern.compile(
            "^\\s*Place\\s*:\\s*(.+?)\\s*$", Pattern.CASE_INSENSITIVE
    );

    @Override
    protected List<LostItem> doParseFile(MultipartFile file) throws Exception {
        String text = extractTextFromPdf(file);
        return parseStructuredFormat(text);
    }

    @Override
    protected List<String> getSupportedExtensions() {
        return List.of(".pdf");
    }

    @Override
    protected List<String> getSupportedMimeTypes() {
        return List.of("application/pdf");
    }

    @Override
    public String getStrategyName() {
        return "PDF Parser";
    }

    private String extractTextFromPdf(MultipartFile file) throws IOException, FileParsingException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            if (document.getNumberOfPages() == 0) {
                throw new FileParsingException("PDF file is empty or corrupted");
            }
            
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            
            if (text == null || text.trim().isEmpty()) {
                throw new FileParsingException("No text content found in PDF");
            }
            
            log.debug("Extracted {} characters from PDF", text.length());
            return text;
        }
    }

    private List<LostItem> parseStructuredFormat(String text) throws FileParsingException {
        List<LostItem> items = new ArrayList<>();
        String[] lines = text.split("\n");
        
        String currentItemName = null;
        Integer currentQuantity = null;
        String currentPlace = null;
        
        for (String line : lines) {
            line = line.trim();
            
            if (line.isEmpty() || isHeaderLine(line)) {
                continue;
            }
            
            // Try to match item name
            Matcher itemNameMatcher = ITEM_NAME_PATTERN.matcher(line);
            if (itemNameMatcher.matches()) {
                // If we have a complete item, save it
                if (currentItemName != null && currentQuantity != null && currentPlace != null) {
                    try {
                        validateItemData(currentItemName, currentQuantity, currentPlace);
                        items.add(createLostItem(currentItemName, currentQuantity, currentPlace));
                        log.debug("Parsed item: {} (qty: {}, place: {})", 
                                 currentItemName, currentQuantity, currentPlace);
                    } catch (Exception e) {
                        log.debug("Failed to create item: {} - {}", currentItemName, e.getMessage());
                    }
                }
                // Start new item
                currentItemName = itemNameMatcher.group(1).trim();
                currentQuantity = null;
                currentPlace = null;
                continue;
            }
            
            // Try to match quantity
            Matcher quantityMatcher = QUANTITY_PATTERN.matcher(line);
            if (quantityMatcher.matches()) {
                try {
                    currentQuantity = Integer.parseInt(quantityMatcher.group(1).trim());
                } catch (NumberFormatException e) {
                    log.debug("Invalid quantity: {}", quantityMatcher.group(1));
                }
                continue;
            }
            
            // Try to match place
            Matcher placeMatcher = PLACE_PATTERN.matcher(line);
            if (placeMatcher.matches()) {
                currentPlace = placeMatcher.group(1).trim();
                continue;
            }
        }
        
        // Handle the last item
        if (currentItemName != null && currentQuantity != null && currentPlace != null) {
            try {
                validateItemData(currentItemName, currentQuantity, currentPlace);
                items.add(createLostItem(currentItemName, currentQuantity, currentPlace));
                log.debug("Parsed item: {} (qty: {}, place: {})", 
                         currentItemName, currentQuantity, currentPlace);
            } catch (Exception e) {
                log.debug("Failed to create last item: {} - {}", currentItemName, e.getMessage());
            }
        }
        
        if (items.isEmpty()) {
            throw new FileParsingException(
                "No valid items found. Expected format:\n" +
                "Item Name: Laptop\n" +
                "Quantity: 1\n" +
                "Place: Library\n"
            );
        }
        
        return items;
    }

    private boolean isHeaderLine(String line) {
        String lower = line.toLowerCase();
        return lower.contains("lost items") || lower.contains("report")
            || lower.matches("^[\\s\\-=]+$");  // Lines with only spaces, dashes, or equals
    }
} 