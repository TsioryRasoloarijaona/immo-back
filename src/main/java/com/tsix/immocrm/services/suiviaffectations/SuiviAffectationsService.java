package com.tsix.immocrm.services.suiviaffectations;

import com.tsix.immocrm.domain.AgregatDemande;
import com.tsix.immocrm.domain.ColonneInconnueException;
import com.tsix.immocrm.domain.ColonneSuiviAffectations;
import com.tsix.immocrm.domain.Colonnes;
import com.tsix.immocrm.domain.Granularite;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.domain.StatutReglement;
import com.tsix.immocrm.domain.TriGroupe;
import com.tsix.immocrm.dto.common.TotauxListeDto;
import com.tsix.immocrm.dto.suiviaffectations.LigneSuiviAffectationsDto;
import com.tsix.immocrm.dto.suiviaffectations.StatutSuiviAffectationsDto;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stl.StlSettlementLineSuivi;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.stl.StlSettlementLineSuiviRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import com.tsix.immocrm.services.groupement.GroupementService;
import com.tsix.immocrm.services.groupement.GroupementService.ResultatGroupement;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.TierNomFormatter;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tsix.immocrm.entities.stl.StlSettlementLineSuivi.SANS_ETAT;
import static com.tsix.immocrm.entities.stl.StlSettlementLineSuivi.SANS_STATUT;

/**
 * Suivi des affectations (FEATURE-suivi-affectations.md) : liste, groupes, totaux et export partent de
 * {@link #specification}, filtres et visibilité ligne par ligne compris (R6, R7).
 */
@Service
@Transactional(readOnly = true)
public class SuiviAffectationsService {

    private static final String SANS_CATEGORIE = "SANS_CATEGORIE";
    private static final String ID = "id";
    private static final String CATEGORIE_ID = "categorieId";

    private final StlSettlementLineSuiviRepository repository;
    private final PerimetreCommercialService perimetreService;
    private final GroupementService groupementService;
    private final StrStatusRepository statusRepository;

    public SuiviAffectationsService(StlSettlementLineSuiviRepository repository,
                                    PerimetreCommercialService perimetreService, GroupementService groupementService,
                                    StrStatusRepository statusRepository) {
        this.repository = repository;
        this.perimetreService = perimetreService;
        this.groupementService = groupementService;
        this.statusRepository = statusRepository;
    }

    /** Groupe déplié de {@code /groupes} : sa clé réémise telle quelle, même expression que le comptage. */
    public record Depliage(ColonneSuiviAffectations colonne, String valeur, Granularite granularite) {

        Specification<StlSettlementLineSuivi> restreindre(Specification<StlSettlementLineSuivi> spec) {
            return spec.and((root, query, cb) -> GroupementService.predicatDeGroupe(root, cb, colonne.chemin(),
                    granularite, valeur));
        }
    }

