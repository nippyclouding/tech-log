package com.nippyclouding.tech_log_back.auth.security;

import com.nippyclouding.tech_log_back.log.login.service.LoginLogService;
import com.nippyclouding.tech_log_back.global.exception.ErrorCode;
import com.nippyclouding.tech_log_back.global.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nippyclouding.tech_log_back.global.web.ClientIpResolver;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final ObjectMapper objectMapper;
    private final LoginLogService loginLogService;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            OAuth2UserService<OAuth2UserRequest, OAuth2User> githubOnlyOAuth2UserService
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin-console", "/admin-console/login").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/posts/*/comments").authenticated()
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form
                        .loginPage("/admin-console")
                        .loginProcessingUrl("/admin-console/login")
                        .usernameParameter("adminId")
                        .passwordParameter("adminPassword")
                        .successHandler((request, response, authentication) -> {
                            loginLogService.record("admin-console", authentication.getName(), ClientIpResolver.resolve(request));
                            response.sendRedirect("/admin-console");
                        })
                        .failureUrl("/admin-console?error")
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(githubOnlyOAuth2UserService))
                        .successHandler((request, response, authentication) -> {
                            String loginId = authentication.getName();
                            if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
                                GithubUser githubUser = GithubUser.from(oauth2User);
                                loginId = githubUser.login().isBlank() ? githubUser.name() : githubUser.login();
                            }
                            loginLogService.record("github", loginId, ClientIpResolver.resolve(request));
                            response.sendRedirect("/");
                        })
                )
                .logout(logout -> logout
                        .logoutUrl("/admin-console/logout")
                        .logoutSuccessUrl("/admin-console?logout")
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                writeError(response, ErrorCode.UNAUTHORIZED, authException.getMessage(), request.getRequestURI()))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeError(response, ErrorCode.FORBIDDEN, accessDeniedException.getMessage(), request.getRequestURI()))
                );
        return http.build();
    }

    @Bean
    public UserDetailsService adminConsoleUserDetailsService(
            @Value("${app.admin-console.username}") String username,
            @Value("${app.admin-console.password}") String password,
            PasswordEncoder passwordEncoder
    ) {
        return new InMemoryUserDetailsManager(User.withUsername(username)
                .password(passwordEncoder.encode(password))
                .roles("ADMIN")
                .build());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> githubOnlyOAuth2UserService() {
        var delegate = new org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService();
        return userRequest -> {
            if (!"github".equals(userRequest.getClientRegistration().getRegistrationId())) {
                throw new org.springframework.security.oauth2.core.OAuth2AuthenticationException("Only GitHub OAuth login is allowed.");
            }
            OAuth2User user = delegate.loadUser(userRequest);
            return new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_USER")), user.getAttributes(), "id");
        };
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode, String message, String path) throws java.io.IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), new ErrorResponse(
                errorCode.getCode(),
                message,
                errorCode.getStatus().value(),
                path,
                LocalDateTime.now(),
                List.of()
        ));
    }
}
