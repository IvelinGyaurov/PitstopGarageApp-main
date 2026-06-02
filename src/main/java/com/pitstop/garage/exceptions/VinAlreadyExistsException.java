package com.pitstop.garage.exceptions;

public class VinAlreadyExistsException extends RuntimeException {
  public VinAlreadyExistsException(String message) {
    super(message);
  }
}
