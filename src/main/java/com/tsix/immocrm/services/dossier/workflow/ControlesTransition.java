package com.tsix.immocrm.services.dossier.workflow;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.cfg.CfgTrancheStatus;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.repositories.cfg.CfgTrancheStatusRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Contrôles d'une transition, dans l'ordre d'OMRAN : champs et pièces obligatoires de la transition puis du statut
 * cible ({@code StrWorkFlow.VerifyForWorkflow}), seuil de versement ({@link SeuilVersement}), schéma de financement
 * ({@link ControleSchema}). Chaque méthode rend le message OMRAN, vide si le contrôle passe.
 */
@Component
public class ControlesTransition {

    static final String ENTETE_TRANSITION = "Pour exécuter la transition : ";
    private static final String FIN_DE_LIGNE = ChampsObligatoires.FIN_DE_LIGNE;

    private final ChampsObligatoires champs;
    private final PiecesObligatoires pieces;
    private final SituationFinanciereDossier situations;
    private final CfgTrancheStatusRepository seuilRepository;

    public ControlesTransition(ChampsObligatoires champs, PiecesObligatoires pieces,
                               SituationFinanciereDossier situations, CfgTrancheStatusRepository seuilRepository) {
        this.champs = champs;
        this.pieces = pieces;
        this.situations = situations;
        this.seuilRepository = seuilRepository;
    }

    /** Texte « Pour exécuter la transition : … » des champs et pièces manquants ; vide si rien ne manque. */
    String obligatoires(ComFolder dossier, TransitionDossier transition, StrStatus cible) {
        String texte = "";
        if (transition.parametree()) {
            texte = champs.manquantsDeTransition(dossier, transition.workflowId())
                    + pieces.manquantesDeTransition(dossier.getId(), transition.workflowId());
        }
        String duStatut = champs.manquantsDuStatut(dossier, cible.getId())
                + pieces.manquantesDuStatut(dossier.getId(), cible.getId());
        if (!duStatut.isEmpty()) {
            duStatut = "Pour le statut " + cible.getDescription() + " : " + FIN_DE_LIGNE + duStatut;
        }
        String separateur = texte.isEmpty() || duStatut.isEmpty() ? "" : FIN_DE_LIGNE + FIN_DE_LIGNE;
        String tout = texte + separateur + duStatut;
        return tout.isEmpty() ? "" : ENTETE_TRANSITION + FIN_DE_LIGNE + tout;
    }

    /** Seuil de versement du statut cible pour ce dossier ({@code Sale.cs:VerifThreshold}). */
    Optional<String> seuil(ComFolder dossier, StrStatus cible) {
        return seuil(dossier, cible, situations.de(dossier));
    }

    /** Vrai si un seuil est paramétré pour la cible et que le dossier l'atteint (passage automatique). */
    boolean seuilAtteint(ComFolder dossier, StrStatus cible) {
        Optional<SeuilVersement.Configuration> config = configuration(dossier, cible);
        return config.filter(c -> c.operateur() != SeuilVersement.SANS_SEUIL).isPresent()
                && seuil(dossier, cible, situations.de(dossier)).isEmpty();
    }

    /** {@code Sale.cs:VerifSchema}, si le statut cible le demande ({@code BoolField012}). */
    Optional<String> schema(ComFolder dossier, StrStatus cible) {
        if (!Boolean.TRUE.equals(cible.getVerificationSchema())) {
            return Optional.empty();
        }
        ControleSchema.Dossier lu = new ControleSchema.Dossier(dossier.getPriceAfterDiscount(),
                dossier.getModificationsAmount(), Boolean.TRUE.equals(dossier.getPriceWithModification()),
                dossier.getAgreementDate(), dossier.getAgreementDatePrevisionel());
        return ControleSchema.controler(lu, situations.de(dossier));
    }

