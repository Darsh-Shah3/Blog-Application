package com.example.blog_app_apis.config;

import com.example.blog_app_apis.security.CustomUserDetailService;
import com.example.blog_app_apis.security.JwtAuthenticationEntryPoint;
import com.example.blog_app_apis.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebSecurity
@EnableWebMvc
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    public static final String[] PUBLIC_URLS={
        "/api/auth/**",
        "/api/test/**",
        "/api/users/",
        "/v3/api-docs/**", 
        "/v2/api-docs/**",
        "swagger-resources/**",
        "/swagger-ui/**", 
        "/swagger-ui.html",
        "/webjars/**"
    };

    @Autowired
    private CustomUserDetailService userDetailsService;

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .requestMatchers(HttpMethod.GET).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(this.jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

// I recommended adding the jakarta.xml.bind-api dependency to your pom.xml file
// (since you're using Maven), because the error:

// java.lang.NoClassDefFoundError: javax/xml/bind/DatatypeConverter
// happens when your application (specifically, the JWT library) tries to use
// classes that were removed from the Java standard library starting with Java
// 11.

// So the fix was:

// Add the JAXB (Jakarta XML Binding) API manually as a dependency.
