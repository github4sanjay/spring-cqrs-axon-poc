package com.example.auth.device;

import com.example.auth.common.OpenApi;
import com.example.auth.token.Claims;
import com.example.spring.core.json.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Validated
@RequestMapping
@Tag(name = "Device API", description = "Set of endpoints to provide managing devices")
public interface DeviceAPI {

  String DEVICE_API = "/api/v1/devices";
  String DEVICE_TRUST_API = "/api/v1/devices/trust";
  String DEVICE_ID_TRUST_API = "/api/v1/devices/{deviceId}/trust";

  @Operation(summary = "Register device", description = "Create a client code for a new device.")
  @ApiResponse(responseCode = "200")
  @ApiResponse(
      responseCode = "400",
      description = "Bad Request",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @PostMapping(value = DEVICE_API)
  Mono<DeviceRegistrationResponse> registerDevice(@RequestBody @Valid DeviceRequest deviceRequest);

  @Operation(summary = "Update device", description = "Update device information.")
  @SecurityRequirement(name = OpenApi.JWT)
  @ApiResponse(responseCode = "204")
  @ApiResponse(
      responseCode = "400",
      description = "Bad Request",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @PutMapping(value = DEVICE_API)
  Mono<ResponseEntity<Void>> updateDevice(
      @Valid @RequestBody DeviceUpdateRequest request, @RequestAttribute("device") Device device);

  @Operation(
      summary = "Trust device",
      description =
          "Mark the current device as *trusted*. This will allow bypassing MFA on future logins.")
  @SecurityRequirement(name = OpenApi.JWT)
  @ApiResponse(responseCode = "204")
  @ApiResponse(
      responseCode = "404",
      description = "Not Found",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @PostMapping(value = DEVICE_TRUST_API)
  Mono<ResponseEntity<Void>> trust(Claims claims, @RequestAttribute("device") Device device);

  @Operation(
      summary = "Untrust device",
      description =
          "Remove the *trusted* status of this device. The user might be forced to use MFA on their next login. Any biometric tokens tied to this device will be invalidated.")
  @SecurityRequirement(name = OpenApi.JWT)
  @ApiResponse(responseCode = "204")
  @ApiResponse(
      responseCode = "404",
      description = "Not Found",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  @DeleteMapping(value = DEVICE_ID_TRUST_API)
  Mono<ResponseEntity<Void>> deleteTrust(
      @RequestAttribute("device") Device device, @PathVariable String deviceId, Claims claims);

  @Operation(
      summary = "List devices",
      description = "List all devices that have been used to login to the current OnePass account.")
  @SecurityRequirement(name = OpenApi.JWT)
  @ApiResponse(responseCode = "200")
  @GetMapping(value = DEVICE_API)
  Mono<List<DeviceResponse>> getDevices(
      @RequestParam(required = false) Boolean isTrusted, Claims claims);

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(name = "DeviceUpdateRequest", description = "Sample request to update device info")
  class DeviceUpdateRequest {

    @Schema(name = "name", description = "Name of the device", example = "My iPhone X")
    private String name;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @Schema(
      name = "DeviceRegistrationResponse",
      description = "Class representing a device registration response to client")
  class DeviceRegistrationResponse {
    @Schema(
        name = "id",
        description = "Id for the device",
        example = "71ac268b-c88f-4ef8-a8cd-8a329805c4b1.c0d3cfa8-1c5b-4de0-b4ac-66e6178fc575")
    private String id;

    @Schema(
        name = "key",
        description = "key for the device",
        example =
            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDIICdFFC9+KZHMrV/bQSlqoAvYjYb6ntYXE6dMp3oaIDVH8Q0Z2f8d+EIqabma3iJbdun7Dk22I1824rq140qmEhBTSmRxWt6AFA1CGTSEBi5bkrtA/wb4P9Bvv2kMCCf3rV1pjQWPr2Aa5iV+Cv5eWk0o8YOK1J/4WzKaHpruQQIDAQAB")
    private String key;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(name = "Device", description = "Class representing a device tracked by the application")
  class DeviceRequest implements Serializable {

    @Schema(name = "client", description = "Name of the client", example = "web-app")
    @NotEmpty(message = "Device client cannot be empty")
    private String client;

    @Schema(name = "name", description = "Name of the device", example = "My iPhone X")
    private String name;

    @Schema(name = "model", description = "Model number of the device", example = "SM-G975F")
    @NotBlank(message = "Device model cannot be empty")
    private String model;

    @Schema(name = "manufacturer", description = "Manufacturer of the device", example = "Iphone")
    @NotBlank(message = "Manufacturer cannot be empty")
    private String manufacturer;

    @Schema(name = "os", description = "Operating system of the device", example = "Windows")
    @NotBlank(message = "OS cannot be empty")
    private String os;

    public void setName(String name) {
      this.name = StringUtils.isBlank(name) ? null : name;
    }
  }

  @Data
  @AllArgsConstructor
  @Builder
  @Schema(name = "DeviceResponse", description = "Class representing a device response to client")
  class DeviceResponse {

    @Schema(
        name = "id",
        description = "Id of the client",
        example = "42a5e856-9661-4e9a-bba6-54b8f7145f5f")
    private String id;

    @Schema(name = "client", description = "Name of the client", example = "msta")
    private String client;

    @Schema(name = "name", description = "Name of the device", example = "My iPhone X")
    private String name;

    @Schema(name = "model", description = "Model number of the device", example = "SM-G975F")
    private String model;

    @Schema(name = "manufacturer", description = "Manufacturer of the device", example = "Iphone")
    private String manufacturer;

    @Schema(name = "os", description = "Operating system of the device", example = "Windows")
    private String os;

    @Schema(name = "trusted", description = "Device trusted or not", example = "true")
    private Boolean trusted;

    @Schema(name = "lastLoginAt", description = "Last login time", example = "true")
    private Instant lastLoginAt;
  }
}
