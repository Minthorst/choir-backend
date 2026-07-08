package me.choir_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(List.of("http://localhost:5173"));
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
                    config.setAllowCredentials(true);
                    return config;
                })).csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/doorman/**").hasAnyRole("ADMIN", "DOORMAN")
                        .anyRequest().permitAll()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )

                .httpBasic(Customizer.withDefaults())

                .formLogin(form -> form
                        .loginProcessingUrl("/api/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler((request, response, authentication) -> response.setStatus(HttpStatus.OK.value()))
                        .failureHandler((request, response, exception) -> response.setStatus(HttpStatus.UNAUTHORIZED.value()))
                )

                .logout(logout -> logout
                        .logoutUrl("/api/logout")
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(HttpStatus.OK.value()))
                );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        String adminUsername = System.getenv("ADMIN_USERNAME");
        if (adminUsername == null) adminUsername = "ChorAdmin";

        String adminPassword = System.getenv("ADMIN_PASSWORD");
        if (adminPassword == null) adminPassword = "Chor2026";

        String doormanUsername = System.getenv("DOORMAN_USERNAME");
        if (doormanUsername == null) doormanUsername = "ChorDoorman";

        String doormanPassword = System.getenv("DOORMAN_PASSWORD");
        if (doormanPassword == null) doormanPassword = "Einlass2026";

        //TODO change this
        UserDetails admin = User.withDefaultPasswordEncoder()
                .username(adminUsername)
                .password(adminPassword)
                .roles("ADMIN")
                .build();

        UserDetails doorman = User.withDefaultPasswordEncoder()
                .username(doormanUsername)
                .password(doormanPassword)
                .roles("DOORMAN")
                .build();

        return new InMemoryUserDetailsManager(admin, doorman);
    }
}
