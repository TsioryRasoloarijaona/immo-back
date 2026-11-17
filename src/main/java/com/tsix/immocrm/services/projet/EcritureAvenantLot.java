package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.projet.CalculAvenant;
import com.tsix.immocrm.domain.projet.CalculAvenant.Retenue;
import com.tsix.immocrm.domain.projet.MontantsLot;
import com.tsix.immocrm.dto.projet.AvenantDetailDto;
import com.tsix.immocrm.dto.projet.AvenantDetailDto.LigneAvenantDto;
import com.tsix.immocrm.dto.projet.AvenantSaisieDto;
import com.tsix.immocrm.dto.projet.AvenantSaisieDto.LigneAvenantSaisieDto;
import com.tsix.immocrm.dto.projet.RetenueAvenantDto;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.prj.PrjClause;
import com.tsix.immocrm.entities.prj.PrjClauseLine;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.services.droits.DroitsOmran;
import com.tsix.immocrm.services.projet.GrillesLot.Grille;
import com.tsix.immocrm.services.sequence.GenerateurCodeSequence;
import com.tsix.immocrm.utils.TierNomFormatter;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Fiche avenant du lot (FEATURE-lot-avenants-avances.md §4.1, R1-R7) ; ordre des refus : 404, 403, 400, 409. */
@Service
public class EcritureAvenantLot {

    static final String CREER = "Yes.Prj.Front.Market.ClauseList.toolNew";
    static final String SUPPRIMER = "Yes.Prj.Front.Market.ClauseList.toolDelete";
    static final String FACTURE_SUPPRESSION = "Vous ne pouvez pas supprimer un avenant avec une facture associée.";
    static final String FACTURE_MODIFICATION = "Avenant facturé : lignes et retenues non modifiables";
    private static final String SEQUENCE = "Avenant";
    private static final String ENREGISTRE = "Avenant enregistré";
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String FACTURES = """
            SELECT COUNT(*), STRING_AGG(f.Code, ', ') FROM BuyDocument f WHERE f.PrjClauseId = :c AND f.DocType = 'INVOICE'""";

    private final LotsProjetService lots;
    private final DroitsOmran droits;
    private final GrillesLot grilles;
    private final GenerateurCodeSequence sequences;
    private final PrevisionsPaiementLot previsions;
    private final EntityManager em;

    EcritureAvenantLot(LotsProjetService lots, DroitsOmran droits, GrillesLot grilles, GenerateurCodeSequence sequences,
                       PrevisionsPaiementLot previsions, EntityManager em) {
        this.lots = lots;
        this.droits = droits;
        this.grilles = grilles;
        this.sequences = sequences;
        this.previsions = previsions;
        this.em = em;
    }

    @Transactional(readOnly = true)
    public AvenantDetailDto lire(UUID lot, UUID avenant) {
        lots.visible(lot);
        return detail(avenantDuLot(lot, avenant));
    }

    @Transactional
    public AvenantDetailDto creer(UUID lotId, AvenantSaisieDto corps) {
        PrjMarket lot = lots.visible(lotId);
        droits.exiger(CREER);
        refuserParMontant(retenues(corps), null);
        valider(lotId, corps);
        PrjClause c = new PrjClause();
        c.setId(UUID.randomUUID());
        Grille<LigneAvenantSaisieDto, PrjClauseLine> g = lignes(c.getId());
        Map<UUID, PrjClauseLine> enBase = new HashMap<>();
        grilles.refuserIds(g, corps.lignes(), enBase, "lignes[].id");
        GrillesLot.Ecriture e = grilles.ecriture();
        c.setPrjMarketId(lotId);
        c.setCfgTierId(lot.getCfgSupplier() == null ? null : lot.getCfgSupplier().getId());
        c.setSorting(Math.toIntExact(em.createQuery("select count(c) from PrjClause c where c.prjMarketId = :lot", Long.class)
                .setParameter("lot", lotId).getSingleResult()) + 1);
        c.setCode(codeLibre());
        entete(c, corps);
        contenu(c, corps);
        e.cree(c);
        em.persist(c);
        grilles.enregistrer(g, corps.lignes(), enBase, e);
        apres(lot);
        return detail(c);
    }

