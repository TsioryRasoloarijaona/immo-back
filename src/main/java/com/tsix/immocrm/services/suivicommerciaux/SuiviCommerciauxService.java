package com.tsix.immocrm.services.suivicommerciaux;

import com.tsix.immocrm.domain.AgregatDemande;
import com.tsix.immocrm.domain.ColonneInconnueException;
import com.tsix.immocrm.domain.ColonneSuiviCommerciaux;
import com.tsix.immocrm.domain.Colonnes;
import com.tsix.immocrm.domain.Granularite;
import com.tsix.immocrm.domain.InteractionKpi;
import com.tsix.immocrm.domain.TriGroupe;
import com.tsix.immocrm.dto.common.TotauxListeDto;
import com.tsix.immocrm.dto.suivicommerciaux.LigneSuiviCommerciauxDto;
import com.tsix.immocrm.dto.suivicommerciaux.LigneSyntheseSuiviDto;
import com.tsix.immocrm.dto.suivicommerciaux.SyntheseSuiviCommerciauxDto;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComActionMessageSuivi;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.com.ComActionMessageSuiviRepository;
import com.tsix.immocrm.services.equipe.CommerciauxVisibles;
import com.tsix.immocrm.services.equipe.MembreEquipe;
import com.tsix.immocrm.services.groupement.GroupementService;
import com.tsix.immocrm.services.groupement.GroupementService.ResultatGroupement;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.services.prospection.ReglesInteraction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
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
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Suivi des commerciaux (FEATURE-suivi-commerciaux.md) : toutes les lectures partent de {@link #base} — visibilité de
 * {@code filtreInteractions()} et commerciaux visibles —, si bien qu'une cellule de la synthèse égale la liste (R9).
 */
@Service
@Transactional(readOnly = true)
public class SuiviCommerciauxService {

    static final int JOURS_MAX = 92;

    private static final String ID = "id";
    private static final String JOUR = "jour";
    private static final String DATE_SAISIE = "dateSaisie";
    private static final String COMMERCIAL_CLE = "commercialCle";
    private static final String TYPE = "type";
    private static final String CATEGORIE = "categorie";
    private static final String JOINT = "joint";

    private final ComActionMessageSuiviRepository repository;
    private final PerimetreCommercialService perimetreService;
    private final CommerciauxVisibles commerciaux;
    private final GroupementService groupementService;
    private final ObjectifCommercialService objectifService;
    private final EntityManager em;
    private final Clock clock;

    public SuiviCommerciauxService(ComActionMessageSuiviRepository repository,
                                   PerimetreCommercialService perimetreService, CommerciauxVisibles commerciaux,
                                   GroupementService groupementService, ObjectifCommercialService objectifService,
                                   EntityManager em, Clock clock) {
        this.repository = repository;
        this.perimetreService = perimetreService;
        this.commerciaux = commerciaux;
        this.groupementService = groupementService;
        this.objectifService = objectifService;
        this.em = em;
        this.clock = clock;
    }

    // ---------------------------------------------------------------- base partagée (R8, R9)

    /** Interactions visibles ({@code filtreInteractions()}, jamais recopié) des commerciaux {@code cles}. */
    public Specification<ComActionMessageSuivi> base(Collection<UUID> cles) {
        Specification<ComActionMessage> interactions = perimetreService.filtreInteractions();
        return (root, query, cb) -> {
            if (cles.isEmpty()) {
                return cb.disjunction();
            }
            Subquery<UUID> visibles = query.subquery(UUID.class);
            Root<ComActionMessage> m = visibles.from(ComActionMessage.class);
            visibles.select(m.get(ID)).where(interactions.toPredicate(m, query, cb));
            return cb.and(root.get(COMMERCIAL_CLE).in(cles), root.get(ID).in(visibles));
        };
    }

    /** Somme d'un drapeau : 1 par ligne qui vérifie {@code condition}. */
    static Expression<Integer> compter(CriteriaBuilder cb, Predicate condition) {
        return cb.sum(cb.<Integer>selectCase().when(condition, 1).otherwise(0));
    }

    /** Visite de la catégorie {@code categorie} ({@link ComActionMessageSuivi#PROSPECTION} ou {@code VENTE}), R5. */
    static Predicate visite(Root<ComActionMessageSuivi> root, CriteriaBuilder cb, String categorie) {
        return cb.and(cb.equal(root.get(TYPE), InteractionKpi.TYPE_VISITE), cb.equal(root.get(CATEGORIE), categorie));
    }

    static Predicate saisieEntre(Root<ComActionMessageSuivi> root, CriteriaBuilder cb, LocalDateTime debut,
                                 LocalDateTime finExclue) {
        Path<LocalDateTime> date = root.get(DATE_SAISIE);
        return cb.and(cb.greaterThanOrEqualTo(date, debut), cb.lessThan(date, finExclue));
    }

    // ---------------------------------------------------------------- synthèse

    private record Cellule(UUID cle, LocalDate jour) {
    }

    private record Compteurs(long appelsPasses, long appelsRecus, long contactsJoints, long personnesJointes,
                             long visitesProspection, long visitesVente, long visitesPrevues, long nonConsignees) {

        static final Compteurs ZERO = new Compteurs(0, 0, 0, 0, 0, 0, 0, 0);

        Compteurs plus(Compteurs o) {
            return new Compteurs(appelsPasses + o.appelsPasses, appelsRecus + o.appelsRecus,
                    contactsJoints + o.contactsJoints, personnesJointes + o.personnesJointes,
                    visitesProspection + o.visitesProspection, visitesVente + o.visitesVente,
                    visitesPrevues + o.visitesPrevues, nonConsignees + o.nonConsignees);
        }

        LigneSyntheseSuiviDto versDto(UUID cle, String nom, LocalDate jour, Integer objectif, BigDecimal moyenne) {
            return new LigneSyntheseSuiviDto(cle, nom, jour, appelsPasses, appelsRecus, contactsJoints,
                    personnesJointes, visitesProspection, visitesVente, visitesPrevues, nonConsignees,
                    objectif != null ? personnesJointes >= objectif : null,
                    objectif != null ? quotient(personnesJointes, objectif) : null, moyenne);
        }
    }

    private static BigDecimal quotient(long numerateur, long denominateur) {
        return BigDecimal.valueOf(numerateur).divide(BigDecimal.valueOf(denominateur), 2, RoundingMode.HALF_UP);
    }

    /** Défaut : aujourd'hui ; plus de {@link #JOURS_MAX} jours ou début après fin ⇒ 400. */
    public SyntheseSuiviCommerciauxDto synthese(LocalDate du, LocalDate au, List<UUID> commercialIds, UUID projetId) {
        LocalDate fin = au != null ? au : LocalDate.now(clock);
        LocalDate debut = du != null ? du : fin;
        if (debut.isAfter(fin) || ChronoUnit.DAYS.between(debut, fin) >= JOURS_MAX) {
            throw new InvalidRequestException("Période invalide : du ≤ au, " + JOURS_MAX + " jours au plus.");
        }
        List<MembreEquipe> retenus = commerciaux.retenus(commercialIds);
        Specification<ComActionMessageSuivi> spec = base(retenus.stream().map(CommerciauxVisibles::cle).toList())
                .and((root, query, cb) -> projetId == null ? cb.conjunction() : cb.equal(root.get("projetId"), projetId));
        Map<Cellule, Compteurs> cellules = new HashMap<>();
        activite(spec, debut, fin, cellules);
        prevues(spec, debut, fin, cellules);

        Set<UUID> actifs = cellules.keySet().stream().map(Cellule::cle).collect(Collectors.toSet());
        // R10 : sans filtre, un commercial sans activité est omis ; filtré, il a une ligne à zéros par jour.
        List<MembreEquipe> affiches = CollectionUtils.isEmpty(commercialIds)
                ? retenus.stream().filter(m -> actifs.contains(CommerciauxVisibles.cle(m))).toList()
                : retenus;
        List<LocalDate> jours = debut.datesUntil(fin.plusDays(1)).toList();
        int objectif = objectifService.objectif();

        List<LigneSyntheseSuiviDto> lignes = new ArrayList<>();
        List<LigneSyntheseSuiviDto> parCommercial = new ArrayList<>();
        Map<LocalDate, Compteurs> parJour = new HashMap<>();
        Compteurs general = Compteurs.ZERO;
        for (MembreEquipe m : affiches) {
            UUID cle = CommerciauxVisibles.cle(m);
            Compteurs total = Compteurs.ZERO;
            for (LocalDate jour : jours) {
                Compteurs c = cellules.getOrDefault(new Cellule(cle, jour), Compteurs.ZERO);
                lignes.add(c.versDto(cle, m.nom(), jour, objectif, null));
                parJour.merge(jour, c, Compteurs::plus);
                total = total.plus(c);
            }
            parCommercial.add(total.versDto(cle, m.nom(), null, null, quotient(total.personnesJointes(), jours.size())));
            general = general.plus(total);
        }
        return new SyntheseSuiviCommerciauxDto(objectif, lignes, new SyntheseSuiviCommerciauxDto.TotauxSynthese(
                parCommercial,
                jours.stream().map(j -> parJour.getOrDefault(j, Compteurs.ZERO).versDto(null, null, j, null, null)).toList(),
                general.versDto(null, null, null, null, null)));
    }

    private void activite(Specification<ComActionMessageSuivi> spec, LocalDate debut, LocalDate fin,
                          Map<Cellule, Compteurs> cellules) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<ComActionMessageSuivi> root = cq.from(ComActionMessageSuivi.class);
        Path<String> type = root.get(TYPE);
        Predicate joint = cb.isTrue(root.get(JOINT));
        cq.multiselect(root.get(COMMERCIAL_CLE), root.get(JOUR),
                        compter(cb, cb.equal(type, InteractionKpi.TYPE_APPEL_SORTANT)),
                        compter(cb, cb.equal(type, InteractionKpi.TYPE_APPEL_ENTRANT)),
                        compter(cb, joint),
                        cb.countDistinct(cb.<UUID>selectCase().when(joint, root.<UUID>get("personne"))
                                .otherwise(cb.nullLiteral(UUID.class))),
                        compter(cb, visite(root, cb, ComActionMessageSuivi.PROSPECTION)),
                        compter(cb, visite(root, cb, ComActionMessageSuivi.VENTE)))
                .where(spec.toPredicate(root, cq, cb),
                        saisieEntre(root, cb, debut.atStartOfDay(), fin.plusDays(1).atStartOfDay()))
                .groupBy(root.get(COMMERCIAL_CLE), root.get(JOUR));
        for (Tuple t : em.createQuery(cq).getResultList()) {
            cellules.merge(new Cellule(t.get(0, UUID.class), t.get(1, LocalDate.class)),
                    new Compteurs(nombre(t, 2), nombre(t, 3), nombre(t, 4), nombre(t, 5), nombre(t, 6), nombre(t, 7),
                            0, 0), Compteurs::plus);
        }
    }

    /** R6 : rendez-vous à l'auteur qui les a planifiés, au jour prévu. */
    private void prevues(Specification<ComActionMessageSuivi> spec, LocalDate debut, LocalDate fin,
                         Map<Cellule, Compteurs> cellules) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<ComActionMessageSuivi> root = cq.from(ComActionMessageSuivi.class);
        Path<LocalDate> jourPrevu = root.get("jourPrevu");
        cq.multiselect(root.get(COMMERCIAL_CLE), jourPrevu, cb.count(root),
                        compter(cb, cb.isFalse(root.get("consignee"))))
                .where(spec.toPredicate(root, cq, cb),
                        root.get("resultat").in(ReglesInteraction.RESULTATS_AVEC_DATE_PLANIFIEE),
                        cb.between(jourPrevu, debut, fin))
                .groupBy(root.get(COMMERCIAL_CLE), jourPrevu);
        for (Tuple t : em.createQuery(cq).getResultList()) {
            cellules.merge(new Cellule(t.get(0, UUID.class), t.get(1, LocalDate.class)),
                    new Compteurs(0, 0, 0, 0, 0, 0, nombre(t, 2), nombre(t, 3)), Compteurs::plus);
        }
    }

    static long nombre(Tuple t, int i) {
        Number n = t.get(i, Number.class);
        return n != null ? n.longValue() : 0L;
    }

    // ---------------------------------------------------------------- liste, groupes, totaux

    public Page<LigneSuiviCommerciauxDto> lister(FiltresSuiviCommerciaux filtres, Pageable pageable) {
        Pageable trie = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), tri(pageable.getSort()));
        return repository.findAll(specification(filtres), trie).map(SuiviCommerciauxService::versDto);
    }

    public ResultatGroupement grouper(FiltresSuiviCommerciaux filtres, ColonneSuiviCommerciaux groupe,
                                      Granularite granularite, List<AgregatDemande> agregats, TriGroupe tri,
                                      boolean inclureSansValeur, Pageable pageable) {
        return groupementService.grouper(ComActionMessageSuivi.class, specification(filtres), groupe.chemin(),
                granularite, agregats, tri, inclureSansValeur, pageable);
    }

    public TotauxListeDto totauxColonnes(FiltresSuiviCommerciaux filtres, List<String> agregats) {
        GroupementService.Totaux totaux = groupementService.totaux(ComActionMessageSuivi.class,
                specification(filtres), Colonnes.resoudreAgregats(ColonneSuiviCommerciaux.class, agregats));
        return new TotauxListeDto(totaux.nombre(), totaux.valeurs());
    }

    /** 403 si {@code commercialCle} est hors de portée ; sans lui, les interactions de tous les commerciaux visibles. */
    private Specification<ComActionMessageSuivi> specification(FiltresSuiviCommerciaux f) {
        List<UUID> demandees = f.commercialCle() != null ? List.of(f.commercialCle()) : List.of();
        List<UUID> cles = commerciaux.retenus(demandees).stream().map(CommerciauxVisibles::cle).toList();
        return base(cles).and((root, query, cb) -> cb.and(predicats(root, cb, f).toArray(new Predicate[0])));
    }

    private static List<Predicate> predicats(Root<ComActionMessageSuivi> root, CriteriaBuilder cb,
                                             FiltresSuiviCommerciaux f) {
        List<Predicate> p = new ArrayList<>();
        Path<LocalDateTime> date = root.get(DATE_SAISIE);
        if (f.debut() != null) {
            p.add(cb.greaterThanOrEqualTo(date, f.debut().atStartOfDay()));
        }
        if (f.fin() != null) {
            p.add(cb.lessThan(date, f.fin().plusDays(1).atStartOfDay()));
        }
        if (f.joint() != null) {
            p.add(cb.equal(root.get(JOINT), f.joint()));
        }
        if (StringUtils.hasText(f.type())) {
            p.add(cb.equal(root.get(TYPE), f.type()));
        }
        if (StringUtils.hasText(f.search())) {
            String motif = "%" + f.search().trim().toLowerCase(Locale.ROOT) + "%";
            p.add(cb.or(List.of("personneNom", "resultat", "prospectionCode").stream()
                    .map(champ -> cb.like(cb.lower(root.<String>get(champ)), motif)).toArray(Predicate[]::new)));
        }
        return p;
    }

    /** Noms du DTO traduits en chemins ; colonne sans chemin = non triable (400), sauf {@code id}. */
    private static Sort tri(Sort demande) {
        Map<String, ColonneSuiviCommerciaux> index = Colonnes.index(ColonneSuiviCommerciaux.class);
        List<Sort.Order> ordres = new ArrayList<>();
        for (Sort.Order o : demande) {
            ColonneSuiviCommerciaux c = index.get(o.getProperty().toLowerCase(Locale.ROOT));
            if (c == ColonneSuiviCommerciaux.ID) {
                ordres.add(o.withProperty(ID));
            } else if (c != null && c.chemin() != null) {
                ordres.add(o.withProperty(c.chemin().cheminCle()));
            } else {
                throw new ColonneInconnueException(o.getProperty(), Arrays.stream(ColonneSuiviCommerciaux.values())
                        .filter(col -> col.chemin() != null).map(ColonneSuiviCommerciaux::nom).toList());
            }
        }
        if (demande.getOrderFor(ID) == null) {
            ordres.add(Sort.Order.asc(ID));
        }
        return Sort.by(ordres);
    }

    private static LigneSuiviCommerciauxDto versDto(ComActionMessageSuivi l) {
        return new LigneSuiviCommerciauxDto(l.getId(), l.getJour(), l.getDateSaisie(), l.getCommercialNom(),
                l.getType(), l.getResultat(), l.getJoint(), l.getPersonneNom(), l.getProspectionId(), l.getProjet());
    }
}
