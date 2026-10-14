package com.tsix.immocrm.services.dossier.liste;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Valeurs d'un dossier lues par {@link ColonnesCalculeesDossier}. */
public record ValeursCalculees(String decisionMeilleurTaux, Integer nombreEtatsModification, Integer nombreCadeaux,
                               BigDecimal superficieAnnexes, LocalDateTime alerte, Integer nombreActions,
                               LocalDateTime derniereCommunicationDate, String derniereCommunicationResultat,
                               String derniereCommunicationMessage, boolean instructionNonLue,
                               String sousArticlesCodes, String sousArticlesLibelles, String autresClients) {
}
