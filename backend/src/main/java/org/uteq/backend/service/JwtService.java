package org.uteq.backend.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms:86400000}")
    private long expirationMs;

    private Key key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // ─── Generación ───────────────────────────────────────────────────────

    /** Versión original —  */
    public String generateToken(String username, List<String> roles) {
        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** Con token_version —  */
    public String generateToken(String username, List<String> roles, int tokenVersion) {
        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .claim("tv", tokenVersion)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Incluye el usuario de PostgreSQL en el token para que el interceptor
     * pueda inyectarlo en la variable de sesión app.usuario_bd,
     * que los triggers leen para auditoría de cambios.
     */
    public String generateToken(String username, List<String> roles, int tokenVersion, String usuarioBd) {
        return Jwts.builder()
                .setSubject(username)
                .claim("roles",      roles)
                .claim("tv",         tokenVersion)
                .claim("usuario_bd", usuarioBd)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ─── Extracción ───────────────────────────────────────────────────────

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return (List<String>) parseClaims(token).get("roles");
    }

    public Integer extractTokenVersion(String token) {
        Object tv = parseClaims(token).get("tv");
        if (tv instanceof Integer) return (Integer) tv;
        if (tv instanceof Long)    return ((Long) tv).intValue();
        return null;
    }

    /**
     * Extrae el usuario de PostgreSQL del claim "usuario_bd".
     * Devuelve null si el token fue emitido antes de este cambio (compatibilidad).
     */
    public String extractUsuarioBd(String token) {
        Object val = parseClaims(token).get("usuario_bd");
        return val instanceof String ? (String) val : null;
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}