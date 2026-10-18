package com.examease;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Configuration @EnableMethodSecurity
class SecurityConfig {
    @Bean PasswordEncoder encoder(){ return new BCryptPasswordEncoder(); }
    @Bean UserDetailsService details(Users users){
        return username -> users.findByUsername(username)
            .map(a -> User.withUsername(a.username).password(a.passwordHash).roles(a.role.name()).build())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
    @Bean DaoAuthenticationProvider provider(UserDetailsService details,PasswordEncoder encoder){
        DaoAuthenticationProvider p=new DaoAuthenticationProvider(); p.setUserDetailsService(details); p.setPasswordEncoder(encoder); return p;
    }
    @Bean AuthenticationManager manager(DaoAuthenticationProvider provider){ return new ProviderManager(provider); }
    @Bean SecurityFilterChain chain(HttpSecurity http,JwtFilter filter,DaoAuthenticationProvider provider) throws Exception {
        http.csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(provider)
            .authorizeHttpRequests(a->a.requestMatchers("/api/auth/login","/api/auth/register","/error").permitAll().anyRequest().authenticated())
            .addFilterBefore(filter,UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

@org.springframework.stereotype.Component
class JwtTokens {
    private final SecretKey key;
    JwtTokens(@Value("${app.jwt.secret}") String secret){
        if(secret.getBytes(StandardCharsets.UTF_8).length<32) throw new IllegalArgumentException("JWT secret must have at least 32 bytes");
        key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    String create(String username){ return Jwts.builder().subject(username).issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis()+86400000)).signWith(key).compact(); }
    String verify(String token){ return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject(); }
}

@org.springframework.stereotype.Component
class JwtFilter extends OncePerRequestFilter {
    private final JwtTokens tokens; private final UserDetailsService users;
    JwtFilter(JwtTokens tokens,UserDetailsService users){this.tokens=tokens;this.users=users;}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
        throws ServletException,IOException {
        String h=request.getHeader("Authorization");
        if(h!=null && h.startsWith("Bearer ")) try {
            String username=tokens.verify(h.substring(7));
            UserDetails user=users.loadUserByUsername(username);
            var auth=new UsernamePasswordAuthenticationToken(user,null,user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch(RuntimeException ignored) { SecurityContextHolder.clearContext(); }
        chain.doFilter(request,response);
    }
}
