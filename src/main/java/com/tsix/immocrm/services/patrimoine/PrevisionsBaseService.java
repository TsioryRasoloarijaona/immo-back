package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.PrevisionsCloche;
import com.tsix.immocrm.domain.PrevisionsCloche.Periode;
import com.tsix.immocrm.dto.patrimoine.LignePrevisionDto;
import com.tsix.immocrm.dto.patrimoine.PrevisionsBaseDto;
import com.tsix.immocrm.dto.patrimoine.StockTrancheDto;
import com.tsix.immocrm.entities.cfg.CfgPrevision;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.cfg.TrancheVisibleRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.fraction;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.ou;
import static java.util.Objects.requireNonNullElse;

/**
 * Prévisions de base de la tranche (FEATURE-patrimoine-tranche-regles.md §3, §11.2-§11.4, §11.9) : en-tête sur
 * {@code CfgTranche}, lignes {@code CfgPrevision} ; « Générer » calcule sans rien lire ni écrire.
 */
@Service
@Transactional(readOnly = true)
public class PrevisionsBaseService {

    private static final String ID = "id";
    private static final String LIGNES = "SELECT p FROM CfgPrevision p WHERE p.cfgTranche.id = :id ORDER BY p.year, p.month";
    private static final String STOCK = " FROM StkItem i WHERE i.stkHierarchy.id = :id AND i.stkVocation.code = 'VENTE'";
    /** Jour de {@code ProjectPrevisionLine.Date} : OMRAN ne garde que l'année et le mois. */
    private static final int JOUR_OMRAN = 15;
    private static final int ECHELLE = 6;

    private final TrancheVisibleRepository tranches;
    private final EntityManager em;

    public PrevisionsBaseService(TrancheVisibleRepository tranches, EntityManager em) {
        this.tranches = tranches;
        this.em = em;
    }

    public PrevisionsBaseDto previsions(UUID trancheId) {
        return lire(tranches.exigerVisible(trancheId));
    }

    private PrevisionsBaseDto lire(CfgTranche t) {
        List<LignePrevisionDto> lignes = em.createQuery(LIGNES, CfgPrevision.class).setParameter(ID, t.getId())
                .getResultList().stream()
                .map(p -> new LignePrevisionDto(p.getId(), p.getDocDate(), p.getYear(), p.getMonth(), p.getQuantity(),
                        p.getTurnoverQuantity(), p.getNotes(), p.getAchievementAmount(), p.getTurnoverAmount(),
                        p.getAmount(), p.getTurnover()))
                .toList();
        return new PrevisionsBaseDto(t.getCountItemBasePrevision(), t.getTotalSaleBasePrevision(),
                t.getBaseConcretisationPercent(), t.getBaseMarketingStartDate(), t.getBaseEndDateConcretisation(),
                t.getBaseStartDateFinalisation(), t.getBaseMarketingEndDate(), t.getBaseEcarttypeConcretisation(),
                t.getBaseEcarttypeFinalisation(), lignes);
    }

    /** Vue {@code StkTrancheValorisation} recalculée : CountItem hors sous-articles, PriceAllItem sous-articles compris. */
    public StockTrancheDto stock(UUID trancheId) {
        CfgTranche t = tranches.exigerVisible(trancheId);
        Long nombre = em.createQuery("SELECT COUNT(i)" + STOCK + " AND i.isUnderItem = false", Long.class)
                .setParameter(ID, t.getId()).getSingleResult();
        BigDecimal prix = em.createQuery("SELECT COALESCE(SUM(i.salePrice), 0)" + STOCK, BigDecimal.class)
                .setParameter(ID, t.getId()).getSingleResult();
        return new StockTrancheDto(nombre, prix);
    }

