package com.example.auth.common;

import com.example.auth.AuthException;
import com.example.auth.token.Claims;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import org.springdoc.core.SpringDocUtils;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

@Configuration
public class OpenApi {

  public static final String OPEN_TD = "<td>";
  public static final String CLOSE_TD = "</td>";
  public static final String JWT = "Authorization";
  public static final String CLIENT_CODE = "x-client-code";
  public static final String SIGNATURE_HEADER = "x-signature";

  @PostConstruct
  void config() {
    SpringDocUtils.getConfig().addRequestWrapperToIgnore(Claims.class);
  }

  @Bean
  public OpenAPI documentation(@Nullable BuildProperties build) {
    return new OpenAPI()
        .components(
            new Components()
                .addParameters(
                    CLIENT_CODE,
                    new HeaderParameter()
                        .required(true)
                        .name(CLIENT_CODE)
                        .description("Client Code")
                        .schema(new StringSchema()))
                .addParameters(
                    SIGNATURE_HEADER,
                    new HeaderParameter()
                        .required(false)
                        .name(SIGNATURE_HEADER)
                        .description("Signature")
                        .schema(new StringSchema()))
                .addSecuritySchemes(
                    JWT,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
        .info(
            new Info()
                .title("Authentication Service")
                .description("Rest api specification for auth service")
                .description(
                    Arrays.stream(AuthException.values())
                        .map(
                            status ->
                                "<tr>"
                                    + OPEN_TD
                                    + status.getEx().getCode()
                                    + CLOSE_TD
                                    + OPEN_TD
                                    + status.getEx().getStatus()
                                    + CLOSE_TD
                                    + OPEN_TD
                                    + status.getEx().getDescription()
                                    + CLOSE_TD
                                    + "</tr>")
                        .collect(
                            Collectors.joining(
                                "",
                                "<h4>List of Error Codes</h4><table><tr><th>Application Error Code</th><th>HTTP Status Code</th><th>Description</th></tr>",
                                "</table>")))
                .version(build == null ? "snapshot" : "v" + build.getVersion())
                .license(new License().name("Example").url("http://www.example.com")));
  }

  @Bean
  public OperationCustomizer operationCustomizer() {
    return (operation, method) -> {
      var responses = operation.getResponses();
      if (operation.getSecurity() != null && !operation.getSecurity().isEmpty()) {
        var mediaType =
            new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"));
        var content = new Content().addMediaType("application/json", mediaType);
        responses.putIfAbsent(
            "401", new ApiResponse().description("Unauthorized").content(content));
      }
      if (!responses.containsKey("500")) {
        var mediaType =
            new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"));
        var content = new Content().addMediaType("application/json", mediaType);
        responses.putIfAbsent(
            "500", new ApiResponse().description("Internal server error").content(content));
      }
      return operation;
    };
  }

  @Bean
  public OpenApiCustomiser openApiCustomiser() {
    return openApi ->
        openApi.getPaths().values().stream()
            .flatMap(
                pathItem ->
                    pathItem.readOperations().stream()
                        .filter(
                            operation -> isClientCodeInHeaderRequired(operation.getOperationId())))
            .forEach(
                operation ->
                    operation
                        .addParametersItem(
                            new HeaderParameter().$ref("#/components/parameters/" + CLIENT_CODE))
                        .addParametersItem(
                            new HeaderParameter()
                                .$ref("#/components/parameters/" + SIGNATURE_HEADER)));
  }

  private boolean isClientCodeInHeaderRequired(String operation) {
    return Stream.of("registerDevice", "onepassInvalidateTokenRequest", "jwks", "introspect")
        .noneMatch(method -> method.equals(operation));
  }
}
