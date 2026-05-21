package com.nippyclouding.tech_log_back.auth.controller;

import com.nippyclouding.tech_log_back.auth.security.GithubUser;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.ok(Map.of("authenticated", false, "admin", false));
        }
        boolean admin = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);

        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            GithubUser githubUser = GithubUser.from(oauth2User);
            return ResponseEntity.ok(Map.of(
                    "authenticated", true,
                    "admin", admin,
                    "provider", "github",
                    "name", githubUser.login().isBlank() ? githubUser.name() : githubUser.login(),
                    "avatar", githubUser.avatarUrl()
            ));
        }

        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "admin", admin,
                "provider", "admin-console",
                "name", authentication.getName(),
                "avatar", ""
        ));
    }
}
