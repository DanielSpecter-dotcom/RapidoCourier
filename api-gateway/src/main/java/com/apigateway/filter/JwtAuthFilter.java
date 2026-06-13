package com.apigateway.filter;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Key;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret:4b6a8e8b9c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f}")
    private String jwtSecret;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 1. Las rutas de autenticación son públicas
        if (path.startsWith("/api/v1/auth/")) {
            return chain.filter(exchange);
        }

        // 2. Extraer el header Authorization
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, "No se proporcionó token de autenticación.", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        try {
            // 3. Validar y parsear el token JWT
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String email = claims.getSubject();
            String rol = claims.get("rol", String.class); // ej. "ROLE_ADMIN", "ROLE_OPERADOR", "ROLE_CLIENTE"

            if (rol == null) {
                return onError(exchange, "El token no contiene un rol válido.", HttpStatus.FORBIDDEN);
            }

            HttpMethod method = exchange.getRequest().getMethod();

            // 4. Aplicar control de acceso por roles (RF-08)
            // Regla A: Solo ADMIN puede realizar eliminaciones (DELETE)
            if (HttpMethod.DELETE.equals(method)) {
                if (!"ROLE_ADMIN".equals(rol)) {
                    return onError(exchange, "Acceso denegado: Solo el Administrador puede eliminar registros.", HttpStatus.FORBIDDEN);
                }
            }

            // Regla B: Solo ADMIN y OPERADOR pueden crear o actualizar recursos (POST, PUT, PATCH) en paquetes
            if (path.contains("/api/v1/paquetes") && 
                (HttpMethod.POST.equals(method) || HttpMethod.PUT.equals(method) || HttpMethod.PATCH.equals(method))) {
                if (!"ROLE_ADMIN".equals(rol) && !"ROLE_OPERADOR".equals(rol)) {
                    return onError(exchange, "Acceso denegado: Solo administradores u operadores pueden registrar y modificar paquetes.", HttpStatus.FORBIDDEN);
                }
            }

            // Regla C: CLIENTE solo puede consultar (GET), pero permitimos también a ADMIN y OPERADOR
            if (HttpMethod.GET.equals(method)) {
                // Se permite a todos los roles válidos. La lógica de verificar si consulta sus propios paquetes
                // se puede realizar internamente en el servicio recibiendo la cabecera del correo
            }

            // 5. Propagar email y rol en los headers downstream
            ServerHttpRequest request = exchange.getRequest().mutate()
                    .header("X-User-Email", email)
                    .header("X-User-Role", rol)
                    .build();

            return chain.filter(exchange.mutate().request(request).build());

        } catch (ExpiredJwtException e) {
            return onError(exchange, "El token de autenticación ha expirado.", HttpStatus.UNAUTHORIZED);
        } catch (JwtException | IllegalArgumentException e) {
            return onError(exchange, "Token de autenticación inválido.", HttpStatus.UNAUTHORIZED);
        }
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // Estructura genérica ApiResponse<Void> (RNF-01)
        String json = String.format("{\"success\":false,\"message\":\"%s\",\"data\":null}", message);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // Ejecutar antes de los filtros de enrutamiento
        return -100;
    }
}
