package com.example.lostfound.service.parser;

import com.example.lostfound.exception.UnsupportedFileTypeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class FileParsingStrategyFactory {
    
    private final List<FileParsingStrategy> strategies;
    
    @Autowired
    public FileParsingStrategyFactory(List<FileParsingStrategy> strategies) {
        this.strategies = strategies;
        log.info("Initialized FileParsingStrategyFactory with {} strategies: {}", 
                strategies.size(), 
                strategies.stream().map(FileParsingStrategy::getStrategyName).toList());
    }
    
    /**
     * Get the appropriate parsing strategy for the given file
     * @param contentType MIME type of the file
     * @param filename name of the file
     * @return appropriate parsing strategy
     * @throws UnsupportedFileTypeException if no strategy supports the file type
     */
    public FileParsingStrategy getStrategy(String contentType, String filename) throws UnsupportedFileTypeException {
        log.debug("Looking for strategy for contentType: {}, filename: {}", contentType, filename);
        
        for (FileParsingStrategy strategy : strategies) {
            if (strategy.supports(contentType, filename)) {
                log.info("Selected strategy: {} for file: {}", strategy.getStrategyName(), filename);
                return strategy;
            }
        }
        
        String message = String.format("No parsing strategy found for file type: %s (filename: %s)", 
                                     contentType, filename);
        log.error(message);
        throw new UnsupportedFileTypeException(message);
    }
    
    /**
     * Get all available strategies
     * @return list of all registered strategies
     */
    public List<FileParsingStrategy> getAllStrategies() {
        return List.copyOf(strategies);
    }
    
    /**
     * Get supported file types
     * @return list of supported file type descriptions
     */
    public List<String> getSupportedFileTypes() {
        return strategies.stream()
                .map(FileParsingStrategy::getStrategyName)
                .toList();
    }
} 