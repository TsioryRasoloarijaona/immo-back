package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.achat.SoldeDocumentAchat;
import com.tsix.immocrm.domain.projet.AgregationPrevisionsProjet;
import com.tsix.immocrm.domain.projet.MontantsLot;
import com.tsix.immocrm.domain.projet.NaturePieceLot;
import com.tsix.immocrm.dto.projet.AvanceLotDto;
import com.tsix.immocrm.dto.projet.AvancesLotDto;
import com.tsix.immocrm.dto.projet.AvenantLotDto;
import com.tsix.immocrm.dto.projet.AvoirLotDto;
import com.tsix.immocrm.dto.projet.PrevisionsLotDto;
import com.tsix.immocrm.dto.projet.PrevisionsLotDto.PointPrevisionDto;
import com.tsix.immocrm.dto.projet.RemboursementsLotDto;
import com.tsix.immocrm.dto.projet.RemboursementsLotDto.EcheanceRgLotDto;
import com.tsix.immocrm.dto.projet.RemboursementsLotDto.OptionGenreDto;
import com.tsix.immocrm.dto.projet.RemboursementsLotDto.OptionReceptionDto;
import com.tsix.immocrm.dto.projet.RemboursementsLotDto.ReceptionLotDto;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.services.droits.DroitsOmran;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.services.projet.FacturesDecomptes.date;
import static com.tsix.immocrm.services.projet.FacturesDecomptes.montant;
import static com.tsix.immocrm.services.projet.FacturesDecomptes.uuid;

/** Sous-fenêtres de la fiche lot en lecture (FEATURE-lot-sous-fenetres.md) ; chaque route lit d'abord le lot visible. */
@Service
@Transactional(readOnly = true)
public class SousFenetresLotService {

    private static final String MARKET_ADVANCE = "MARKET_ADVANCE";
    private static final String MARKET_WARRANTY = "MARKET_WARRANTY";

    // Réglé par SoldeDocumentAchat (règle 6) ; modifiable / supprimable par DroitsPieceLot (FEATURE-lot-pieces-a-payer.md R17).
    private static final String PAR_DATE = " ORDER BY d.DocDate DESC, d.Code ASC";

    private static final String PIECES = """
            SELECT d.PKey, d.Code, d.BuyDocumentStatusId, st.Description, st.ColorName, d.DocDate, d.AmountToPay,
                   (""" + SoldeDocumentAchat.SQL_REGLE + "), c.Code, d.Notes, d.ReferenceType, " + DroitsPieceLot.ETAT_PIECE
            + DroitsPieceLot.PIECES_DU_LOT + DroitsPieceLot.DES_NATURES + PAR_DATE;

    private static final String AVOIRS = """
            SELECT d.PKey, d.DocDate, d.Code, st.Description, st.ColorName, d.PersonToContact,
                   d.AmountHT, d.AmountVat, d.AmountTTC, d.OfficeOrderRef, """ + DroitsPieceLot.ETAT_PIECE
            + DroitsPieceLot.PIECES_DU_LOT + DroitsPieceLot.AVOIR + PAR_DATE;

    private static final String AVENANTS = """
            SELECT c.PKey, c.Sorting, c.DocDate, c.Code, c.Description, c.AmountHT, c.AmountVat, c.AmountTTC, c.AmountToPay,
                   (SELECT STRING_AGG(f.Code, ', ') FROM BuyDocument f WHERE f.PrjClauseId = c.PKey AND f.DocType = 'INVOICE')
            FROM PrjClause c WHERE c.PrjMarketId = :lot ORDER BY c.DocDate DESC, c.Code ASC""";

    private static final String ECHEANCES = """
            SELECT PKey, Sorting, Description, Delay, EcheanceDate, Ratio FROM PrjMarketWarrantyLine
            WHERE PrjMarketId = :lot ORDER BY Sorting ASC, EcheanceDate ASC""";

    private static final String RECEPTIONS = """
            SELECT r.PKey, r.sorting, r.Code, r.DocDate, r.RequiredBySupplier, r.MeetingPlace, t.Description, k.Description, k.IsFinal
            FROM PrjMarketReception r
            LEFT JOIN PrjMarketReceptionType t ON t.PKey = r.PrjMarketReceptionTypeId
            LEFT JOIN PrjMarketReceptionKind k ON k.PKey = r.PrjMarketReceptionKindId
            WHERE r.PrjMarketId = :lot ORDER BY r.sorting ASC, r.Code ASC""";

    private static final String TYPES_RECEPTION = "SELECT PKey, Description FROM PrjMarketReceptionType ORDER BY Code ASC";
    private static final String GENRES_RECEPTION = "SELECT PKey, Description, IsFinal FROM PrjMarketReceptionKind ORDER BY Code ASC";

    private final LotsProjetService lots;
    private final DroitsPieceLot pieceLot;
    private final EntityManager em;
    private final DroitsOmran droits;
    private final SeriesPrevisionsLots series;
    private final DateReceptionDefinitive dateDefinitive;

    SousFenetresLotService(LotsProjetService lots, DroitsPieceLot pieceLot, EntityManager em, DroitsOmran droits,
                           SeriesPrevisionsLots series, DateReceptionDefinitive dateDefinitive) {
        this.lots = lots;
        this.pieceLot = pieceLot;
        this.em = em;
        this.droits = droits;
        this.series = series;
        this.dateDefinitive = dateDefinitive;
    }

    public AvancesLotDto avances(UUID id) {
        Map<String, List<AvanceLotDto>> parReference = pieces(lots.visible(id).getId(), MARKET_ADVANCE, "WORKINGOUT_ADVANCE");
        return new AvancesLotDto(parReference.get(MARKET_ADVANCE), parReference.get("WORKINGOUT_ADVANCE"),
                pieceLot.droits().avance());
    }

