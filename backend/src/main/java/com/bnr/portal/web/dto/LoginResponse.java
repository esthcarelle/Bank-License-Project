package com.bnr.portal.web.dto;

import com.bnr.portal.domain.UserRole;

public record LoginResponse(String token, UserRole role, String fullName, String email) {}
