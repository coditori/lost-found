 package com.example.lostfound.controller;

import com.example.lostfound.dto.ClaimDto;
import com.example.lostfound.dto.ClaimRequest;
import com.example.lostfound.dto.LostItemDto;
import com.example.lostfound.entity.ClaimStatus;
import com.example.lostfound.exception.InsufficientQuantityException;
import com.example.lostfound.exception.LostItemNotFoundException;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
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
@DisplayName("User Controller Tests")
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
        sampleLostItem = LostItemDto.builder()
                .id(1L)
                .itemName("Laptop")
                .quantity(2)
                .remainingQuantity(2)
                .place("Library")
                .description("Found in library")
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

        validClaimRequest = new ClaimRequest(1L, 1, "I think this is mine");
    }

    @Nested
    @DisplayName("Get Available Items Tests")
    class GetAvailableItemsTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should retrieve available items with default pagination")
        void shouldRetrieveAvailableItemsWithDefaultPagination() throws Exception {
            List<LostItemDto> items = List.of(sampleLostItem);
            Page<LostItemDto> itemsPage = new PageImpl<>(items, PageRequest.of(0, 20), 1);
            when(lostItemService.getAvailableItems(any(Pageable.class))).thenReturn(itemsPage);

            mockMvc.perform(get("/api/user/items"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].id", is(1)))
                    .andExpect(jsonPath("$.content[0].itemName", is("Laptop")))
                    .andExpect(jsonPath("$.content[0].place", is("Library")))
                    .andExpect(jsonPath("$.content[0].remainingQuantity", is(2)))
                    .andExpect(jsonPath("$.content[0].available", is(true)))
                    .andExpect(jsonPath("$.totalElements", is(1)))
                    .andExpect(jsonPath("$.size", is(20)))
                    .andExpect(jsonPath("$.number", is(0)));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should retrieve items with custom pagination")
        void shouldRetrieveItemsWithCustomPagination() throws Exception {
            List<LostItemDto> items = List.of(sampleLostItem);
            Page<LostItemDto> itemsPage = new PageImpl<>(items, PageRequest.of(1, 10), 1);
            when(lostItemService.getAvailableItems(any(Pageable.class))).thenReturn(itemsPage);

            mockMvc.perform(get("/api/user/items")
                            .param("page", "1")
                            .param("size", "10")
                            .param("sort", "itemName,asc"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.totalElements", is(1)))
                    .andExpect(jsonPath("$.size", is(10)))
                    .andExpect(jsonPath("$.number", is(1)));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should retrieve empty items page")
        void shouldRetrieveEmptyItemsPage() throws Exception {
            Page<LostItemDto> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
            when(lostItemService.getAvailableItems(any(Pageable.class))).thenReturn(emptyPage);

            mockMvc.perform(get("/api/user/items"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements", is(0)));
        }

        @Test
        @DisplayName("Should return 401 for unauthenticated user")
        void shouldReturn401ForUnauthenticatedUser() throws Exception {
            mockMvc.perform(get("/api/user/items"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Create Claim Tests")
    class CreateClaimTests {

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        @DisplayName("Should create claim successfully")
        void shouldCreateClaimSuccessfully() throws Exception {
            when(claimService.createClaim(any(ClaimRequest.class), eq("testuser"))).thenReturn(sampleClaim);

            mockMvc.perform(post("/api/user/claims")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validClaimRequest))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.userName", is("John Doe")))
                    .andExpect(jsonPath("$.itemName", is("Laptop")))
                    .andExpect(jsonPath("$.claimedQuantity", is(1)))
                    .andExpect(jsonPath("$.status", is("PENDING")))
                    .andExpect(jsonPath("$.notes", is("I think this is mine")));
        }

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        @DisplayName("Should return 400 for insufficient quantity")
        void shouldReturn400ForInsufficientQuantity() throws Exception {
            when(claimService.createClaim(any(ClaimRequest.class), eq("testuser")))
                    .thenThrow(new InsufficientQuantityException("Insufficient quantity. Requested: 5, Available: 2"));

            ClaimRequest invalidRequest = new ClaimRequest(1L, 5, "I need all of them");

            mockMvc.perform(post("/api/user/claims")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error", is("Insufficient Quantity")))
                    .andExpect(jsonPath("$.message", containsString("Insufficient quantity")));
        }

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        @DisplayName("Should return 404 for non-existent item")
        void shouldReturn404ForNonExistentItem() throws Exception {
            when(claimService.createClaim(any(ClaimRequest.class), eq("testuser")))
                    .thenThrow(new LostItemNotFoundException("Lost item not found with id: 999"));

            ClaimRequest invalidRequest = new ClaimRequest(999L, 1, "This item doesn't exist");

            mockMvc.perform(post("/api/user/claims")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error", is("Lost Item Not Found")))
                    .andExpect(jsonPath("$.message", containsString("Lost item not found")));
        }

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        @DisplayName("Should return 400 for invalid request - null lostItemId")
        void shouldReturn400ForNullLostItemId() throws Exception {
            ClaimRequest invalidRequest = new ClaimRequest(null, 1, "Invalid request");

            mockMvc.perform(post("/api/user/claims")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error", is("Validation Failed")))
                    .andExpect(jsonPath("$.message", is("Invalid input data")))
                    .andExpect(jsonPath("$.validationErrors.lostItemId", is("Lost item ID is required")));
        }

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        @DisplayName("Should return 400 for invalid request - zero quantity")
        void shouldReturn400ForZeroQuantity() throws Exception {
            ClaimRequest invalidRequest = new ClaimRequest(1L, 0, "Invalid quantity");

            mockMvc.perform(post("/api/user/claims")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error", is("Validation Failed")))
                    .andExpect(jsonPath("$.message", is("Invalid input data")))
                    .andExpect(jsonPath("$.validationErrors.claimedQuantity", is("Claimed quantity must be at least 1")));
        }

        @Test
        @DisplayName("Should return 401 for unauthenticated user")
        void shouldReturn401ForUnauthenticatedUser() throws Exception {
            mockMvc.perform(post("/api/user/claims")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validClaimRequest))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "testuser", roles = "USER")
        @DisplayName("Should return 400 for duplicate claim attempt")
        void shouldReturn400ForDuplicateClaimAttempt() throws Exception {
            when(claimService.createClaim(any(ClaimRequest.class), eq("testuser")))
                    .thenThrow(new IllegalStateException("User has already claimed this item"));

            mockMvc.perform(post("/api/user/claims")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validClaimRequest))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error", is("Invalid Operation")))
                    .andExpect(jsonPath("$.message", containsString("User has already claimed this item")));
        }
    }
}