    public List<AvoirLotDto> avoirs(UUID id) {
        DroitsPieceLot.Droits d = pieceLot.droits();
        return lignes(pieceLot.duLot(AVOIRS, lots.visible(id).getId())).stream().map(r -> new AvoirLotDto(uuid(r[0]),
                date(r[1]), (String) r[2], (String) r[3], (String) r[4], (String) r[5], montant(r[6]), montant(r[7]),
                montant(r[8]), (String) r[9], DroitsPieceLot.refusSuppressionAvoir(d, DroitsPieceLot.etat(r, 10)) == null))
                .toList();
    }

    public List<AvenantLotDto> avenants(UUID id) {
        return lignes(duLot(AVENANTS, lots.visible(id).getId())).stream().map(r -> new AvenantLotDto(uuid(r[0]),
                entier(r[1]), date(r[2]), (String) r[3], (String) r[4], montant(r[5]), montant(r[6]), montant(r[7]),
                montant(r[8]), (String) r[9])).toList();
    }

    public RemboursementsLotDto remboursements(UUID id) {
        UUID lot = lots.visible(id).getId();
        List<EcheanceRgLotDto> echeances = lignes(duLot(ECHEANCES, lot)).stream().map(r -> new EcheanceRgLotDto(uuid(r[0]),
                entier(r[1]), (String) r[2], entier(r[3]), date(r[4]), MontantsLot.pourcentage(montant(r[5])))).toList();
        List<ReceptionLotDto> receptions = lignes(duLot(RECEPTIONS, lot)).stream().map(r -> new ReceptionLotDto(uuid(r[0]),
                entier(r[1]), (String) r[2], date(r[3]), vrai(r[4]), (String) r[5], (String) r[6], (String) r[7],
                vrai(r[8]))).toList();
        Map<String, List<AvanceLotDto>> parReference = pieces(lot, MARKET_WARRANTY, "MARKET_PROPORTION");
        BigDecimal total = echeances.stream().map(EcheanceRgLotDto::pourcentage).map(SousFenetresLotService::zero)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<OptionReceptionDto> types = lignes(em.createNativeQuery(TYPES_RECEPTION)).stream()
                .map(r -> new OptionReceptionDto(uuid(r[0]), (String) r[1])).toList();
        List<OptionGenreDto> genres = lignes(em.createNativeQuery(GENRES_RECEPTION)).stream()
                .map(r -> new OptionGenreDto(uuid(r[0]), (String) r[1], vrai(r[2]))).toList();
        return new RemboursementsLotDto(echeances, receptions, parReference.get(MARKET_WARRANTY),
                parReference.get("MARKET_PROPORTION"), dateDefinitive.du(lot), total, types, genres,
                pieceLot.droits().avance());
    }

    public PrevisionsLotDto previsions(UUID id) {
        PrjMarket lot = lots.visible(id);
        // Sommes non arrondies : la moitié des montants prévus a plus de 2 décimales, Σ des mois = Σ de la table.
        List<PointPrevisionDto> mois = new AgregationPrevisionsProjet(List.of(series.lire(List.of(lot)).getFirst().series()),
                Map.of(lot.getId(), BigDecimal.ONE)).mois();
        return new PrevisionsLotDto(lot.getId(), mois.isEmpty() ? null : mois.getFirst().mois(),
                DroitsPrevisionsLot.de(droits).saisir(), mois);
    }

    /** Pièces DOCUMENT_TO_PAY du lot par ReferenceType (chaque type demandé présent, liste vide au besoin). */
    private Map<String, List<AvanceLotDto>> pieces(UUID lot, String... references) {
        Map<String, List<AvanceLotDto>> parReference = new HashMap<>();
        for (String reference : references) {
            parReference.put(reference, new ArrayList<>());
        }
        DroitsPieceLot.Droits droits = pieceLot.droits();
        DroitsPieceLot.EtatLot etatLot = pieceLot.etatLot(lot);
        for (Object[] r : lignes(pieceLot.duLot(PIECES, lot).setParameter("references", List.of(references)))) {
            SoldeDocumentAchat solde = SoldeDocumentAchat.de(montant(r[6]), montant(r[7]));
            NaturePieceLot nature = NaturePieceLot.valueOf((String) r[10]);
            DroitsPieceLot.EtatPiece etat = DroitsPieceLot.etat(r, 11);
            parReference.get(r[10]).add(new AvanceLotDto(uuid(r[0]), (String) r[1], uuidOuNull(r[2]),
                    (String) r[3], (String) r[4], date(r[5]), solde.montantAPayer(), solde.montantRegle(),
                    solde.resteARegler(), (String) r[8], (String) r[9],
                    DroitsPieceLot.refusModification(nature, etatLot, etat) == null,
                    DroitsPieceLot.refusSuppression(nature, droits, etat) == null));
        }
        return parReference;
    }

    private Query duLot(String sql, UUID lot) {
        return em.createNativeQuery(sql).setParameter("lot", lot);
    }

    private static List<Object[]> lignes(Query requete) {
        List<?> brutes = requete.getResultList();
        return brutes.stream().map(Object[].class::cast).toList();
    }

    private static BigDecimal zero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static Integer entier(Object brut) {
        return brut instanceof Number n ? n.intValue() : null;
    }

    private static boolean vrai(Object brut) {
        return Boolean.TRUE.equals(brut) || (brut instanceof Number n && n.intValue() == 1);
    }

    private static UUID uuidOuNull(Object brut) {
        return brut == null ? null : uuid(brut);
    }
}