    /**
     * Contrôles de chaque enregistrement de la fiche ({@code Sale.cs:VerifThreshold} avant son retour l.3127) :
     * délai maximal de paiement puis crédit maximal ; message OMRAN du premier qui échoue.
     */
    public Optional<String> enregistrement(ComFolder dossier) {
        CfgTranche tranche = tranche(dossier);
        if (tranche == null) {
            return Optional.empty();
        }
        SeuilVersement.DelaiMax delai = new SeuilVersement.DelaiMax(
                Boolean.TRUE.equals(tranche.getIsWithVerifMaxEcheanceDelai()), tranche.getMaxEcheanceDelai());
        if (delai.verifier()) {
            Optional<String> horsDelai = SeuilVersement.delaiMax(delai,
                    situations.derniereEcheance(dossier).orElse(null), remiseClesPrevue(dossier, tranche),
                    dossier.getDeliveryDateReal());
            if (horsDelai.isPresent()) {
                return horsDelai;
            }
        }
        SeuilVersement.Regles regles = regles(tranche, null);
        if (!regles.verifCreditMax()) {
            return Optional.empty();
        }
        return SeuilVersement.creditMax(regles, situations.de(dossier).credit(), dossier.getPrice());
    }

    /** {@code Sale.cs:DeliveryDate} : date de livraison de l'article, sinon début de finalisation de la tranche. */
    private static LocalDateTime remiseClesPrevue(ComFolder dossier, CfgTranche tranche) {
        LocalDateTime surArticle = dossier.getStkItem().getDeliveryDate();
        return surArticle != null ? surArticle : tranche.getStartDateFinalisation();
    }

    private static SeuilVersement.Regles regles(CfgTranche tranche, StrStatus cible) {
        return new SeuilVersement.Regles(
                cible != null && Boolean.TRUE.equals(cible.getVenteAuComptant()),
                cible != null && Boolean.TRUE.equals(cible.getVenteACredit()),
                tranche != null && Boolean.TRUE.equals(tranche.getIsWithVerifCredit()),
                tranche != null ? tranche.getCreditPercent() : null);
    }

    private Optional<String> seuil(ComFolder dossier, StrStatus cible, SituationFinanciere situation) {
        CfgTranche tranche = tranche(dossier);
        SeuilVersement.Regles regles = regles(tranche, cible);
        return SeuilVersement.controler(configuration(dossier, cible).orElse(null), regles, situation,
                dossier.getPriceAfterDiscount(), dossier.getPrice());
    }

    /** Ligne de la tranche du lot, sinon ligne globale ; taux plafonné pour un ancien client à la concrétisation. */
    private Optional<SeuilVersement.Configuration> configuration(ComFolder dossier, StrStatus cible) {
        CfgTranche tranche = tranche(dossier);
        List<CfgTrancheStatus> lignes = tranche != null
                ? seuilRepository.deLaTranche(cible.getId(), tranche.getId()) : List.of();
        if (lignes.isEmpty()) {
            lignes = seuilRepository.global(cible.getId());
        }
        return lignes.stream().findFirst().map(ligne -> {
            BigDecimal ratio = ligne.getThresholdRatio() != null ? ligne.getThresholdRatio() : BigDecimal.ZERO;
            if (plafonnerPourAncienClient(dossier, tranche, cible, ratio)) {
                ratio = tranche.getAdvancePercentDiscount();
            }
            return new SeuilVersement.Configuration(
                    ligne.getThresholdOperator() != null ? ligne.getThresholdOperator() : SeuilVersement.SANS_SEUIL,
                    ratio,
                    ligne.getComFolderStatusPaymentType() != null ? ligne.getComFolderStatusPaymentType().getId() : null);
        });
    }

    private static boolean plafonnerPourAncienClient(ComFolder dossier, CfgTranche tranche, StrStatus cible,
                                                     BigDecimal ratio) {
        return tranche != null && Boolean.TRUE.equals(tranche.getIsActiveAdvancePercentDiscount())
                && StatutDossier.CONCRETISATION.getCode().equalsIgnoreCase(cible.getCode())
                && ratio.signum() != 0 && Boolean.TRUE.equals(dossier.getIsOldCustomer())
                && tranche.getAdvancePercentDiscount() != null
                && ratio.compareTo(tranche.getAdvancePercentDiscount()) > 0;
    }

    private static CfgTranche tranche(ComFolder dossier) {
        return dossier.getStkItem() != null ? dossier.getStkItem().getStkHierarchy() : null;
    }
}
