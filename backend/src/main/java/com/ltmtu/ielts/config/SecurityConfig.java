package com.ltmtu.ielts.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.ltmtu.ielts.security.JwtAuthFilter;
import com.ltmtu.ielts.security.AccessTokenVerifier;
import com.ltmtu.ielts.security.SecurityErrorWriter;

/**
 * Bảo mật REST: stateless, xác thực bằng JWT ({@link JwtAuthFilter}).
 * Phân quyền theo đường dẫn: /api/admin → ADMIN, /api/teacher → TEACHER, /api/student → STUDENT (docs/05-api.md A.1).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, AccessTokenVerifier verifier,
			SecurityErrorWriter errorWriter) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(cors -> {
				})
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.exceptionHandling(e -> e.authenticationEntryPoint(errorWriter).accessDeniedHandler(errorWriter))
				.addFilterBefore(new JwtAuthFilter(verifier, errorWriter),
						UsernamePasswordAuthenticationFilter.class)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers("/actuator/health/**", "/api/time", "/error").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh", "/api/auth/logout")
						.permitAll()
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
						// Ảnh đề bài: tên file UUID, xem WebConfig.
						.requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
						// Bắt tay WebSocket: xác thực ở khung STOMP CONNECT (StompAuthInterceptor).
						.requestMatchers("/ws/**").permitAll()
						.requestMatchers("/api/admin/**").hasRole("ADMIN")
						.requestMatchers("/api/teacher/**").hasRole("TEACHER")
						.requestMatchers("/api/student/**").hasRole("STUDENT")
						.anyRequest().authenticated());
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(AppProperties props) {
		CorsConfiguration config = new CorsConfiguration();
		// Nhận cả mẫu (vd https://*.trycloudflare.com khi demo qua tunnel), giống endpoint WebSocket.
		config.setAllowedOriginPatterns(props.cors().allowedOrigins());
		config.addAllowedMethod("*");
		config.addAllowedHeader("*");
		config.addExposedHeader("Content-Disposition");
		config.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
