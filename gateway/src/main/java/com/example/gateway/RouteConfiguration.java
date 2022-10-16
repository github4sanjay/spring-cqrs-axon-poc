package com.example.gateway;

import java.util.*;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

@Configuration
@RequiredArgsConstructor
public class RouteConfiguration {

  private final GatewayProperties gatewayProperties;
  private final Map<HttpMethod, Set<String>> routesNotRequireAuthentication = new HashMap<>();
  private final Set<String> routesWithoutMethodNotRequireAuthentication = new HashSet<>();

  @PostConstruct
  public void postConstruct() {
    for (var routeDefinition : gatewayProperties.getRoutes()) {
      var isGatewayHeaderFilterAvailable = false;
      for (var filterDefinition : routeDefinition.getFilters()) {
        if ("AddAuthHeaderGatewayFilter".equals(filterDefinition.getName())) {
          isGatewayHeaderFilterAvailable = true;
        }
      }
      if (!isGatewayHeaderFilterAvailable) {
        var predicates = routeDefinition.getPredicates();
        if (predicates.size() == 0) continue;
        if (predicates.size() == 1) { // just path
          var predicate = predicates.get(0);
          var paths = predicate.getArgs();
          routesWithoutMethodNotRequireAuthentication.addAll(paths.values());
        }
        if (predicates.size() == 2) { // path and method
          var predicatePath = predicates.get(0);
          var paths = predicatePath.getArgs();
          var predicateMethod = predicates.get(1);
          var methods = predicateMethod.getArgs();
          for (var method : methods.values()) {
            var httpMethod = HttpMethod.valueOf(method);
            var routes = routesNotRequireAuthentication.get(httpMethod);
            if (routes == null) routes = new HashSet<>();
            routes.addAll(paths.values());
            routesNotRequireAuthentication.put(httpMethod, routes);
          }
        }
      }
    }
  }

  public Map<HttpMethod, Set<String>> getRoutesNotRequireAuthentication() {
    return routesNotRequireAuthentication;
  }

  public Set<String> getRoutesWithoutMethodNotRequireAuthentication() {
    return routesWithoutMethodNotRequireAuthentication;
  }
}