    public PrevisionsBaseDto calculerPrevisions(UUID trancheId, PrevisionsBaseDto entete) {
        tranches.exigerVisible(trancheId);
        if (entete == null || Stream.of(entete.debutConcretisation(), entete.finConcretisation(),
                entete.debutFinalisation(), entete.finFinalisation()).anyMatch(Objects::isNull)) {
            throw new InvalidRequestException("Toutes les dates doivent avoir une valeur !");
        }
        List<LignePrevisionDto> lignes = PrevisionsCloche.generer(
                        new Periode(entete.debutConcretisation(), entete.finConcretisation(),
                                ecartType(entete.ecartTypeConcretisation())),
                        new Periode(entete.debutFinalisation(), entete.finFinalisation(),
                                ecartType(entete.ecartTypeFinalisation())),
                        requireNonNullElse(entete.nombreArticles(), 0)).stream()
                .map(m -> ligne(entete, m.mois(), m.concretisation(), m.finalisation(), m.notes()))
                .toList();
        return avecLignes(entete, lignes);
    }

    /** {@code ProjectPrevision.Verif}, puis en-tête et lignes remplacés ; lignes à deux quantités nulles non gardées. */
    @Transactional
    public PrevisionsBaseDto enregistrerPrevisions(UUID trancheId, PrevisionsBaseDto corps) {
        CfgTranche t = tranches.exigerVisible(trancheId);
        if (corps == null) {
            throw new InvalidRequestException("Le corps est obligatoire.");
        }
        List<LignePrevisionDto> lignes = verifier(corps);
        t.setCountItemBasePrevision(requireNonNullElse(corps.nombreArticles(), 0));
        t.setTotalSaleBasePrevision(ou(corps.valorisationStock(), BigDecimal.ZERO));
        t.setBaseConcretisationPercent(ou(fraction(corps.pourcentageConcretisation(), "pourcentageConcretisation"),
                BigDecimal.ZERO));
        t.setBaseMarketingStartDate(corps.debutConcretisation());
        t.setBaseEndDateConcretisation(corps.finConcretisation());
        t.setBaseStartDateFinalisation(corps.debutFinalisation());
        t.setBaseMarketingEndDate(corps.finFinalisation());
        t.setBaseEcarttypeConcretisation(ou(corps.ecartTypeConcretisation(), BigDecimal.ONE));
        t.setBaseEcarttypeFinalisation(ou(corps.ecartTypeFinalisation(), BigDecimal.ONE));
        em.createQuery(LIGNES, CfgPrevision.class).setParameter(ID, t.getId()).getResultList().forEach(em::remove);
        lignes.stream().filter(l -> l.quantiteConcretisation() != 0 || l.quantiteFinalisation() != 0)
                .forEach(l -> em.persist(prevision(t, l)));
        return lire(t);
    }

    /** Lignes normalisées (mois résolu, quantités ≥ 0, montants calculés) ; refus OMRAN en 400. */
    private static List<LignePrevisionDto> verifier(PrevisionsBaseDto corps) {
        List<LignePrevisionDto> recues = requireNonNullElse(corps.lignes(), List.of());
        List<LignePrevisionDto> lignes = new ArrayList<>();
        for (int i = 0; i < recues.size(); i++) {
            LignePrevisionDto l = recues.get(i);
            String champ = "lignes[%d]".formatted(i);
            if (l == null) {
                throw new InvalidRequestException(champ + " est obligatoire.");
            }
            lignes.add(ligne(corps, mois(l, champ), quantite(l.quantiteConcretisation(), champ),
                    quantite(l.quantiteFinalisation(), champ), l.notes()));
        }
        int stock = requireNonNullElse(corps.nombreArticles(), 0);
        if (lignes.stream().mapToInt(LignePrevisionDto::quantiteConcretisation).sum() != stock) {
            throw new InvalidRequestException(
                    "La somme des quantités de concrétisation prévisionnelles doit être égal au nombre d'article en stock !");
        }
        if (lignes.stream().mapToInt(LignePrevisionDto::quantiteFinalisation).sum() != stock) {
            throw new InvalidRequestException(
                    "La somme des quantités de finalisation prévisionnelles doit être égal au nombre d'article en stock !");
        }
        if (lignes.stream().map(l -> YearMonth.of(l.annee(), l.mois())).distinct().count() != lignes.size()) {
            throw new InvalidRequestException("Il ne faut pas que les prévisions contiennent des mois en double !");
        }
        return lignes;
    }

