package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.projet.AlignementPrevisions;
import com.tsix.immocrm.domain.projet.EntreesSimulation;
import com.tsix.immocrm.dto.projet.AlignementPrevisionsDto;
import com.tsix.immocrm.dto.projet.AlignementPrevisionsRequest;
import com.tsix.immocrm.dto.projet.LigneModeleDto;
import com.tsix.immocrm.dto.projet.ModeleSimulationSaisieDto;
import com.tsix.immocrm.dto.projet.PointSaisieDto;
import com.tsix.immocrm.dto.projet.PropositionPointsDto;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.entities.prj.PrjMarketPrevisionModel;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.services.droits.DroitsOmran;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.services.projet.FacturesDecomptes.montant;
import static java.math.BigDecimal.ZERO;

/** Propositions de points « Prévisions » d'un lot, sans écriture (FEATURE-lot-previsions-reel.md §4 A1-A6, M5, M6). */
@Service
@Transactional(readOnly = true)
public class AlignementPrevisionsService {

    private static final String FIXEE = "FIXEE";
    private static final List<String> MODES = List.of("INCHANGEE", FIXEE, "SYSTEME");
    private static final BigDecimal SEUIL_MONTANT = new BigDecimal("0.1");

    // GetDeltaPayment : pièces payables « Facture » (BuyDocumentToPayView = CanBePayed) − nets des décomptes facturés.
    private static final String ECART_MONTANT = """
            SELECT ISNULL((SELECT SUM(AmountToPay) FROM BuyDocument WHERE PrjMarketId = :lot AND CanBePayed = 1
                    AND DocType <> 'REFUND'
                    AND ISNULL(ReferenceType, '') NOT IN ('MARKET_ADVANCE', 'WORKINGOUT_ADVANCE', 'MARKET_WARRANTY')), 0)
                 - ISNULL((SELECT SUM(AmountToPay) FROM PrjWorkingOut WHERE PrjMarketId = :lot AND BuyDocumentId IS NOT NULL), 0)""";

    private final LotsProjetService lots;
    private final DroitsOmran droits;
    private final LectureSimulationLot lecture;
    private final EntityManager em;

    AlignementPrevisionsService(LotsProjetService lots, DroitsOmran droits, LectureSimulationLot lecture, EntityManager em) {
        this.lots = lots;
        this.droits = droits;
        this.lecture = lecture;
        this.em = em;
    }

    public AlignementPrevisionsDto ecarts(UUID id) {
        PrjMarket lot = lots.visible(id);
        DroitsPrevisionsLot.de(droits).exigerSaisie();
        AlignementPrevisions.Ecarts e = AlignementPrevisions.ecarts(entreesDatees(lot));
        BigDecimal montant = zero(montant(em.createNativeQuery(ECART_MONTANT).setParameter("lot", id).getSingleResult()));
        return new AlignementPrevisionsDto(e.finActuelle(), e.finSysteme(), e.dernierReel(),
                montant.abs().compareTo(SEUIL_MONTANT) < 0 ? ZERO : montant, e.ecartAvancement(), e.ecartJours());
    }

    public PropositionPointsDto aligner(UUID id, AlignementPrevisionsRequest corps) {
        PrjMarket lot = lots.visible(id);
        DroitsPrevisionsLot.de(droits).exigerSaisie();
        if (corps.mode() == null || !MODES.contains(corps.mode())) {
            throw new InvalidRequestException("Mode inconnu : %s (INCHANGEE, FIXEE ou SYSTEME).".formatted(corps.mode()));
        }
        if (FIXEE.equals(corps.mode()) && corps.dateFin() == null) {
            throw new InvalidRequestException("La date fin est obligatoire pour le mode FIXEE.");
        }
        EntreesSimulation e = lecture.entrees(lot);
        if (e.etat().multi()) {
            throw new ConflictException("Alignement indisponible pour un lot multi-fournisseur.");
        }
        AlignementPrevisions.Ecarts ecarts = AlignementPrevisions.ecarts(datees(e));
        LocalDate fin = switch (corps.mode()) {
            case FIXEE -> corps.dateFin();
            case "SYSTEME" -> ecarts.finSysteme();
            default -> null;
        };
        if (FIXEE.equals(corps.mode()) && fin != null && ecarts.dernierReel() != null && fin.isBefore(ecarts.dernierReel())) {
            throw new ConflictException("Veuillez sélectionnez une date supérieur ou égale à la date dérnière décompte / facture");
        }
        List<PointSaisieDto> points = AlignementPrevisions.aligner(e, fin == null ? ecarts.finActuelle() : fin).stream()
                .map(p -> new PointSaisieDto(p.date(), p.avancement())).toList();
        Integer duree = fin == null ? null : (int) ChronoUnit.DAYS.between(e.planning().debut(), fin);
        return new PropositionPointsDto(fin, duree, points);
    }

    public PropositionPointsDto appliquerModele(UUID id, UUID modeleId) {
        PrjMarket lot = lots.visible(id);
        if (em.find(PrjMarketPrevisionModel.class, modeleId) == null) {
            throw new ResourceNotFoundException("Modèle de simulation introuvable : " + modeleId);
        }
        DroitsPrevisionsLot.de(droits).exigerSaisie();
        if (lot.getStartDate() == null) {
            throw sansDebut();
        }
        LocalDate debut = lot.getStartDate().toLocalDate();
        int duree = lot.getDelay() == null ? 0 : lot.getDelay();
        List<PointSaisieDto> points = em.createQuery("""
                        SELECT l.pointTime, l.pointAchivement FROM PrjMarketPrevisionModelLine l
                        WHERE l.prjMarketPrevisionModelId = :id ORDER BY l.pointTime ASC""", Object[].class)
                .setParameter("id", modeleId).getResultList().stream()
                .map(r -> new PointSaisieDto(AlignementPrevisions.dateDuTemps(debut, duree, zero((BigDecimal) r[0])),
                        zero((BigDecimal) r[1])))
                .toList();
        return new PropositionPointsDto(null, null, points);
    }

    public ModeleSimulationSaisieDto modelePropose(UUID id) {
        lots.visible(id);
        DroitsPrevisionsLot.de(droits).exigerSaisie();
        List<?> brutes = em.createNativeQuery(TablePoints.BASE.lire).setParameter("lot", id).getResultList();
        List<LigneModeleDto> lignes = brutes.stream().map(b -> (Object[]) b)
                .map(r -> new LigneModeleDto(zero(montant(r[3])), zero(montant(r[2])), zero(montant(r[4])))).toList();
        return new ModeleSimulationSaisieDto(null, null, null, lignes);
    }

    private EntreesSimulation entreesDatees(PrjMarket lot) {
        return datees(lecture.entrees(lot));
    }

    private static EntreesSimulation datees(EntreesSimulation e) {
        if (e.planning().debut() == null) {
            throw sansDebut();
        }
        return e;
    }

    private static ConflictException sansDebut() {
        return new ConflictException("Le lot n'a pas de date début de prévisions.");
    }

    private static BigDecimal zero(BigDecimal valeur) {
        return valeur == null ? ZERO : valeur;
    }
}
