package com.example.lostfound.controller;

import com.example.lostfound.dto.ClaimDto;
import com.example.lostfound.dto.LostItemDto;
import com.example.lostfound.service.ClaimService;
import com.example.lostfound.service.LostItemService;
import com.example.lostfound.exception.FileParsingException;
import com.example.lostfound.exception.UnsupportedFileTypeException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "basicAuth")
@Tag(name = "Admin", description = "Admin-only endpoints for managing lost items and claims")
public class AdminController {
    
    private final LostItemService lostItemService;
    private final ClaimService claimService;
    
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upload lost items file", 
               description = "Upload a PDF file containing lost item records. The file will be parsed and items will be stored in the database.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "File uploaded and processed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid file or parsing error"),
        @ApiResponse(responseCode = "415", description = "Unsupported file type"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
    })
    public ResponseEntity<Map<String, Object>> uploadFile(
            @Parameter(description = "PDF file containing lost item records", required = true)
            @RequestParam("file") MultipartFile file) 
            throws UnsupportedFileTypeException, FileParsingException {
        
        log.info("Admin file upload request: {} (size: {} bytes)", 
                file.getOriginalFilename(), file.getSize());
        
        List<LostItemDto> items = lostItemService.uploadAndParseFile(file);
        
        Map<String, Object> response = Map.of(
            "message", "File uploaded and processed successfully",
            "itemsCount", items.size(),
            "items", items
        );
        
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @GetMapping("/claims")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all claims", 
          description = """
              Retrieve all claims with user and item information.
              
              ## Sorting
              You can sort by the following fields:
              - `id` - Claim ID
              - `claimDate` - When the claim was made
              - `claimedQuantity` - Number of items claimed
              - `status` - Claim status (PENDING, APPROVED, REJECTED, FULFILLED)
              - `notes` - Additional notes
              
              ## Examples
              - Sort by claim date (newest first): `sort=claimDate,desc`
              - Sort by status: `sort=status`
              - Multiple sorts: `sort=status,asc&sort=claimDate,desc`
              """)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Claims retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
    })
    public ResponseEntity<Page<ClaimDto>> getAllClaims(
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
                    
                    Valid sort fields: id, claimDate, claimedQuantity, status, notes
                    """,
                example = "claimDate,desc"
            ) @RequestParam(required = false) String[] sort) {
        
        log.debug("Admin retrieving all claims with pagination: {}", pageable);
        Page<ClaimDto> claims = claimService.getAllClaims(pageable);
        return ResponseEntity.ok(claims);
    }
} 