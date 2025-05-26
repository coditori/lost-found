package com.example.lostfound.controller;

import com.example.lostfound.dto.ClaimDto;
import com.example.lostfound.dto.LostItemDto;
import com.example.lostfound.entity.ClaimStatus;
import com.example.lostfound.exception.FileParsingException;
import com.example.lostfound.exception.UnsupportedFileTypeException;
import com.example.lostfound.service.ClaimService;
import com.example.lostfound.service.LostItemService;
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
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
@EnableMethodSecurity
@DisplayName("Admin Controller Tests")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LostItemService lostItemService;

    @MockBean
    private ClaimService claimService;

    private static final LostItemDto SAMPLE_ITEM = LostItemDto.builder()
            .id(1L)
            .itemName("Laptop")
            .quantity(2)
            .remainingQuantity(2)
            .place("Library")
            .description("Test item")
            .createdAt(LocalDateTime.now())
            .isAvailable(true)
            .build();

    private static final ClaimDto SAMPLE_CLAIM = ClaimDto.builder()
            .id(1L)
            .userId(1L)
            .userName("John Doe")
            .lostItemId(1L)
            .itemName("Laptop")
            .place("Library")
            .claimedQuantity(1)
            .claimDate(LocalDateTime.now())
            .status(ClaimStatus.PENDING)
            .notes("Test claim")
            .build();

    @Nested
    @DisplayName("File Upload Tests")
    class FileUploadTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should upload PDF file successfully")
        void shouldUploadPdfSuccessfully() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.pdf", "application/pdf", "test content".getBytes());
            when(lostItemService.uploadAndParseFile(any())).thenReturn(List.of(SAMPLE_ITEM));

            mockMvc.perform(multipart("/api/admin/upload").file(file).with(csrf()))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message", is("File uploaded and processed successfully")))
                    .andExpect(jsonPath("$.itemsCount", is(1)))
                    .andExpect(jsonPath("$.items[0].itemName", is("Laptop")));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should handle file parsing errors")
        void shouldHandleFileParsingErrors() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "invalid.pdf", "application/pdf", "invalid".getBytes());
            when(lostItemService.uploadAndParseFile(any()))
                    .thenThrow(new FileParsingException("No valid items found"));

            mockMvc.perform(multipart("/api/admin/upload").file(file).with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error", is("File Parsing Error")));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should reject unsupported file types")
        void shouldRejectUnsupportedFileTypes() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.txt", "text/plain", "content".getBytes());
            when(lostItemService.uploadAndParseFile(any()))
                    .thenThrow(new UnsupportedFileTypeException("Unsupported file type"));

            mockMvc.perform(multipart("/api/admin/upload").file(file).with(csrf()))
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(jsonPath("$.error", is("Unsupported File Type")));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should deny access to non-admin users")
        void shouldDenyNonAdminAccess() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.pdf", "application/pdf", "content".getBytes());

            mockMvc.perform(multipart("/api/admin/upload").file(file).with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should require authentication")
        void shouldRequireAuthentication() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.pdf", "application/pdf", "content".getBytes());

            mockMvc.perform(multipart("/api/admin/upload").file(file).with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Claims Management Tests")
    class ClaimsTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should retrieve claims with pagination")
        void shouldRetrieveClaimsWithPagination() throws Exception {
            Page<ClaimDto> claimsPage = new PageImpl<>(List.of(SAMPLE_CLAIM), PageRequest.of(0, 20), 1);
            when(claimService.getAllClaims(any(Pageable.class))).thenReturn(claimsPage);

            mockMvc.perform(get("/api/admin/claims"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].userName", is("John Doe")))
                    .andExpect(jsonPath("$.content[0].status", is("PENDING")))
                    .andExpect(jsonPath("$.totalElements", is(1)));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should handle empty claims result")
        void shouldHandleEmptyClaimsResult() throws Exception {
            Page<ClaimDto> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
            when(claimService.getAllClaims(any(Pageable.class))).thenReturn(emptyPage);

            mockMvc.perform(get("/api/admin/claims"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements", is(0)));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should deny access to non-admin users")
        void shouldDenyNonAdminAccess() throws Exception {
            mockMvc.perform(get("/api/admin/claims"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should require authentication")
        void shouldRequireAuthentication() throws Exception {
            mockMvc.perform(get("/api/admin/claims"))
                    .andExpect(status().isUnauthorized());
        }
    }
}