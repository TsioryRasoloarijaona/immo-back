package com.tsix.immocrm.web;

import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.DoublonsProspectException;
import com.tsix.immocrm.exceptions.ForbiddenException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.InvalidStatutTransitionException;
import com.tsix.immocrm.exceptions.ProspectionExistanteException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.exceptions.UnauthorizedException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import com.tsix.immocrm.domain.ColonneInconnueException;
import com.tsix.immocrm.domain.FonctionAgregatInconnueException;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Exceptions métier → {@link ProblemDetail} RFC 7807. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ProblemDetail handleInvalidRequest(InvalidRequestException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** Le corps liste les noms valides : ignorer le nom laisserait croire à une colonne vide. */
    @ExceptionHandler(ColonneInconnueException.class)
    public ProblemDetail handleColonneInconnue(ColonneInconnueException ex) {
        ProblemDetail probleme = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        probleme.setProperty("colonne", ex.colonne());
        // Noms tirés de l'exception, pas d'un catalogue en dur : chaque liste a le sien.
        probleme.setProperty("colonnesValides", ex.nomsValides());
        return probleme;
    }

    /** Le corps porte les fonctions acceptées par la colonne visée (une date : MIN/MAX/COUNT), pas la liste générale. */
    @ExceptionHandler(FonctionAgregatInconnueException.class)
    public ProblemDetail handleFonctionAgregatInconnue(FonctionAgregatInconnueException ex) {
        ProblemDetail probleme = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        probleme.setProperty("agregat", ex.jeton());
        probleme.setProperty("colonne", ex.colonne());
        probleme.setProperty("fonctionsValides", ex.fonctionsValides());
        return probleme;
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ProblemDetail handleUnauthorized(UnauthorizedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    /** Écriture sur une ressource nommée par l'appelant ; une lecture hors périmètre est un 404, pas un 403. */
    @ExceptionHandler(ForbiddenException.class)
    public ProblemDetail handleForbidden(ForbiddenException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** FEATURE-doublons-prospect-projet.md §2. */
    @ExceptionHandler(DoublonsProspectException.class)
    public ProblemDetail handleDoublonsProspect(DoublonsProspectException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setProperty("doublons", ex.getDoublons());
        return problem;
    }

    /** Une prospection ouverte par prospect et par projet (FEATURE-doublons-prospect-projet.md §3). */
    @ExceptionHandler(ProspectionExistanteException.class)
    public ProblemDetail handleProspectionExistante(ProspectionExistanteException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setProperty("prospectionExistante", ex.getProspectionExistante());
        return problem;
    }

    @ExceptionHandler(InvalidStatutTransitionException.class)
    public ProblemDetail handleInvalidStatutTransition(InvalidStatutTransitionException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setProperty("transitionsAutorisees", ex.getTransitionsAutorisees());
        return problem;
    }

    /** {@code ?sort=} sur un nom de champ DTO au lieu de l'attribut d'entité : 400 au lieu d'un 500. */
    @ExceptionHandler(PropertyReferenceException.class)
    public ProblemDetail handleInvalidSortProperty(PropertyReferenceException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Propriété de tri invalide: '" + ex.getPropertyName() + "' — vérifiez le paramètre 'sort'.");
    }
}