    @Transactional
    public AvenantDetailDto modifier(UUID lotId, UUID avenant, AvenantSaisieDto corps) {
        PrjMarket lot = lots.visible(lotId);
        PrjClause c = avenantDuLot(lotId, avenant);
        List<Retenue> enBaseRetenues = retenuesEnBase(c);
        refuserParMontant(retenues(corps), enBaseRetenues);
        valider(lotId, corps);
        Grille<LigneAvenantSaisieDto, PrjClauseLine> g = lignes(avenant);
        Map<UUID, PrjClauseLine> enBase = grilles.enBase(g, avenant);
        grilles.refuserIds(g, corps.lignes(), enBase, "lignes[].id");
        boolean facture = factures(avenant).facture();
        if (facture && (lignesChangees(corps, enBase, avenant) || retenuesChangees(retenues(corps), enBaseRetenues))) {
            throw new ConflictException(FACTURE_MODIFICATION);
        }
        GrillesLot.Ecriture e = grilles.ecriture();
        entete(c, corps);
        if (!facture) {
            contenu(c, corps);
            grilles.enregistrer(g, corps.lignes(), enBase, e);
        }
        e.modifie(c);
        apres(lot);
        return detail(c);
    }

    @Transactional
    public void supprimer(UUID lotId, UUID avenant) {
        PrjMarket lot = lots.visible(lotId);
        PrjClause c = avenantDuLot(lotId, avenant);
        droits.exiger(SUPPRIMER);
        if (factures(avenant).facture()) {
            throw new ConflictException(FACTURE_SUPPRESSION);
        }
        grilles.enBase(lignes(avenant), avenant).values().forEach(em::remove);
        em.flush();
        em.remove(c);
        apres(lot);
    }

    private void apres(PrjMarket lot) {
        grilles.tracer(lot, ENREGISTRE);
        previsions.recalculer(lot.getId());
    }

    private PrjClause avenantDuLot(UUID lot, UUID avenant) {
        PrjClause c = em.find(PrjClause.class, avenant);
        if (c == null || !lot.equals(c.getPrjMarketId())) {
            throw new ResourceNotFoundException("Avenant introuvable");
        }
        return c;
    }

    private void valider(UUID lot, AvenantSaisieDto corps) {
        ValidationAvenant.valider(corps, ids -> existants("select c.id from StlCategory c where c.id in :ids", ids, null),
                ids -> existants("select l.id from PrjMarketLine l where l.id in :ids and l.prjMarketId = :lot"
                        + " and (l.isStock is null or l.isStock = false)", ids, lot));
    }

    private Set<UUID> existants(String requete, Set<UUID> ids, UUID lot) {
        if (ids.isEmpty()) {
            return Set.of();
        }
        var q = em.createQuery(requete, UUID.class).setParameter("ids", ids);
        if (lot != null) {
            q.setParameter("lot", lot);
        }
        return new HashSet<>(q.getResultList());
    }

    /** R4 : passer une retenue au montant, ou en changer le montant, exige le droit ; renvoyer l'état en base est permis. */
    private void refuserParMontant(List<Retenue> envoyees, List<Retenue> enBase) {
        boolean change = IntStream.range(0, envoyees.size()).anyMatch(i -> envoyees.get(i).parMontant()
                && (enBase == null || !enBase.get(i).parMontant() || !egal(envoyees.get(i).montant(), enBase.get(i).montant())));
        if (change) {
            droits.exiger(VerrouDecompte.PAR_MONTANT);
        }
    }

    private static boolean retenuesChangees(List<Retenue> envoyees, List<Retenue> enBase) {
        return IntStream.range(0, envoyees.size()).anyMatch(i -> {
            Retenue s = envoyees.get(i);
            Retenue b = enBase.get(i);
            return s.parMontant() != b.parMontant() || !egal(s.parMontant() ? s.montant() : s.ratio(),
                    s.parMontant() ? b.montant() : b.ratio());
        });
    }

    private static boolean lignesChangees(AvenantSaisieDto corps, Map<UUID, PrjClauseLine> enBase, UUID avenant) {
        return corps.lignes().size() != enBase.size() || corps.lignes().stream().anyMatch(s -> {
            PrjClauseLine b = s.id() == null ? null : enBase.get(s.id());
            if (b == null) {
                return true;
            }
            PrjClauseLine envoyee = new PrjClauseLine();
            remplir(envoyee, s, avenant);
            return !etat(envoyee).equals(etat(b));
        });
    }

    private String codeLibre() {
        String code = sequences.suivantSelonLigne(SEQUENCE);
        while (em.createQuery("select count(c) from PrjClause c where c.code = :c", Long.class).setParameter("c", code)
                .getSingleResult() > 0) {
            code = sequences.suivantSelonLigne(SEQUENCE);
        }
        return code;
    }

