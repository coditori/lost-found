package com.example.lostfound.service.parser;

import com.example.lostfound.entity.LostItem;
import com.example.lostfound.exception.FileParsingException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileParsingStrategy {
    
    /**
     * Parse the uploaded file and extract lost items
     * @param file the uploaded file
     * @return list of parsed lost items
     * @throws FileParsingException if parsing fails
     */
    List<LostItem> parseFile(MultipartFile file) throws FileParsingException;
    
    /**
     * Check if this strategy supports the given file type
     * @param contentType the MIME type of the file
     * @param filename the name of the file
     * @return true if this strategy can handle the file
     */
    boolean supports(String contentType, String filename);
    
    /**
     * Get the name of this parsing strategy
     * @return strategy name
     */
    String getStrategyName();
} 