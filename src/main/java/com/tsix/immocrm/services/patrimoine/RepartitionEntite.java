package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.RepartitionActionnaires;
import com.tsix.immocrm.dto.patrimoine.LigneRepartitionDto;
import com.tsix.immocrm.dto.patrimoine.LigneRepartitionEcritureDto;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgCompanyShareholder;
import com.tsix.immocrm.entities.cfg.CfgShareholder;
import com.tsix.immocrm.exceptions.ChampInvalideException;
import com.tsix.immocrm.repositories.cfg.CfgCompanyShareholderRepository;
import com.tsix.immocrm.repositories.cfg.CfgShareholderRepository;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Répartition d'une entité juridique ({@code CfgCompanyShareholder}) : seul endroit qui valide, contrôle
 * ({@code CfgCompany.Verif}) et remplace les lignes, pour la fiche comme pour les routes {@code …/repartition}.
 */
@Component
class RepartitionEntite {

    static final String CHAMP_REPARTITION = "repartition";
    static final String CHAMP_NOMBRE_ACTIONNAIRES = "nombreActionnaires";
    static final String CHAMP_NOMBRE_PARTS = "nombreParts";

    private final CfgCompanyShareholderRepository repository;
    private final CfgShareholderRepository actionnaireRepository;

    RepartitionEntite(CfgCompanyShareholderRepository repository, CfgShareholderRepository actionnaireRepository) {
        this.repository = repository;
        this.actionnaireRepository = actionnaireRepository;
    }

    List<CfgCompanyShareholder> actuelles(UUID entiteId) {
        return repository.deLEntite(entiteId);
    }

    /** Actionnaires connus, sans doublon, parts renseignées et positives : tout est contrôlé avant d'écrire. */
    List<CfgCompanyShareholder> lignesValides(List<LigneRepartitionEcritureDto> demandees) {
        List<CfgCompanyShareholder> lignes = demandees.stream().map(this::ligne).toList();
        if (RepartitionActionnaires.contientUnDoublon(
                lignes.stream().map(l -> l.getCfgShareholder().getId()).toList())) {
            throw new ChampInvalideException(CHAMP_REPARTITION, RepartitionActionnaires.DOUBLON);
        }
        return lignes;
    }

    /** Lignes construites par le calcul de groupe ({@code SetRepartitions}), actionnaires déjà connus. */
    List<CfgCompanyShareholder> depuisParts(List<RepartitionActionnaires.Parts> parts) {
        return parts.stream().map(p -> nouvelle(actionnaireRepository.getReferenceById(p.actionnaireId()), p.nombre()))
                .toList();
    }

    /** {@code CfgCompany.Verif} sur l'état final de la fiche et de ses lignes. */
    static void verifier(CfgCompany entite, List<CfgCompanyShareholder> lignes) {
        List<Integer> parts = lignes.stream().map(l -> Objects.requireNonNullElse(l.getSharingNumber(), 0)).toList();
        RepartitionActionnaires.erreurRepartition(zeroSiNul(entite.getTotalSharing()),
                zeroSiNul(entite.getSharingNumber()), parts).ifPresent(message -> {
                    String champ = RepartitionActionnaires.NOMBRE_ACTIONNAIRES.equals(message)
                            ? CHAMP_NOMBRE_ACTIONNAIRES : CHAMP_NOMBRE_PARTS;
                    throw new ChampInvalideException(champ, message);
                });
    }

    /** Supprime les lignes existantes puis enregistre les nouvelles, dans la transaction de l'appelant. */
    void remplacer(CfgCompany entite, List<CfgCompanyShareholder> nouvelles, String auteur, LocalDateTime maintenant) {
        repository.deleteAll(repository.deLEntite(entite.getId()));
        repository.flush();
        nouvelles.forEach(ligne -> {
            ligne.setCfgCompany(entite);
            ligne.setUserCreate(auteur);
            ligne.setUserCreateDateTime(maintenant);
            ligne.setUserUpdate(auteur);
            ligne.setUserUpdateDateTime(maintenant);
        });
        repository.saveAll(nouvelles);
    }

    void supprimerTout(UUID entiteId) {
        repository.deleteAll(repository.deLEntite(entiteId));
    }

    static List<LigneRepartitionDto> versDto(List<CfgCompanyShareholder> lignes) {
        return lignes.stream().map(l -> new LigneRepartitionDto(l.getId(),
                        Champs.de(l.getCfgShareholder(), CfgShareholder::getId),
                        Champs.de(l.getCfgShareholder(), CfgShareholder::getCode),
                        Champs.de(l.getCfgShareholder(), CfgShareholder::getDescription),
                        l.getSharingNumber()))
                .toList();
    }

    static int zeroSiNul(Integer valeur) {
        return valeur == null ? 0 : valeur;
    }

    private CfgCompanyShareholder ligne(LigneRepartitionEcritureDto demandee) {
        if (demandee == null || demandee.actionnaireId() == null || demandee.nombreParts() == null) {
            throw new ChampInvalideException(CHAMP_REPARTITION,
                    "Chaque ligne de répartition exige un actionnaire et un nombre de parts.");
        }
        if (demandee.nombreParts() < 0) {
            throw new ChampInvalideException(CHAMP_REPARTITION, "Un nombre de parts ne peut pas être négatif.");
        }
        CfgShareholder actionnaire = actionnaireRepository.findById(demandee.actionnaireId())
                .orElseThrow(() -> new ChampInvalideException(CHAMP_REPARTITION,
                        "Actionnaire introuvable : " + demandee.actionnaireId()));
        return nouvelle(actionnaire, demandee.nombreParts());
    }

    private static CfgCompanyShareholder nouvelle(CfgShareholder actionnaire, int parts) {
        CfgCompanyShareholder ligne = new CfgCompanyShareholder();
        ligne.setId(UUID.randomUUID());
        ligne.setCfgShareholder(actionnaire);
        ligne.setSharingNumber(parts);
        return ligne;
    }
}
