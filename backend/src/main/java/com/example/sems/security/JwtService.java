package com.example.sems.security;

import com.example.sems.common.BadRequestException;
import com.example.sems.common.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Dependency-free HS256 JWT implementation (no third-party JWT library needed).
 * Token claims: sub=userId, role, iat, exp.
 */
@Component
public class JwtService {

    @Value("${sems.jwt.secret}")
    private String secret;

    @Value("${sems.jwt.expiry-minutes:1440}")
    private long expiryMinutes;

    private SecretKeySpec key() {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    public String generate(Long userId, Role role) {
        long nowSec = System.currentTimeMillis() / 1000;
        String header = b64url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = b64url("{\"sub\":\"" + userId + "\",\"role\":\"" + role.name()
                + "\",\"iat\":" + nowSec + ",\"exp\":" + (nowSec + expiryMinutes * 60) + "}");
        String signingInput = header + "." + payload;
        return signingInput + "." + b64url(hmac(signingInput));
    }

    public Claims parse(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new BadRequestException("Invalid token");
        }
        String signingInput = parts[0] + "." + parts[1];
        if (!b64url(hmac(signingInput)).equals(parts[2])) {
            throw new BadRequestException("Invalid token signature");
        }
        String json = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        Long sub = extractLong(json, "sub");
        String role = extract(json, "role");
        Long exp = extractLong(json, "exp");
        if (exp != null && exp * 1000 < System.currentTimeMillis()) {
            throw new BadRequestException("Token expired");
        }
        return new Claims(sub, role == null ? null : Role.valueOf(role), exp);
    }

    private byte[] hmac(String input) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key());
            return mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String b64url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String b64url(String s) {
        return b64url(s.getBytes(StandardCharsets.UTF_8));
    }

    private Long extractLong(String json, String key) {
        String v = extract(json, key);
        return v == null ? null : Long.parseLong(v);
    }

    private String extract(String json, String key) {
        int i = json.indexOf("\"" + key + "\"");
        if (i < 0) {
            return null;
        }
        int colon = json.indexOf(':', i);
        int p = colon + 1;
        while (p < json.length() && (json.charAt(p) == ' ' || json.charAt(p) == '\t')) {
            p++;
        }
        if (json.charAt(p) == '"') {
            int start = p + 1;
            int end = json.indexOf('"', start);
            return json.substring(start, end);
        }
        // unquoted value (e.g. numbers like iat/exp): read until ',' or '}'
        int start = p;
        int end = start;
        while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') {
            end++;
        }
        return json.substring(start, end).trim();
    }

    public record Claims(Long sub, Role role, Long exp) {
    }
}
