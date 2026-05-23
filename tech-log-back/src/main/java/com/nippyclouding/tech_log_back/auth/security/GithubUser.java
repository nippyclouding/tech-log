package com.nippyclouding.tech_log_back.auth.security;

import java.util.Map;
import org.springframework.security.oauth2.core.user.OAuth2User;

public record GithubUser(Long id, String login, String name, String email, String avatarUrl) {

    public static GithubUser from(OAuth2User user) {
        Map<String, Object> attributes = user.getAttributes();
        return new GithubUser(
                toLong(attributes.get("id")),
                toString(attributes.get("login")),
                toString(attributes.get("name")),
                toString(attributes.get("email")),
                toString(attributes.get("avatar_url"))
        );
    }

    private static Long toLong(Object value) {
        if (value == null) {
            return null; // Safe guard against null IDs
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private static String toString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}