    /** Champs modifiables même quand l'avenant est facturé. */
    private static void entete(PrjClause c, AvenantSaisieDto corps) {
        c.setDocDate(corps.date().atStartOfDay());
        c.setDocRef(corps.referenceInterne());
        c.setNotes(corps.notes());
    }

    /** Description, montants et retenues : figés une fois l'avenant facturé (R5). */
    private static void contenu(PrjClause c, AvenantSaisieDto corps) {
        c.setDescription("Avenant N° %s du %s".formatted(c.getCode(), corps.date().format(JOUR)));
        CalculAvenant.Totaux t = CalculAvenant.calculer(corps.lignes().stream().map(EcritureAvenantLot::calcul).toList(),
                retenues(corps));
        c.setAmountHT(t.ht());
        c.setAmountVat(t.tva());
        c.setAmountTTC(t.ttc());
        c.setAmountToPay(t.net());
        List<Retenue> r = t.retenues();
        c.setWarrantyBasedOnAmount(r.get(0).parMontant());
        c.setWarrantyRatio(r.get(0).ratio());
        c.setWarrantyAmount(r.get(0).montant());
        c.setProrataBasedOnAmount(r.get(1).parMontant());
        c.setProportionRatio(r.get(1).ratio());
        c.setProportionAmount(r.get(1).montant());
        c.setDecBasedOnAmount(r.get(2).parMontant());
        c.setInsuranceDECRatio(r.get(2).ratio());
        c.setInsuranceDECAmount(r.get(2).montant());
        c.setTrcBasedOnAmount(r.get(3).parMontant());
        c.setInsuranceTRCRatio(r.get(3).ratio());
        c.setInsuranceTRCAmount(r.get(3).montant());
    }

    /** Garantie, prorata, DEC, TRC : l'ordre de {@link #retenuesEnBase}. */
    private static List<Retenue> retenues(AvenantSaisieDto corps) {
        return Arrays.asList(retenue(corps.garantie()), retenue(corps.prorata()), retenue(corps.assuranceDEC()),
                retenue(corps.assuranceTRC()));
    }

    private static Retenue retenue(RetenueAvenantDto r) {
        return r == null ? new Retenue(false, BigDecimal.ZERO, null) : new Retenue(r.parMontant(), ratio(r.taux()), r.montant());
    }

    private static List<Retenue> retenuesEnBase(PrjClause c) {
        return List.of(new Retenue(vrai(c.getWarrantyBasedOnAmount()), c.getWarrantyRatio(), c.getWarrantyAmount()),
                new Retenue(vrai(c.getProrataBasedOnAmount()), c.getProportionRatio(), c.getProportionAmount()),
                new Retenue(vrai(c.getDecBasedOnAmount()), c.getInsuranceDECRatio(), c.getInsuranceDECAmount()),
                new Retenue(vrai(c.getTrcBasedOnAmount()), c.getInsuranceTRCRatio(), c.getInsuranceTRCAmount()));
    }

    private static Grille<LigneAvenantSaisieDto, PrjClauseLine> lignes(UUID avenant) {
        return new Grille<>(PrjClauseLine.class, "prjClauseId", LigneAvenantSaisieDto::id, PrjClauseLine::getId, id -> {
            PrjClauseLine l = new PrjClauseLine();
            l.setId(id);
            return l;
        }, (l, s) -> remplir(l, s, avenant), EcritureAvenantLot::etat);
    }

    private static CalculAvenant.Ligne calcul(LigneAvenantSaisieDto s) {
        return new CalculAvenant.Ligne(s.quantite().setScale(6, RoundingMode.HALF_UP), s.prixUnitaireHT(), ratio(s.tauxFODEC()),
                ratio(s.tauxTVA()));
    }

    private static void remplir(PrjClauseLine l, LigneAvenantSaisieDto s, UUID avenant) {
        CalculAvenant.Ligne c = calcul(s);
        l.setPrjClauseId(avenant);
        l.setSorting(s.ordre());
        l.setCode(s.code());
        l.setDescription(s.designation());
        l.setUnity(s.unite());
        l.setQuantity(c.quantite());
        l.setUnitPriceHT(c.prixHT());
        l.setVatRatio(c.tvaRatio());
        l.setFodecRatio(c.fodecRatio());
        l.setUnitPriceTTC(c.prixTTC());
        l.setStlCategoryId(s.categorieId());
        l.setPrjMarketLineId(s.ligneBordereauId());
        l.setNotes(s.notes());
    }

