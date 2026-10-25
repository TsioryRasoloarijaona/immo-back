package com.tsix.immocrm.services.rapport.vente.flux;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Un fait source retenu par {@link FluxVenteMensuel#mouvements} (FEATURE-tdb-vente-rapports-rythme.md §3.1) : dossier,
 * ligne de règlement ou échéance ; {@code ligneId} = ligne de règlement ou échéance, {@code null} pour un dossier.
 */
public record MouvementFlux(MesureFlux mesure, UUID trancheId, LocalDate date, BigDecimal valeur, UUID dossierId,
                            UUID commercialId, UUID categorieArticleId, boolean sousArticle, UUID ligneId) {
}
