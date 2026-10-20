package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.domain.StatutReglement;
import com.tsix.immocrm.dto.dossier.AffectationReglementRequest;
import com.tsix.immocrm.dto.dossier.CreerReglementRequest;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.dossier.AffectationsReglement.LigneAffectee;
import com.tsix.immocrm.utils.Requis;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNullElse;

/**
 * Seule écriture d'un règlement de vente : un {@code StlSettlement} et ses lignes, tout contrôlé avant la
 * première écriture (pénalités comprises). <b>Saisie complète</b>, sinon l'autorisation d'encaissement sort
 * trouée ; voir FEATURE-reglement-statut-preuve.md §4 et FEATURE-reglement-multi-echeances.md §2.
 */
@Component
public class EcritureReglement {

    /** Le règlement écrit et ses lignes, dans l'ordre des affectations reçues. */
    public record ReglementEcrit(StlSettlement reglement, List<StlSettlementLine> lignes) {
    }

    private final AffectationsReglement affectations;
    private final DepotsReglement depots;
    private final UtilisateurAffiche utilisateurAffiche;
    private final ReglementOmran reglementOmran;
    private final PenaliteRetardService penalites;
    private final AlertesImpayes alertes;
    private final Clock clock;

    public EcritureReglement(AffectationsReglement affectations, DepotsReglement depots,
                             UtilisateurAffiche utilisateurAffiche, ReglementOmran reglementOmran,
                             PenaliteRetardService penalites, AlertesImpayes alertes, Clock clock) {
        this.affectations = affectations;
        this.alertes = alertes;
        this.depots = depots;
        this.utilisateurAffiche = utilisateurAffiche;
        this.reglementOmran = reglementOmran;
        this.penalites = penalites;
        this.clock = clock;
    }

    public ReglementEcrit creer(ComFolder folder, CreerReglementRequest entete,
                                List<AffectationReglementRequest> repartition, AuthenticatedUser principal) {
        return creer(folder, entete, repartition, principal, false);
    }

    /**
     * {@code decaissement} : remboursement au client ({@code IsCredit = 0}) sur une pièce au débit, plafonné par
     * {@link AffectationsReglement#resteDu} ; ni pénalité de retard, ni alerte d'impayé.
     */
    public ReglementEcrit creer(ComFolder folder, CreerReglementRequest entete,
                                List<AffectationReglementRequest> repartition, AuthenticatedUser principal,
                                boolean decaissement) {
        EnTeteValidee valide = validerEnTete(folder, entete);
        List<LigneAffectee> lignes = affectations.valider(folder.getId(), repartition, List.of(), decaissement);
        BigDecimal affecte = AffectationsReglement.somme(lignes);
        if (affecte.compareTo(valide.montant()) > 0) {
            throw new InvalidRequestException("Le reste à affecter doit être >= 0 : les affectations ("
                    + affecte.toPlainString() + ") dépassent le montant du règlement ("
                    + valide.montant().toPlainString() + ")");
        }
        if (!decaissement) {
            genererPenalites(lignes);
        }

        StlSettlement enregistre = depots.reglements().save(
                nouveauReglement(folder, entete, valide, principal, decaissement));
        return new ReglementEcrit(enregistre, ecrireLignes(enregistre, lignes, !decaissement));
    }

    private record EnTeteValidee(BigDecimal montant, LocalDateTime dateEcheance, StlSettlementType type,
                                 CfgTier banque, CfgTier payeur) {
    }

    private EnTeteValidee validerEnTete(ComFolder folder, CreerReglementRequest entete) {
        validerEtatInitial(entete, folder.getId());
        BigDecimal montant = Requis.champ(entete.montant(), "montant est requis");
        if (montant.signum() <= 0) {
            throw new InvalidRequestException("montant doit être strictement positif");
        }
        LocalDateTime dateEcheance = Requis.champ(entete.dateEcheance(), "dateEcheance est requise");
        UUID typeId = Requis.champ(entete.typeId(), "typeId est requis");
        if (entete.payeurId() == null && folder.getCfgCustomer() == null) {
            throw new InvalidRequestException("payeurId est requis : le dossier n'a pas de client");
        }
        StlSettlementType type = depots.types().findById(typeId)
                .orElseThrow(() -> new InvalidRequestException("StlSettlementType introuvable: " + typeId));
        validerSaisieDuType(type, entete);
        CfgTier banque = entete.banqueId() == null ? null : depots.tiers().findById(entete.banqueId())
                .orElseThrow(() -> new InvalidRequestException("Banque (CfgTier) introuvable: " + entete.banqueId()));
        return new EnTeteValidee(montant, dateEcheance, type, banque, payeurSaisi(entete.payeurId(), folder));
    }

