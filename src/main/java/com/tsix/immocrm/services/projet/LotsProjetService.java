package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.achat.SoldeDocumentAchat;
import com.tsix.immocrm.domain.Colonne;
import com.tsix.immocrm.domain.projet.CoutPrevisionnelLot;
import com.tsix.immocrm.domain.projet.GenreLot;
import com.tsix.immocrm.domain.projet.MontantsLot;
import com.tsix.immocrm.domain.projet.MontantsLot.Trio;
import com.tsix.immocrm.dto.projet.BordereauDto;
import com.tsix.immocrm.dto.projet.DecompteListItemDto;
import com.tsix.immocrm.dto.projet.ElementReferentielProjetDto;
import com.tsix.immocrm.dto.projet.EntiteFiltreDto;
import com.tsix.immocrm.dto.projet.TotauxLotsDto;
import com.tsix.immocrm.dto.projet.TransitionLotDto;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.entities.str.StrWorkFlow;
import com.tsix.immocrm.services.droits.DroitsOmran;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.web.export.ExportTableur;
import com.tsix.immocrm.web.export.FeuilleExport;
import com.tsix.immocrm.web.export.LignesExport;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.tsix.immocrm.dto.projet.LigneBordereauDto;
import com.tsix.immocrm.dto.projet.LotDetailDto;
import com.tsix.immocrm.dto.projet.LotListItemDto;
import com.tsix.immocrm.dto.projet.ProjetFiltreDto;
import com.tsix.immocrm.dto.projet.ReferentielsProjetDto;
import com.tsix.immocrm.dto.projet.StatutLotProjetDto;
import com.tsix.immocrm.entities.CodeLibelleOmran;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.entities.prj.PrjMarketLine;
import com.tsix.immocrm.entities.prj.PrjProject;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.prj.PrjMarketRepository;
import com.tsix.immocrm.services.dossier.workflow.IntervenantCourant;
import com.tsix.immocrm.services.perimetre.PerimetreProjetService;
import com.tsix.immocrm.utils.TierNomFormatter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.tsix.immocrm.services.projet.DecomptesProjetService.jour;

@Service
@Transactional(readOnly = true)
public class LotsProjetService {

    static final String LOT_INTROUVABLE = "Lot introuvable";
    private static final String ID = "id";
    private static final String DESCRIPTION = "description";
    private static final String PROJETS = "projets";
    private static final String STATUTS = "statuts";
    private static final String COUT_HT = "coutPrevisionnelHT";
    private static final String COUT_TTC = "coutPrevisionnelTTC";
    private static final String COUT_NET = "coutPrevisionnelNetAPayer";
    private static final String COUT_REGLE = "coutPrevisionnelRegle";
    private static final String COUT_RESTE = "coutPrevisionnelReste";
    // Clause de statut de Visibilite.lier, sur l'alias m (lot) ; FK lue sans jointure : un lot sans statut reste compté.
    private static final String STATUT_M = " and (:tousStatuts = true or m.strStatus.id in :statuts)";

    // Lignes de stock exclues ; un lot sans ligne retenue n'est pas dans le résultat → repli sur le forfait.
    private static final String SOMMES = "select l.prjMarketId, sum(l.quantity * l.unitPriceHT),"
            + " sum(l.quantity * l.unitPriceHT * l.vatRatio), sum(l.quantity * l.unitPriceTTC), count(l)";
    private static final String OU_LOTS = " l where l.prjMarketId in :lots and (l.isStock is null or l.isStock = false)"
            + " group by l.prjMarketId";
    private static final String SOMMES_BORDEREAU = SOMMES + " from PrjMarketLine" + OU_LOTS;
    private static final String SOMMES_ESTIMATION = SOMMES + " from PrjMarketLineEstimation" + OU_LOTS;

    private static final String CUMULS = "select w.prjMarket.id, count(w), sum(w.amountHT), sum(w.amountTTC),"
            + " sum(w.extraAmount), sum(w.warrantyAmount), sum(w.amountToPay)"
            + " from PrjWorkingOut w where w.prjMarket.id in :lots group by w.prjMarket.id";

    private static final String STATUTS_LOT = """
            SELECT s.PKey, s.Code, s.Description, s.Sorting, t.Code, e.Code, s.ColorName, e.PKey
            FROM StrStatus s
            JOIN StrEntity e ON e.PKey = s.StrConfigEntityId
            LEFT JOIN StrStatusType t ON t.PKey = s.StrStatusTypeId
            WHERE e.Code LIKE 'PrjMarket%'
            ORDER BY e.Code ASC, s.Sorting ASC""";

