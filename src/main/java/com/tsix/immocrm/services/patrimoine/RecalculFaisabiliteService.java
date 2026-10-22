package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.faisabilite.BilanFaisabilite;
import com.tsix.immocrm.domain.faisabilite.BilanFaisabilite.Noeud;
import com.tsix.immocrm.domain.faisabilite.EtatFaisabilite;
import com.tsix.immocrm.domain.faisabilite.EtatFaisabilite.Categorie;
import com.tsix.immocrm.domain.faisabilite.EtatFaisabilite.Credit;
import com.tsix.immocrm.domain.faisabilite.EtatFaisabilite.Relation;
import com.tsix.immocrm.domain.faisabilite.EtatFaisabilite.Saisie;
import com.tsix.immocrm.domain.faisabilite.EtatFaisabilite.Tranche;
import com.tsix.immocrm.domain.faisabilite.EtatFaisabilite.Unite;
import com.tsix.immocrm.domain.faisabilite.FinancementFaisabilite.Echeance;
import com.tsix.immocrm.domain.faisabilite.TresorerieFaisabilite;
import com.tsix.immocrm.entities.stk.FSFunding;
import com.tsix.immocrm.entities.stk.FSPrevision;
import com.tsix.immocrm.entities.stk.FSReimbursement;
import com.tsix.immocrm.entities.stk.StkFSBaseUnit;
import com.tsix.immocrm.entities.stk.StkFSBaseUnitStkFeasibilityStudy;
import com.tsix.immocrm.entities.stk.StkFSPrevisionLine;
import com.tsix.immocrm.entities.stk.StkFSPrevisionStkTranche;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudy;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudyCfgTrancheStkBaseUnit;
import com.tsix.immocrm.entities.stl.StlCategory;
import com.tsix.immocrm.entities.stl.StlSettlementRelation;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tsix.immocrm.domain.faisabilite.BilanFaisabilite.r;

/**
 * Recalcul unique d'une étude (FEATURE-patrimoine-faisabilite-workflow.md §3) : état relu en base, calcul complet en
 * mémoire (tout refus 400 avant la moindre écriture), puis FSPrevision mise à jour en place, lignes par tranche et de
 * trésorerie supprimées puis réinsérées, colonnes du bilan réécrites. Rejouable : deux recalculs = mêmes lignes.
 */
@Service
@Transactional(readOnly = true)
public class RecalculFaisabiliteService {

    private static final String ETUDE = "etude";

    /** Bilan et lignes de trésorerie calculés pour un état. */
    public record Calcul(EtatFaisabilite etat, BilanFaisabilite.Resultat bilan, List<TresorerieFaisabilite.Ligne> lignes) {
    }

    private final EntityManager em;
    private final GrillesEtude grilles;
    private final TracePatrimoine trace;

    RecalculFaisabiliteService(EntityManager em, GrillesEtude grilles, TracePatrimoine trace) {
        this.em = em;
        this.grilles = grilles;
        this.trace = trace;
    }

    public static Calcul calculer(EtatFaisabilite etat) {
        BilanFaisabilite.Resultat bilan = BilanFaisabilite.calculer(etat);
        return new Calcul(etat, bilan, TresorerieFaisabilite.lignes(etat, bilan));
    }

    @Transactional
    public void recalculer(StkFeasibilityStudy e) {
        ecrire(e, calculer(lire(e, null)));
    }

    /** Écrit un calcul déjà fait (saisie des prévisions) dans la transaction de l'appelant. */
    @Transactional
    public void enregistrerCalcul(StkFeasibilityStudy e, Calcul calcul) {
        ecrire(e, calcul);
    }

    // ------------------------------------------------------------------ lecture de l'état

    /** {@code saisies} null = lignes FSPrevision enregistrées (hors lignes de relation, imposées). */
    EtatFaisabilite lire(StkFeasibilityStudy e, List<Saisie> saisies) {
        UUID id = e.getId();
        Map<UUID, Relation> relations = relations();
        List<StkFSBaseUnitStkFeasibilityStudy> lignes = grilles.lignes(id);
        Map<UUID, Unite> unites = lignes.stream().collect(Collectors.toMap(StkFSBaseUnitStkFeasibilityStudy::getId,
                l -> new Unite(l.getId(), l.getStkFSBaseUnit().getCode(), l.getQuantity(),
                        Boolean.TRUE.equals(l.getStkFSBaseUnit().getIsArea()) && l.getStkFSBaseUnit().getStkItemCategory() != null),
                (a, b) -> a, LinkedHashMap::new));
        // NumberOfItemForSale (StkFeasibilityStudy.cs:1960) : catégorie d'article hors sous-article, non surface.
        int articles = lignes.stream().filter(l -> article(l.getStkFSBaseUnit())
                        && !Boolean.TRUE.equals(l.getStkFSBaseUnit().getStkItemCategory().getForUnderItem()))
                .map(l -> BilanFaisabilite.z(l.getQuantity())).reduce(BigDecimal.ZERO, BigDecimal::add).intValue();
        List<Credit> credits = financements(id).entrySet().stream().map(f -> new Credit(f.getKey().getPaymentDate(),
                f.getKey().getAmount(), f.getValue().stream().map(x -> new Echeance(x.getPaymentDate(), x.getAmountBase(),
                        x.getInterestRate(), BilanFaisabilite.z(x.getAmountInterest()), x.getAmountToPay())).toList())).toList();
        return new EtatFaisabilite(FicheEtude.parametres(e, articles), categories(), relations, unites, tranches(id),
                credits, saisies != null ? saisies : saisiesEnregistrees(id, relations));
    }

