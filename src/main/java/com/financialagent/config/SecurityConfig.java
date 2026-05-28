package com.financialagent.config;

import com.financialagent.service.AngelOneSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;

/**
 * Spring Security configuration for authentication and authorization.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final ApplicationContext applicationContext;

    /**
     * Main security filter chain configuration.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF for API endpoints
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**")
                        .ignoringRequestMatchers("/ws/**")
                        .ignoringRequestMatchers("/h2-console/**")
                )

                // Configure authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/register", "/login", "/error", "/totp-login", "/totp-login.html").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/totp-login/**").permitAll()
                        .requestMatchers("/h2-console", "/h2-console/**").permitAll()

                        // Static resources
                        .requestMatchers("/favicon.ico", "/manifest.json").permitAll()

                        // Dashboard and main pages require authentication
                        .requestMatchers("/dashboard", "/").authenticated()

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )

                // Configure form login
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error=true")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(customAuthenticationSuccessHandler())
                        .failureHandler(customAuthenticationFailureHandler())
                        .permitAll()
                )

                // Configure logout
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .clearAuthentication(true)
                        .permitAll()
                )

                // Configure session management
                .sessionManagement(session -> session
                        .sessionFixation().migrateSession()
                )

                // Configure exception handling
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            if (request.getRequestURI().startsWith("/api/")) {
                                response.setStatus(401);
                                response.setContentType("application/json");
                                response.getWriter().write("""
                                        {
                                            "success": false,
                                            "error": "Authentication required",
                                            "message": "Please login to access this resource"
                                        }
                                        """);
                            } else {
                                response.sendRedirect("/login");
                            }
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            if (request.getRequestURI().startsWith("/api/")) {
                                response.setStatus(403);
                                response.setContentType("application/json");
                                response.getWriter().write("""
                                        {
                                            "success": false,
                                            "error": "Access denied",
                                            "message": "You don't have permission to access this resource"
                                        }
                                        """);
                            } else {
                                response.sendRedirect("/error/403");
                            }
                        })
                )

                // Security headers
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.sameOrigin())
                        .contentTypeOptions().and()
                        .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                                .maxAgeInSeconds(31536000)
                                .includeSubDomains(true)
                        )
                );

        return http.build();
    }

    /**
     * Authentication provider configuration.
     */
    @Bean
    public AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Authentication manager bean.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Password encoder bean.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Session registry for managing user sessions.
     */
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    /**
     * HTTP session event publisher for session management.
     */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    /**
     * Custom authentication success handler.
     */
    private AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return (request, response, authentication) -> {
            String username = authentication.getName();

            // Log successful login
            System.out.println("User logged in successfully: " + username);
            System.out.println("Authentication success handler called for user: " + username);

            // Establish AngelOne session asynchronously in background
            // Capture the authentication context for the background thread
            org.springframework.security.core.context.SecurityContext securityContext =
                org.springframework.security.core.context.SecurityContextHolder.getContext();

            new Thread(() -> {
                try {
                    // Set the security context in the background thread
                    org.springframework.security.core.context.SecurityContextHolder.setContext(securityContext);

                    System.out.println("Background thread: Getting AngelOneSessionService bean...");
                    AngelOneSessionService sessionService = applicationContext.getBean(AngelOneSessionService.class);
                    System.out.println("Background thread: AngelOneSessionService bean obtained: " + sessionService);

                    System.out.println("Background thread: Calling sessionService.authenticate() for user: " + username);
                    String token = sessionService.authenticate();
                    System.out.println("Background thread: Authentication successful, token obtained: " + (token != null ? "SUCCESS" : "NULL"));

                    String feedToken = sessionService.getFeedToken();
                    System.out.println("Background thread: Feed token obtained: " + (feedToken != null ? "SUCCESS" : "NULL"));

                    if (token != null && feedToken != null) {
                        System.out.println("Background thread: AngelOne connection established successfully for user: " + username);
                    } else {
                        System.err.println("Background thread: AngelOne connection failed - token or feed token is null");
                    }
                } catch (Exception e) {
                    System.err.println("Background thread: Failed to establish AngelOne connection: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    // Clear the security context from the background thread
                    org.springframework.security.core.context.SecurityContextHolder.clearContext();
                }
            }).start();

            // Redirect immediately without waiting for AngelOne
            if (authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
                System.out.println("Redirecting admin user to /admin/dashboard");
                response.sendRedirect("/admin/dashboard");
            } else {
                System.out.println("Redirecting user to /dashboard");
                response.sendRedirect("/dashboard");
            }
        };
    }

    /**
     * Custom authentication failure handler.
     */
    private AuthenticationFailureHandler customAuthenticationFailureHandler() {
        return (request, response, exception) -> {
            String username = request.getParameter("username");

            // Log failed login attempt
            System.err.println("Login failed for user: " + username + " - " + exception.getMessage());

            String errorMessage;
            
            // Check specific error types and provide user-friendly messages
            if (exception.getMessage() != null) {
                if (exception.getMessage().contains("locked")) {
                    errorMessage = "Account is locked. Please try again later.";
                } else if (exception.getMessage().contains("disabled")) {
                    errorMessage = "Account is disabled. Please contact support.";
                } else if (exception.getMessage().contains("Bad credentials")) {
                    errorMessage = "Invalid username or password. Please check your credentials and try again.";
                } else if (exception.getMessage().contains("User not found")) {
                    errorMessage = "User not found. Please check your username or register for an account.";
                } else if (exception.getMessage().contains("disabled")) {
                    errorMessage = "Account is disabled. Please contact support.";
                } else {
                    errorMessage = "Login failed. Please check your credentials and try again.";
                }
            } else {
                errorMessage = "Login failed. Please check your credentials and try again.";
            }

            // Set error message in session and redirect
            request.getSession().setAttribute("error", errorMessage);
            response.sendRedirect("/login?error=true");
        };
    }
}
