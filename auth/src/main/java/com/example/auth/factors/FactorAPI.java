package com.example.auth.factors;

import com.example.auth.common.OpenApi;
import com.example.auth.device.Device;
import com.example.auth.token.Claims;
import com.example.spring.core.json.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Validated
@RestController
@Tag(name = "Factor API", description = "Set of endpoints to manage authentication factors")
public interface FactorAPI {

  String FACTOR_API = "/api/v1/factors/{factor-type}";
  String FACTOR_EMAIL_API = "/api/v1/factors/email";
  String FACTOR_VERIFY_EMAIL_API = "/api/v1/factors/email/verify";
  String FACTOR_CHALLENGE_EMAIL_API = "/api/v1/factors/email/challenge";

  String FACTOR_TOTP_API = "/api/v1/factors/totp";
  String FACTOR_TOTP_RECOVERY_API = "/api/v1/factors/totp/recovery";
  String FACTOR_VERIFY_TOTP_API = "/api/v1/factors/totp/verify";

  @Operation(
      summary = "Create email factor",
      description =
          "Create an email factor which will be used as an additional factor at the time of authentication.")
  @ApiResponse(responseCode = "200")
  @ApiResponse(
      responseCode = "400",
      description = "Bad Request",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @SecurityRequirement(name = OpenApi.JWT)
  @PostMapping(value = FACTOR_EMAIL_API)
  Mono<ChallengeEmailFactorResponse> createEmailFactor(
      Claims claims, @RequestAttribute("device") Device device);

  @Operation(
      summary = "Challenge email factor",
      description = "Challenge email factor which will send an email otp to registered email.")
  @ApiResponse(responseCode = "200")
  @ApiResponse(
      responseCode = "400",
      description = "Bad Request",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @SecurityRequirement(name = OpenApi.JWT)
  @PostMapping(value = FACTOR_CHALLENGE_EMAIL_API)
  Mono<ChallengeEmailFactorResponse> challengeEmailFactor(
      Claims claims, @RequestAttribute("device") Device device);

  @Operation(
      summary = "Delete a factor",
      description =
          "Delete a factor and after this factor will not be used as an additional factor at the time of authentication.")
  @ApiResponse(responseCode = "204")
  @ApiResponse(
      responseCode = "400",
      description = "Bad Request",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @SecurityRequirement(name = OpenApi.JWT)
  @DeleteMapping(value = FACTOR_API)
  Mono<ResponseEntity<Void>> deleteEmailFactor(
      Claims claims,
      @RequestAttribute("device") Device device,
      @PathVariable("factor-type") FactorType factorType);

  @Operation(
      summary = "Verify an email factor",
      description =
          "Verify an email factor which will be used as an additional factor at the time of authentication.")
  @ApiResponse(responseCode = "204")
  @ApiResponse(
      responseCode = "400",
      description = "Bad Request",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @SecurityRequirement(name = OpenApi.JWT)
  @PostMapping(value = FACTOR_VERIFY_EMAIL_API)
  Mono<ResponseEntity<Void>> verifyEmailFactor(
      @RequestBody VerifyEmailFactorRequest request,
      Claims claims,
      @RequestAttribute("device") Device device);

  @Operation(
      summary = "Create totp factor",
      description =
          "Create totp factor which will be used as an additional factor at the time of authentication.")
  @ApiResponse(responseCode = "200")
  @ApiResponse(
      responseCode = "400",
      description = "Bad Request",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @SecurityRequirement(name = OpenApi.JWT)
  @PostMapping(value = FACTOR_TOTP_API)
  Mono<TotpFactorResponse> createTotpFactor(
      Claims claims, @RequestAttribute("device") Device device);

  @Operation(
      summary = "Recover totp factor",
      description =
          "Recover top secrets with recovery code provided during totp factor activation process")
  @ApiResponse(responseCode = "200")
  @ApiResponse(
      responseCode = "400",
      description = "Bad Request",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @SecurityRequirement(name = OpenApi.JWT)
  @PostMapping(value = FACTOR_TOTP_RECOVERY_API)
  Mono<TotpFactorResponse> recoverTotpFactor(
      @RequestBody TotpFactorRecoveryRequest request,
      Claims claims,
      @RequestAttribute("device") Device device);

  @Operation(
      summary = "Verify totp factor",
      description =
          "Verify totp factor which will be used as an additional factor at the time of authentication.")
  @ApiResponse(responseCode = "204")
  @ApiResponse(
      responseCode = "400",
      description = "Bad Request",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @SecurityRequirement(name = OpenApi.JWT)
  @PostMapping(value = FACTOR_VERIFY_TOTP_API)
  Mono<ResponseEntity<Void>> verifyTotpFactor(
      @RequestBody VerifyTotpFactorRequest request,
      Claims claims,
      @RequestAttribute("device") Device device);

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(
      name = "ChallengeEmailFactorResponse",
      description = "Class representing challenge email factor response")
  class ChallengeEmailFactorResponse {

    @Schema(
        name = "challengeToken",
        description = "Challenge token",
        example = "00d79e72-de48-411b-94db-b0e824e11d9d")
    private String challengeToken;

    @Schema(name = "retryAfter", description = "Retry after seconds", example = "60")
    private Long retryAfter;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(name = "TotpFactorResponse", description = "Class representing totp factor response")
  class TotpFactorResponse {

    @Schema(name = "data", description = "Data for totp")
    private QrDataResponse data;

    @Schema(
        name = "qrCode",
        description = "image data as a base64 string which can be used in an <img> tag",
        example =
            "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAV4AAAFeAQAAAADlUEq3AAADAElEQVR4Xu2ZPZbqMAyFxaGgZAlZCktLlsZSsgRKCg56uldKxpk4M7x6rooxtj9TXPRnj/nn9rLvKz+Y4NYEtya4NcGt/Q34bbQYrjEzu/h8m5bh/OTmWXAXxiefYnLF4M/rfPMT1u4jZgsiuAOHlreJOt8tZtwOcYdQPWZz/AaCf4YvDwuBX5fH4JiFzib4I5jrY6w/BsjtPAqHFXwMxx+ENSlLCosM8l10C25go1HnkrsduCm4Cy+WNaXqMJWlpzb7X58FrzojFWZYr05JgSNN+ryJbsEtfOZ2ro/wxhgwg+pMmtnXCO7AyIFT+GboPLKhAYV9emrsCe7D0SNbUCcMnDK66yKSFYYmuANzOyYIckudEd1UnS2h4D5cklZirDNrNx17d4a84D1c247ECLkzyNHJGA3iC+7C6YbOCxrLB+SGi1oe3dYUwVsY/V7KjTVEd6rOLxJ8CIdhgZXXcAYwi7Nz0eGwgrsw3RD9HvIjJaX4KCZVWrgkeA/jsoHKa4juN3JhhHUFuaGptiaLCt7A9M3qnzFYeWrc4V74hmnjz4KbgDW86hnyY4Z16Mw7HP02Tm6iW/BG54xuRw6smy6CfF7S5K6mCE74jZsZXwfgm6eqMIx1qj4IPoQtjXfbCcszBGZGJIXSIvgA5s2MGXFk21cDgjw6w3ueEbyDq5PJB9EpZ446zDQJT8Vri+Ae7E+8fXqVFsb64rDsDL81gYJXGJLO0BnbkBQuitkAnRHkgg/g0rkeo1hMUu6Bm2ijBfdh3swykKNjrgGFZoVHwX2YC+7VtLAO39jeBEW5tz+K4AbOmvLG455Zqu64BcNTYdneCO7AaVmAHVVkRmLkKym6P84E92AEcljmR/wHI7aNZ7Icz6w3gnswOrzq/uCibGH4DYAdsY5BcA+mwIznm9fwXm4g6xnBhzCHvK5lAU6YDaLgX+GlAPuUfuv1XiX4CMZywqQocGZE7M2bmiJ4X2FzO28gjidl5Md6exHchz81wa0Jbk1wa4JbE9za/8H/AKOu2UPfZDUhAAAAAElFTkSuQmCC")
    private String qrCode;

    @Schema(
        name = "recoveryCode",
        description = "recovery code to get totp secret",
        example =
            """
            [
                "8hc5-h6kp-9oqk-5mha",
                "elhv-nrfn-fyvf-u5xs",
                "hog2-ef0f-785x-ugzm",
                "ptur-x07c-zhbl-3w55",
                "y661-c7iy-24nv-s4zb",
                "5te2-w3fr-00yl-rj9h",
                "ltg1-b9jj-grwk-970q",
                "vr2v-5pra-xs9w-7tn2",
                "tgvc-3nf9-5uah-nfwu",
                "smnb-ebmf-gskg-9foz",
                "thxy-b7d1-pxen-oai0",
                "9pbu-9j3j-bzrb-jx0x",
                "1mm8-8s0w-13aq-1h7k",
                "32qy-dgve-p6en-en8e",
                "tlru-zwe7-eljo-avup",
                "hkhf-4ill-6udn-0mtd"
              ]
            """)
    private String[] recoveryCode;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(name = "QrDataResponse", description = "Class representing qr data response")
  class QrDataResponse {

    @Schema(name = "type", description = "type of otp", example = "totp")
    private String type;

    @Schema(name = "label", description = "label of totp", example = "admin@smrt.com")
    private String label;

    @Schema(
        name = "secret",
        description = "secret of totp",
        example = "GMLHTO27JXSXJAIMLEPLNWWIWPOIASFO")
    private String secret;

    @Schema(name = "issuer", description = "issuer of totp", example = "web-app")
    private String issuer;

    @Schema(name = "algorithm", description = "algorithm of totp", example = "SHA1")
    private String algorithm;

    @Schema(name = "digits", description = "digits of totp", example = "6")
    private Integer digits;

    @Schema(name = "period", description = "period of totp", example = "30")
    private Integer period;

    @Schema(
        name = "uri",
        description = "uri of totp",
        example =
            "otpauth://totp/admin%40smrt.com?secret=GMLHTO27JXSXJAIMLEPLNWWIWPOIASFO&issuer=web-app&algorithm=SHA1&digits=6&period=30")
    private String uri;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(
      name = "TotpFactorRecoveryRequest",
      description = "Class representing totp factor recovery request")
  class TotpFactorRecoveryRequest {

    @Schema(
        name = "recoveryCode",
        description = "recovery code to get totp secret",
        example =
            """
                    [
                        "8hc5-h6kp-9oqk-5mha",
                        "elhv-nrfn-fyvf-u5xs",
                        "hog2-ef0f-785x-ugzm",
                        "ptur-x07c-zhbl-3w55",
                        "y661-c7iy-24nv-s4zb",
                        "5te2-w3fr-00yl-rj9h",
                        "ltg1-b9jj-grwk-970q",
                        "vr2v-5pra-xs9w-7tn2",
                        "tgvc-3nf9-5uah-nfwu",
                        "smnb-ebmf-gskg-9foz",
                        "thxy-b7d1-pxen-oai0",
                        "9pbu-9j3j-bzrb-jx0x",
                        "1mm8-8s0w-13aq-1h7k",
                        "32qy-dgve-p6en-en8e",
                        "tlru-zwe7-eljo-avup",
                        "hkhf-4ill-6udn-0mtd"
                      ]
                    """)
    @NotNull
    private String[] recoveryCode;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(
      name = "VerifyEmailFactorRequest",
      description = "Class representing verify email factor request")
  class VerifyEmailFactorRequest {

    @Schema(
        name = "challengeToken",
        description = "Challenge token",
        example = "00d79e72-de48-411b-94db-b0e824e11d9d")
    @NotBlank
    private String challengeToken;

    @Schema(name = "otp", description = "One time password code sent on email", example = "602332")
    @NotBlank
    private String otp;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(
      name = "VerifyTotpFactorRequest",
      description = "Class representing verify totp factor request")
  class VerifyTotpFactorRequest {

    @Schema(name = "otp", description = "totp code sent on authenticator app", example = "602332")
    @NotBlank
    private String otp;
  }
}
