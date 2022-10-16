package com.example.auth.account;

import com.example.auth.device.Device;
import com.example.spring.core.json.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.Serializable;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Validated
@RestController
// @ReactiveFeignClient(value = "auth", configuration = ClientConfig.class)
@Tag(name = "Account API", description = "Set of endpoints to to manage account")
public interface AccountAPI {

  String ACCOUNT_API = "/api/v1/account";

  @Operation(summary = "Register an account", description = "Create an account.")
  @ApiResponse(responseCode = "200")
  @ApiResponse(
      responseCode = "400",
      description = "Bad Request",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @PostMapping(value = ACCOUNT_API)
  Mono<AccountResponse> registerAccount(
      @RequestBody @Valid AccountRequest request, @RequestAttribute("device") Device device);

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(name = "AccountRequest", description = "Class representing an account request")
  class AccountRequest implements Serializable {

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
  @Schema(name = "AccountResponse", description = "Class representing an account response")
  class AccountResponse implements Serializable {

    @Schema(
        name = "id",
        description = "Id of the account",
        example = "00d79e72-de48-411b-94db-b0e824e11d9d")
    private UUID id;
  }
}
