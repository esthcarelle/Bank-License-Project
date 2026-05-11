package com.bnr.portal.web;

import com.bnr.portal.security.JwtIssuer;
import com.bnr.portal.security.SignedInUser;
import com.bnr.portal.web.dto.LoginRequest;
import com.bnr.portal.web.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtIssuer jwtIssuer;

    public AuthController(AuthenticationManager authenticationManager, JwtIssuer jwtIssuer) {
        this.authenticationManager = authenticationManager;
        this.jwtIssuer = jwtIssuer;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password()));
        SignedInUser person = (SignedInUser) auth.getPrincipal();
        String token = jwtIssuer.issueTokenFor(person);
        return ResponseEntity.ok(new LoginResponse(
                token,
                person.getRole(),
                person.getFullName(),
                request.email().trim().toLowerCase()
        ));
    }
}