    private StlSettlement nouveauReglement(ComFolder folder, CreerReglementRequest entete, EnTeteValidee valide,
                                           AuthenticatedUser principal, boolean decaissement) {
        StlSettlement settlement = new StlSettlement();
        settlement.setId(UUID.randomUUID());
        settlement.setDocDate(LocalDate.now(clock).atStartOfDay());
        settlement.setEcheanceDate(valide.dateEcheance());
        settlement.setSettlementDate(null);
        settlement.setAmount(valide.montant());
        settlement.setCode(requireNonNullElse(entete.reference(), ""));
        settlement.setNotes(requireNonNullElse(entete.notes(), ""));
        settlement.setStatus(StatutReglement.INITIAL.name());
        settlement.setIsCredit(!decaissement);
        String auteur = utilisateurAffiche.nom(principal);
        LocalDateTime maintenant = LocalDateTime.now(clock);
        settlement.setUserCreate(auteur);
        settlement.setUserCreateDateTime(maintenant);
        settlement.setUserUpdate(auteur);
        settlement.setUserUpdateDateTime(maintenant);
        settlement.setStlSettlementType(valide.type());
        settlement.setCfgBank(valide.banque());
        // Comme OMRAN : TierId = client du dossier, TierPayerId = qui a payé (à défaut le client).
        settlement.setTier(folder.getCfgCustomer() != null ? folder.getCfgCustomer() : valide.payeur());
        settlement.setTierPayer(valide.payeur());
        reglementOmran.completer(settlement, folder);
        return settlement;
    }

    /**
     * Ajoute des lignes sans toucher {@code Amount} ni les lignes existantes ; {@code resteAAffecter} est lu
     * par l'appelant avant cet appel.
     */
    public List<StlSettlementLine> affecter(StlSettlement reglement, UUID dossierId,
                                            List<AffectationReglementRequest> repartition,
                                            Collection<UUID> dejaAffectees, BigDecimal resteAAffecter) {
        List<LigneAffectee> lignes = affectations.valider(dossierId, repartition, dejaAffectees);
        BigDecimal affecte = AffectationsReglement.somme(lignes);
        if (affecte.compareTo(resteAAffecter) > 0) {
            throw new InvalidRequestException("Les affectations (" + affecte.toPlainString()
                    + ") dépassent le reste à affecter (" + resteAAffecter.toPlainString() + ")");
        }
        genererPenalites(lignes);
        return ecrireLignes(reglement, lignes, true);
    }

    /** Avant l'enregistrement : la pénalité se calcule sur le reste d'avant ce règlement (FEATURE-impayes.md P3). */
    private void genererPenalites(List<LigneAffectee> lignes) {
        lignes.forEach(ligne -> penalites.genererPenalite(ligne.echeance()).ifPresent(alertes::synchroniser));
    }

    /** {@code SettlementLine.cs} AfterInsert : l'alerte d'impayé de chaque échéance réglée suit son nouveau reste. */
    private List<StlSettlementLine> ecrireLignes(StlSettlement reglement, List<LigneAffectee> lignes, boolean alerter) {
        List<StlSettlementLine> ecrites = lignes.stream().map(ligne -> {
            StlSettlementLine line = new StlSettlementLine();
            line.setId(UUID.randomUUID());
            line.setStlSettlement(reglement);
            line.setComDocument(ligne.echeance());
            line.setAmount(ligne.montant());
            return depots.lignes().save(line);
        }).toList();
        if (alerter) {
            lignes.stream().map(LigneAffectee::echeance).distinct().forEach(alertes::synchroniser);
        }
        return ecrites;
    }

    /** Un règlement naît IN_WORK, sans date de règlement : elle n'appartient qu'au passage à POINTED. */
    private static void validerEtatInitial(CreerReglementRequest request, UUID dossierId) {
        // FEATURE-reglement-statut-preuve.md §2 : aucune ligne IN_WORK réelle n'est datée.
        if (request.dateReglement() != null) {
            throw new InvalidRequestException("dateReglement ne peut pas être fournie à la création : "
                    + "elle se renseigne au passage à POINTED, via PATCH /api/dossiers/" + dossierId
                    + "/reglements/{reglementId}/statut");
        }
        if (StringUtils.hasText(request.statut())
                && !StatutReglement.INITIAL.name().equalsIgnoreCase(request.statut().trim())) {
            throw new InvalidRequestException("Un règlement est toujours créé en "
                    + StatutReglement.INITIAL + " : pour le pointer, déposez d'abord la preuve de "
                    + "paiement puis appelez PATCH .../reglements/{reglementId}/statut");
        }
    }

    /** IsReferenceMandatory / IsBankMandatory varient par type (chèques, banque, certifié, effet). */
    private static void validerSaisieDuType(StlSettlementType type, CreerReglementRequest request) {
        if (Boolean.TRUE.equals(type.getIsReferenceMandatory()) && !StringUtils.hasText(request.reference())) {
            throw new InvalidRequestException("reference est requise pour le type de règlement " + type.getCode());
        }
        if (Boolean.TRUE.equals(type.getIsBankMandatory()) && request.banqueId() == null) {
            throw new InvalidRequestException("banqueId est requise pour le type de règlement " + type.getCode());
        }
    }

    private CfgTier payeurSaisi(UUID payeurId, ComFolder folder) {
        if (payeurId == null) {
            return folder.getCfgCustomer();
        }
        return depots.tiers().findById(payeurId)
                .orElseThrow(() -> new InvalidRequestException("Payeur (CfgTier) introuvable: " + payeurId));
    }
}