    /** Unité comptée en articles : catégorie d'article, non surface ({@code NumberOfItemForSale}, ventilation). */
    private static boolean article(StkFSBaseUnit u) {
        return u.getStkItemCategory() != null && !Boolean.TRUE.equals(u.getIsArea());
    }

    private Map<UUID, Categorie> categories() {
        return em.createQuery("SELECT c FROM StlCategory c", StlCategory.class).getResultList().stream()
                .collect(Collectors.toMap(StlCategory::getId, c -> new Categorie(c.getId(), c.getParentId(), c.getCode(),
                        c.getDescription(), Boolean.TRUE.equals(c.getIsCredit()), Objects.requireNonNullElse(c.getSorting(), 0),
                        Boolean.TRUE.equals(c.getIsWithoutPrevision()))));
    }

    /** Catégorie liée → relation ; une relation sans catégorie n'ajoute rien (cas de la production). */
    Map<UUID, Relation> relations() {
        Map<UUID, Relation> parPkey = Arrays.stream(Relation.values()).collect(Collectors.toMap(x -> x.pkey, x -> x));
        Map<UUID, Relation> relations = new HashMap<>();
        em.createQuery("SELECT r FROM StlSettlementRelation r WHERE r.pKey IN :ids AND r.stlCategoryId IS NOT NULL",
                        StlSettlementRelation.class).setParameter("ids", parPkey.keySet()).getResultList()
                .forEach(r -> relations.put(r.getStlCategoryId(), parPkey.get(r.getPKey())));
        return relations;
    }

    private List<Tranche> tranches(UUID id) {
        Map<UUID, BigDecimal> articles = grilles.ventilations(id).stream()
                .filter(v -> v.getLigneUnite() != null && article(v.getLigneUnite().getStkFSBaseUnit()))
                .collect(Collectors.toMap(v -> v.getTranche().getId(),
                        StkFeasibilityStudyCfgTrancheStkBaseUnit::getQuantity, BigDecimal::add));
        return grilles.tranches(id).stream().map(t -> new Tranche(t.getId(), t.getCode(), t.getWeight(), t.getWorkStartDate(),
                t.getWorkEndDate(), t.getMarketingStartDate(), t.getEndDateConcretisation(), t.getStartDateFinalisation(),
                t.getMarketingEndDate(), articles.getOrDefault(t.getId(), BigDecimal.ZERO).intValue())).toList();
    }

    private List<Saisie> saisiesEnregistrees(UUID id, Map<UUID, Relation> relations) {
        return previsions(id).stream().filter(p -> !relations.containsKey(p.getStlCategoryId()))
                .map(p -> new Saisie(p.getStlCategoryId(), p.getUnityId(), p.getParentId(), p.getRatio(), p.getAmountHt(),
                        null, p.getVatRatio(), p.getIsForProject(), p.getApplyOnDate())).toList();
    }

    /** Lignes FSPrevision à catégorie ; une ligne sans catégorie (saisie hors CRM) est laissée telle quelle. */
    List<FSPrevision> previsions(UUID id) {
        return em.createQuery("SELECT p FROM FSPrevision p WHERE p.feasibilityStudyId = :etude AND p.stlCategoryId IS NOT NULL",
                FSPrevision.class).setParameter(ETUDE, id).getResultList();
    }

    /** Crédits par date, chacun avec ses remboursements par date. */
    Map<FSFunding, List<FSReimbursement>> financements(UUID id) {
        List<FSFunding> credits = em.createQuery("SELECT f FROM FSFunding f WHERE f.feasibilityStudyId = :etude "
                + "ORDER BY f.paymentDate, f.id", FSFunding.class).setParameter(ETUDE, id).getResultList();
        Map<UUID, List<FSReimbursement>> parCredit = credits.isEmpty() ? Map.of()
                : em.createQuery("SELECT r FROM FSReimbursement r WHERE r.fsFundingId IN :credits ORDER BY r.paymentDate, r.id",
                        FSReimbursement.class).setParameter("credits", credits.stream().map(FSFunding::getId).toList())
                .getResultList().stream().collect(Collectors.groupingBy(FSReimbursement::getFsFundingId));
        Map<FSFunding, List<FSReimbursement>> resultat = new LinkedHashMap<>();
        credits.forEach(c -> resultat.put(c, parCredit.getOrDefault(c.getId(), List.of())));
        return resultat;
    }