    /**
     * Lots visibles (FEATURE-visibilite-omran.md R1) : projet habilité ET, pour un compte restreint, statut visible ;
     * les mêmes statuts filtrent la liste des projets (R7). {@code statuts} vide = aucune restriction de statut.
     */
    private record Visibilite(Set<UUID> projets, Optional<Set<UUID>> statuts) {

        boolean vide() {
            return projets.isEmpty() || statuts.map(Set::isEmpty).orElse(false);
        }

        /** Lie {@code :projets}, {@code :tousStatuts} et {@code :statuts} (jamais vide : SQL Server refuse IN ()). */
        <T> TypedQuery<T> lier(TypedQuery<T> requete) {
            return requete.setParameter(PROJETS, projets).setParameter("tousStatuts", statuts.isEmpty())
                    .setParameter(STATUTS, statuts.filter(s -> !s.isEmpty()).orElse(Set.of(new UUID(0, 0))));
        }

        boolean voit(PrjMarket lot) {
            return statuts.map(s -> lot.getStrStatus() != null && s.contains(lot.getStrStatus().getId())).orElse(true);
        }
    }

    /** Cumuls des décomptes d'un lot. */
    private record Cumuls(long nb, BigDecimal ht, BigDecimal ttc, BigDecimal extras, BigDecimal rg, BigDecimal net) {
        static final Cumuls AUCUN = new Cumuls(0, null, null, null, null, null);
    }

    private final PrjMarketRepository lots;
    private final DecomptesProjetService decomptes;
    private final FacturesDecomptes factures;
    private final PerimetreProjetService perimetre;
    private final IntervenantCourant intervenant;
    private final EntityManager em;
    private final MontantsListeLots montants;
    private final TransitionsLot transitionsLot;
    private final DroitsOmran droits;

    public LotsProjetService(PrjMarketRepository lots, DecomptesProjetService decomptes, FacturesDecomptes factures,
                             PerimetreProjetService perimetre, IntervenantCourant intervenant, EntityManager em,
                             MontantsListeLots montants, TransitionsLot transitionsLot, DroitsOmran droits) {
        this.lots = lots;
        this.decomptes = decomptes;
        this.factures = factures;
        this.perimetre = perimetre;
        this.intervenant = intervenant;
        this.em = em;
        this.montants = montants;
        this.transitionsLot = transitionsLot;
        this.droits = droits;
    }

    public ReferentielsProjetDto referentiels() {
        Visibilite v = new Visibilite(perimetre.projetsVisibles(), intervenant.statutsDeLotsEtProjets());
        List<ProjetFiltreDto> projets = projets(v);
        return new ReferentielsProjetDto(projets, elements("select t from PrjMarketType t order by t.code"),
                statutsLot(), statutsFacture(), categories(v), fournisseurs(v), entites(projets, em));
    }

    public Page<LotListItemDto> lots(FiltresLots filtres, Pageable pageable) {
        Visibilite v = new Visibilite(perimetre.restreindre(filtres.projetIds()), intervenant.statutsDeLotsEtProjets());
        if (v.vide()) {
            return Page.empty(pageable);
        }
        Page<PrjMarket> page = lots.findAll((root, q, cb) -> filtre(root, cb, filtres, v), pageable);
        return new PageImpl<>(lignes(page.getContent()), pageable, page.getTotalElements());
    }

    /** Σ des lignes de {@link #lots} sur toutes les pages : aucune seconde formule (R5). */
    public TotauxLotsDto totaux(FiltresLots filtres) {
        return totaliser(lots(filtres, Pageable.unpaged()).getContent());
    }

    /** Feuille d'export : lignes de la liste triées comme elle, ligne « Total » = {@link #totaux}. 403 sans le droit. */
    public FeuilleExport feuilleExport(FiltresLots filtres, Sort tri, List<? extends Colonne> colonnes) {
        droits.exiger(GenreLot.DROIT_EXPORTER);
        List<LotListItemDto> lignes = LignesExport.toutes(tri, page -> lots(filtres, page));
        TotauxLotsDto t = totaliser(lignes);
        return new FeuilleExport(ExportTableur.FEUILLE, colonnes, lignes, Map.of(COUT_HT, t.coutPrevisionnelHT(),
                COUT_TTC, t.coutPrevisionnelTTC(), COUT_NET, t.coutPrevisionnelNetAPayer(), COUT_REGLE,
                t.coutPrevisionnelRegle(), COUT_RESTE, t.coutPrevisionnelReste()));
    }

