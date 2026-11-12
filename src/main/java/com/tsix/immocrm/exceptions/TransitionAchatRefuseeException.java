package com.tsix.immocrm.exceptions;

import com.tsix.immocrm.dto.achat.ActionsPieceAchatDto.TransitionPermiseAchatDto;

import java.util.List;

/** Statut d'arrivée hors workflow de la pièce → 409 avec {@code transitionsPermises} (FEATURE-achat-fiche-ecritures.md E-B2). */
public class TransitionAchatRefuseeException extends ConflictException {

    private final transient List<TransitionPermiseAchatDto> transitionsPermises;

    public TransitionAchatRefuseeException(String message, List<TransitionPermiseAchatDto> transitionsPermises) {
        super(message);
        this.transitionsPermises = List.copyOf(transitionsPermises);
    }

    public List<TransitionPermiseAchatDto> getTransitionsPermises() {
        return transitionsPermises;
    }
}
