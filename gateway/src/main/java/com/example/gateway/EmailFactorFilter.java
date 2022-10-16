package com.example.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class EmailFactorFilter implements GlobalFilter, Ordered {

  private final MFAUtil mfaUtil;
  private final ApiMfaConfig apiMfaConfig;
  private final OktaService oktaService;

  @Autowired
  public EmailFactorFilter(MFAUtil mfaUtil, ApiMfaConfig apiMfaConfig, OktaService oktaService) {
    this.mfaUtil = mfaUtil;
    this.apiMfaConfig = apiMfaConfig;
    this.oktaService = oktaService;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    var request = exchange.getRequest();
    var uri = request.getURI();
    var path = uri.getPath();
    var response = exchange.getResponse();
    if (request.getMethod() != null
        && !apiMfaConfig.isMfaAPI(request.getMethod().name(), path)) { // mfa not required
      return chain.filter(exchange);
    } else {
      var headers = request.getHeaders();
      var emailPasscode = headers.getFirst(ApiMfaConfig.X_MFA_EMAIL);
      var userId = headers.getFirst("X-AUTHORIZATION-ID");
      var factorsResponseMono = oktaService.getFactors(userId);
      oktaService.clearFromCache(userId); // last one so clear
      return factorsResponseMono.flatMap(
          factors -> {
            if (factors.getEmail() != null && factors.getEmail().getStatus().equals("ACTIVE")) {
              if (emailPasscode == null) {
                return mfaUtil.errorResult(
                    mfaUtil.getErrorResponse(factors, "mfa-email-required"), response);
              }
              return mfaUtil
                  .verifyFactor(emailPasscode, userId, "email")
                  .flatMap(
                      verifyFactorResponse -> {
                        if (verifyFactorResponse.getErrorResponse() != null) {
                          return mfaUtil.errorResult(
                              verifyFactorResponse.getErrorResponse(), response);
                        } else {
                          return chain.filter(exchange);
                        }
                      });
            }
            return chain.filter(exchange);
          });
    }
  }

  @Override
  public int getOrder() {
    return 4;
  }
}