    /** Transitions exécutables par l'appelant (même calcul que l'exécution, R7). */
    public List<TransitionLotDto> transitions(UUID id) {
        PrjMarket m = visible(id);
        List<StrWorkFlow> permises = transitionsLot.permises(m);
        Map<UUID, StrStatus> cibles = transitionsLot.cibles(permises);
        return permises.stream().map(w -> {
            StrStatus cible = cibles.get(w.getStatusToId());
            return new TransitionLotDto(w.getId(), w.getDescription(), w.getStatusToId(),
                    Champs.de(cible, StrStatus::getDescription), Champs.de(cible, StrStatus::getColorName));
        }).toList();
    }

    /** Ligne de liste d'un lot déjà lu (après écriture). */
    LotListItemDto ligne(PrjMarket m) {
        return lignes(List.of(m)).get(0);
    }

    private List<LotListItemDto> lignes(List<PrjMarket> marches) {
        List<UUID> ids = marches.stream().map(PrjMarket::getId).toList();
        Map<UUID, Trio> bordereaux = sommes(SOMMES_BORDEREAU, ids);
        Map<UUID, Cumuls> cumuls = cumuls(ids);
        Map<UUID, SoldeDocumentAchat> soldes = factures.totauxParLot(ids);
        Map<UUID, CoutPrevisionnelLot> couts = montants.calculer(marches);
        return marches.stream().map(m -> ligne(m, marche(m, bordereaux), cumuls.getOrDefault(m.getId(), Cumuls.AUCUN),
                soldes.get(m.getId()), couts.get(m.getId()))).toList();
    }

    private static TotauxLotsDto totaliser(List<LotListItemDto> lignes) {
        return new TotauxLotsDto(lignes.size(), somme(lignes, LotListItemDto::coutPrevisionnelHT),
                somme(lignes, LotListItemDto::coutPrevisionnelTTC), somme(lignes, LotListItemDto::coutPrevisionnelNetAPayer),
                somme(lignes, LotListItemDto::coutPrevisionnelRegle), somme(lignes, LotListItemDto::coutPrevisionnelReste));
    }

