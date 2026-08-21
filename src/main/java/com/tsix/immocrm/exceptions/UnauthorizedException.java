package com.tsix.immocrm.exceptions;

/**
 * Thrown when authentication fails (unknown username, wrong password, or disabled account) on
 * {@code POST /api/auth/connexion}. Translated to a 401 {@link org.springframework.http.ProblemDetail}
 * response by {@link com.tsix.immocrm.web.GlobalExceptionHandler}. The message is intentionally
 * generic ("Identifiants invalides") regardless of which of the three cases triggered it, to avoid
 * leaking whether a given username exists.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
