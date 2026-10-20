package com.tsix.immocrm.services.etatventes;

import com.tsix.immocrm.domain.AgregatDemande;
import com.tsix.immocrm.domain.ColonneDossier;
import com.tsix.immocrm.domain.ColonneEtatVentes;
import com.tsix.immocrm.domain.ColonneInconnueException;
import com.tsix.immocrm.domain.Colonnes;
import com.tsix.immocrm.domain.EtatVente;
import com.tsix.immocrm.domain.FamilleArticle;
import com.tsix.immocrm.domain.Granularite;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.domain.TriGroupe;
import com.tsix.immocrm.dto.common.TotauxListeDto;
import com.tsix.immocrm.dto.etatventes.LigneEtatVentesDto;
import com.tsix.immocrm.dto.etatventes.SyntheseEtatVentesDto;
import com.tsix.immocrm.dto.etatventes.SyntheseEtatVentesDto.LigneSynthese;
import com.tsix.immocrm.dto.etatventes.SyntheseEtatVentesDto.StatutSynthese;
import com.tsix.immocrm.dto.etatventes.SyntheseEtatVentesDto.TotalCategorieSynthese;
import com.tsix.immocrm.dto.etatventes.SyntheseEtatVentesDto.TotalGeneralSynthese;
import com.tsix.immocrm.dto.etatventes.SyntheseVendusLibresDto;
import com.tsix.immocrm.dto.etatventes.SyntheseVendusLibresDto.CompteVendusLibres;
import com.tsix.immocrm.dto.etatventes.SyntheseVendusLibresDto.FamilleVendusLibres;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkBuilding;
import com.tsix.immocrm.entities.stk.StkFloor;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemEtatVentes;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.stk.StkItemEtatVentesRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import com.tsix.immocrm.services.dossier.liste.MontantsParNatureDossier;
import com.tsix.immocrm.services.groupement.GroupementService;
import com.tsix.immocrm.services.groupement.GroupementService.ResultatGroupement;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.TierNomFormatter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tsix.immocrm.entities.stk.StkItemEtatVentes.LIBRE;
import static com.tsix.immocrm.entities.stk.StkItemEtatVentes.SANS_STATUT;

/**
 * État des ventes (FEATURE-etat-des-ventes.md) : toutes les lectures partent de {@link #specification}, filtres et
 * visibilité compris (R4, R5), si bien qu'une cellule de la synthèse égale le {@code totalElements} de la liste.
 */
@Service
@Transactional(readOnly = true)
public class EtatVentesService {

    private static final String TRANCHE = "tranche";
    private static final String CATEGORIE = "categorie";
    private static final String DOSSIER = "dossier";
    private static final String STATUT_CODE = "statutCode";
    private static final String FAMILLE = "famille";
    private static final String ETAT_VENTE = "etatVente";
    private static final String CODE = "code";
    private static final String DESCRIPTION = "description";
    private static final String ID = "id";
    private static final String LIBELLE_LIBRE = "Libre";
    private static final String LIBELLE_SANS_STATUT = "Sans statut";

    /** Montants calculés en Java par la liste des dossiers, jamais refaits en SQL : sommés ici pour les totaux. */
    private static final Map<String, ColonneDossier> MONTANTS = Map.of(
            ColonneEtatVentes.MONTANT_REGLE.nom(), ColonneDossier.PRIX_REGLE,
            ColonneEtatVentes.RESTE_A_REGLER.nom(), ColonneDossier.PRIX_RESTE);

    private final StkItemEtatVentesRepository repository;
    private final PerimetreCommercialService perimetreService;
    private final GroupementService groupementService;
    private final MontantsParNatureDossier montantsParNature;
    private final StrStatusRepository statusRepository;
    private final EntityManager em;

    public EtatVentesService(StkItemEtatVentesRepository repository, PerimetreCommercialService perimetreService,
                             GroupementService groupementService, MontantsParNatureDossier montantsParNature,
                             StrStatusRepository statusRepository, EntityManager em) {
        this.repository = repository;
        this.perimetreService = perimetreService;
        this.groupementService = groupementService;
        this.montantsParNature = montantsParNature;
        this.statusRepository = statusRepository;
        this.em = em;
    }

    // ---------------------------------------------------------------- synthèse

