 package com.example.lostfound.controller;

import com.example.lostfound.dto.ClaimDto;
import com.example.lostfound.dto.LostItemDto;
import com.example.lostfound.entity.ClaimStatus;
import com.example.lostfound.exception.FileParsingException;
import com.example.lostfound.exception.UnsupportedFileTypeException;
import com.example.lostfound.service.ClaimService;
import com.example.lostfound.service.LostItemService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@DisplayName("Admin Controller Tests")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LostItemService lostItemService;

    @MockBean
    private ClaimService claimService;

    @Autowired
    private ObjectMapper objectMapper;

    private LostItemDto sampleLostItem;
    private ClaimDto sampleClaim;

    @BeforeEach
    void setUp() {
        sampleLostItem = LostItemDto.builder()
                .id(1L)
                .itemName("Laptop")
                .quantity(2)
                .remainingQuantity(2)
                .place("Library")
                .description("Imported from PDF Parser")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isAvailable(true)
                .build();

        sampleClaim = ClaimDto.builder()
                .id(1L)
                .userId(1L)
                .userName("John Doe")
                .lostItemId(1L)
                .itemName("Laptop")
                .place("Library")
                .claimedQuantity(1)
                .claimDate(LocalDateTime.now())
                .status(ClaimStatus.PENDING)
                .notes("I think this is mine")
                .build();
    }

    @Nested
    @DisplayName("File Upload Tests")
    class FileUploadTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should upload PDF file successfully")
        void shouldUploadPdfFileSuccessfully() throws Exception {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "lost_items.pdf",
                    "application/pdf",
                    "Item Name: Laptop\nQuantity: 1\nPlace: Library".getBytes()
            );

            List<LostItemDto> expectedItems = List.of(sampleLostItem);
            when(lostItemService.uploadAndParseFile(any())).thenReturn(expectedItems);

            // When & Then
            mockMvc.perform(multipart("/api/admin/upload")
                            .file(file)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message", is("File uploaded and processed successfully")))
                    .andExpect(jsonPath("$.itemsCount", is(1)))
                    .andExpect(jsonPath("$.items", hasSize(1)))
                    .andExpect(jsonPath("$.items[0].id", is(1)))
                    .andExpect(jsonPath("$.items[0].itemName", is("Laptop")))
                    .andExpect(jsonPath("$.items[0].place", is("Library")));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 400 when file parsing fails")
        void shouldReturn400WhenFileParsingFails() throws Exception {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "invalid.pdf",
                    "application/pdf",
                    "Invalid content".getBytes()
            );

            when(lostItemService.uploadAndParseFile(any()))
                    .thenThrow(new FileParsingException("No valid items found in the file"));

            // When & Then
            mockMvc.perform(multipart("/api/admin/upload")
                            .file(file)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error", is("File Parsing Error")))
                    .andExpect(jsonPath("$.message", is("No valid items found in the file")));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 415 for unsupported file type")
        void shouldReturn415ForUnsupportedFileType() throws Exception {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "document.txt",
                    "text/plain",
                    "Some text content".getBytes()
            );

            when(lostItemService.uploadAndParseFile(any()))
                    .thenThrow(new UnsupportedFileTypeException("No parsing strategy found for file type: text/plain"));

            // When & Then
            mockMvc.perform(multipart("/api/admin/upload")
                            .file(file)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(jsonPath("$.error", is("Unsupported File Type")))
                    .andExpect(jsonPath("$.message", containsString("No parsing strategy found")));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return 403 for non-admin user")
        void shouldReturn403ForNonAdminUser() throws Exception {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "lost_items.pdf",
                    "application/pdf",
                    "Item Name: Laptop\nQuantity: 1\nPlace: Library".getBytes()
            );

            // When & Then
            mockMvc.perform(multipart("/api/admin/upload")
                            .file(file)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 401 for unauthenticated user")
        void shouldReturn401ForUnauthenticatedUser() throws Exception {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "lost_items.pdf",
                    "application/pdf",
                    "Item Name: Laptop\nQuantity: 1\nPlace: Library".getBytes()
            );

            // When & Then
            mockMvc.perform(multipart("/api/admin/upload")
                            .file(file)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Get All Claims Tests")
    class GetAllClaimsTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should retrieve claims with default pagination")
        void shouldRetrieveClaimsWithDefaultPagination() throws Exception {
            // Given
            List<ClaimDto> claims = List.of(sampleClaim);
            Page<ClaimDto> claimsPage = new PageImpl<>(claims, PageRequest.of(0, 20), 1);
            when(claimService.getAllClaims(any(Pageable.class))).thenReturn(claimsPage);

            // When & Then
            mockMvc.perform(get("/api/admin/claims"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].id", is(1)))
                    .andExpect(jsonPath("$.content[0].userName", is("John Doe")))
                    .andExpect(jsonPath("$.content[0].itemName", is("Laptop")))
                    .andExpect(jsonPath("$.content[0].status", is("PENDING")))
                    .andExpect(jsonPath("$.totalElements", is(1)))
                    .andExpect(jsonPath("$.size", is(20)))
                    .andExpect(jsonPath("$.number", is(0)));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should retrieve claims with custom pagination")
        void shouldRetrieveClaimsWithCustomPagination() throws Exception {
            // Given
            List<ClaimDto> claims = List.of(sampleClaim);
            Page<ClaimDto> claimsPage = new PageImpl<>(claims, PageRequest.of(1, 5), 10);
            when(claimService.getAllClaims(any(Pageable.class))).thenReturn(claimsPage);

            // When & Then
            mockMvc.perform(get("/api/admin/claims")
                            .param("page", "1")
                            .param("size", "5")
                            .param("sort", "claimDate,desc"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.totalElements", is(10)))
                    .andExpect(jsonPath("$.size", is(5)))
                    .andExpect(jsonPath("$.number", is(1)));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should retrieve empty claims page")
        void shouldRetrieveEmptyClaimsPage() throws Exception {
            // Given
            Page<ClaimDto> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
            when(claimService.getAllClaims(any(Pageable.class))).thenReturn(emptyPage);

            // When & Then
            mockMvc.perform(get("/api/admin/claims"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements", is(0)));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return 403 for non-admin user")
        void shouldReturn403ForNonAdminUser() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/admin/claims"))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 401 for unauthenticated user")
        void shouldReturn401ForUnauthenticatedUser() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/admin/claims"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }
}