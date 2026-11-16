package com.tsix.immocrm.dto.achat.config;

import java.util.UUID;

/** Champs communs aux fiches statut d'achat et statut de lot, contrôlés par {@code FicheStatut.lire}. */
public interface FicheStatutSaisie {

    Integer ordre();

    String code();

    String description();

    UUID typeId();

    Integer dureeVieJours();

    String couleur();

    UUID imageId();

    String notes();
}
