package com.tsix.immocrm.services.dossier.edition;

import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.exceptions.InvalidRequestException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

/**
 * Ordre des jalons d'un dossier ({@code Sale.cs:Verif} → {@code VerifDates}) : chaque paire est contrôlée dans
 * l'ordre d'OMRAN, à la journée près, et la première inversion lève un 400 avec le message d'OMRAN.
 */
final class OrdreDatesDossier {

    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String PROSPECTION = "Date prospection";
    private static final String DOSSIER = "Date dossier";
    private static final String CONCRETISATION = "Date Concrétisation / Promesse de vente";
    private static final String NOTAIRE_PREVISIONNEL = "Date Envoie au notaire prévisionnelle";
    private static final String NOTAIRE = "Date Envoie au notaire / Avocat";
    private static final String MINUTE = "Date Minute / Enregistrement du contrat";
    private static final String REMISE_CLES = "Date Remise des clés effective";

    private record Jalon(String libelle, Function<ComFolder, LocalDateTime> date) {
    }

    private record Paire(Jalon avant, Jalon apres) {
    }

    private static final Jalon J_PROSPECTION = new Jalon(PROSPECTION,
            f -> f.getComProspection() != null ? f.getComProspection().getDocDate() : null);
    private static final Jalon J_DOSSIER = new Jalon(DOSSIER, ComFolder::getDocDate);
    private static final Jalon J_CONCRETISATION = new Jalon(CONCRETISATION, ComFolder::getAdvanceDate);
    private static final Jalon J_NOTAIRE_PREV = new Jalon(NOTAIRE_PREVISIONNEL, ComFolder::getAgreementDatePrevisionel);
    private static final Jalon J_NOTAIRE = new Jalon(NOTAIRE, ComFolder::getAgreementDate);
    private static final Jalon J_MINUTE = new Jalon(MINUTE, ComFolder::getAgreementSavingDate);
    private static final Jalon J_REMISE = new Jalon(REMISE_CLES, ComFolder::getDeliveryDateReal);

    private static final List<Paire> PAIRES = List.of(
            new Paire(J_PROSPECTION, J_DOSSIER),
            new Paire(J_DOSSIER, J_CONCRETISATION),
            new Paire(J_DOSSIER, J_NOTAIRE_PREV),
            new Paire(J_CONCRETISATION, J_NOTAIRE_PREV),
            new Paire(J_DOSSIER, J_NOTAIRE),
            new Paire(J_CONCRETISATION, J_NOTAIRE),
            new Paire(J_NOTAIRE_PREV, J_NOTAIRE),
            new Paire(J_DOSSIER, J_MINUTE),
            new Paire(J_CONCRETISATION, J_MINUTE),
            new Paire(J_NOTAIRE_PREV, J_MINUTE),
            new Paire(J_NOTAIRE, J_MINUTE),
            new Paire(J_DOSSIER, J_REMISE),
            new Paire(J_CONCRETISATION, J_REMISE),
            new Paire(J_NOTAIRE_PREV, J_REMISE),
            new Paire(J_NOTAIRE, J_REMISE),
            new Paire(J_MINUTE, J_REMISE));

    private OrdreDatesDossier() {
    }

    static void verifier(ComFolder f) {
        for (Paire p : PAIRES) {
            LocalDateTime avant = p.avant().date().apply(f);
            LocalDateTime apres = p.apres().date().apply(f);
            if (avant != null && apres != null && apres.toLocalDate().isBefore(avant.toLocalDate())) {
                throw new InvalidRequestException("La date '%s: %s' doit être supérieur ou égal à la date '%s: %s'."
                        .formatted(p.apres().libelle(), JOUR.format(apres), p.avant().libelle(), JOUR.format(avant)));
            }
        }
    }
}
