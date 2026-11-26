package com.tsix.immocrm.web;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.io.IOException;

/**
 * Écrit un {@link ProblemDetail} hors d'un {@code @ExceptionHandler} (intercepteur, chaîne de sécurité) avec le
 * convertisseur de Spring : même enveloppe que {@link GlobalExceptionHandler}, sans exception (qui deviendrait un 500).
 */
@Component
public class ReponseProbleme {

    private final ObjectProvider<RequestMappingHandlerAdapter> handlerAdapter;
    private volatile HttpMessageConverter<Object> convertisseurProbleme;

    public ReponseProbleme(ObjectProvider<RequestMappingHandlerAdapter> handlerAdapter) {
        this.handlerAdapter = handlerAdapter;
    }

    public void ecrire(ProblemDetail probleme, HttpServletResponse response) throws IOException {
        response.setStatus(probleme.getStatus());
        convertisseurProbleme().write(probleme, MediaType.APPLICATION_PROBLEM_JSON,
                new ServletServerHttpResponse(response));
    }

    /** Résolu paresseusement : injecter l'adapter directement crée un cycle (intercepteur enregistré par un WebMvcConfigurer). */
    @SuppressWarnings("unchecked")
    private HttpMessageConverter<Object> convertisseurProbleme() {
        HttpMessageConverter<Object> convertisseur = this.convertisseurProbleme;
        if (convertisseur == null) {
            convertisseur = handlerAdapter.getObject().getMessageConverters().stream()
                    .filter(c -> c.canWrite(ProblemDetail.class, MediaType.APPLICATION_PROBLEM_JSON))
                    .map(c -> (HttpMessageConverter<Object>) c)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Aucun HttpMessageConverter ne sait écrire un ProblemDetail en application/problem+json"));
            this.convertisseurProbleme = convertisseur;
        }
        return convertisseur;
    }
}
