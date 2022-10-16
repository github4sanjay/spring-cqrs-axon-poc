package com.example.auth.token;

import com.example.auth.device.Device;
import com.example.spring.core.json.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.Serializable;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Validated
@RequestMapping
@Tag(name = "Token API", description = "Set of endpoints to to manage tokens")
public interface TokenAPI {

  String TOKEN_API = "/api/v1/token";
  String TOKEN_REFRESH_API = "/api/v1/token/refresh";
  String WELL_KNOWN_JWKS_JSON = "/.well-known/jwks.json";

  @Operation(summary = "Account Token", description = "Create tokens for an account.")
  @ApiResponse(responseCode = "200", useReturnTypeSchema = true)
  @ApiResponse(
      responseCode = "400",
      description = "Bad Request",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @PostMapping(value = TOKEN_API)
  Mono<TokenResponse> accountToken(
      @RequestBody @Valid AccountTokenRequest request, @RequestAttribute("device") Device device);

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(name = "AccountTokenRequest", description = "Class representing an account token request")
  class AccountTokenRequest implements Serializable {

    @Schema(name = "email", description = "Email Id", example = "admin@smrt.com")
    @NotBlank(message = "email id is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank
    @Schema(name = "password", description = "Password for the account", example = "my-password")
    private String password;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(name = "TokenResponse", description = "Sample auth token response object")
  class TokenResponse {
    @Schema(
        name = "accessToken",
        description = "Access Token",
        example =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJtc3RhIiwic3ViIjoib25lcGFzc3wxMDk2MDYyfDEwOTYwNjIiLCJhbXIiOlsicHdkIl0sImlzcyI6ImF1dGgiLCJleHAiOjE1OTE3MDM3ODUsImlhdCI6MTU5MTcwMzE4NX0.1rq4lKzN7hKXQX9j-YcEyvlvKQBjs4twbQCdL4Ohd08")
    String accessToken;

    @Schema(
        name = "refreshToken",
        description = "Refresh Token",
        example = "00d79e72-de48-411b-94db-b0e824e11d9d")
    String refreshToken;
  }

  @Operation(
      summary = "Refresh Token",
      description =
          "Creates a new access token and refresh token pair. Invalidates the current refresh token.")
  @ApiResponse(responseCode = "200")
  @ApiResponse(
      responseCode = "401",
      description = "Unauthorized",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Bad Request",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @PostMapping(value = TOKEN_REFRESH_API)
  Mono<TokenResponse> refreshToken(
      @RequestBody @Valid RefreshTokenRequest request, @RequestAttribute("device") Device device);

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(name = "RefreshTokenRequest", description = "Sample refresh token request")
  public static class RefreshTokenRequest {
    @Schema(
        name = "refreshToken",
        description = "Refresh Token",
        example =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJtc3RhIiwic3ViIjoib25lcGFzc3wxMDk2MDYyfDEwOTYwNjIiLCJhbXIiOlsicHdkIl0sImlzcyI6ImF1dGgiLCJleHAiOjE1OTE3MDM3ODUsImlhdCI6MTU5MTcwMzE4NSwianRpIjoiMzM3YzBhMWMtYTg2Yi00NDQ2LTllMmYtZTE2OTRmNGI0OWJmIn0.BdnPJOOh0AjraUMsCcVa6S6iqHAkQKCO-LvsEHK60zk")
    @NotBlank(message = "refreshToken cannot be empty")
    private String refreshToken;
  }

  @Operation(
      summary = "Expose keys as JWKS",
      description = "Retrieve the keys that are needed to verify JWT tokens.")
  @ApiResponse(responseCode = "200")
  @GetMapping(WELL_KNOWN_JWKS_JSON)
  Mono<JwksResponse> jwks();

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "Sample JWKS response")
  @Builder
  class JwksResponse {
    @Schema(
        description = "Contains array of JWKs.",
        example =
            """
            [
              {
                "kty": "RSA",
                "e": "AQAB",
                "use": "sig",
                "kid": "c07aa2f2-c7fd-11ea-87d0-0242ac130003",
                "n": "51o1jVIaelEvwNqK3U9Z_8k81pLAA4mwSPvSme-xdb6BJgtYn5vETS4sdyCcECEsZ2gIGohx4smWQoe2bUcZqM78E9vhv_EBfSXnmC-tDCbptCJmT0h-Bz_ywsxGE-SdwITEKq8BqV9azQ5yH6HhMf6KisKMH-dCQyZbYn5ZsDt2wmQslpzIrqiif5Ogx402AN1tHY5VVcRL1s3m5YKfsdTd16NCFhfgTcS4m9M2HI2Ma5lZH8xyeAafftggBxf0hko37mhS5h-MJHetRyUv6miKtkH7amcXAyAVeNmwoxhj-Dg5FwCJmKElYz5YKWKDD9N5qsqxEIURkdXo08WyhQ"
              }
            ]
            """)
    private List<Jwk> keys;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "Sample JWK")
  class Jwk {

    @Schema(name = "kty", description = "type (kty) of this JWK", example = "RSA")
    private String kty;

    @Schema(name = "e", description = "the public RSA key exponent", example = "AQAB")
    private String e;

    @Schema(name = "use", description = "the use (use) of this JWK", example = "sig")
    private String use;

    @Schema(
        name = "kid",
        description =
            "Gets the ID (kid) of this JWK. The key ID can be used to match a specific key. This can be used, for instance, to choose a key within a JWKSet during key rollover",
        example = "c07aa2f2-c7fd-11ea-87d0-0242ac130003")
    private String kid;

    @Schema(
        name = "n",
        description = "the modulus value (n) of the RSA key",
        example =
            "51o1jVIaelEvwNqK3U9Z_8k81pLAA4mwSPvSme-xdb6BJgtYn5vETS4sdyCcECEsZ2gIGohx4smWQoe2bUcZqM78E9vhv_EBfSXnmC-tDCbptCJmT0h-Bz_ywsxGE-SdwITEKq8BqV9azQ5yH6HhMf6KisKMH-dCQyZbYn5ZsDt2wmQslpzIrqiif5Ogx402AN1tHY5VVcRL1s3m5YKfsdTd16NCFhfgTcS4m9M2HI2Ma5lZH8xyeAafftggBxf0hko37mhS5h-MJHetRyUv6miKtkH7amcXAyAVeNmwoxhj-Dg5FwCJmKElYz5YKWKDD9N5qsqxEIURkdXo08WyhQ")
    private String n;
  }
}
