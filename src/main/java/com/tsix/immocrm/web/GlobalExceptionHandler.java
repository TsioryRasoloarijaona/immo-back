package com.tsix.immocrm.web;

import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.ForbiddenException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.InvalidStatutTransitionException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.exceptions.UnauthorizedException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import com.tsix.immocrm.domain.ColonneInconnueException;
import com.tsix.immocrm.domain.FonctionAgregatInconnueException;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates domain exceptions into RFC 7807 {@link ProblemDetail} responses,
 * shared by every REST controller in the application.
 */
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

    /**
     * Nom hors catalogue passé à {@code ?colonnes=}.
     *
     * <p>Le corps porte la liste des noms valides : une faute de frappe se corrige alors depuis
     * la réponse, sans aller lire la doc. Ignorer le nom en silence laisserait le front croire à
     * une colonne vide plutôt qu'à une erreur d'appel.
     */
    @ExceptionHandler(ColonneInconnueException.class)
    public ProblemDetail handleColonneInconnue(ColonneInconnueException ex) {
        ProblemDetail probleme = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        probleme.setProperty("colonne", ex.colonne());
        // Les noms viennent de l'exception, pas d'un catalogue code en dur : trois listes sont
        // desormais filtrables, et repondre les colonnes de prospection a une faute de frappe sur
        // un prospect designerait le mauvais ecran.
        probleme.setProperty("colonnesValides", ex.nomsValides());
        return probleme;
    }

    /**
     * Fonction d'agrégat refusée — {@code ?agregats=dateDocument:SUM}, ou un nom de fonction qui
     * n'existe pas.
     *
     * <p>Le corps porte les fonctions acceptées <strong>par la colonne visée</strong>, pas la liste
     * générale : une date accepte MIN/MAX/COUNT, un montant les cinq. Répondre la liste complète
     * enverrait réessayer avec une valeur qui sera refusée elle aussi.
     */
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

    /**
     * Écriture refusée sur une ressource explicitement nommée par l'appelant (typiquement un projet
     * hors de son périmètre commercial). Les <em>lectures</em> hors périmètre passent au contraire
     * par {@link ResourceNotFoundException} (404) — voir {@link ForbiddenException}.
     */
    @ExceptionHandler(ForbiddenException.class)
    public ProblemDetail handleForbidden(ForbiddenException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidStatutTransitionException.class)
    public ProblemDetail handleInvalidStatutTransition(InvalidStatutTransitionException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setProperty("transitionsAutorisees", ex.getTransitionsAutorisees());
        return problem;
    }

    /**
     * Thrown when a {@code ?sort=} query param on a paginated {@code GET} names a property that
     * doesn't exist on the underlying JPA entity — e.g. {@code sort=dateDocument} instead of the
     * real {@code ComFolder} column {@code docDate} (a DTO field name, not the entity's). Without
     * this handler it surfaces as an unhandled 500; the real problem is a bad request param.
     */
    @ExceptionHandler(PropertyReferenceException.class)
    public ProblemDetail handleInvalidSortProperty(PropertyReferenceException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Propriété de tri invalide: '" + ex.getPropertyName() + "' — vérifiez le paramètre 'sort'.");
    }
}
