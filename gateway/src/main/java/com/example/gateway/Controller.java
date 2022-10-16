package com.example.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.IOException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping
public class Controller {

  private final OpenAPIService openAPIService;

  @Autowired
  public Controller(OpenAPIService openAPIService) {
    this.openAPIService = openAPIService;
  }

  @Operation(hidden = true)
  @GetMapping(value = "/api-docs", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Mono<String>> openAPI(
      @RequestParam(defaultValue = "false") boolean websocket) throws IOException {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(Duration.ofHours(1)))
        .body(Mono.just(writeJsonValue(openAPIService.get(true, websocket))));
  }

  protected String writeJsonValue(OpenAPI openAPI) throws JsonProcessingException {
    ObjectMapper objectMapper = Json.mapper();
    return objectMapper
        .writerWithDefaultPrettyPrinter()
        .forType(OpenAPI.class)
        .writeValueAsString(openAPI);
  }
}
