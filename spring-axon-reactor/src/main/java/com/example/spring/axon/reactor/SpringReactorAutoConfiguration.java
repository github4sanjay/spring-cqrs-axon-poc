package com.example.spring.axon.reactor;

import com.example.spring.core.exceptions.CoreExceptions;
import com.example.spring.core.exceptions.IException;
import org.axonframework.commandhandling.CommandExecutionException;
import org.axonframework.commandhandling.NoHandlerForCommandException;
import org.axonframework.config.EventProcessingConfigurer;
import org.axonframework.extensions.reactor.autoconfig.ReactorAutoConfiguration;
import org.axonframework.extensions.reactor.commandhandling.gateway.ReactorCommandGateway;
import org.axonframework.extensions.reactor.queryhandling.gateway.ReactorQueryGateway;
import org.axonframework.queryhandling.QueryExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureAfter(ReactorAutoConfiguration.class)
public class SpringReactorAutoConfiguration {

  @Autowired
  public void configure(
      ReactorQueryGateway reactorQueryGateway, ReactorCommandGateway reactorCommandGateway) {
    reactorCommandGateway.registerResultHandlerInterceptor(
        (command, result) ->
            result.onErrorMap(
                throwable -> {
                  if (throwable instanceof CommandExecutionException e) {
                    if (e.getDetails().isPresent()) {
                      if (e.getDetails().get() instanceof IException iException)
                        throw iException.getEx();
                    } else {
                      if (e.getMessage() != null
                          && e.getMessage()
                              .equals("The aggregate was not found in the event store")) {
                        throw CoreExceptions.AGGREGATE_NOT_FOUND.getEx();
                      }
                    }
                  } else if (throwable instanceof NoHandlerForCommandException e) {
                    throw e;
                  }
                  throw new RuntimeException(throwable);
                }));

    reactorQueryGateway.registerResultHandlerInterceptor(
        (query, result) ->
            result.onErrorMap(
                throwable -> {
                  var e = (QueryExecutionException) throwable;
                  if (e.getDetails().isPresent()) {
                    if (e.getDetails().get() instanceof IException iException)
                      throw iException.getEx();
                  }
                  throw e;
                }));
  }

  @Autowired
  public void configureProcessingGroupErrorHandling(
      EventProcessingConfigurer processingConfigurer) {
    // To configure a default ...
    processingConfigurer.registerDefaultListenerInvocationErrorHandler(
        configuration ->
            (exception, event, eventHandler) -> {
              throw exception;
            });
  }
}
