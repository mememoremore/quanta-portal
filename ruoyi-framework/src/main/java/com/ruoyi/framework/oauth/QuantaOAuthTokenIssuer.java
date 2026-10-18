package com.ruoyi.framework.oauth;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * 使用授权服务器同一套 RS256 组件签发 ID Token。私钥与业务 JWT 密钥隔离。
 */
@Component
public class QuantaOAuthTokenIssuer
{
    public static final Duration ID_TOKEN_TTL = Duration.ofMinutes(5);

    public static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(5);

    private final QuantaOAuthProperties properties;

    private RSAKey rsaKey;

    public QuantaOAuthTokenIssuer(QuantaOAuthProperties properties)
    {
        this.properties = properties;
    }

    @PostConstruct
    void loadKey()
    {
        if (properties.ready())
        {
            this.rsaKey = parse(properties.getPrivateKeyPem(), properties.getKeyId());
        }
    }

    public boolean ready()
    {
        return properties.ready() && rsaKey != null;
    }

    public Map<String, Object> publicJwkSet()
    {
        if (!ready())
        {
            return Map.of();
        }
        return new JWKSet(rsaKey.toPublicJWK()).toJSONObject();
    }

    public String idToken(String subject, String name, String nonce, Instant issuedAt)
    {
        return sign(claims(subject, name, nonce, issuedAt, ID_TOKEN_TTL)
                .claim("quanta_member", Boolean.TRUE)
                .build());
    }

    public String accessToken(String subject, Instant issuedAt)
    {
        return sign(claims(subject, null, null, issuedAt, ACCESS_TOKEN_TTL)
                .claim("token_use", "oauth_access")
                .build());
    }

    private JWTClaimsSet.Builder claims(String subject, String name, String nonce, Instant issuedAt, Duration ttl)
    {
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .issuer(properties.getIssuer())
                .subject(subject)
                .audience(properties.getClientId())
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(issuedAt.plus(ttl)));
        if (name != null)
        {
            builder.claim("name", name);
        }
        if (nonce != null)
        {
            builder.claim("nonce", nonce);
        }
        return builder;
    }

    private String sign(JWTClaimsSet claims)
    {
        try
        {
            SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(properties.getKeyId())
                    .type(JOSEObjectType.JWT).build(), claims);
            jwt.sign(new RSASSASigner(rsaKey));
            return jwt.serialize();
        }
        catch (Exception ex)
        {
            throw new IllegalStateException("ID Token 签发失败");
        }
    }

    static RSAKey parse(String pem, String keyId)
    {
        try
        {
            String body = pem.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(body));
            RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
            RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(
                    new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent()));
            return new RSAKey.Builder(publicKey).privateKey(privateKey).keyID(keyId).algorithm(JWSAlgorithm.RS256)
                    .build();
        }
        catch (Exception ex)
        {
            throw new IllegalStateException("OAuth 签名私钥无法解析");
        }
    }

    static Map<String, Object> discovery(String issuer)
    {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("issuer", issuer);
        document.put("authorization_endpoint", issuer + "/oauth2/authorize");
        document.put("token_endpoint", issuer + "/oauth2/token");
        document.put("jwks_uri", issuer + "/oauth2/jwks");
        document.put("response_types_supported", java.util.List.of("code"));
        document.put("grant_types_supported", java.util.List.of("authorization_code"));
        document.put("subject_types_supported", java.util.List.of("public"));
        document.put("id_token_signing_alg_values_supported", java.util.List.of("RS256"));
        document.put("code_challenge_methods_supported", java.util.List.of("S256"));
        document.put("token_endpoint_auth_methods_supported", java.util.List.of("client_secret_basic"));
        document.put("scopes_supported", java.util.List.of("openid", "profile"));
        return document;
    }
}
