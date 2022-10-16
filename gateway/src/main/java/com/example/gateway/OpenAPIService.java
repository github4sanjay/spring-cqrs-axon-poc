package com.example.gateway;

import com.example.spring.core.exceptions.CoreExceptions;
import com.google.common.io.Resources;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.callbacks.Callback;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.links.Link;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.WordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OpenAPIService {

  private final List<OpenAPIConfig.Client> services;
  private final ApiMfaConfig apiMfaConfig;
  private final RouteConfiguration routeConfiguration;
  private final Set<String> apiFilter;
  private final boolean applyFilter;
  private final String serverUrl;

  @Autowired
  public OpenAPIService(
      List<OpenAPIConfig.Client> services,
      @Value("${springdoc.api-filter}") Set<String> apiFilter,
      @Value("${springdoc.apply-filter}") boolean applyFilter,
      @Value("${springdoc.server-url:null}") String serverUrl,
      ApiMfaConfig apiMfaConfig,
      RouteConfiguration routeConfiguration) {
    this.services = services;
    this.apiMfaConfig = apiMfaConfig;
    this.apiFilter = apiFilter;
    this.applyFilter = applyFilter;
    this.serverUrl = serverUrl;
    this.routeConfiguration = routeConfiguration;
  }

  public OpenAPI get(boolean async, boolean websocket) throws IOException {
    var openAPIList = getAllOpenApi3(services, async);
    var finalAPI = new OpenAPI();
    finalAPI.setOpenapi("3.0.1");
    var resolver = new PathMatchingResourcePatternResolver();
    var resource = resolver.getResource("/docs/description.md");
    var content = Resources.toString(resource.getURL(), StandardCharsets.UTF_8);
    var initialContent = websocket ? content : "";
    var configurations = getConfigurations(openAPIList);
    var combinedDescription = combineDescription(openAPIList);
    finalAPI.setInfo(
        new Info()
            .version("latest")
            .license(new License().name("Example").url("https://www.example.com"))
            .title("Example API Documentation")
            .description(
                initialContent
                    + configurations
                    + "\n\n # Errors\n ## Common Error Codes \n"
                    + Arrays.stream(CoreExceptions.values())
                        .map(
                            status ->
                                "<tr>"
                                    + "<td>"
                                    + status.getEx().getCode()
                                    + "</td>"
                                    + "<td>"
                                    + status.getEx().getStatus()
                                    + "</td>"
                                    + "<td>"
                                    + status.getEx().getMessage()
                                    + "</td>"
                                    + "</tr>")
                        .collect(
                            Collectors.joining(
                                "",
                                "<h4>List of common error codes</h4><table><tr><th>Application Error Code</th><th>HTTP Status Code</th><th>Description</th></tr>",
                                "</table>"))
                    + combinedDescription));
    finalAPI.setTags(getAllTags(openAPIList));
    finalAPI.setPaths(getAllPaths(openAPIList));
    finalAPI.setComponents(getAllComponents(openAPIList));
    var components = finalAPI.getComponents();
    var securitySchemes = components.getSecuritySchemes();
    var securityScheme =
        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT");
    securitySchemes.put("Authorization", securityScheme);
    components.addParameters(
        ApiMfaConfig.X_MFA_EMAIL,
        new HeaderParameter()
            .required(false)
            .name(ApiMfaConfig.X_MFA_EMAIL)
            .description("MFA email code")
            .schema(new StringSchema()));

    components.addParameters(
        ApiMfaConfig.X_MFA_SMS,
        new HeaderParameter()
            .required(false)
            .name(ApiMfaConfig.X_MFA_SMS)
            .description("MFA sms code")
            .schema(new StringSchema()));

    components.addParameters(
        ApiMfaConfig.X_MFA_AUTHENTICATOR,
        new HeaderParameter()
            .required(false)
            .name(ApiMfaConfig.X_MFA_AUTHENTICATOR)
            .description("MFA authenticator code")
            .schema(new StringSchema()));

    if (serverUrl != null) finalAPI.addServersItem(new Server().url(serverUrl));

    // some cleaning
    clean(finalAPI);
    return finalAPI;
  }

  private String combineDescription(List<OpenAPI> openAPIList) {
    return openAPIList.stream()
        .filter(
            openAPI ->
                openAPI.getInfo() != null
                    && openAPI.getInfo().getTitle() != null
                    && openAPI.getInfo().getDescription() != null)
        .filter(openAPI -> !openAPI.getInfo().getTitle().equals("Configurations"))
        .map(
            openAPI ->
                "\n\n## "
                    + openAPI.getInfo().getTitle()
                    + " \n"
                    + openAPI.getInfo().getDescription())
        .collect(Collectors.joining(""));
  }

  private String getConfigurations(List<OpenAPI> openAPIList) {
    return openAPIList.stream()
        .filter(
            openAPI ->
                openAPI.getInfo() != null
                    && openAPI.getInfo().getTitle() != null
                    && openAPI.getInfo().getDescription() != null)
        .filter(openAPI -> openAPI.getInfo().getTitle().equals("Configurations"))
        .findAny()
        .map(openAPI -> openAPI.getInfo().getDescription())
        .orElse("");
  }

  private void clean(OpenAPI finalAPI) {
    var routesNotRequireAuthentication = routeConfiguration.getRoutesNotRequireAuthentication();
    var routesWithoutMethodNotRequireAuthentication =
        routeConfiguration.getRoutesWithoutMethodNotRequireAuthentication();
    var paths = new Paths();
    finalAPI.getPaths().entrySet().stream()
        .filter(stringPathEntry -> !stringPathEntry.getKey().startsWith("/internal"))
        .toList()
        .forEach(
            pathItemEntry -> paths.addPathItem(pathItemEntry.getKey(), pathItemEntry.getValue()));
    finalAPI.setPaths(paths);
    for (var pathEntry : finalAPI.getPaths().entrySet()) {
      var path = pathEntry.getValue();
      var map = new HashMap<String, Operation>();
      map.put(HttpMethod.GET.name(), path.getGet());
      map.put(HttpMethod.PUT.name(), path.getPut());
      map.put(HttpMethod.POST.name(), path.getPost());
      map.put(HttpMethod.PATCH.name(), path.getPatch());
      map.put(HttpMethod.TRACE.name(), path.getTrace());
      map.put(HttpMethod.HEAD.name(), path.getHead());
      map.put(HttpMethod.OPTIONS.name(), path.getOptions());
      map.put(HttpMethod.DELETE.name(), path.getDelete());
      for (var entry : map.entrySet()) {
        var operation = entry.getValue();
        if (operation != null) {
          var newTags = new ArrayList<String>();
          for (var tag : operation.getTags()) {
            newTags.add(transformTag(tag));
          }
          operation.setTags(newTags);
          operation.operationId(UUID.randomUUID().toString());
          var parameters = operation.getParameters();
          if (parameters != null) {
            operation.setParameters(
                parameters.stream()
                    .filter(
                        parameter ->
                            !("header".equals(parameter.getIn())
                                && "X-AUTHORIZATION-ID".equals(parameter.getName())))
                    .collect(Collectors.toList()));
            if (apiMfaConfig.isMfaAPI(entry.getKey(), pathEntry.getKey())) {
              operation.addParametersItem(
                  new HeaderParameter()
                      .$ref("#/components/parameters/" + ApiMfaConfig.X_MFA_AUTHENTICATOR));
              operation.addParametersItem(
                  new HeaderParameter()
                      .$ref("#/components/parameters/" + ApiMfaConfig.X_MFA_EMAIL));
              operation.addParametersItem(
                  new HeaderParameter().$ref("#/components/parameters/" + ApiMfaConfig.X_MFA_SMS));
            }
          }
          var routes = routesNotRequireAuthentication.get(HttpMethod.valueOf(entry.getKey()));
          if ((routes != null && routes.contains(pathEntry.getKey()))
              || routesWithoutMethodNotRequireAuthentication.contains(pathEntry.getKey())) continue;
          var alreadyPresent = false;
          if (operation.getSecurity() != null) {
            for (var securityRequirement : operation.getSecurity()) {
              if (securityRequirement.get("Authorization") != null) {
                alreadyPresent = true;
                break;
              }
            }
          }
          if (!alreadyPresent) {
            operation.addSecurityItem(
                new SecurityRequirement().addList("Authorization", Collections.emptyList()));
          }
        }
      }
    }
  }

  private String transformTag(String tag) {
    try {
      return WordUtils.capitalizeFully(String.join(" ", tag.split("-")));
    } catch (Exception e) {
      return tag;
    }
  }

  private Components getAllComponents(List<OpenAPI> openAPIList) {
    var components = new Components();
    components.setCallbacks(getCallBacks(openAPIList));
    components.setExamples(getExamples(openAPIList));
    components.setHeaders(getHeaders(openAPIList));
    components.setLinks(getLinks(openAPIList));
    components.setParameters(getParameters(openAPIList));
    components.setRequestBodies(getRequestBodies(openAPIList));
    components.setResponses(getResponses(openAPIList));
    components.setSchemas(getSchemas(openAPIList));
    components.setSecuritySchemes(getSecuritySchemes(openAPIList));
    return components;
  }

  private Map<String, SecurityScheme> getSecuritySchemes(List<OpenAPI> openAPIList) {
    return openAPIList.stream()
        .filter(openApi3 -> openApi3.getComponents().getSecuritySchemes() != null)
        .flatMap(openApi3 -> openApi3.getComponents().getSecuritySchemes().entrySet().stream())
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (securityScheme1, securityScheme2) -> {
                  log.info(
                      "duplicate key found securityScheme1 {} and securityScheme2 {}",
                      securityScheme1,
                      securityScheme2);
                  return securityScheme1;
                }));
  }

  private Map<String, Schema> getSchemas(List<OpenAPI> openAPIList) {
    return openAPIList.stream()
        .filter(openApi3 -> openApi3.getComponents().getSchemas() != null)
        .flatMap(openApi3 -> openApi3.getComponents().getSchemas().entrySet().stream())
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (schema1, schema2) -> {
                  log.info("duplicate key found schema1 {} and schema2 {}", schema1, schema2);
                  return schema1;
                }));
  }

  private Map<String, ApiResponse> getResponses(List<OpenAPI> openAPIList) {
    return openAPIList.stream()
        .filter(openApi3 -> openApi3.getComponents().getResponses() != null)
        .flatMap(openApi3 -> openApi3.getComponents().getResponses().entrySet().stream())
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (response1, response2) -> {
                  log.info(
                      "duplicate key found response1 {} and response2 {}", response1, response2);
                  return response1;
                }));
  }

  private Map<String, RequestBody> getRequestBodies(List<OpenAPI> openAPIList) {
    return openAPIList.stream()
        .filter(openApi3 -> openApi3.getComponents().getRequestBodies() != null)
        .flatMap(openApi3 -> openApi3.getComponents().getRequestBodies().entrySet().stream())
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (requestBody1, requestBody2) -> {
                  log.info(
                      "duplicate key found requestBody1 {} and requestBody2 {}",
                      requestBody1,
                      requestBody2);
                  return requestBody1;
                }));
  }

  private Map<String, Parameter> getParameters(List<OpenAPI> openAPIList) {
    return openAPIList.stream()
        .filter(openApi3 -> openApi3.getComponents().getParameters() != null)
        .flatMap(openApi3 -> openApi3.getComponents().getParameters().entrySet().stream())
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (parameter1, parameter2) -> {
                  log.info(
                      "duplicate key found parameter1 {} and parameter2 {}",
                      parameter1,
                      parameter2);
                  return parameter1;
                }));
  }

  private Map<String, Link> getLinks(List<OpenAPI> openAPIList) {
    return openAPIList.stream()
        .filter(openApi3 -> openApi3.getComponents().getLinks() != null)
        .flatMap(openApi3 -> openApi3.getComponents().getLinks().entrySet().stream())
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (link1, link2) -> {
                  log.info("duplicate key found link1 {} and link2 {}", link1, link2);
                  return link1;
                }));
  }

  private Map<String, Header> getHeaders(List<OpenAPI> openAPIList) {
    return openAPIList.stream()
        .filter(openApi3 -> openApi3.getComponents().getHeaders() != null)
        .flatMap(openApi3 -> openApi3.getComponents().getHeaders().entrySet().stream())
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (header1, header2) -> {
                  log.info("duplicate key found header1 {} and header1 {}", header1, header1);
                  return header1;
                }));
  }

  private Map<String, Example> getExamples(List<OpenAPI> openAPIList) {
    return openAPIList.stream()
        .filter(openApi3 -> openApi3.getComponents().getExamples() != null)
        .flatMap(openApi3 -> openApi3.getComponents().getExamples().entrySet().stream())
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (example1, example2) -> {
                  log.info("duplicate key found example1 {} and example2 {}", example1, example2);
                  return example1;
                }));
  }

  private Map<String, Callback> getCallBacks(List<OpenAPI> openAPIList) {
    return openAPIList.stream()
        .filter(openApi3 -> openApi3.getComponents().getCallbacks() != null)
        .flatMap(openApi3 -> openApi3.getComponents().getCallbacks().entrySet().stream())
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (callback1, callback2) -> {
                  log.info(
                      "duplicate key found callback1 {} and callback2 {}", callback1, callback2);
                  return callback1;
                }));
  }

  private Paths getAllPaths(List<OpenAPI> openAPIS) {
    var combinedPaths = new Paths();
    for (var openAPI : openAPIS) {
      var paths = openAPI.getPaths();
      if (paths != null) {
        paths.forEach(
            (s, pathItem) -> {
              var oldPathItem = combinedPaths.get(s);
              if (oldPathItem == null) {
                combinedPaths.addPathItem(s, pathItem);
              } else {
                if (pathItem.getDelete() != null) {
                  oldPathItem.setDelete(pathItem.getDelete());
                }
                if (pathItem.getPatch() != null) {
                  oldPathItem.setPatch(pathItem.getPatch());
                }
                if (pathItem.getOptions() != null) {
                  oldPathItem.setOptions(pathItem.getOptions());
                }
                if (pathItem.getGet() != null) {
                  oldPathItem.setGet(pathItem.getGet());
                }
                if (pathItem.getPost() != null) {
                  oldPathItem.setPost(pathItem.getPost());
                }
                if (pathItem.getPut() != null) {
                  oldPathItem.setPut(pathItem.getPut());
                }
              }
            });
      }
    }
    return combinedPaths;
  }

  private List<Tag> getAllTags(List<OpenAPI> openApiList) {
    return openApiList.stream()
        .filter(openApi -> openApi.getTags() != null)
        .flatMap(openApi -> openApi.getTags().stream())
        .peek(tag -> tag.setName(transformTag(tag.getName())))
        .collect(Collectors.toList());
  }

  private List<OpenAPI> getAllOpenApi3(List<OpenAPIConfig.Client> services, boolean async) {
    if (async) {
      return services.parallelStream()
          .map(service -> CompletableFuture.supplyAsync(() -> getOpenApi(service)))
          .map(CompletableFuture::join)
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
    } else {
      return services.stream()
          .map(this::getOpenApi)
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
    }
  }

  private OpenAPI getOpenApi(OpenAPIConfig.Client client) {
    if (applyFilter && !apiFilter.contains(client.getName())) {
      return null;
    } else {
      try {
        return client.getApiDoc();
      } catch (Exception e) {
        log.error("Error in fetching api doc for {}", client.getClass());
        return null;
      }
    }
  }
}
