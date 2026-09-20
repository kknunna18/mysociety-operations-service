package com.mysociety.operations;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

class NotFoundException extends RuntimeException {}
class ForbiddenException extends RuntimeException {}
class DomainException extends RuntimeException { DomainException(String message) { super(message); } }

@RestControllerAdvice
class ProblemHandling {
    @ExceptionHandler(NotFoundException.class) ProblemDetail notFound() { return problem(HttpStatus.NOT_FOUND,"Resource not found"); }
    @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class}) ProblemDetail forbidden() { return problem(HttpStatus.FORBIDDEN,"Access is denied"); }
    @ExceptionHandler(DomainException.class) ProblemDetail conflict(DomainException e) { return problem(HttpStatus.CONFLICT,e.getMessage()); }
    @ExceptionHandler(MethodArgumentNotValidException.class) ProblemDetail invalid(MethodArgumentNotValidException e) {
        ProblemDetail p=problem(HttpStatus.BAD_REQUEST,"Request validation failed");
        p.setProperty("errors",e.getBindingResult().getFieldErrors().stream().map(x->x.getField()+": "+x.getDefaultMessage()).toList()); return p;
    }
    private ProblemDetail problem(HttpStatus status,String detail) {
        ProblemDetail p=ProblemDetail.forStatusAndDetail(status,detail);p.setType(URI.create("https://mysociety.example/problems/"+status.value()));
        p.setProperty("correlationId",CorrelationIds.current());return p;
    }
}