    public SyntheseEtatVentesDto synthese(FiltresEtatVentes filtres) {
        Map<List<UUID>, Tuple> entetes = new LinkedHashMap<>();
        Map<List<UUID>, Map<String, Long>> comptes = new HashMap<>();
        for (Tuple t : compter(filtres, root -> {
            Join<StkItemEtatVentes, StkItemCategory> categorie = root.join(CATEGORIE);
            return List.of(categorie.get(CODE), categorie.get(ID), categorie.get(DESCRIPTION), root.get(STATUT_CODE));
        })) {
            List<UUID> cle = Arrays.asList(t.get(0, UUID.class), t.get(4, UUID.class));
            entetes.putIfAbsent(cle, t);
            comptes.computeIfAbsent(cle, k -> new LinkedHashMap<>()).merge(t.get(6, String.class), t.get(7, Long.class),
                    Long::sum);
        }
        List<LigneSynthese> lignes = entetes.entrySet().stream().map(e -> {
            Tuple t = e.getValue();
            Map<String, Long> parStatut = comptes.get(e.getKey());
            return new LigneSynthese(t.get(0, UUID.class), t.get(1, String.class), t.get(2, String.class),
                    t.get(4, UUID.class), t.get(3, String.class), t.get(5, String.class), total(parStatut), parStatut);
        }).toList();

        Map<String, Long> general = fusion(lignes.stream().map(LigneSynthese::parStatut));
        return new SyntheseEtatVentesDto(colonnesStatut(general.keySet()), lignes,
                new SyntheseEtatVentesDto.TotauxSynthese(parCategorie(lignes),
                        new TotalGeneralSynthese(total(general), general)));
    }

