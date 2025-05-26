package com.example.lostfound.dto;

import com.example.lostfound.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    
    private Long id;
    private String username;
    private String name;
    private String email;
    private Role role;
    private LocalDateTime createdAt;
    private boolean enabled;
} 