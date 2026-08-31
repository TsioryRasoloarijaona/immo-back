package com.tsix.immocrm.exceptions;

/**
 * L'appelant est authentifié mais n'a pas le droit d'effectuer cette opération sur cette ressource
 * — traduit en {@code 403 Forbidden} par {@code GlobalExceptionHandler}.
 *
 * <p>À réserver aux <b>écritures</b> nommant explicitement une ressource hors périmètre (création
 * d'un dossier sur un projet non affecté, par exemple) : là, l'appelant a lui-même désigné la
 * ressource, il n'y a rien à cacher. Une <b>lecture</b> hors périmètre renvoie au contraire
 * {@code 404} ({@link ResourceNotFoundException}) — un 403 confirmerait l'existence de la ressource
 * et permettrait d'énumérer les ids.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
