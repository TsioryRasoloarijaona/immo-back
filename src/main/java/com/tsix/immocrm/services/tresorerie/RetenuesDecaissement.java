package com.tsix.immocrm.services.tresorerie;

import com.tsix.immocrm.domain.achat.CalculReglement;
import com.tsix.immocrm.domain.achat.CalculRetenue;
import com.tsix.immocrm.dto.tresorerie.LigneRetenueDto;
import com.tsix.immocrm.dto.tresorerie.TypeRetenueDto;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.stl.StlDeduction;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.stl.StlDeductionRepository;
import com.tsix.immocrm.repositories.stl.StlDeductionTypeRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Retenues à la source d'un décaissement (FEATURE-n3bis-b2a-retenue.md §6 RS-a à RS-p). */
@Component
public class RetenuesDecaissement {

    private static final String GESTION_RS =
            "SELECT COUNT(*) FROM AdmParamsBoolean WHERE Code = 'ManageRS' AND CodeValue = 1";
    private static final String MSG_INACTIVE = "La retenue à la source n'est pas activée.";
    private static final String MSG_LIGNE_NULLE = "Vous devez corriger le montant ou supprimer la ligne !";

    private final StlDeductionRepository deductions;
    private final StlDeductionTypeRepository types;
    private final EntityManager em;

    public RetenuesDecaissement(StlDeductionRepository deductions, StlDeductionTypeRepository types,
                                EntityManager em) {
        this.deductions = deductions;
        this.types = types;
        this.em = em;
    }

    /** Lignes calculées ; {@code remplacer} faux = lignes existantes gardées (PUT sans {@code retenues}). */
    public record Retenues(List<StlDeduction> lignes, boolean remplacer) {
        BigDecimal total() {
            return CalculReglement.affecte(lignes.stream().map(StlDeduction::getAmountRS).toList());
        }
    }

    /** Paramètre {@code ManageRS} (ligne absente = 0), lu à la demande. */
    public boolean gestionRs() {
        return ((Number) em.createNativeQuery(GESTION_RS).getSingleResult()).intValue() > 0;
    }

    public List<TypeRetenueDto> types() {
        return types.findAllByOrderBySortingAscCodeAsc().stream()
                .map(t -> new TypeRetenueDto(t.getId(), t.getCode(), t.getDescription(), t.getRasValue(), t.getRasYear()))
                .toList();
    }

    public List<LigneRetenueDto> lire(UUID reglementId) {
        return deductions.findByStlSettlementIdOrderByAmountBaseAsc(reglementId).stream()
                .map(d -> new LigneRetenueDto(d.getId(), d.getStlDeductionTypeId(), d.getAmountBase(), d.getRsRatio(),
                        d.getAmountRS(), CalculReglement.arrondi(d.getAmount()), d.getNotes()))
                .toList();
    }

    /** Contrôle et calcul avant tout setter ; {@code reglementId} null = création (RS-g). */
    Retenues preparer(List<LigneRetenueDto> saisies, UUID reglementId, CfgTier fournisseur, BigDecimal brut) {
        if (saisies == null && reglementId != null) {
            return new Retenues(deductions.findByStlSettlementIdOrderByAmountBaseAsc(reglementId), false);
        }
        if (saisies == null) {
            return new Retenues(parDefaut(fournisseur, brut), true);
        }
        if (!saisies.isEmpty() && !gestionRs()) {
            throw new ConflictException(MSG_INACTIVE);
        }
        return new Retenues(saisies.stream().map(this::ligne).toList(), true);
    }

    private List<StlDeduction> parDefaut(CfgTier fournisseur, BigDecimal brut) {
        BigDecimal taux = fournisseur.getRsRatio();
        if (taux == null || taux.signum() == 0 || !gestionRs()) {
            return List.of();
        }
        return List.of(nouvelle(fournisseur.getStlDeductionTypeId(), brut, taux, CalculRetenue.montantRs(brut, taux),
                null));
    }

    private StlDeduction ligne(LigneRetenueDto saisie) {
        BigDecimal base = saisie.base();
        if (base == null) {
            throw new InvalidRequestException("base de la retenue est requise");
        }
        if (saisie.typeRetenueId() != null && !types.existsById(saisie.typeRetenueId())) {
            throw new InvalidRequestException("Type de retenue introuvable : " + saisie.typeRetenueId());
        }
        boolean parMontant = saisie.montantRs() != null;
        BigDecimal montantRs = parMontant
                ? CalculReglement.arrondi(saisie.montantRs()) : CalculRetenue.montantRs(base, saisie.taux());
        if (CalculReglement.estNul(montantRs)) {
            throw new InvalidRequestException(MSG_LIGNE_NULLE);
        }
        BigDecimal taux = parMontant ? CalculRetenue.taux(base, montantRs) : saisie.taux();
        return nouvelle(saisie.typeRetenueId(), base, taux, montantRs, saisie.notes());
    }

    private static StlDeduction nouvelle(UUID type, BigDecimal base, BigDecimal taux, BigDecimal montantRs,
                                         String notes) {
        StlDeduction d = new StlDeduction();
        d.setId(UUID.randomUUID());
        d.setStlDeductionTypeId(type);
        d.setAmountBase(base);
        d.setRsRatio(taux);
        d.setAmountRS(montantRs);
        d.setAmount(CalculRetenue.net(base, montantRs));
        d.setNotes(notes);
        return d;
    }

    /** RS-a, RS-b, RS-c, RS-l : à appeler après {@code ReglementOmran.completer}, qui remet les montants RS à 0. */
    void poser(StlSettlement reglement, BigDecimal brut, Retenues retenues, Boolean rsDeclaree, LocalDate date) {
        BigDecimal total = retenues.total();
        reglement.setAmountRS(total);
        reglement.setRsRatio(CalculRetenue.taux(brut, total));
        reglement.setAmount(CalculReglement.resteAAffecter(brut, total));
        reglement.setAmountStamp(BigDecimal.ZERO);
        boolean declaree = Boolean.TRUE.equals(rsDeclaree);
        reglement.setRSAmountSettled(declaree);
        reglement.setRSSettlementDate(declaree && date != null ? date.atStartOfDay() : null);
    }

    /** Remplace les lignes du règlement (rien si elles sont gardées). */
    void ecrire(UUID reglementId, Retenues retenues, String auteur, LocalDateTime maintenant) {
        if (!retenues.remplacer()) {
            return;
        }
        supprimer(reglementId);
        for (StlDeduction d : retenues.lignes()) {
            d.setStlSettlementId(reglementId);
            d.setUserCreate(auteur);
            d.setUserCreateDateTime(maintenant);
            d.setUserUpdate(auteur);
            d.setUserUpdateDateTime(maintenant);
            em.persist(d);
        }
    }

    void supprimer(UUID reglementId) {
        deductions.findByStlSettlementIdOrderByAmountBaseAsc(reglementId).forEach(em::remove);
    }
}
