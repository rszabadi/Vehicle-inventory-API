package rszabadi.swiss;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
    private static final int MIN_PASSWORD_LENGTH = 12;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, LoginAttemptLimiter limiter) throws Exception {
        SavedRequestAwareAuthenticationSuccessHandler onSuccess = new SavedRequestAwareAuthenticationSuccessHandler();
        onSuccess.setDefaultTargetUrl("/");

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/error").permitAll()
                        // viewers and admins may read; everything else is admin only
                        .requestMatchers(HttpMethod.GET, "/vehicles", "/vehicles/**", "/me",
                                "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**")
                        .hasAnyRole("ADMIN", "VIEWER")
                        .anyRequest().hasRole("ADMIN"))
                .addFilterBefore(new LoginRateLimitFilter(limiter), UsernamePasswordAuthenticationFilter.class)
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler((request, response, authentication) -> {
                            limiter.recordSuccess(request.getRemoteAddr());
                            onSuccess.onAuthenticationSuccess(request, response, authentication);
                        })
                        .failureHandler((request, response, exception) -> {
                            limiter.recordFailure(request.getRemoteAddr());
                            response.sendRedirect("/login?error");
                        }))
                .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
                .exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        request -> request.getRequestURI().startsWith("/vehicles")
                                || request.getRequestURI().equals("/me")))
                .csrf(csrf -> csrf.spa());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(PasswordEncoder encoder,
            @Value("${app.admin.user:admin}") String adminUser,
            @Value("${app.admin.password:}") String adminPassword,
            @Value("${app.viewer.user:demo}") String viewerUser,
            @Value("${app.viewer.password:}") String viewerPassword) {
        List<UserDetails> users = new ArrayList<>();

        if (adminPassword.length() < MIN_PASSWORD_LENGTH) {
            log.warn("No admin password with at least {} characters is configured: nobody can log in as admin",
                    MIN_PASSWORD_LENGTH);
        } else {
            log.info("Login enabled for admin user '{}'", adminUser);
            users.add(User.withUsername(adminUser).password(encoder.encode(adminPassword)).roles("ADMIN").build());
        }

        if (viewerPassword.isBlank()) {
            log.info("No viewer password configured: the read-only demo login is off");
        } else if (viewerUser.equals(adminUser)) {
            log.warn("The viewer user name must differ from the admin user name: the demo login is off");
        } else {
            log.info("Read-only demo login enabled for user '{}'", viewerUser);
            users.add(User.withUsername(viewerUser).password(encoder.encode(viewerPassword)).roles("VIEWER").build());
        }

        return new InMemoryUserDetailsManager(users);
    }
}