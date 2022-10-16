package com.example.spring.core.json;

import com.example.spring.core.exceptions.Error;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ErrorResponse {

  public Error error;
}