    private static YearMonth mois(LignePrevisionDto l, String champ) {
        if (l.annee() != null && l.mois() != null) {
            if (l.mois() < 1 || l.mois() > 12) {
                throw new InvalidRequestException(champ + ".mois : entre 1 et 12.");
            }
            return YearMonth.of(l.annee(), l.mois());
        }
        if (l.date() == null) {
            throw new InvalidRequestException(champ + ".annee et .mois sont obligatoires.");
        }
        return YearMonth.from(l.date());
    }

    private static int quantite(Integer q, String champ) {
        int valeur = requireNonNullElse(q, 0);
        if (valeur < 0) {
            throw new InvalidRequestException(champ + " : quantité positive ou nulle.");
        }
        return valeur;
    }

    /** Montants de {@code ProjectPrevisionLine} : quantités × valeurs unitaires de l'en-tête (stock / nombre d'articles). */
    private static LignePrevisionDto ligne(PrevisionsBaseDto e, YearMonth mois, int concretisation, int finalisation,
                                           String notes) {
        int stock = requireNonNullElse(e.nombreArticles(), 0);
        BigDecimal valeur = ou(e.valorisationStock(), BigDecimal.ZERO);
        BigDecimal pourcentage = ou(e.pourcentageConcretisation(), BigDecimal.ZERO);
        BigDecimal versementC = unitaire(valeur.multiply(pourcentage), concretisation, stock);
        BigDecimal versementF = unitaire(valeur.multiply(BigDecimal.ONE.subtract(pourcentage)), finalisation, stock);
        return new LignePrevisionDto(null, mois.atDay(JOUR_OMRAN).atStartOfDay(), mois.getYear(), mois.getMonthValue(),
                concretisation, finalisation, notes, versementC, versementF, versementC.add(versementF),
                unitaire(valeur, finalisation, stock));
    }

    private static BigDecimal unitaire(BigDecimal total, int quantite, int stock) {
        return stock == 0 ? BigDecimal.ZERO
                : total.multiply(BigDecimal.valueOf(quantite)).divide(BigDecimal.valueOf(stock), ECHELLE, RoundingMode.HALF_EVEN);
    }

    private static CfgPrevision prevision(CfgTranche t, LignePrevisionDto l) {
        CfgPrevision p = new CfgPrevision();
        p.setId(UUID.randomUUID());
        p.setCfgTranche(t);
        p.setDocDate(l.date());
        p.setYear(l.annee());
        p.setMonth(l.mois());
        p.setQuantity(l.quantiteConcretisation());
        p.setTurnoverQuantity(l.quantiteFinalisation());
        p.setAchievementAmount(l.versementConcretisation());
        p.setTurnoverAmount(l.versementFinalisation());
        p.setAmount(l.versementTotal());
        p.setTurnover(l.chiffreAffaires());
        p.setNotes(requireNonNullElse(l.notes(), ""));
        return p;
    }

    private static double ecartType(BigDecimal valeur) {
        return ou(valeur, BigDecimal.ONE).doubleValue();
    }

    private static PrevisionsBaseDto avecLignes(PrevisionsBaseDto e, List<LignePrevisionDto> lignes) {
        return new PrevisionsBaseDto(e.nombreArticles(), e.valorisationStock(), e.pourcentageConcretisation(),
                e.debutConcretisation(), e.finConcretisation(), e.debutFinalisation(), e.finFinalisation(),
                e.ecartTypeConcretisation(), e.ecartTypeFinalisation(), lignes);
    }
}
