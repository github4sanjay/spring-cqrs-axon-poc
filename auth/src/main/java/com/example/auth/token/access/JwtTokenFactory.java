package com.example.auth.token.access;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.auth.AuthException;
import com.example.auth.common.ClientConfiguration;
import com.example.auth.token.Claims;
import com.example.auth.token.access.keys.PublicSigningKey;
import com.example.auth.token.access.keys.SigningKeyService;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenFactory {

  private final JWKSSettings jwksSettings;
  private final SigningKeyService signingKeyService;
  private final ClientConfiguration clientConfig;

  @Autowired
  public JwtTokenFactory(
      JWKSSettings jwksSettings,
      SigningKeyService signingKeyService,
      ClientConfiguration clientConfig) {
    this.jwksSettings = jwksSettings;
    this.signingKeyService = signingKeyService;
    this.clientConfig = clientConfig;
  }

  public AccessJwtToken createAccessJwtToken(Claims claims, String client, String identifier) {
    var configuration = clientConfig.getCurrentClient(client);
    var signingKey = signingKeyService.getCurrentSigningKey();
    var tokenBuilder =
        JWT.create()
            .withKeyId(signingKey.getId())
            .withIssuer(jwksSettings.getIssuer())
            .withIssuedAt(Date.from(Instant.now()))
            .withExpiresAt(
                Date.from(Instant.now().plus(configuration.getJwt().getAccessTokenExpiry())))
            .withAudience(claims.getAud())
            .withSubject(claims.getSubject().get())
            .withClaim("amr", List.of(claims.getAmr().name()));

    var customClaims = claims.getCustomClaims();
    customClaims.forEach(tokenBuilder::withClaim);
    tokenBuilder.withClaim("flags", Claims.getFlags(configuration, identifier));
    var token = tokenBuilder.sign(signingKey.getSignAlgorithm());
    return new AccessJwtToken(token, claims);
  }

  public Claims decodeToken(String token) {
    var jwt = decode(token);
    Claims.Subject subject = null;
    Claims.AMR amr = null;
    Map<String, String> customClaims = new HashMap<>();
    String aud = null;
    for (var entrySet : jwt.getClaims().entrySet()) {
      var key = entrySet.getKey();
      var claim = entrySet.getValue();
      switch (key) {
        case "sub" -> subject = Claims.Subject.parse(claim.asString());
        case "aud" -> aud = claim.asString();
        case "amr" -> amr = Claims.AMR.valueOf(claim.asList(String.class).get(0));
        case Claims.ACCOUNT, Claims.PHONE_NUMBER, Claims.EMAIL, Claims.DEVICE -> customClaims.put(
            key, claim.asString());
        default -> {
          // skip unknown claims
        }
      }
    }

    return Claims.builder().aud(aud).amr(amr).customClaims(customClaims).subject(subject).build();
  }

  public DecodedJWT decode(String token) {
    var signingKey = getKeyFromToken(token);
    return this.parseClaims(token, signingKey, jwksSettings.getIssuer());
  }

  private PublicSigningKey getKeyFromToken(String token) {
    try {
      var decodedJwt = JWT.decode(token);
      return signingKeyService.getByKeyId(decodedJwt.getKeyId());
    } catch (JWTDecodeException exception) {
      throw AuthException.INVALID_TOKEN.getEx();
    }
  }

  private DecodedJWT parseClaims(String token, PublicSigningKey publicSigningKey, String issuer) {
    try {
      var verifier = JWT.require(publicSigningKey.getDecodeAlgorithm()).withIssuer(issuer).build();
      return verifier.verify(token);
    } catch (TokenExpiredException exception) {
      throw AuthException.EXPIRED_TOKEN.getEx();
    } catch (JWTVerificationException | IllegalArgumentException exception) {
      throw AuthException.INVALID_TOKEN.getEx();
    }
  }
}
