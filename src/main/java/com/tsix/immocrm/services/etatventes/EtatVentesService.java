package com.tsix.immocrm.services.etatventes;

import com.tsix.immocrm.domain.AgregatDemande;
import com.tsix.immocrm.domain.ColonneDossier;
import com.tsix.immocrm.domain.ColonneEtatVentes;
import com.tsix.immocrm.domain.ColonneInconnueException;
import com.tsix.immocrm.domain.Colonnes;
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
import com.tsix.immocrm.services.dossier.liste.CalculsDeLaPage;
import com.tsix.immocrm.services.dossier.liste.ColonnesCalculeesDossier;
import com.tsix.immocrm.services.dossier.liste.MappingListeDossier;
import com.tsix.immocrm.services.dossier.liste.MontantsParNatureDossier;
import com.tsix.immocrm.services.dossier.liste.ValeursCalculees;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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

import static com.tsix.immocrm.domain.ColonneEtatVentes.*;
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
    private static final String CODE = "code";
    private static final String DESCRIPTION = "description";
    private static final String ID = "id";
    private static final String LIBELLE_LIBRE = "Libre";
    private static final String LIBELLE_SANS_STATUT = "Sans statut";

    /** Jetons d'{@code agregats} sommés en Java, par nom exact. */
    private static final Map<String, ColonneEtatVentes> SOMMES_CALCULEES = Arrays.stream(ColonneEtatVentes.values())
            .filter(EtatVentesService::sommeCalculee)
            .collect(Collectors.toUnmodifiableMap(ColonneEtatVentes::nom, Function.identity()));

    private final StkItemEtatVentesRepository repository;
    private final PerimetreCommercialService perimetreService;
    private final GroupementService groupementService;
    private final CalculsDeLaPage calculs;
    private final StrStatusRepository statusRepository;
    private final EntityManager em;

    public EtatVentesService(StkItemEtatVentesRepository repository, PerimetreCommercialService perimetreService,
                             GroupementService groupementService, CalculsDeLaPage calculs,
                             StrStatusRepository statusRepository, EntityManager em) {
        this.repository = repository;
        this.perimetreService = perimetreService;
        this.groupementService = groupementService;
        this.calculs = calculs;
        this.statusRepository = statusRepository;
        this.em = em;
    }

    /** Montant du dossier dont la liste des dossiers donne la somme : Σ calculée en Java, jamais en SQL. */
    public static boolean sommeCalculee(ColonneEtatVentes colonne) {
        return colonne.source() != null && MontantsParNatureDossier.SOMMABLES.contains(colonne.source());
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
        Map<UUID, Map<ColonneDossier, BigDecimal>> montants = montantsDuDossier(
                page.stream().map(StkItemEtatVentes::getDossier).filter(Objects::nonNull).distinct().toList(),
                colonnes.stream().map(ColonneEtatVentes::source).filter(Objects::nonNull).collect(Collectors.toSet()));
        return page.map(l -> versDto(l, l.getDossier() != null ? montants.get(l.getDossier().getId()) : null));
    }

    /** Montants des colonnes {@code sources}, par les calculs de la liste des dossiers ; aucune requête sans source. */
    private Map<UUID, Map<ColonneDossier, BigDecimal>> montantsDuDossier(List<ComFolder> dossiers,
                                                                         Set<ColonneDossier> sources) {
        Map<UUID, Map<ColonneDossier, BigDecimal>> parDossier = new HashMap<>();
        if (MontantsParNatureDossier.requises(sources)) {
            calculs.montants().pour(dossiers).forEach((id, m) -> parDossier.put(id, new EnumMap<>(m)));
        }
        if (ColonnesCalculeesDossier.requises(sources)) {
            Map<UUID, ValeursCalculees> valeurs = calculs.colonnes().pour(
                    dossiers.stream().map(ComFolder::getId).toList(), null);
            for (ComFolder f : dossiers) {
                parDossier.computeIfAbsent(f.getId(), k -> new EnumMap<>(ColonneDossier.class))
                        .putAll(MappingListeDossier.prixVenteAuM2(f, valeurs.get(f.getId())));
            }
        }
        return parDossier;
    }

    public ResultatGroupement grouper(FiltresEtatVentes filtres, ColonneEtatVentes groupe, Granularite granularite,
                                      List<AgregatDemande> agregats, TriGroupe tri, boolean inclureSansValeur,
                                      Pageable pageable) {
        return groupementService.grouper(StkItemEtatVentes.class, specification(filtres), groupe.chemin(),
                granularite, agregats, tri, inclureSansValeur, pageable);
    }

    /** Agrégats SQL du catalogue, plus les montants du dossier sommés depuis le calcul Java ({@link #sommeCalculee}). */
    public TotauxListeDto totauxColonnes(FiltresEtatVentes filtres, List<String> agregats) {
        List<String> demandes = agregats != null ? agregats : List.of();
        Specification<StkItemEtatVentes> spec = specification(filtres);
        GroupementService.Totaux totaux = groupementService.totaux(StkItemEtatVentes.class, spec,
                Colonnes.resoudreAgregats(ColonneEtatVentes.class,
                        demandes.stream().filter(a -> !SOMMES_CALCULEES.containsKey(a)).toList()));
        Map<String, Object> valeurs = new LinkedHashMap<>(totaux.valeurs());
        List<String> enJava = demandes.stream().filter(SOMMES_CALCULEES::containsKey).distinct().toList();
        if (!enJava.isEmpty()) {
            Map<UUID, Map<ColonneDossier, BigDecimal>> parDossier = calculs.montants().pour(dossiers(spec));
            // Une fois par ligne, comme la liste : un dossier à deux articles racines compte deux fois (et son prix aussi).
            List<UUID> lignes = dossierParLigne(spec);
            for (String nom : enJava) {
                ColonneDossier source = SOMMES_CALCULEES.get(nom).source();
                valeurs.put(nom, lignes.stream().map(id -> parDossier.get(id).get(source))
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
        Function<ColonneEtatVentes, BigDecimal> m = c -> montants != null ? montants.get(c.source()) : null;
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
                m.apply(MONTANT_REGLE), m.apply(RESTE_A_REGLER),
                l.getPrixArticleHt(), l.getPrixArticleTtc(), l.getPrixSousArticlesHt(), l.getPrixSousArticlesTtc(),
                l.getPrixHt(), l.getRemiseMontant(), l.getPrixContractuel(),
                m.apply(PRIX_VENTE_M2_TTC), m.apply(PRIX_VENTE_M2_HT),
                m.apply(NET_A_PAYER), m.apply(NET_A_PAYER_REGLE), m.apply(NET_A_PAYER_RESTE),
                m.apply(AVANCE), m.apply(AVANCE_REGLEE), m.apply(AVANCE_RESTE),
                m.apply(AUTOFINANCEMENT), m.apply(AUTOFINANCEMENT_REGLE), m.apply(AUTOFINANCEMENT_RESTE),
                m.apply(CREDIT), m.apply(CREDIT_REGLE), m.apply(CREDIT_RESTE),
                m.apply(FRAIS), m.apply(FRAIS_REGLES), m.apply(FRAIS_RESTE),
                m.apply(MODIFICATIONS), m.apply(MODIFICATIONS_REGLEES), m.apply(MODIFICATIONS_RESTE),
                m.apply(TROP_PERCU), m.apply(TROP_PERCU_REGLE), m.apply(TROP_PERCU_RESTE),
                m.apply(PENALITES), m.apply(PENALITES_REGLES), m.apply(PENALITES_RESTE),
                m.apply(PRIX_CONTRACTUEL_REGLE), m.apply(PRIX_CONTRACTUEL_RESTE),
                m.apply(TAUX_AVANCE), m.apply(TAUX_AVANCE_REEL));
    }

    private static String libelleStatut(StkItemEtatVentes l) {
        return switch (l.getStatutCode()) {
            case LIBRE -> LIBELLE_LIBRE;
            case SANS_STATUT -> LIBELLE_SANS_STATUT;
            case null, default -> Champs.de(l.getDossier(), ComFolder::getStrStatus, StrStatus::getDescription);
        };
    }
}