    private static BigDecimal somme(List<LotListItemDto> lignes, Function<LotListItemDto, BigDecimal> champ) {
        return lignes.stream().map(champ).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public LotDetailDto lot(UUID id) {
        PrjMarket m = visible(id);
        List<UUID> ids = List.of(id);
        Trio marche = marche(m, sommes(SOMMES_BORDEREAU, ids));
        Trio estimation = MontantsLot.choisir(sommes(SOMMES_ESTIMATION, ids).get(id), m.getEstimatedPriceHTManual(),
                m.getEstimatedVatAmountManual(), m.getEstimatedPriceTTCManual());
        Cumuls c = cumuls(ids).getOrDefault(id, Cumuls.AUCUN);
        return new LotDetailDto(ligne(m),
                m.getPrjProject().getDescription(), m.getStrEntity() == null ? null : m.getStrEntity().getCode(),
                m.getStrEntity() == null ? null : m.getStrEntity().getDescription(),
                MontantsLot.arrondi(m.getEstimatedPriceHTManual()), MontantsLot.arrondi(m.getEstimatedVatAmountManual()),
                MontantsLot.arrondi(m.getEstimatedPriceTTCManual()), estimation.ht(), estimation.tva(), estimation.ttc(),
                estimation.depuisBordereau(), MontantsLot.applique(marche.ttc(), m.getWarrantyRatio()),
                MontantsLot.applique(marche.ttc(), m.getAdvanceRatio()), MontantsLot.pourcentage(m.getAdvanceRatio()),
                MontantsLot.pourcentage(m.getProportionRatio()), MontantsLot.pourcentage(m.getInsuranceDECRatio()),
                MontantsLot.pourcentage(m.getInsuranceTRCRatio()), MontantsLot.pourcentage(m.getDefaultVat()),
                m.getPaymentDelay(), m.getWarrantyInMonths(), Boolean.TRUE.equals(m.getBlokWarrantyRestitution()),
                Boolean.TRUE.equals(m.getWithMarketView()), Boolean.TRUE.equals(m.getIsClosed()), MontantsLot.arrondi(c.net()),
                jour(m.getStartDate()), m.getDelay(), jour(m.getEndDate()), jour(m.getLaunchOfTenderDate()),
                jour(m.getWithdrawalTenderDocumentsDate()), jour(m.getTenderMaturityDate()), jour(m.getBidOpeningDate()),
                jour(m.getTenderDate()), jour(m.getContractSignatureDate()), jour(m.getServiceOrderDate()),
                jour(m.getWorkLaunchDate()), m.getUserCreateDateTime(), m.getUserCreate(), m.getUserUpdateDateTime(),
                m.getUserUpdate());
    }

    public BordereauDto bordereau(UUID id) {
        PrjMarket m = visible(id);
        List<Object[]> lignes = em.createQuery("select l, g.description from PrjMarketLine l"
                        + " left join PrjMarketLineGrouping g on g.id = l.prjMarketLineGroupingId"
                        + " where l.prjMarketId = :lot and (l.isStock is null or l.isStock = false)"
                        + " order by l.sorting, l.code", Object[].class)
                .setParameter("lot", id)
                .getResultList();
        Trio totaux = sommes(SOMMES_BORDEREAU, List.of(id)).getOrDefault(id, new Trio(null, null, null, true));
        return new BordereauDto(m.getId(), lignes.stream().map(r -> ligneBordereau((PrjMarketLine) r[0], (String) r[1])).toList(),
                new BordereauDto.TotauxBordereauDto(MontantsLot.arrondi(totaux.ht()), MontantsLot.arrondi(totaux.tva()),
                        MontantsLot.arrondi(totaux.ttc()), lignes.size()));
    }

    public List<DecompteListItemDto> decomptesDuLot(UUID id) {
        return decomptes.duLot(visible(id).getId());
    }

    /** Seule lecture d'un lot par id (fiche, bordereau, création de décompte) : 404 hors projet habilité ou statut visible. */
    PrjMarket visible(UUID id) {
        Visibilite v = new Visibilite(perimetre.projetsVisibles(), intervenant.statutsDeLotsEtProjets());
        return (v.vide() ? Optional.<PrjMarket>empty() : lots.findByIdAndPrjProjectIdIn(id, v.projets()))
                .filter(v::voit)
                .orElseThrow(() -> new ResourceNotFoundException(LOT_INTROUVABLE));
    }

    /** Projet où l'appelant peut créer un lot : même règle que la liste (R3) ; 404 sinon. */
    PrjProject projetVisible(UUID projetId) {
        Visibilite v = new Visibilite(perimetre.projetsVisibles(), intervenant.statutsDeLotsEtProjets());
        return Optional.ofNullable(v.projets().contains(projetId) ? em.find(PrjProject.class, projetId) : null)
                .filter(p -> v.statuts().map(s -> p.getStrStatus() != null && s.contains(p.getStrStatus().getId()))
                        .orElse(true))
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable"));
    }

    private static LotListItemDto ligne(PrjMarket m, Trio marche, Cumuls c, SoldeDocumentAchat solde,
                                       CoutPrevisionnelLot cout) {
        SoldeDocumentAchat s = solde != null ? solde : SoldeDocumentAchat.de(null, null);
        CoutPrevisionnelLot k = Objects.requireNonNullElse(cout, CoutPrevisionnelLot.ZERO);
        BigDecimal cumulTTC = MontantsLot.arrondi(c.ttc());
        return new LotListItemDto(m.getId(), m.getCode(), m.getDescription(), m.getPrjProject().getId(),
                m.getPrjProject().getCode(), code(m.getPrjMarketType()), description(m.getPrjMarketType()),
                m.getStrStatus() == null ? null : m.getStrStatus().getCode(),
                m.getStrStatus() == null ? null : m.getStrStatus().getDescription(),
                m.getStrStatus() == null ? null : m.getStrStatus().getSorting(),
                m.getCfgSupplier() == null ? null : m.getCfgSupplier().getId(), TierNomFormatter.nom(m.getCfgSupplier()),
                m.getStlCategory() == null ? null : m.getStlCategory().getCode(),
                m.getStlCategory() == null ? null : m.getStlCategory().getDescription(),
                jour(m.getStartDateManual()), jour(m.getEndDateManual()), m.getDelayManual(),
                Boolean.TRUE.equals(m.getIsMultiSupplier()), marche.ht(), marche.tva(), marche.ttc(),
                marche.depuisBordereau(), MontantsLot.pourcentage(m.getWarrantyRatio()), c.nb(),
                MontantsLot.arrondi(c.ht()), cumulTTC, MontantsLot.arrondi(c.extras()), MontantsLot.arrondi(c.rg()),
                MontantsLot.avancementPct(cumulTTC, marche.ttc()), s.montantAPayer(), s.montantRegle(), s.resteARegler(),
                k.avancement(), k.ht(), k.ttc(), k.netAPayer(), k.regle(), k.reste(),
                Champs.de(m.getStrStatus(), StrStatus::getColorName),
                GenreLot.deEntite(Champs.de(m.getStrEntity(), StrEntity::getId)).map(Enum::name).orElse(null), m.getNotes());
    }

    private static Trio marche(PrjMarket m, Map<UUID, Trio> bordereaux) {
        return MontantsLot.choisir(bordereaux.get(m.getId()), m.getEstimatedPriceHTManual(),
                m.getEstimatedVatAmountManual(), m.getEstimatedPriceTTCManual());
    }

    private static LigneBordereauDto ligneBordereau(PrjMarketLine l, String groupe) {
        return new LigneBordereauDto(l.getId(), l.getSorting(), groupe, l.getCode(), l.getDescription(), l.getUnity(),
                l.getQuantity(), l.getUnitPriceHT(), MontantsLot.pourcentage(l.getVatRatio()),
                MontantsLot.applique(l.getQuantity(), l.getUnitPriceHT()),
                MontantsLot.applique(l.getQuantity(), l.getUnitPriceTTC()));
    }

    /** Σ bordereau (définitif ou estimatif) par lot ; un lot absent de la map n'a aucune ligne retenue. */
    private Map<UUID, Trio> sommes(String jpql, Collection<UUID> ids) {
        Map<UUID, Trio> parLot = new HashMap<>();
        if (!ids.isEmpty()) {
            em.createQuery(jpql, Object[].class).setParameter("lots", ids).getResultList()
                    .forEach(r -> parLot.put((UUID) r[0], new Trio((BigDecimal) r[1], (BigDecimal) r[2], (BigDecimal) r[3], true)));
        }
        return parLot;
    }

    private Map<UUID, Cumuls> cumuls(Collection<UUID> ids) {
        Map<UUID, Cumuls> parLot = new HashMap<>();
        if (!ids.isEmpty()) {
            em.createQuery(CUMULS, Object[].class).setParameter("lots", ids).getResultList()
                    .forEach(r -> parLot.put((UUID) r[0], new Cumuls((Long) r[1], (BigDecimal) r[2], (BigDecimal) r[3],
                            (BigDecimal) r[4], (BigDecimal) r[5], (BigDecimal) r[6])));
        }
        return parLot;
    }

    private List<ProjetFiltreDto> projets(Visibilite v) {
        if (v.vide()) {
            return List.of();
        }
        return v.lier(em.createQuery("select p.id, p.code, p.description,"
                        + " (select count(m) from PrjMarket m where m.prjProject = p" + STATUT_M + "), p.cfgCompany.id"
                        + " from PrjProject p where p.id in :projets"
                        + " and (:tousStatuts = true or p.strStatus.id in :statuts) order by p.code", Object[].class))
                .getResultList().stream()
                .map(r -> new ProjetFiltreDto((UUID) r[0], (String) r[1], (String) r[2], (Long) r[3], (UUID) r[4]))
                .toList();
    }

    private static List<EntiteFiltreDto> entites(List<ProjetFiltreDto> projets, EntityManager em) {
        Set<UUID> ids = projets.stream().map(ProjetFiltreDto::entiteId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return List.of();
        }
        return em.createQuery("select c from CfgCompany c where c.id in :ids order by c.code", CfgCompany.class)
                .setParameter("ids", ids).getResultList().stream()
                .map(c -> new EntiteFiltreDto(c.getId(), c.getCode(), c.getDescription())).toList();
    }

    private List<ElementReferentielProjetDto> elements(String jpql) {
        return em.createQuery(jpql, CodeLibelleOmran.class).getResultList().stream()
                .map(e -> new ElementReferentielProjetDto(e.getId(), e.getCode(), e.getDescription(), null))
                .toList();
    }

    // Sorting (ordre du circuit) n'est pas mappé sur l'entité BuyDocumentStatus, partagée avec le module Achat.
    private List<ElementReferentielProjetDto> statutsFacture() {
        List<?> lignes = em.createNativeQuery("SELECT PKey, Code, Description, Sorting FROM BuyDocumentStatus"
                + " ORDER BY Sorting ASC, Code ASC").getResultList();
        return lignes.stream().map(Object[].class::cast)
                .map(r -> new ElementReferentielProjetDto(UUID.fromString(String.valueOf(r[0])), (String) r[1], (String) r[2],
                        r[3] instanceof Number n ? n.intValue() : null))
                .toList();
    }

    private List<ElementReferentielProjetDto> categories(Visibilite v) {
        if (v.vide()) {
            return List.of();
        }
        return v.lier(em.createQuery("select distinct c.id, c.code, c.description, c.sorting from PrjMarket m"
                        + " join m.stlCategory c where m.prjProject.id in :projets" + STATUT_M
                        + " order by c.sorting, c.code", Object[].class))
                .getResultList().stream()
                .map(r -> new ElementReferentielProjetDto((UUID) r[0], (String) r[1], (String) r[2], (Integer) r[3]))
                .toList();
    }

    // Un décompte peut porter son propre tiers quand le lot n'a pas de fournisseur : les deux sources comptent.
    private List<ElementReferentielProjetDto> fournisseurs(Visibilite v) {
        if (v.vide()) {
            return List.of();
        }
        Map<UUID, CfgTier> parId = new HashMap<>();
        for (String jpql : List.of("select distinct t from PrjMarket m join m.cfgSupplier t where m.prjProject.id in :projets"
                        + STATUT_M,
                "select distinct t from PrjWorkingOut w join w.prjMarket m join w.cfgTier t"
                        + " where m.prjProject.id in :projets" + STATUT_M)) {
            v.lier(em.createQuery(jpql, CfgTier.class)).getResultList()
                    .forEach(t -> parId.putIfAbsent(t.getId(), t));
        }
        return parId.values().stream()
                .map(t -> new ElementReferentielProjetDto(t.getId(), t.getCode(), TierNomFormatter.nom(t), null))
                .sorted(Comparator.comparing(ElementReferentielProjetDto::description,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    private List<StatutLotProjetDto> statutsLot() {
        List<?> lignes = em.createNativeQuery(STATUTS_LOT).getResultList();
        return lignes.stream().map(Object[].class::cast)
                .map(r -> new StatutLotProjetDto(UUID.fromString(String.valueOf(r[0])), (String) r[1], (String) r[2],
                        r[3] instanceof Number n ? n.intValue() : null, (String) r[4], (String) r[5], (String) r[6],
                        GenreLot.deEntite(UUID.fromString(String.valueOf(r[7]))).map(Enum::name).orElse(null)))
                .toList();
    }

    private static Predicate filtre(Root<PrjMarket> root, CriteriaBuilder cb, FiltresLots f, Visibilite v) {
        List<Predicate> p = new ArrayList<>();
        p.add(root.get("prjProject").get(ID).in(v.projets()));
        v.statuts().ifPresent(s -> p.add(root.get("strStatus").get(ID).in(s)));
        if (f.statutIds() != null && !f.statutIds().isEmpty()) {
            p.add(root.get("strStatus").get(ID).in(f.statutIds()));
        }
        if (f.entiteId() != null) {
            p.add(cb.equal(root.get("prjProject").get("cfgCompany").get(ID), f.entiteId()));
        }
        if (f.typeId() != null) {
            p.add(cb.equal(root.get("prjMarketType").get(ID), f.typeId()));
        }
        From<?, ?> fournisseur = root.join("cfgSupplier", JoinType.LEFT);
        if (f.fournisseurId() != null) {
            p.add(cb.equal(fournisseur.get(ID), f.fournisseurId()));
        }
        if (StringUtils.hasText(f.search())) {
            String motif = "%" + f.search().strip() + "%";
            p.add(cb.or(cb.like(root.get("code"), motif), cb.like(root.get(DESCRIPTION), motif),
                    cb.like(fournisseur.get("socialReason"), motif), cb.like(fournisseur.get(DESCRIPTION), motif)));
        }
        return cb.and(p.toArray(Predicate[]::new));
    }

    private static String code(CodeLibelleOmran e) {
        return e == null ? null : e.getCode();
    }

    private static String description(CodeLibelleOmran e) {
        return e == null ? null : e.getDescription();
    }
}
