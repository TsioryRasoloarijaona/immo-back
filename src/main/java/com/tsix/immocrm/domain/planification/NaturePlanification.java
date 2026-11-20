package com.tsix.immocrm.domain.planification;

import io.swagger.v3.oas.annotations.media.Schema;

/** R5 : modèle = ObjectId et VersionDate NULL ; projet = ObjectId renseigné ; version = VersionDate renseignée. */
@Schema(description = "MODELE, PROJET (planification d'un projet) ou VERSION (copie datée, lecture seule).")
public enum NaturePlanification {
    MODELE, PROJET, VERSION
}