    public Page<LigneSuiviAffectationsDto> lister(FiltresSuiviAffectations filtres, Depliage depliage,
                                                  Pageable pageable) {
        Sort ordre = tri(pageable.getSort());
        Specification<StlSettlementLineSuivi> base = specification(filtres);
        return repository.findAll(depliage == null ? base : depliage.restreindre(base),
                        PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), ordre))
                .map(SuiviAffectationsService::versDto);
    }

    public ResultatGroupement grouper(FiltresSuiviAffectations filtres, ColonneSuiviAffectations groupe,
                                      Granularite granularite, List<AgregatDemande> agregats, TriGroupe tri,
                                      boolean inclureSansValeur, Pageable pageable) {
        return groupementService.grouper(StlSettlementLineSuivi.class, specification(filtres), groupe.chemin(),
                granularite, agregats, tri, inclureSansValeur, pageable);
    }

    public TotauxListeDto totauxColonnes(FiltresSuiviAffectations filtres, List<AgregatDemande> agregats) {
        GroupementService.Totaux totaux = groupementService.totaux(StlSettlementLineSuivi.class,
                specification(filtres), agregats);
        return new TotauxListeDto(totaux.nombre(), totaux.valeurs());
    }

    /** Tous les statuts de dossier de vente, sorties comprises (D9 : aucun filtrage par intervenant). */
    public List<StatutSuiviAffectationsDto> statuts() {
        return statutsDossier().stream()
                .sorted(Comparator.comparing(StrStatus::getSorting, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(s -> new StatutSuiviAffectationsDto(s.getCode(), s.getDescription()))
                .toList();
    }

    private List<StrStatus> statutsDossier() {
        return statusRepository.findByStrConfigEntityId(StatutDossier.CONFIG_ENTITY_ID);
    }

    // ---------------------------------------------------------------- base partagée (R5–R7)

    private Specification<StlSettlementLineSuivi> specification(FiltresSuiviAffectations f) {
        if (f.du() != null && f.au() != null && f.du().isAfter(f.au())) {
            throw new InvalidRequestException("Période invalide : du (" + f.du() + ") après au (" + f.au() + ").");
        }
        Set<String> statuts = codes(f.statut(), () -> Stream.concat(Stream.of(SANS_STATUT),
                statutsDossier().stream().map(StrStatus::getCode)).toList(), "Statut");
        Set<String> etats = codes(f.etatPiece(), () -> Stream.concat(Stream.of(SANS_ETAT),
                Arrays.stream(StatutReglement.values()).map(Enum::name)).toList(), "État de pièce");
        Categories categories = Categories.de(f.categorieId());
        Specification<StlSettlementLineSuivi> visibles = perimetreService.filtreParTranche("tranche");
        return visibles.and((root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            dansLaListe(p, root.get("entiteId"), f.entiteId());
            dansLaListe(p, root.get("tranche").get(ID), f.trancheId());
            dansLaListe(p, root.get("typePieceId"), f.typeId());
            dansLaListe(p, cb.upper(root.<String>get("statutDossierCode")), statuts);
            dansLaListe(p, cb.upper(root.<String>get("etatPiece")), etats);
            if (f.projetId() != null) {
                p.add(cb.equal(root.get("projetId"), f.projetId()));
            }
            if (f.sens() != FiltresSuiviAffectations.Sens.TOUS) {
                p.add(cb.equal(root.get("sens"), f.sens().name()));
            }
            categories.predicat(root, cb).ifPresent(p::add);
            if (StringUtils.hasText(f.search())) {
                String motif = "%" + f.search().replace(" ", "").toLowerCase(Locale.ROOT) + "%";
                p.add(cb.like(cb.lower(root.<String>get("recherche")), motif));
            }
            periode(p, root, cb, f.du(), f.au());
            return cb.and(p.toArray(new Predicate[0]));
        });
    }

    /** Codes en majuscules ; un code hors {@code valides} (lus seulement si un code est demandé) rend 400. */
    private static Set<String> codes(List<String> demandes, Supplier<List<String>> lecture, String quoi) {
        if (CollectionUtils.isEmpty(demandes)) {
            return Set.of();
        }
        List<String> valides = lecture.get();
        Set<String> admis = valides.stream().map(SuiviAffectationsService::majuscules).collect(Collectors.toSet());
        Set<String> codes = demandes.stream().map(SuiviAffectationsService::majuscules)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        codes.stream().filter(c -> !admis.contains(c)).findFirst().ifPresent(inconnu -> {
            throw new InvalidRequestException(quoi + " inconnu : " + inconnu + ". Valeurs : " + valides);
        });
        return codes;
    }

    private static String majuscules(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

    private static void dansLaListe(List<Predicate> p, Expression<?> chemin, Collection<?> valeurs) {
        if (!CollectionUtils.isEmpty(valeurs)) {
            p.add(chemin.in(valeurs));
        }
    }

    /** {@code du} / {@code au} : jours entiers inclus ; une ligne sans date tombe d'elle-même (R4). */
    private static void periode(List<Predicate> p, Root<StlSettlementLineSuivi> root, CriteriaBuilder cb,
                                LocalDate du, LocalDate au) {
        Path<LocalDateTime> date = root.get("dateMouvement");
        if (du != null) {
            p.add(cb.greaterThanOrEqualTo(date, du.atStartOfDay()));
        }
        if (au != null) {
            p.add(cb.lessThan(date, au.plusDays(1).atStartOfDay()));
        }
    }

    /** Filtre catégorie : ids et/ou {@link #SANS_CATEGORIE} (OR) ; autre valeur que ces deux formes = 400. */
    private record Categories(List<UUID> ids, boolean sansCategorie) {

        static Categories de(List<String> demandes) {
            List<UUID> ids = new ArrayList<>();
            boolean sans = false;
            for (String valeur : demandes == null ? List.<String>of() : demandes) {
                String v = valeur.trim();
                if (SANS_CATEGORIE.equalsIgnoreCase(v)) {
                    sans = true;
                } else {
                    ids.add(uuid(v));
                }
            }
            return new Categories(ids, sans);
        }

        private static UUID uuid(String valeur) {
            try {
                return UUID.fromString(valeur);
            } catch (IllegalArgumentException e) {
                throw new InvalidRequestException("Catégorie inconnue : " + valeur + ". Attendu : un id ou "
                        + SANS_CATEGORIE + ".");
            }
        }

        Optional<Predicate> predicat(Root<StlSettlementLineSuivi> root, CriteriaBuilder cb) {
            List<Predicate> ou = new ArrayList<>();
            if (!ids.isEmpty()) {
                ou.add(root.get(CATEGORIE_ID).in(ids));
            }
            if (sansCategorie) {
                ou.add(cb.isNull(root.get(CATEGORIE_ID)));
            }
            return ou.isEmpty() ? Optional.empty() : Optional.of(cb.or(ou.toArray(new Predicate[0])));
        }
    }

    /** Noms du DTO traduits en chemins de l'entité ; colonne sans chemin = non triable (400), {@code id} admis. */
    private static Sort tri(Sort demande) {
        Map<String, ColonneSuiviAffectations> index = Colonnes.index(ColonneSuiviAffectations.class);
        List<Sort.Order> ordres = new ArrayList<>();
        for (Sort.Order o : demande) {
            if (ID.equals(o.getProperty())) {
                ordres.add(o);
                continue;
            }
            ColonneSuiviAffectations c = index.get(o.getProperty().toLowerCase(Locale.ROOT));
            if (c == null || c.chemin() == null) {
                throw new ColonneInconnueException(o.getProperty(), Arrays.stream(ColonneSuiviAffectations.values())
                        .filter(col -> col.chemin() != null).map(ColonneSuiviAffectations::nom).toList());
            }
            ordres.add(o.withProperty(c.chemin().cheminCle()));
        }
        if (ordres.stream().noneMatch(o -> ID.equals(o.getProperty()))) {
            ordres.add(Sort.Order.asc(ID));
        }
        return Sort.by(ordres);
    }

    // ---------------------------------------------------------------- restitution

    private static LigneSuiviAffectationsDto versDto(StlSettlementLineSuivi l) {
        return new LigneSuiviAffectationsDto(l.getId(), jour(l.getDateMouvement()),
                l.getCategorieId(), l.getCategorieCode(), l.getCategorieLibelle(),
                l.getEntiteId(), l.getEntiteCode(), l.getEntiteLibelle(), l.getProjetId(), l.getProjetLibelle(),
                Champs.de(l.getTranche(), CfgTranche::getId), l.getTrancheCode(), l.getTrancheLibelle(),
                l.getArticleCode(), Champs.de(l.getTiers(), CfgTier::getId), TierNomFormatter.nom(l.getTiers()),
                l.getMontantAffecte(), l.getSens(), l.getDossierId(), l.getDossierCode(),
                l.getStatutDossierCode(), l.getStatutDossierLibelle(),
                l.getPieceId(), Boolean.TRUE.equals(l.getPieceOuvrable()), l.getPieceLibelle(), l.getPieceReference(), l.getRecuReference(),
                jour(l.getDatePiece()), jour(l.getDateEcheancePiece()), jour(l.getDateOperationPiece()),
                l.getEtatPiece(), l.getMontantPiece(), l.getTypePieceId(), l.getTypePieceCode(),
                l.getTypePieceLibelle(), TierNomFormatter.nom(l.getBanque()), l.getCompteCode(),
                l.getCompteLibelle(), l.getNotesPiece(), jour(l.getDateEcheance()), l.getMontantEcheance(),
                l.getNotesEcheance());
    }

    private static LocalDate jour(LocalDateTime date) {
        return date == null ? null : date.toLocalDate();
    }
}
