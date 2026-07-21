package com.ember.security;

import com.ember.config.EmberProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Stateless JWT security. Reads (public) endpoints stay open so the customer board
 * and initial loads work without a login; mutations require the right staff role.
 *
 * <ul>
 *   <li>public: {@code POST /api/auth/login}, {@code GET /api/menu}, {@code GET /api/orders/**},
 *       {@code POST /api/orders/*&#47;collect} (the board is customer-facing), and {@code /ws}</li>
 *   <li>{@code POST /api/orders} → CASHIER or MANAGER</li>
 *   <li>advance / recall → COOK or MANAGER</li>
 *   <li>menu writes and reports → MANAGER</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final EmberProperties props;

    public SecurityConfig(EmberProperties props) {
        this.props = props;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Error dispatches (e.g. a 403/405 forwarded to /error) must not be
                        // re-secured, or the JWT filter (skipped on ERROR dispatch) turns them into 401.
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/menu", "/api/menu/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/orders", "/api/orders/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/orders/*/collect").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/orders").hasAnyRole("CASHIER", "MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/orders/*/advance", "/api/orders/*/recall")
                        .hasAnyRole("COOK", "MANAGER")
                        .requestMatchers("/api/menu/**").hasRole("MANAGER")
                        .requestMatchers("/api/reports/**").hasRole("MANAGER")
                        .requestMatchers("/api/staff/**").hasRole("MANAGER")
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        // 401 when unauthenticated, 403 when authenticated but lacking the role.
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, denied) ->
                                response.sendError(HttpStatus.FORBIDDEN.value())))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Patterns (not setAllowedOrigins) so allowCredentials works even when a "*"
        // pattern is configured (e.g. in tests). SockJS's XHR transport is credentialed.
        config.setAllowedOriginPatterns(props.getAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Password sign-in is backed by the staff table via StaffUserDetailsService.

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
