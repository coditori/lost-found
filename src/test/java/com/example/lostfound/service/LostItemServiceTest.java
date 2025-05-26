package com.example.lostfound.service;

import com.example.lostfound.dto.LostItemDto;
import com.example.lostfound.entity.LostItem;
import com.example.lostfound.exception.FileParsingException;
import com.example.lostfound.exception.UnsupportedFileTypeException;
import com.example.lostfound.repository.LostItemRepository;
import com.example.lostfound.service.parser.FileParsingStrategy;
import com.example.lostfound.service.parser.FileParsingStrategyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LostItemServiceTest {

    @Mock
    private LostItemRepository lostItemRepository;

    @Mock
    private FileParsingStrategyFactory parsingStrategyFactory;

    @Mock
    private FileParsingStrategy fileParsingStrategy;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private LostItemService lostItemService;

    private LostItem testLostItem1;
    private LostItem testLostItem2;
    private List<LostItem> testLostItems;

    @BeforeEach
    void setUp() {
        testLostItem1 = LostItem.builder()
                .id(1L)
                .itemName("Laptop")
                .quantity(3)
                .remainingQuantity(2)
                .place("Library")
                .description("Dell Laptop")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .version(1L)
                .build();

        testLostItem2 = LostItem.builder()
                .id(2L)
                .itemName("Phone")
                .quantity(2)
                .remainingQuantity(1)
                .place("Cafeteria")
                .description("iPhone")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .version(1L)
                .build();

        testLostItems = List.of(testLostItem1, testLostItem2);
    }

    @Test
    void uploadAndParseFile_Success() throws Exception {
        // Given
        String filename = "test.pdf";
        String contentType = "application/pdf";
        
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn(filename);
        when(multipartFile.getContentType()).thenReturn(contentType);
        when(multipartFile.getSize()).thenReturn(1024L);
        
        when(parsingStrategyFactory.getStrategy(contentType, filename)).thenReturn(fileParsingStrategy);
        when(fileParsingStrategy.parseFile(multipartFile)).thenReturn(testLostItems);
        when(lostItemRepository.saveAll(testLostItems)).thenReturn(testLostItems);

        // When
        List<LostItemDto> result = lostItemService.uploadAndParseFile(multipartFile);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        
        LostItemDto item1 = result.get(0);
        assertThat(item1.getId()).isEqualTo(1L);
        assertThat(item1.getItemName()).isEqualTo("Laptop");
        assertThat(item1.getQuantity()).isEqualTo(3);
        assertThat(item1.getRemainingQuantity()).isEqualTo(2);
        assertThat(item1.getPlace()).isEqualTo("Library");
        assertThat(item1.getDescription()).isEqualTo("Dell Laptop");
        assertThat(item1.isAvailable()).isTrue();

        LostItemDto item2 = result.get(1);
        assertThat(item2.getId()).isEqualTo(2L);
        assertThat(item2.getItemName()).isEqualTo("Phone");
        assertThat(item2.getQuantity()).isEqualTo(2);
        assertThat(item2.getRemainingQuantity()).isEqualTo(1);
        assertThat(item2.getPlace()).isEqualTo("Cafeteria");
        assertThat(item2.getDescription()).isEqualTo("iPhone");
        assertThat(item2.isAvailable()).isTrue();

        verify(multipartFile).isEmpty();
        verify(multipartFile, atLeastOnce()).getOriginalFilename();
        verify(multipartFile).getContentType();
        verify(parsingStrategyFactory).getStrategy(contentType, filename);
        verify(fileParsingStrategy).parseFile(multipartFile);
        verify(lostItemRepository).saveAll(testLostItems);
    }

    @Test
    void uploadAndParseFile_EmptyFile() {
        // Given
        when(multipartFile.isEmpty()).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> lostItemService.uploadAndParseFile(multipartFile))
                .isInstanceOf(FileParsingException.class)
                .hasMessage("File is empty");

        verify(multipartFile).isEmpty();
        verifyNoInteractions(parsingStrategyFactory, fileParsingStrategy, lostItemRepository);
    }

    @Test
    void uploadAndParseFile_UnsupportedFileType() throws Exception {
        // Given
        String filename = "test.txt";
        String contentType = "text/plain";
        
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn(filename);
        when(multipartFile.getContentType()).thenReturn(contentType);
        when(multipartFile.getSize()).thenReturn(1024L);
        
        when(parsingStrategyFactory.getStrategy(contentType, filename))
                .thenThrow(new UnsupportedFileTypeException("Unsupported file type: text/plain"));

        // When & Then
        assertThatThrownBy(() -> lostItemService.uploadAndParseFile(multipartFile))
                .isInstanceOf(UnsupportedFileTypeException.class)
                .hasMessage("Unsupported file type: text/plain");

        verify(multipartFile).isEmpty();
        verify(multipartFile, atLeastOnce()).getOriginalFilename();
        verify(multipartFile).getContentType();
        verify(parsingStrategyFactory).getStrategy(contentType, filename);
        verifyNoInteractions(fileParsingStrategy, lostItemRepository);
    }

    @Test
    void uploadAndParseFile_ParsingFailure() throws Exception {
        // Given
        String filename = "test.pdf";
        String contentType = "application/pdf";
        
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn(filename);
        when(multipartFile.getContentType()).thenReturn(contentType);
        when(multipartFile.getSize()).thenReturn(1024L);
        
        when(parsingStrategyFactory.getStrategy(contentType, filename)).thenReturn(fileParsingStrategy);
        when(fileParsingStrategy.parseFile(multipartFile))
                .thenThrow(new FileParsingException("Failed to parse PDF file"));

        // When & Then
        assertThatThrownBy(() -> lostItemService.uploadAndParseFile(multipartFile))
                .isInstanceOf(FileParsingException.class)
                .hasMessage("Failed to parse PDF file");

        verify(multipartFile).isEmpty();
        verify(multipartFile, atLeastOnce()).getOriginalFilename();
        verify(multipartFile).getContentType();
        verify(parsingStrategyFactory).getStrategy(contentType, filename);
        verify(fileParsingStrategy).parseFile(multipartFile);
        verifyNoInteractions(lostItemRepository);
    }

    @Test
    void uploadAndParseFile_NoItemsParsed() throws Exception {
        // Given
        String filename = "empty.pdf";
        String contentType = "application/pdf";
        List<LostItem> emptyList = new ArrayList<>();
        
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn(filename);
        when(multipartFile.getContentType()).thenReturn(contentType);
        when(multipartFile.getSize()).thenReturn(1024L);
        
        when(parsingStrategyFactory.getStrategy(contentType, filename)).thenReturn(fileParsingStrategy);
        when(fileParsingStrategy.parseFile(multipartFile)).thenReturn(emptyList);

        // When & Then
        assertThatThrownBy(() -> lostItemService.uploadAndParseFile(multipartFile))
                .isInstanceOf(FileParsingException.class)
                .hasMessage("No valid items found in the file");

        verify(multipartFile).isEmpty();
        verify(multipartFile, atLeastOnce()).getOriginalFilename();
        verify(multipartFile).getContentType();
        verify(parsingStrategyFactory).getStrategy(contentType, filename);
        verify(fileParsingStrategy).parseFile(multipartFile);
        verifyNoInteractions(lostItemRepository);
    }

    @Test
    void uploadAndParseFile_DatabaseSaveFailure() throws Exception {
        // Given
        String filename = "test.pdf";
        String contentType = "application/pdf";
        
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn(filename);
        when(multipartFile.getContentType()).thenReturn(contentType);
        when(multipartFile.getSize()).thenReturn(1024L);
        
        when(parsingStrategyFactory.getStrategy(contentType, filename)).thenReturn(fileParsingStrategy);
        when(fileParsingStrategy.parseFile(multipartFile)).thenReturn(testLostItems);
        when(lostItemRepository.saveAll(testLostItems))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        assertThatThrownBy(() -> lostItemService.uploadAndParseFile(multipartFile))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database connection failed");

        verify(multipartFile).isEmpty();
        verify(multipartFile, atLeastOnce()).getOriginalFilename();
        verify(multipartFile).getContentType();
        verify(parsingStrategyFactory).getStrategy(contentType, filename);
        verify(fileParsingStrategy).parseFile(multipartFile);
        verify(lostItemRepository).saveAll(testLostItems);
    }

    @Test
    void getAvailableItems_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<LostItem> availableItems = List.of(testLostItem1, testLostItem2);
        Page<LostItem> itemPage = new PageImpl<>(availableItems, pageable, 2);

        when(lostItemRepository.findByRemainingQuantityGreaterThan(0, pageable)).thenReturn(itemPage);

        // When
        Page<LostItemDto> result = lostItemService.getAvailableItems(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);

        LostItemDto item1 = result.getContent().get(0);
        assertThat(item1.getId()).isEqualTo(1L);
        assertThat(item1.getItemName()).isEqualTo("Laptop");
        assertThat(item1.getRemainingQuantity()).isEqualTo(2);
        assertThat(item1.isAvailable()).isTrue();

        LostItemDto item2 = result.getContent().get(1);
        assertThat(item2.getId()).isEqualTo(2L);
        assertThat(item2.getItemName()).isEqualTo("Phone");
        assertThat(item2.getRemainingQuantity()).isEqualTo(1);
        assertThat(item2.isAvailable()).isTrue();

        verify(lostItemRepository).findByRemainingQuantityGreaterThan(0, pageable);
    }

    @Test
    void getAvailableItems_EmptyPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<LostItem> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(lostItemRepository.findByRemainingQuantityGreaterThan(0, pageable)).thenReturn(emptyPage);

        // When
        Page<LostItemDto> result = lostItemService.getAvailableItems(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);

        verify(lostItemRepository).findByRemainingQuantityGreaterThan(0, pageable);
    }

    @Test
    void getAvailableItems_UnavailableItems() {
        // Given
        Pageable pageable = PageRequest.of(1, 5);
        
        // Create item with no remaining quantity (unavailable)
        LostItem unavailableItem = LostItem.builder()
                .id(3L)
                .itemName("Tablet")
                .quantity(1)
                .remainingQuantity(0)
                .place("Lab")
                .description("iPad")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Only available items should be returned
        List<LostItem> availableItems = List.of(testLostItem1);
        Page<LostItem> itemPage = new PageImpl<>(availableItems, pageable, 1);

        when(lostItemRepository.findByRemainingQuantityGreaterThan(0, pageable)).thenReturn(itemPage);

        // When
        Page<LostItemDto> result = lostItemService.getAvailableItems(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(5);

        LostItemDto item = result.getContent().get(0);
        assertThat(item.getId()).isNotNull();
        assertThat(item.getItemName()).isEqualTo("Laptop");
        assertThat(item.getRemainingQuantity()).isGreaterThan(0);
        assertThat(item.isAvailable()).isTrue();

        verify(lostItemRepository).findByRemainingQuantityGreaterThan(0, pageable);
    }

    @Test
    void getAvailableItems_DifferentPageSizes() {
        // Given
        Pageable smallPageable = PageRequest.of(0, 1);
        List<LostItem> singleItem = List.of(testLostItem1);
        Page<LostItem> singleItemPage = new PageImpl<>(singleItem, smallPageable, 2);

        when(lostItemRepository.findByRemainingQuantityGreaterThan(0, smallPageable)).thenReturn(singleItemPage);

        // When
        Page<LostItemDto> result = lostItemService.getAvailableItems(smallPageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(2);

        verify(lostItemRepository).findByRemainingQuantityGreaterThan(0, smallPageable);
    }

    @Test
    void uploadAndParseFile_SingleItem() throws Exception {
        // Given
        String filename = "single.pdf";
        String contentType = "application/pdf";
        List<LostItem> singleItem = List.of(testLostItem1);
        
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn(filename);
        when(multipartFile.getContentType()).thenReturn(contentType);
        when(multipartFile.getSize()).thenReturn(512L);
        
        when(parsingStrategyFactory.getStrategy(contentType, filename)).thenReturn(fileParsingStrategy);
        when(fileParsingStrategy.parseFile(multipartFile)).thenReturn(singleItem);
        when(lostItemRepository.saveAll(singleItem)).thenReturn(singleItem);

        // When
        List<LostItemDto> result = lostItemService.uploadAndParseFile(multipartFile);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        
        LostItemDto item = result.get(0);
        assertThat(item.getId()).isEqualTo(1L);
        assertThat(item.getItemName()).isEqualTo("Laptop");
        assertThat(item.getQuantity()).isEqualTo(3);
        assertThat(item.getRemainingQuantity()).isEqualTo(2);

        verify(parsingStrategyFactory).getStrategy(contentType, filename);
        verify(fileParsingStrategy).parseFile(multipartFile);
        verify(lostItemRepository).saveAll(singleItem);
    }
} 