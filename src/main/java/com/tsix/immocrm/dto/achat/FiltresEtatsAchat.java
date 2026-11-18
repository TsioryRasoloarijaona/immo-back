package com.tsix.immocrm.dto.achat;

import com.tsix.immocrm.exceptions.InvalidRequestException;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Filtres des états Achat (TVA déductible, suivi des avances, suivi des retenues), partagés par l'état et son export. */
public record FiltresEtatsAchat(
        @Parameter(description = "Début de période, date de pièce incluse, obligatoire.", example = "2026-01-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
        @Parameter(description = "Fin de période, date de pièce incluse jusqu'à minuit, obligatoire.", example = "2026-12-31")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
        @Parameter(description = "Société (CfgCompany.id) ; une société non habilitée ne rend rien.")
        UUID societeId,
        @Parameter(description = "Projets (PrjProject.id), paramètre répété ; hors périmètre ignoré ; exclut les pièces sans projet.")
        List<UUID> projetIds,
        @Parameter(description = "Fournisseur : texte contenu dans son nom (CfgTier.Description).")
        String tiers,
        @Parameter(description = "Projet : texte contenu dans son code ou sa description.")
        String projet,
        @Parameter(description = "Lot : texte contenu dans son code ou sa description (PrjMarket).")
        String lot) {

    public FiltresEtatsAchat {
        projetIds = projetIds == null ? List.of() : projetIds;
    }

    /** 400 sans période complète ou si {@code du > au}. */
    public void exigerPeriode() {
        if (du == null || au == null) {
            throw new InvalidRequestException("Période obligatoire : du et au.");
        }
        if (du.isAfter(au)) {
            throw new InvalidRequestException("Période invalide : du est après au.");
        }
    }
}
