package com.example.lostfound.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    
    private String token;
    @Builder.Default
    private String type = "Bearer";
    private String username;
    private String name;
    private String email;
    private String role;
    private Long expiresIn; // in milliseconds
} 