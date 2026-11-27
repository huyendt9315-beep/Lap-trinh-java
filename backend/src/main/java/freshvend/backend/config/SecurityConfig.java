package freshvend.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.ignoringRequestMatchers(
                    "/api/v1/products",
                    "/api/v1/products/**",
                    "/api/v1/machines",
                    "/api/v1/machines/**",
                    "/api/v1/slots",
                    "/api/v1/slots/**",
                    "/api/v1/lots/**",
                    "/api/v1/orders",
                    "/api/v1/orders/**",
                    "/api/v1/gateway/payment-events",
                    "/api/v1/alerts/**"
            ))
            .authorizeHttpRequests(auth -> auth
                    .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}