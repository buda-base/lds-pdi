package io.bdrc.restapi.exceptions;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice
public class LdsPdiExceptionHandler {

    @ExceptionHandler(value = { RestException.class })
    public ResponseEntity<ErrorMessage> handleAnyException(RestException ex, WebRequest request) {
        ErrorMessage err = new ErrorMessage(ex);
        return new ResponseEntity<ErrorMessage>(err, new HttpHeaders(), HttpStatus.resolve(ex.status));
    }

}
