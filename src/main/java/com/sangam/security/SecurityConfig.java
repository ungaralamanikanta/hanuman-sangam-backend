package com.sangam.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    // ===============================
    // 🔐 SECURITY FILTER CHAIN
    // ===============================
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth

                // ✅ PUBLIC APIs — No token needed
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/admin/stats").permitAll()
                .requestMatchers("/api/admin/announcements").permitAll()

                // ✅ OPTIONS preflight — MUST be permitted for CORS to work
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()

                // 🔒 ADMIN only
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // 🔒 MEMBER only
                .requestMatchers("/api/member/**").hasRole("MEMBER")

                // 🌐 STATIC FILES
                .requestMatchers("/", "/*.html", "/css/**", "/js/**", "/images/**").permitAll()

                // 🔒 Everything else requires authentication
                .anyRequest().authenticated()
            )

            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ===============================
    // 🔑 PASSWORD ENCODER
    // ===============================
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ===============================
    // 🔐 AUTH MANAGER
    // ===============================
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // ===============================
    // 🌍 CORS CONFIG — Fixed for Netlify + Render
    // ===============================
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // ✅ ALL allowed frontend origins
        config.setAllowedOrigins(Arrays.asList(
            "https://hanuman-sangam-ui.netlify.app",  // 🔥 Production Netlify
            "http://localhost:3000",                   // React dev server
            "http://localhost:5500",                   // Live Server (VSCode)
            "http://127.0.0.1:5500",                  // Live Server alternate
            "http://localhost:8080",                   // Other local ports
            "http://127.0.0.1:3000"
        ));

        // ✅ All HTTP methods including OPTIONS (preflight)
        config.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"
        ));

        // ✅ All common headers
        config.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-Requested-With",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));

        // ✅ Expose Authorization header to frontend JS
        config.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials"
        ));

        // ✅ Allow cookies / Authorization header
        config.setAllowCredentials(true);

        // ✅ Cache preflight for 1 hour (reduces OPTIONS requests)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
