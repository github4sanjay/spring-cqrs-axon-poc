package com.example.spring.axon.reactor;

import com.example.spring.core.exceptions.IException;
import lombok.Getter;
import org.axonframework.queryhandling.QueryExecutionException;

@Getter
public class ApplicationQueryExecutionException extends QueryExecutionException {

  public ApplicationQueryExecutionException(IException e) {
    super(e.getEx().code, e.getEx(), e);
  }
}
