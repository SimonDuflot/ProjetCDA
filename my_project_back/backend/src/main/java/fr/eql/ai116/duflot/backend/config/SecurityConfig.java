package fr.eql.ai116.duflot.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

//    @Bean
//    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//            .authorizeHttpRequests(authorizeRequests ->
//                authorizeRequests
//                    .requestMatchers("/api/**").permitAll() // Permit access to /api/test
//                    .anyRequest().authenticated() // Require auth for all other requests
//            )
//            .httpBasic(withDefaults()); // Enable basic auth for other requests (or use formLogin etc.)
//        return http.build();
//    }
//    // Add other beans like PasswordEncoder, UserDetailsService later if needed

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Configure CORS using the corsConfigurationSource bean below
                .cors(withDefaults())
                // Disable CSRF (Consider enabling and configuring properly for production)
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                // Explicitly permit OPTIONS requests for CORS preflight
                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                // Permit access to your API endpoint
                                .requestMatchers("/api/uploadResumeBlob").permitAll()
                                // Require authentication for all other requests (adjust as needed)
                                .anyRequest().authenticated()
                )
                // Keep basic auth for now, but you might switch later
                .httpBasic(withDefaults());
        return http.build();
    }

    // Define a CORS configuration source bean
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Specify the allowed origin (your frontend)
        configuration.setAllowedOrigins(Arrays.asList("http://127.0.0.1:5500"));
        // Specify allowed HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Specify allowed headers
        configuration.setAllowedHeaders(Arrays.asList("*")); // Allow all headers for simplicity, refine if needed
        // Allow credentials if you need cookies/auth headers later
        // configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Apply this configuration to all paths ("/**")
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