    /**
     * Nombre d'articles du filtre par tranche puis {@code cles}, trié dans cet ordre. Colonnes du tuple : id, code et
     * libellé de tranche, les {@code cles}, le nombre.
     */
    private List<Tuple> compter(FiltresEtatVentes filtres,
                                Function<Root<StkItemEtatVentes>, List<Expression<?>>> autresCles) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<StkItemEtatVentes> root = cq.from(StkItemEtatVentes.class);
        Join<StkItemEtatVentes, CfgTranche> tranche = root.join(TRANCHE);
        List<Expression<?>> cles = new ArrayList<>(List.of(tranche.get(ID), tranche.get(CODE),
                tranche.get(DESCRIPTION)));
        List<Expression<?>> autres = autresCles.apply(root);
        cles.addAll(autres);
        List<Selection<?>> selections = new ArrayList<>(cles);
        selections.add(cb.count(root));
        List<Order> ordre = new ArrayList<>(List.of(cb.asc(tranche.get(CODE))));
        autres.forEach(c -> ordre.add(cb.asc(c)));
        cq.multiselect(selections)
                .where(specification(filtres).toPredicate(root, cq, cb))
                .groupBy(cles)
                .orderBy(ordre);
        return em.createQuery(cq).getResultList();
    }

    /** Une seule requête de comptage, la base de la liste (R1) ; l'état et la famille viennent du {@code @Subselect}. */
    public SyntheseVendusLibresDto syntheseVendusLibres(FiltresEtatVentes filtres) {
        Map<UUID, Tuple> tranches = new LinkedHashMap<>();
        Map<UUID, Map<FamilleArticle, Comptes>> parTranche = new HashMap<>();
        for (Tuple t : compter(filtres, root -> List.of(root.get(FAMILLE), root.get(ETAT_VENTE), root.get(STATUT_CODE)))) {
            UUID id = t.get(0, UUID.class);
            tranches.putIfAbsent(id, t);
            Comptes c = Comptes.de(t.get(4, EtatVente.class), SANS_STATUT.equals(t.get(5, String.class)),
                    t.get(6, Long.class));
            parTranche.computeIfAbsent(id, k -> new EnumMap<>(FamilleArticle.class))
                    .merge(t.get(3, FamilleArticle.class), c, Comptes::plus);
        }
        List<CompteVendusLibres> lignes = new ArrayList<>();
        List<CompteVendusLibres> totalTranche = new ArrayList<>();
        Map<FamilleArticle, Comptes> parFamille = new EnumMap<>(FamilleArticle.class);
        tranches.forEach((id, t) -> {
            String code = t.get(1, String.class);
            String libelle = t.get(2, String.class);
            Map<FamilleArticle, Comptes> familles = parTranche.get(id);
            familles.forEach((famille, c) -> {
                lignes.add(c.vers(id, code, libelle, famille));
                parFamille.merge(famille, c, Comptes::plus);
            });
            totalTranche.add(Comptes.somme(familles.values()).vers(id, code, libelle, null));
        });
        List<FamilleVendusLibres> familles = Arrays.stream(FamilleArticle.values())
                .filter(f -> CollectionUtils.isEmpty(filtres.famille()) || filtres.famille().contains(f))
                .map(FamilleVendusLibres::de).toList();
        return new SyntheseVendusLibresDto(familles, lignes, totalTranche,
                parFamille.entrySet().stream().map(e -> e.getValue().vers(null, null, null, e.getKey())).toList(),
                Comptes.somme(parFamille.values()).vers(null, null, null, null));
    }

    /** Lignes du classeur, dans l'ordre du JSON : familles d'une tranche, son total, puis le total général. */
    public static List<LigneTableurVendusLibres> lignesTableur(SyntheseVendusLibresDto synthese) {
        List<LigneTableurVendusLibres> lignes = new ArrayList<>();
        for (CompteVendusLibres total : synthese.totalTranche()) {
            synthese.lignes().stream().filter(l -> total.trancheId().equals(l.trancheId())).forEach(l -> lignes.add(
                    LigneTableurVendusLibres.de(l.trancheCode(), l.familleCode().libelle(), l)));
            lignes.add(LigneTableurVendusLibres.de("Total tranche", null, total));
        }
        lignes.add(LigneTableurVendusLibres.de("Total général", null, synthese.general()));
        return lignes;
    }

    /** Comptes d'une cellule ; {@link #vers} porte la seule règle de pourcentage (R5), reprise par l'export. */
    private record Comptes(long libres, long reserves, long vendus, long sansStatut) {

        static Comptes de(EtatVente etat, boolean sansStatut, long n) {
            return new Comptes(etat == EtatVente.LIBRE ? n : 0, etat == EtatVente.RESERVE ? n : 0,
                    etat == EtatVente.VENDU ? n : 0, sansStatut ? n : 0);
        }

        static Comptes somme(Collection<Comptes> comptes) {
            return comptes.stream().reduce(new Comptes(0, 0, 0, 0), Comptes::plus);
        }

        Comptes plus(Comptes o) {
            return new Comptes(libres + o.libres, reserves + o.reserves, vendus + o.vendus,
                    sansStatut + o.sansStatut);
        }

        CompteVendusLibres vers(UUID trancheId, String trancheCode, String trancheLibelle, FamilleArticle famille) {
            long total = libres + reserves + vendus;
            return new CompteVendusLibres(trancheId, trancheCode, trancheLibelle, famille, libres, reserves, vendus,
                    sansStatut, total, pct(libres, total), pct(reserves, total), pct(vendus, total));
        }

        private static double pct(long n, long total) {
            if (total == 0) {
                return 0;
            }
            return BigDecimal.valueOf(n * 100L).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP)
                    .doubleValue();
        }
    }

    private static List<TotalCategorieSynthese> parCategorie(List<LigneSynthese> lignes) {
        Map<UUID, List<LigneSynthese>> parId = lignes.stream()
                .sorted(Comparator.comparing(LigneSynthese::categorieCode, Comparator.nullsFirst(Comparator.naturalOrder())))
                .collect(Collectors.groupingBy(LigneSynthese::categorieId, LinkedHashMap::new, Collectors.toList()));
        return parId.values().stream().map(groupe -> {
            LigneSynthese premiere = groupe.getFirst();
            Map<String, Long> parStatut = fusion(groupe.stream().map(LigneSynthese::parStatut));
            return new TotalCategorieSynthese(premiere.categorieId(), premiere.categorieCode(),
                    premiere.categorieLibelle(), total(parStatut), parStatut);
        }).toList();
    }

    private static Map<String, Long> fusion(Stream<Map<String, Long>> comptes) {
        Map<String, Long> somme = new LinkedHashMap<>();
        comptes.forEach(c -> c.forEach((code, n) -> somme.merge(code, n, Long::sum)));
        return somme;
    }

    private static long total(Map<String, Long> parStatut) {
        return parStatut.values().stream().mapToLong(Long::longValue).sum();
    }

    /** Libre, puis les statuts de dossier actifs (ou comptés) dans l'ordre {@code Sorting}, puis « Sans statut ». */
    private List<StatutSynthese> colonnesStatut(Set<String> presents) {
        List<StatutSynthese> statuts = new ArrayList<>();
        BiConsumer<String, String> ajouter = (code, libelle) -> statuts.add(
                new StatutSynthese(code, libelle, statuts.size()));
        ajouter.accept(LIBRE, LIBELLE_LIBRE);
        statutsDossier().stream()
                .filter(s -> s.estActif() || presents.contains(s.getCode()))
                .sorted(Comparator.comparing(StrStatus::getSorting, Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(s -> ajouter.accept(s.getCode(), s.getDescription()));
        if (presents.contains(SANS_STATUT)) {
            ajouter.accept(SANS_STATUT, LIBELLE_SANS_STATUT);
        }
        return statuts;
    }

    private List<StrStatus> statutsDossier() {
        return statusRepository.findByStrConfigEntityId(StatutDossier.CONFIG_ENTITY_ID);
    }

    // ---------------------------------------------------------------- liste, groupes, totaux

    /** Groupe déplié de {@code /groupes} : sa clé réémise telle quelle, même expression que le comptage. */
    public record Depliage(ColonneEtatVentes colonne, String valeur, Granularite granularite) {
    }

    public Page<LigneEtatVentesDto> lister(FiltresEtatVentes filtres, Depliage depliage,
                                           Set<ColonneEtatVentes> colonnes, Pageable pageable) {
        Specification<StkItemEtatVentes> spec = specification(filtres);
        if (depliage != null) {
            spec = spec.and((root, query, cb) -> GroupementService.predicatDeGroupe(root, cb,
                    depliage.colonne().chemin(), depliage.granularite(), depliage.valeur()));
        }
        Pageable trie = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), tri(pageable.getSort()));
        Page<StkItemEtatVentes> page = repository.findAll(spec, trie);
        boolean avecMontants = colonnes.stream().anyMatch(c -> MONTANTS.containsKey(c.nom()));
        Map<UUID, Map<ColonneDossier, BigDecimal>> montants = avecMontants
                ? montantsParNature.pour(page.stream().map(StkItemEtatVentes::getDossier).filter(Objects::nonNull).toList())
                : new HashMap<>();
        return page.map(l -> versDto(l, l.getDossier() != null ? montants.get(l.getDossier().getId()) : null));
    }

    public ResultatGroupement grouper(FiltresEtatVentes filtres, ColonneEtatVentes groupe, Granularite granularite,
                                      List<AgregatDemande> agregats, TriGroupe tri, boolean inclureSansValeur,
                                      Pageable pageable) {
        return groupementService.grouper(StkItemEtatVentes.class, specification(filtres), groupe.chemin(),
                granularite, agregats, tri, inclureSansValeur, pageable);
    }

    /** Agrégats SQL du catalogue, plus {@code montantRegle} / {@code resteARegler} sommés depuis le calcul Java. */
    public TotauxListeDto totauxColonnes(FiltresEtatVentes filtres, List<String> agregats) {
        List<String> demandes = agregats != null ? agregats : List.of();
        Specification<StkItemEtatVentes> spec = specification(filtres);
        GroupementService.Totaux totaux = groupementService.totaux(StkItemEtatVentes.class, spec,
                Colonnes.resoudreAgregats(ColonneEtatVentes.class,
                        demandes.stream().filter(a -> !MONTANTS.containsKey(a)).toList()));
        Map<String, Object> valeurs = new LinkedHashMap<>(totaux.valeurs());
        List<String> enJava = demandes.stream().filter(MONTANTS::containsKey).distinct().toList();
        if (!enJava.isEmpty()) {
            Map<UUID, Map<ColonneDossier, BigDecimal>> parDossier = montantsParNature.pour(dossiers(spec));
            // Une fois par ligne, comme la liste : un dossier à deux articles racines compte deux fois (et son prix aussi).
            List<UUID> lignes = dossierParLigne(spec);
            for (String nom : enJava) {
                valeurs.put(nom, lignes.stream().map(id -> parDossier.get(id).get(MONTANTS.get(nom)))
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
            }
        }
        return new TotauxListeDto(totaux.nombre(), valeurs);
    }

    /** Dossiers retenant un article du filtre, statut chargé d'avance (lu par le calcul des montants). */
    private List<ComFolder> dossiers(Specification<StkItemEtatVentes> spec) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<ComFolder> cq = cb.createQuery(ComFolder.class);
        Root<ComFolder> dossier = cq.from(ComFolder.class);
        dossier.fetch("strStatus", JoinType.LEFT);
        // Pas de DISTINCT sur l'entité : ComFolder.ComDocumentDetail est de type text en prod (non comparable).
        Subquery<UUID> ids = cq.subquery(UUID.class);
        Root<StkItemEtatVentes> ligne = ids.from(StkItemEtatVentes.class);
        ids.select(ligne.get(DOSSIER).get(ID)).where(spec.toPredicate(ligne, cq, cb));
        cq.select(dossier).where(dossier.get(ID).in(ids));
        return em.createQuery(cq).getResultList();
    }

    /** Id du dossier de chaque ligne vendue du filtre, doublons compris. */
    private List<UUID> dossierParLigne(Specification<StkItemEtatVentes> spec) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<UUID> cq = cb.createQuery(UUID.class);
        Root<StkItemEtatVentes> root = cq.from(StkItemEtatVentes.class);
        Path<UUID> dossierId = root.get(DOSSIER).get(ID);
        cq.select(dossierId).where(cb.isNotNull(dossierId), spec.toPredicate(root, cq, cb));
        return em.createQuery(cq).getResultList();
    }

    // ---------------------------------------------------------------- base partagée (R5)

    private Specification<StkItemEtatVentes> specification(FiltresEtatVentes filtres) {
        Set<String> statuts = statutsDemandes(filtres.statut());
        Specification<StkItemEtatVentes> visibles = perimetreService.filtreParTranche(TRANCHE);
        return visibles.and((root, query, cb) -> cb.and(predicats(root, cb, filtres, statuts)
                .toArray(new Predicate[0])));
    }

    /** Codes en majuscules ; un code ni LIBRE, ni SANS_STATUT, ni statut de dossier rend 400. */
    private Set<String> statutsDemandes(List<String> demandes) {
        if (CollectionUtils.isEmpty(demandes)) {
            return Set.of();
        }
        Set<String> valides = new LinkedHashSet<>(List.of(LIBRE, SANS_STATUT));
        statutsDossier().forEach(s -> valides.add(majuscules(s.getCode())));
        Set<String> codes = demandes.stream().map(EtatVentesService::majuscules)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        codes.stream().filter(c -> !valides.contains(c)).findFirst().ifPresent(inconnu -> {
            throw new InvalidRequestException("Statut inconnu : " + inconnu + ". Valeurs : " + valides);
        });
        return codes;
    }

    private static String majuscules(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

    private static List<Predicate> predicats(Root<StkItemEtatVentes> root, CriteriaBuilder cb,
                                             FiltresEtatVentes f, Set<String> statuts) {
        List<Predicate> p = new ArrayList<>();
        dansLaListe(p, root.get(TRANCHE).get(ID), f.trancheId());
        dansLaListe(p, root.get(CATEGORIE).get(ID), f.categorieId());
        dansLaListe(p, root.get("type").get(ID), f.typeId());
        dansLaListe(p, root.get(FAMILLE), f.famille());
        dansLaListe(p, root.get(ETAT_VENTE), f.etat());
        if (f.projetId() != null) {
            p.add(cb.equal(root.get(TRANCHE).get("cfgProject").get(ID), f.projetId()));
        }
        if (!statuts.isEmpty()) {
            p.add(cb.upper(root.get(STATUT_CODE)).in(statuts));
        }
        Path<Boolean> sousArticle = root.get("sousArticle");
        switch (f.nature()) {
            case ARTICLES -> p.add(cb.or(cb.isNull(sousArticle), cb.isFalse(sousArticle)));
            case SOUS_ARTICLES -> p.add(cb.isTrue(sousArticle));
            case TOUS -> { /* aucune restriction */ }
        }
        if (StringUtils.hasText(f.search())) {
            p.add(recherche(root, cb, f.search()));
        }
        periode(p, root, cb, f);
        return p;
    }

    private static void dansLaListe(List<Predicate> p, Path<Object> chemin, List<?> ids) {
        if (!CollectionUtils.isEmpty(ids)) {
            p.add(chemin.in(ids));
        }
    }

    /** Jointures LEFT : un article libre n'a ni dossier ni client et doit rester cherchable par son code. */
    private static Predicate recherche(Root<StkItemEtatVentes> root, CriteriaBuilder cb, String search) {
        String motif = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        Join<StkItemEtatVentes, ComFolder> dossier = root.join(DOSSIER, JoinType.LEFT);
        Join<ComFolder, CfgTier> client = dossier.join("cfgCustomer", JoinType.LEFT);
        List<Expression<String>> champs = List.of(root.get("articleCode"),
                root.join(CATEGORIE, JoinType.LEFT).get(CODE), root.join("type", JoinType.LEFT).get(CODE),
                dossier.get(CODE), client.get("socialReason"), client.get(DESCRIPTION), client.get("firstName"),
                client.get("lastName"), client.get("phone"), client.get("mobile"), client.get("iDNumber"));
        return cb.or(champs.stream().map(c -> cb.like(cb.lower(c), motif)).toArray(Predicate[]::new));
    }

    /** {@code du} / {@code au} : jours entiers inclus, sur une date du dossier (un libre n'en a aucune). */
    private static void periode(List<Predicate> p, Root<StkItemEtatVentes> root, CriteriaBuilder cb,
                                FiltresEtatVentes f) {
        if (f.du() == null && f.au() == null) {
            return;
        }
        From<?, ?> courant = root.join(DOSSIER, JoinType.LEFT);
        String[] maillons = f.champDate().getChampEntite().split("\\.");
        for (int i = 0; i < maillons.length - 1; i++) {
            courant = courant.join(maillons[i], JoinType.LEFT);
        }
        Path<LocalDateTime> date = courant.get(maillons[maillons.length - 1]);
        if (f.du() != null) {
            p.add(cb.greaterThanOrEqualTo(date, f.du().atStartOfDay()));
        }
        if (f.au() != null) {
            p.add(cb.lessThan(date, f.au().plusDays(1).atStartOfDay()));
        }
    }

    /** Noms du DTO traduits en chemins de l'entité ; colonne sans chemin = non triable (400). */
    private static Sort tri(Sort demande) {
        Map<String, ColonneEtatVentes> index = Colonnes.index(ColonneEtatVentes.class);
        List<Sort.Order> ordres = new ArrayList<>();
        for (Sort.Order o : demande) {
            // « id » = départage ajouté par LignesExport.toutes : couvert par le départage articleId ci-dessous.
            if ("id".equals(o.getProperty())) {
                continue;
            }
            ColonneEtatVentes c = index.get(o.getProperty().toLowerCase(Locale.ROOT));
            if (c == null || c.chemin() == null) {
                throw new ColonneInconnueException(o.getProperty(), Arrays.stream(ColonneEtatVentes.values())
                        .filter(col -> col.chemin() != null).map(ColonneEtatVentes::nom).toList());
            }
            ordres.add(o.withProperty(c.chemin().cheminCle()));
        }
        // Départage stable de la pagination ; même nom sur l'entité et le DTO.
        ordres.add(Sort.Order.asc(ColonneEtatVentes.ARTICLE_ID.nom()));
        return Sort.by(ordres);
    }

    // ---------------------------------------------------------------- restitution

    private static LigneEtatVentesDto versDto(StkItemEtatVentes l, Map<ColonneDossier, BigDecimal> montants) {
        ComFolder d = l.getDossier();
        Function<ColonneDossier, BigDecimal> montant = c -> montants != null ? montants.get(c) : null;
        return new LigneEtatVentesDto(l.getArticleId(), l.getArticleCode(), l.getArticleLibelle(),
                Champs.de(l.getTranche(), CfgTranche::getCode),
                Champs.de(l.getCategorie(), StkItemCategory::getCode),
                Champs.de(l.getCategorie(), StkItemCategory::getDescription),
                Champs.de(l.getType(), StkItemType::getCode),
                Champs.de(l.getEtage(), StkFloor::getCode),
                Champs.de(l.getImmeuble(), StkBuilding::getCode),
                l.getSurface(), l.getPrixVenteTTC(), l.getStatutCode(), libelleStatut(l),
                Champs.de(d, ComFolder::getId), Champs.de(d, ComFolder::getCode),
                TierNomFormatter.nom(Champs.de(d, ComFolder::getCfgCustomer)),
                TierNomFormatter.nom(Champs.de(d, ComFolder::getCfgCommercial)),
                Champs.de(d, ComFolder::getDocDate),
                montant.apply(ColonneDossier.PRIX_REGLE), montant.apply(ColonneDossier.PRIX_RESTE));
    }

    private static String libelleStatut(StkItemEtatVentes l) {
        return switch (l.getStatutCode()) {
            case LIBRE -> LIBELLE_LIBRE;
            case SANS_STATUT -> LIBELLE_SANS_STATUT;
            case null, default -> Champs.de(l.getDossier(), ComFolder::getStrStatus, StrStatus::getDescription);
        };
    }
}
