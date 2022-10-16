package com.example.spring.axon.reactor;

import com.example.spring.core.exceptions.IException;
import org.axonframework.commandhandling.CommandExecutionException;

public class ApplicationCommandExecutionException extends CommandExecutionException {

  public ApplicationCommandExecutionException(IException e) {
    super(e.getEx().code, e.getEx(), e);
  }
}
