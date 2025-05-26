package com.example.lostfound.controller;

import com.example.lostfound.dto.ClaimDto;
import com.example.lostfound.dto.ClaimRequest;
import com.example.lostfound.dto.LostItemDto;
import com.example.lostfound.exception.*;
import com.example.lostfound.service.ClaimService;
import com.example.lostfound.service.LostItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@SecurityRequirement(name = "basicAuth")
@Tag(name = "User", description = "User endpoints for browsing items and managing claims")
public class UserController {
    
    private final LostItemService lostItemService;
    private final ClaimService claimService;
    
    @GetMapping("/items")
    @Operation(summary = "Browse available lost items", 
              description = """
                  Get paginated list of available lost items (items with remaining quantity > 0).
                  
                  ## Sorting
                  You can sort by the following fields:
                  - `id` - Item ID
                  - `itemName` - Name of the item
                  - `quantity` - Original quantity
                  - `remainingQuantity` - Remaining quantity available
                  - `place` - Location where item was found
                  - `createdAt` - When item was added to system
                  - `updatedAt` - When item was last updated
                  
                  ## Examples
                  - Sort by newest items: `sort=createdAt,desc`
                  - Sort by item name: `sort=itemName`
                  - Sort by location: `sort=place`
                  """)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Items retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<LostItemDto>> getAvailableItems(
            @Parameter(hidden = true) @PageableDefault(size = 20) Pageable pageable,
            
            @Parameter(
                name = "page",
                description = "Page number (0-based)",
                example = "0"
            ) @RequestParam(defaultValue = "0") int page,
            
            @Parameter(
                name = "size", 
                description = "Number of items per page",
                example = "20"
            ) @RequestParam(defaultValue = "20") int size,
            
            @Parameter(
                name = "sort",
                description = """
                    Sort criteria in format: property[,direction]. 
                    Direction can be 'asc' or 'desc'. Default is 'asc'.
                    Multiple sort criteria are supported.
                    
                    Valid sort fields: id, itemName, quantity, remainingQuantity, place, createdAt, updatedAt
                    """,
                example = "createdAt,desc"
            ) @RequestParam(required = false) String[] sort) {
        
        log.debug("User browsing available items with pagination: {}", pageable);
        Page<LostItemDto> items = lostItemService.getAvailableItems(pageable);
        return ResponseEntity.ok(items);
    }
    
    @PostMapping("/claims")
    @Operation(summary = "Create a claim", description = "Claim a quantity of a lost item")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Claim created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid claim request or insufficient quantity"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Item or user not found"),
        @ApiResponse(responseCode = "409", description = "Concurrent modification or user already claimed this item")
    })
    public ResponseEntity<ClaimDto> createClaim(
            @Valid @RequestBody ClaimRequest request,
            Authentication authentication) 
            throws LostItemNotFoundException, UserNotFoundException, InsufficientQuantityException {
        
        String username = authentication.getName();
        log.info("User {} creating claim for item {} with quantity {}", 
                username, request.getLostItemId(), request.getClaimedQuantity());
        
        ClaimDto claim = claimService.createClaim(request, username);
        return new ResponseEntity<>(claim, HttpStatus.CREATED);
    }
} 