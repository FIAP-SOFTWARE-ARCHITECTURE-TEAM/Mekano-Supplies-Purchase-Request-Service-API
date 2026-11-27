package com.fiap.mekano.supplies.rest.api.exception;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.stream.Collectors;

@Provider
@ApplicationScoped
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    private static final String PROBLEM_JSON = "application/problem+json";

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        String detail = exception.getConstraintViolations().stream()
                .map(this::describe)
                .sorted()
                .collect(Collectors.joining("; "));
        String instance = uriInfo != null ? uriInfo.getRequestUri().toString() : null;

        return Response.status(400)
                .entity(ProblemDetail.of(400, detail, instance))
                .type(PROBLEM_JSON)
                .build();
    }

    private String describe(ConstraintViolation<?> violation) {
        return violation.getPropertyPath() + ": " + violation.getMessage();
    }
}
