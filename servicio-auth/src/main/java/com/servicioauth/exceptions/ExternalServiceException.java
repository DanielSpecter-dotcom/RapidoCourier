package com.servicioauth.exceptions;

/**
 * Excepción lanzada cuando falla la comunicación con un servicio externo.
 * Se mapea a 502 Bad Gateway en el GlobalExceptionHandler.
 */
public class ExternalServiceException extends RuntimeException {

    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