    private static List<Object> etat(PrjClauseLine l) {
        return Arrays.asList(l.getSorting(), l.getCode(), l.getDescription(), l.getUnity(), n(l.getQuantity()),
                n(l.getUnitPriceHT()), n(l.getVatRatio()), n(l.getFodecRatio()), l.getStlCategoryId(), l.getPrjMarketLineId(),
                l.getNotes());
    }

    // ------------------------------------------------------------------ lecture

    private record Factures(boolean facture, String codes) {
    }

    private Factures factures(UUID avenant) {
        Object[] r = (Object[]) em.createNativeQuery(FACTURES).setParameter("c", avenant).getSingleResult();
        return new Factures(((Number) r[0]).intValue() > 0, (String) r[1]);
    }

    private AvenantDetailDto detail(PrjClause c) {
        List<PrjClauseLine> lignes = em.createQuery("select l from PrjClauseLine l where l.prjClauseId = :a"
                + " order by l.sorting asc, l.code asc", PrjClauseLine.class).setParameter("a", c.getId()).getResultList();
        Set<UUID> idsCategories = lignes.stream().map(PrjClauseLine::getStlCategoryId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, String> categories = idsCategories.isEmpty() ? Map.of() : em.createQuery(
                        "select c.id, c.description from StlCategory c where c.id in :ids", Object[].class)
                .setParameter("ids", idsCategories).getResultList().stream()
                .collect(Collectors.toMap(r -> (UUID) r[0], r -> Objects.toString(r[1], "")));
        Factures f = factures(c.getId());
        Set<String> d = droits.parmi(SUPPRIMER, VerrouDecompte.PAR_MONTANT);
        CfgTier fournisseur = c.getCfgTierId() == null ? null : em.find(CfgTier.class, c.getCfgTierId());
        List<Retenue> r = retenuesEnBase(c);
        return new AvenantDetailDto(c.getId(), c.getPrjMarketId(), c.getSorting(), c.getCode(), c.getDescription(),
                c.getDocDate() == null ? null : c.getDocDate().toLocalDate(), c.getDocRef(), c.getNotes(),
                TierNomFormatter.nom(fournisseur), dto(r.get(0)), dto(r.get(1)), dto(r.get(2)),
                dto(r.get(3)), c.getAmountHT(), c.getAmountVat(), c.getAmountTTC(), c.getAmountToPay(), f.facture(), f.codes(),
                !f.facture(), d.contains(SUPPRIMER) && !f.facture(), d.contains(VerrouDecompte.PAR_MONTANT),
                lignes.stream().map(l -> ligne(l, categories.get(l.getStlCategoryId()))).toList());
    }

    private static RetenueAvenantDto dto(Retenue r) {
        return new RetenueAvenantDto(r.parMontant(), MontantsLot.pourcentage(r.ratio()), r.montant());
    }

    private static LigneAvenantDto ligne(PrjClauseLine l, String categorie) {
        CalculAvenant.Ligne c = new CalculAvenant.Ligne(l.getQuantity(), l.getUnitPriceHT(), l.getFodecRatio(), l.getVatRatio());
        return new LigneAvenantDto(l.getId(), l.getSorting(), l.getCode(), l.getDescription(), l.getUnity(), l.getQuantity(),
                l.getUnitPriceHT(), MontantsLot.pourcentage(l.getVatRatio()), MontantsLot.pourcentage(l.getFodecRatio()),
                c.prixTTC(), c.totalHT(), c.totalTTC(), l.getStlCategoryId(), categorie, l.getPrjMarketLineId(), l.getNotes());
    }

    // ------------------------------------------------------------------ outils

    private static BigDecimal ratio(BigDecimal pourcentage) {
        return Objects.requireNonNullElse(pourcentage, BigDecimal.ZERO).movePointLeft(2).setScale(6, RoundingMode.HALF_UP);
    }

    private static boolean vrai(Boolean b) {
        return Boolean.TRUE.equals(b);
    }

    private static boolean egal(BigDecimal a, BigDecimal b) {
        return Objects.requireNonNullElse(a, BigDecimal.ZERO).compareTo(Objects.requireNonNullElse(b, BigDecimal.ZERO)) == 0;
    }

    // Échelles différentes en base et dans le corps (0.200000 / 0.20) : comparer la valeur, pas l'écriture.
    private static BigDecimal n(BigDecimal b) {
        return b == null ? null : b.stripTrailingZeros();
    }
}
