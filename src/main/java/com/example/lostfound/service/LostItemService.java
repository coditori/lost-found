package com.example.lostfound.service;

import com.example.lostfound.dto.LostItemDto;
import com.example.lostfound.entity.LostItem;
import com.example.lostfound.repository.LostItemRepository;
import com.example.lostfound.exception.FileParsingException;
import com.example.lostfound.service.parser.FileParsingStrategy;
import com.example.lostfound.service.parser.FileParsingStrategyFactory;
import com.example.lostfound.exception.UnsupportedFileTypeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LostItemService {
    
    private final LostItemRepository lostItemRepository;
    private final FileParsingStrategyFactory parsingStrategyFactory;
    
    @Transactional
    public List<LostItemDto> uploadAndParseFile(MultipartFile file) 
            throws UnsupportedFileTypeException, FileParsingException {
        
        log.info("Processing file upload: {} (size: {} bytes)", 
                file.getOriginalFilename(), file.getSize());
        
        if (file.isEmpty()) {
            throw new FileParsingException("File is empty");
        }
        
        FileParsingStrategy strategy = parsingStrategyFactory.getStrategy(
                file.getContentType(), file.getOriginalFilename());
        
        List<LostItem> parsedItems = strategy.parseFile(file);
        
        if (parsedItems.isEmpty()) {
            log.warn("No items were parsed from file: {}", file.getOriginalFilename());
            throw new FileParsingException("No valid items found in the file");
        }
        
        List<LostItem> savedItems = lostItemRepository.saveAll(parsedItems);
        log.info("Successfully saved {} items from file: {}", 
                savedItems.size(), file.getOriginalFilename());
        
        return savedItems.stream()
                .map(this::convertToDto)
                .toList();
    }
    
    public Page<LostItemDto> getAvailableItems(Pageable pageable) {
        log.debug("Retrieving available items with pagination: {}", pageable);
        return lostItemRepository.findByRemainingQuantityGreaterThan(0, pageable)
                .map(this::convertToDto);
    }
    
    private LostItemDto convertToDto(LostItem item) {
        return LostItemDto.builder()
                .id(item.getId())
                .itemName(item.getItemName())
                .quantity(item.getQuantity())
                .remainingQuantity(item.getRemainingQuantity())
                .place(item.getPlace())
                .description(item.getDescription())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .isAvailable(item.isAvailable())
                .build();
    }
} 