package com.example.rate.limiting.api;

import com.example.rate.limiting.exception.MissingHeaderException;
import com.example.rate.limiting.exception.RateLimitExceededException;
import com.example.rate.limiting.model.Response;
import jakarta.servlet.ServletException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class ApiAdvice {

    @ExceptionHandler(MissingHeaderException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    ResponseEntity<Response> handleMissingHeaderException(MissingHeaderException ex) {
        return new ResponseEntity<>(new Response(ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    void handleMissingHeaderException(RateLimitExceededException ex) {}

    @ExceptionHandler(ServletException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    ResponseEntity<Response> handleServletException(Exception ex) {
        return new ResponseEntity<>(new Response(ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    ResponseEntity<Response> handleException(Exception ex) {
        log.error(ex.getMessage(), ex);
        return new ResponseEntity<>(new Response(ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