    // ------------------------------------------------------------------ écriture

    private void ecrire(StkFeasibilityStudy e, Calcul calcul) {
        TracePatrimoine.Trace t = trace.maintenant();
        UUID id = e.getId();
        grilles.supprimerDerivees(id);
        Map<UUID, FSPrevision> existantes = previsions(id).stream()
                .collect(Collectors.toMap(FSPrevision::getStlCategoryId, Function.identity(), (a, b) -> a));
        List<Noeud> noeuds = calcul.bilan().noeuds();
        noeuds.forEach(n -> ecrirePrevision(id, n, existantes.remove(n.categorie().id()), t));
        existantes.values().forEach(em::remove);
        noeuds.forEach(n -> n.ventilation().forEach(part -> em.persist(parTranche(id, n, part, t))));
        calcul.lignes().forEach(l -> em.persist(tresorerie(id, l)));
        e.setNumberOfItemForSale(calcul.etat().parametres().nombreArticles());
        // Sans aucune ligne, le bilan stocké (OMRAN) est gardé : il ne vient d'aucune prévision tenue ici.
        if (!noeuds.isEmpty()) {
            FicheEtude.ecrireBilan(e, calcul.bilan().bilan());
        }
        em.flush();
    }

    private void ecrirePrevision(UUID etude, Noeud n, FSPrevision existante, TracePatrimoine.Trace t) {
        FSPrevision p = existante != null ? existante : new FSPrevision();
        Saisie s = n.saisie();
        boolean ecrite = LignesTracees.appliquer(p, RecalculFaisabiliteService::valeurs, x -> {
            x.setFeasibilityStudyId(etude);
            x.setStlCategoryId(n.categorie().id());
            x.setAmount(r(n.propre()));
            x.setAmountHt(r(n.htPropre()));
            x.setRatio(r(n.ratio()));
            x.setVatRatio(r(n.tva()));
            x.setUnityId(s == null ? null : s.uniteId());
            x.setParentId(s == null ? null : s.referenceId());
            x.setApplyOnDate(n.dateApplication());
            x.setUndivided(n.dateApplication() != null);
            x.setIsForProject(n.pourProjet());
            x.setGlobalRatio(Objects.requireNonNullElse(x.getGlobalRatio(), r(BigDecimal.ZERO)));
            x.setNote(Objects.requireNonNullElse(x.getNote(), ""));
        }, t);
        if (ecrite && existante == null) {
            em.persist(p);
        }
    }

    private static List<Object> valeurs(FSPrevision p) {
        return Arrays.asList(p.getAmount(), p.getAmountHt(), p.getRatio(), p.getVatRatio(), p.getUnityId(), p.getParentId(),
                p.getApplyOnDate(), p.getUndivided(), p.getIsForProject(), p.getGlobalRatio(), p.getNote());
    }

    /** Écart 7c : {@code Amount} = AmountHT × (1 + TVA) (OMRAN y stockait une valeur incohérente). */
    private static StkFSPrevisionStkTranche parTranche(UUID etude, Noeud n, BilanFaisabilite.Part part,
                                                      TracePatrimoine.Trace t) {
        StkFSPrevisionStkTranche l = new StkFSPrevisionStkTranche();
        l.setId(UUID.randomUUID());
        l.setFeasibilityStudyId(etude);
        l.setStlCategoryId(n.categorie().id());
        l.setStkTrancheId(part.trancheId());
        l.setAmountHt(r(part.ht()));
        l.setAmount(r(part.ht().multiply(BigDecimal.ONE.add(n.tva()))));
        l.setVatRatio(r(n.tva()));
        l.setRatio(r(n.ratio()));
        l.setUndivided(n.dateApplication() != null);
        l.setUserCreate(t.auteur());
        l.setUserCreateDateTime(t.le());
        l.setUserUpdate(t.auteur());
        l.setUserUpdateDateTime(t.le());
        return l;
    }

    private static StkFSPrevisionLine tresorerie(UUID etude, TresorerieFaisabilite.Ligne l) {
        StkFSPrevisionLine ligne = new StkFSPrevisionLine();
        ligne.setId(UUID.randomUUID());
        ligne.setStkFeasibilityStudyId(etude);
        ligne.setStkFeasibilityStudyCfgTrancheId(l.trancheId());
        ligne.setDocDate(l.date());
        ligne.setDateMonth(l.date().getMonthValue());
        ligne.setDateYear(l.date().getYear());
        ligne.setPrevisionType(l.type().code);
        ligne.setIsCredit(l.type().credit);
        ligne.setAmount(l.montant());
        ligne.setAmountHt(r(BigDecimal.ZERO));
        ligne.setNotes("");
        return ligne;
    }
}
