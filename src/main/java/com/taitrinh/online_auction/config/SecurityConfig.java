package com.taitrinh.online_auction.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.taitrinh.online_auction.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    private static class SecurityEndpoints {
        // Swagger/OpenAPI documentation
        static final String[] SWAGGER = {
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/swagger-ui.html"
        };

        // Public endpoints - no authentication required
        static final String[] PUBLIC = {
                "/api/v1/auth/**",
                "/ws/**",
                "/api/v1/webhooks/stripe"
        };

        // Public read-only endpoints (GET only)
        static final String[] PUBLIC_READ_ONLY = {
                "/api/v1/products/**",
                "/api/v1/categories/**",
                "/api/v1/comments/**",
                "/api/v1/config/**",
                "/api/v1/roles/**"
        };

        // Authenticated user endpoints (must be ordered before seller-only product
        // patterns)
        static final String BID_CREATE = "/api/v1/products/*/bids";
        static final String BUY_NOW = "/api/v1/products/*/buy-now";
        static final String BIDDER_DELETE = "/api/v1/products/*/bidders/*";
        static final String PROFILE = "/api/v1/profile/**";

        // Seller-only product endpoints
        static final String PRODUCTS_API = "/api/v1/products/**";

        // Admin endpoints
        static final String ADMIN = "/api/v1/admin/**";
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // Swagger/OpenAPI endpoints
                        .requestMatchers(SecurityEndpoints.SWAGGER).permitAll()

                        // Public endpoints
                        .requestMatchers(SecurityEndpoints.PUBLIC).permitAll()

                        // Public read-only endpoints (GET only)
                        .requestMatchers(HttpMethod.GET, SecurityEndpoints.PUBLIC_READ_ONLY).permitAll()

                        // Authenticated user endpoints (must be before seller-only product rules)
                        .requestMatchers(HttpMethod.POST, SecurityEndpoints.BID_CREATE).authenticated()
                        .requestMatchers(HttpMethod.POST, SecurityEndpoints.BUY_NOW).authenticated()
                        .requestMatchers(HttpMethod.DELETE, SecurityEndpoints.BIDDER_DELETE).authenticated()
                        .requestMatchers(SecurityEndpoints.PROFILE).authenticated()

                        // Seller endpoints - require SELLER role
                        .requestMatchers(HttpMethod.POST, SecurityEndpoints.PRODUCTS_API).hasRole("SELLER")
                        .requestMatchers(HttpMethod.PUT, SecurityEndpoints.PRODUCTS_API).hasRole("SELLER")
                        .requestMatchers(HttpMethod.PATCH, SecurityEndpoints.PRODUCTS_API).hasRole("SELLER")

                        // Admin endpoints
                        .requestMatchers(SecurityEndpoints.ADMIN).hasRole("ADMIN")

                        // All other endpoints require authentication
                        .anyRequest().authenticated())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // Strength: 12 rounds
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Set-Cookie", "Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Define role hierarchy: ADMIN > SELLER > BIDDER
     * This means:
     * - ADMIN has all permissions of SELLER + BIDDER + their own
     * - SELLER has all permissions of BIDDER + their own
     * - BIDDER has only their own permissions
     */
    @Bean
    public RoleHierarchy roleHierarchy() {
        // Use fromHierarchy() factory method (Spring Security 6.x+)
        return RoleHierarchyImpl.fromHierarchy("ROLE_ADMIN > ROLE_SELLER \n ROLE_SELLER > ROLE_BIDDER");
    }

    /**
     * Configure method security to use role hierarchy
     * This enables @PreAuthorize annotations to respect the hierarchy
     */
    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setRoleHierarchy(roleHierarchy);
        return expressionHandler;
    }
}
