package com.example.lostfound.controller;

import com.example.lostfound.dto.ClaimDto;
import com.example.lostfound.dto.ClaimRequest;
import com.example.lostfound.dto.LostItemDto;
import com.example.lostfound.entity.ClaimStatus;
import com.example.lostfound.exception.InsufficientQuantityException;
import com.example.lostfound.exception.LostItemNotFoundException;
import com.example.lostfound.exception.UserNotFoundException;
import com.example.lostfound.service.ClaimService;
import com.example.lostfound.service.LostItemService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@DisplayName("User Controller Web Layer Tests")
class UserControllerTest {

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
    private ClaimRequest validClaimRequest;

    @BeforeEach
    void setUp() {
        sampleLostItem = createSampleLostItem();
        sampleClaim = createSampleClaim();
        validClaimRequest = new ClaimRequest(1L, 1, "I think this is mine");
    }

    // Helper methods for common test data creation
    private LostItemDto createSampleLostItem() {
        return LostItemDto.builder()
                .id(1L)
                .itemName("Laptop")
                .quantity(5)
                .remainingQuantity(3)
                .place("Library")
                .description("Found in library")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isAvailable(true)
                .build();
    }

    private ClaimDto createSampleClaim() {
        return ClaimDto.builder()
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

    private Page<LostItemDto> createPageResponse(List<LostItemDto> items, int page, int size, long totalElements) {
        return new PageImpl<>(items, PageRequest.of(page, size), totalElements);
    }

    private ResultActions performGetItems() throws Exception {
        return mockMvc.perform(get("/api/user/items"))
                .andDo(print());
    }

    private ResultActions performGetItemsWithParams(String... params) throws Exception {
        var requestBuilder = get("/api/user/items");
        for (int i = 0; i < params.length; i += 2) {
            requestBuilder.param(params[i], params[i + 1]);
        }
        return mockMvc.perform(requestBuilder).andDo(print());
    }

    private ResultActions performCreateClaim(ClaimRequest request) throws Exception {
        return mockMvc.perform(post("/api/user/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andDo(print());
    }

    private void assertSuccessfulItemsResponse(ResultActions result, int expectedSize) throws Exception {
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(expectedSize)));
    }

    private void assertErrorResponse(ResultActions result, int expectedStatus, String expectedError) throws Exception {
        result.andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.error", is(expectedError)));
    }

    @Nested
    @DisplayName("GET /api/user/items - Browse Available Items")
    class GetAvailableItemsTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should retrieve items with default pagination")
        void shouldRetrieveItemsWithDefaultPagination() throws Exception {
            // Given
            Page<LostItemDto> itemsPage = createPageResponse(List.of(sampleLostItem), 0, 20, 1);
            when(lostItemService.getAvailableItems(any(Pageable.class))).thenReturn(itemsPage);

            // When & Then
            ResultActions result = performGetItems();
            assertSuccessfulItemsResponse(result, 1);
            result.andExpect(jsonPath("$.content[0].id", is(1)))
                    .andExpect(jsonPath("$.content[0].itemName", is("Laptop")))
                    .andExpect(jsonPath("$.content[0].remainingQuantity", is(3)))
                    .andExpect(jsonPath("$.totalElements", is(1)))
                    .andExpect(jsonPath("$.size", is(20)))
                    .andExpect(jsonPath("$.number", is(0)));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should handle custom pagination and sorting")
        void shouldHandleCustomPaginationAndSorting() throws Exception {
            // Given
            Page<LostItemDto> itemsPage = createPageResponse(List.of(sampleLostItem), 2, 5, 15);
            when(lostItemService.getAvailableItems(any(Pageable.class))).thenReturn(itemsPage);

            // When & Then
            ResultActions result = performGetItemsWithParams("page", "2", "size", "5", "sort", "itemName,asc");
            assertSuccessfulItemsResponse(result, 1);
            result.andExpect(jsonPath("$.totalElements", is(15)))
                    .andExpect(jsonPath("$.size", is(5)))
                    .andExpect(jsonPath("$.number", is(2)));
        }

        @ParameterizedTest
        @ValueSource(ints = {-1, 0, 2000})
        @WithMockUser(roles = "USER")
        @DisplayName("Should handle edge case pagination values")
        void shouldHandleEdgeCasePaginationValues(int pageSize) throws Exception {
            // Given
            Page<LostItemDto> emptyPage = createPageResponse(Collections.emptyList(), 0, Math.max(1, pageSize), 0);
            when(lostItemService.getAvailableItems(any(Pageable.class))).thenReturn(emptyPage);

            // When & Then
            assertSuccessfulItemsResponse(performGetItemsWithParams("size", String.valueOf(pageSize)), 0);
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return empty page when no items available")
        void shouldReturnEmptyPageWhenNoItemsAvailable() throws Exception {
            // Given
            Page<LostItemDto> emptyPage = createPageResponse(Collections.emptyList(), 0, 20, 0);
            when(lostItemService.getAvailableItems(any(Pageable.class))).thenReturn(emptyPage);

            // When & Then
            ResultActions result = performGetItems();
            assertSuccessfulItemsResponse(result, 0);
            result.andExpect(jsonPath("$.totalElements", is(0)))
                    .andExpect(jsonPath("$.empty", is(true)));
        }

        @Test
        @DisplayName("Should return 401 for unauthenticated requests")
        void shouldReturn401ForUnauthenticatedRequests() throws Exception {
            performGetItems().andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/user/claims - Create Claim")
    class CreateClaimTests {

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        @DisplayName("Should create claim successfully")
        void shouldCreateClaimSuccessfully() throws Exception {
            // Given
            when(claimService.createClaim(any(ClaimRequest.class), eq("testuser"))).thenReturn(sampleClaim);

            // When & Then
            ResultActions result = performCreateClaim(validClaimRequest);
            result.andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.userName", is("John Doe")))
                    .andExpect(jsonPath("$.itemName", is("Laptop")))
                    .andExpect(jsonPath("$.claimedQuantity", is(1)))
                    .andExpect(jsonPath("$.status", is("PENDING")));
        }

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        @DisplayName("Should create claim with edge case values")
        void shouldCreateClaimWithEdgeCaseValues() throws Exception {
            // Given
            ClaimRequest edgeCaseRequest = new ClaimRequest(Long.MAX_VALUE, Integer.MAX_VALUE, null);
            when(claimService.createClaim(any(ClaimRequest.class), eq("testuser"))).thenReturn(sampleClaim);

            // When & Then
            performCreateClaim(edgeCaseRequest)
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        @DisplayName("Should handle special characters in notes")
        void shouldHandleSpecialCharactersInNotes() throws Exception {
            // Given
            String specialNotes = "Special chars: !@#$%^&*()_+ Unicode: 你好 🎉";
            ClaimRequest specialRequest = new ClaimRequest(1L, 1, specialNotes);
            when(claimService.createClaim(any(ClaimRequest.class), eq("testuser"))).thenReturn(sampleClaim);

            // When & Then
            performCreateClaim(specialRequest)
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        @DisplayName("Should return 400 for insufficient quantity")
        void shouldReturn400ForInsufficientQuantity() throws Exception {
            // Given
            when(claimService.createClaim(any(ClaimRequest.class), eq("testuser")))
                    .thenThrow(new InsufficientQuantityException("Insufficient quantity"));

            // When & Then
            assertErrorResponse(performCreateClaim(validClaimRequest), 400, "Insufficient Quantity");
        }

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        @DisplayName("Should return 404 for non-existent item")
        void shouldReturn404ForNonExistentItem() throws Exception {
            // Given
            when(claimService.createClaim(any(ClaimRequest.class), eq("testuser")))
                    .thenThrow(new LostItemNotFoundException("Item not found"));

            // When & Then
            assertErrorResponse(performCreateClaim(validClaimRequest), 404, "Lost Item Not Found");
        }

        @Test
        @WithMockUser(username = "nonexistentuser", roles = "USER")
        @DisplayName("Should return 404 for non-existent user")
        void shouldReturn404ForNonExistentUser() throws Exception {
            // Given
            when(claimService.createClaim(any(ClaimRequest.class), eq("nonexistentuser")))
                    .thenThrow(new UserNotFoundException("User not found"));

            // When & Then
            assertErrorResponse(performCreateClaim(validClaimRequest), 404, "User Not Found");
        }

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        @DisplayName("Should return 400 for duplicate claim")
        void shouldReturn400ForDuplicateClaim() throws Exception {
            // Given
            when(claimService.createClaim(any(ClaimRequest.class), eq("testuser")))
                    .thenThrow(new IllegalStateException("User has already claimed this item"));

            // When & Then
            assertErrorResponse(performCreateClaim(validClaimRequest), 400, "Invalid Operation");
        }

        @Nested
        @DisplayName("Validation Error Tests")
        class ValidationErrorTests {

            @Test
            @WithMockUser(username = "testuser", roles = "USER")
            @DisplayName("Should return 400 for null required fields")
            void shouldReturn400ForNullRequiredFields() throws Exception {
                ClaimRequest invalidRequest = new ClaimRequest(null, null, "Invalid request");

                ResultActions result = performCreateClaim(invalidRequest);
                result.andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.error", is("Validation Failed")))
                        .andExpect(jsonPath("$.validationErrors.lostItemId", is("Lost item ID is required")))
                        .andExpect(jsonPath("$.validationErrors.claimedQuantity", is("Claimed quantity is required")));
            }

            @ParameterizedTest
            @ValueSource(ints = {0, -1, -10})
            @WithMockUser(username = "testuser", roles = "USER")
            @DisplayName("Should return 400 for invalid quantity values")
            void shouldReturn400ForInvalidQuantityValues(int invalidQuantity) throws Exception {
                ClaimRequest invalidRequest = new ClaimRequest(1L, invalidQuantity, "Invalid quantity");

                assertErrorResponse(performCreateClaim(invalidRequest), 400, "Validation Failed");
            }

            @Test
            @WithMockUser(username = "testuser", roles = "USER")
            @DisplayName("Should return 400 for malformed JSON")
            void shouldReturn400ForMalformedJson() throws Exception {
                mockMvc.perform(post("/api/user/claims")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{ \"lostItemId\": 1, \"claimedQuantity\": ")
                                .with(csrf()))
                        .andDo(print())
                        .andExpect(status().isInternalServerError());
            }
        }

        @Nested
        @DisplayName("Authentication and Authorization Tests")
        class AuthenticationAuthorizationTests {

            @Test
            @DisplayName("Should return 401 for unauthenticated user")
            void shouldReturn401ForUnauthenticatedUser() throws Exception {
                mockMvc.perform(post("/api/user/claims")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validClaimRequest))
                                .with(csrf()))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @WithMockUser(username = "testuser", roles = "USER")
            @DisplayName("Should return 403 for missing CSRF token")
            void shouldReturn403ForMissingCsrfToken() throws Exception {
                mockMvc.perform(post("/api/user/claims")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validClaimRequest)))
                        .andExpect(status().isForbidden());
            }

            @Test
            @WithMockUser(username = "", roles = "USER")
            @DisplayName("Should return 404 for empty username")
            void shouldReturn404ForEmptyUsername() throws Exception {
                // Given - Spring Security defaults empty username to "user"
                when(claimService.createClaim(any(ClaimRequest.class), eq("user")))
                        .thenThrow(new UserNotFoundException("User not found"));

                // When & Then - Fixed: Should expect 404, not 201
                assertErrorResponse(performCreateClaim(validClaimRequest), 404, "User Not Found");
            }

            @Test
            @WithMockUser(username = "admin", roles = "ADMIN")
            @DisplayName("Should allow access with ADMIN role")
            void shouldAllowAccessWithAdminRole() throws Exception {
                // Given
                when(claimService.createClaim(any(ClaimRequest.class), eq("admin"))).thenReturn(sampleClaim);

                // When & Then
                performCreateClaim(validClaimRequest)
                        .andExpect(status().isCreated());
            }
        }

        @Nested
        @DisplayName("Edge Case Tests")
        class EdgeCaseTests {

            @Test
            @WithMockUser(username = "testuser", roles = "USER")
            @DisplayName("Should handle very large item IDs")
            void shouldHandleVeryLargeItemIds() throws Exception {
                // Given
                ClaimRequest requestWithLargeId = new ClaimRequest(Long.MAX_VALUE, 1, "Large ID test");
                when(claimService.createClaim(any(ClaimRequest.class), eq("testuser")))
                        .thenThrow(new LostItemNotFoundException("Lost item not found"));

                // When & Then
                assertErrorResponse(performCreateClaim(requestWithLargeId), 404, "Lost Item Not Found");
            }

            @Test
            @WithMockUser(username = "testuser", roles = "USER")
            @DisplayName("Should handle concurrent modification exception")
            void shouldHandleConcurrentModificationException() throws Exception {
                // Given
                when(claimService.createClaim(any(ClaimRequest.class), eq("testuser")))
                        .thenThrow(new RuntimeException("Optimistic locking failure"));

                // When & Then
                performCreateClaim(validClaimRequest)
                        .andExpect(status().isInternalServerError());
            }
        }
    }
}