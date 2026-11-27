package com.fiap.mekano.supplies.rest.api.exception;

public record ProblemDetail(
        String type,
        String title,
        int status,
        String detail,
        String instance
) {

    public static ProblemDetail of(int status, String detail, String instance) {
        return new ProblemDetail("about:blank", titleFor(status), status, detail, instance);
    }

    private static String titleFor(int status) {
        return switch (status) {
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 409 -> "Conflict";
            case 422 -> "Unprocessable Entity";
            case 503 -> "Service Unavailable";
            default -> "Internal Server Error";
        };
    }
}
