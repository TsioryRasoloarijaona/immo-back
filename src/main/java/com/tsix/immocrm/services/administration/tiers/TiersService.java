package com.tsix.immocrm.services.administration.tiers;

import com.tsix.immocrm.domain.FamilleTiers;
import com.tsix.immocrm.dto.administration.tiers.EcritureTiersRequest;
import com.tsix.immocrm.dto.administration.tiers.FicheTiersDto;
import com.tsix.immocrm.dto.administration.tiers.FiltresTiers;
import com.tsix.immocrm.dto.administration.tiers.LigneTiersDto;
import com.tsix.immocrm.dto.administration.tiers.NatureTiersDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.entities.CodeLibelleOmran;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierKind;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.entities.stl.StlCurrency;
import com.tsix.immocrm.exceptions.ConfigurationManquanteException;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.cfg.CfgTierKindRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierTypeRepository;
import com.tsix.immocrm.services.administration.HistoriqueService;
import com.tsix.immocrm.services.patrimoine.TracePatrimoine;
import com.tsix.immocrm.services.sequence.GenerateurCodeSequence;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Socle des familles de tiers (FEATURE-admin-tiers.md) : une famille = une entrée de {@link FamilleTiers}. */
@Service
public class TiersService {

    private static final String SELECT_DTO = "select new com.tsix.immocrm.dto.referentiel.ReferentielItemDto";
    private static final Map<String, String> REFERENTIELS = Map.of(
            "formes-sociales", SELECT_DTO + "(x.id, x.code, x.description, cast(null as Integer)) "
                    + "from AdmSocialForm x order by x.code",
            "assujettissements", SELECT_DTO + "(x.id, x.code, x.description, x.sorting) "
                    + "from AdmLiability x order by x.sorting, x.code",
            "notes", SELECT_DTO + "(x.id, x.code, x.code, x.rang) from AdmRank x order by x.rang, x.code",
            "journaux", SELECT_DTO + "(x.id, x.code, x.description, cast(null as Integer)) "
                    + "from ActJournal x order by x.code");
    private static final List<String> LIBELLES_LISTE =
            List.of("cfgTierKind", "admCountry", "admCountryResidence", "admLiability");

    private final EntityManager em;
    private final CfgTierRepository tiers;
    private final CfgTierTypeRepository types;
    private final CfgTierKindRepository natures;
    private final SaisieTiers ecriture;
    private final GenerateurCodeSequence codes;
    private final TracePatrimoine traces;

    public TiersService(EntityManager em, CfgTierRepository tiers, CfgTierTypeRepository types,
                        CfgTierKindRepository natures, SaisieTiers ecriture, GenerateurCodeSequence codes,
                        TracePatrimoine traces) {
        this.em = em;
        this.tiers = tiers;
        this.types = types;
        this.natures = natures;
        this.ecriture = ecriture;
        this.codes = codes;
        this.traces = traces;
    }

    @Transactional(readOnly = true)
    public PageDto<LigneTiersDto> lister(String famille, FiltresTiers filtres, int page, int size) {
        Page<CfgTier> lignes = tiers.findAll(specification(famille(famille), filtres),
                PageRequest.of(page, size, Sort.by("code", "id")));
        Map<UUID, String> contacts = contacts(lignes.getContent());
        return PageDto.of(lignes.map(t -> ligne(t, contacts.get(t.getId()))));
    }

    @Transactional(readOnly = true)
    public List<NatureTiersDto> natures(String famille) {
        return natures.findByCfgTierTypeCodeOrderBySortingAsc(famille(famille).codeType()).stream()
                .map(n -> new NatureTiersDto(n.getId(), n.getCode(), n.getDescription())).toList();
    }

    @Transactional(readOnly = true)
    public List<ReferentielItemDto> referentiel(String liste) {
        String jpql = REFERENTIELS.get(liste);
        if (jpql == null) {
            throw new ResourceNotFoundException("Liste inconnue : " + liste);
        }
        return em.createQuery(jpql, ReferentielItemDto.class).getResultList();
    }

    @Transactional(readOnly = true)
    public FicheTiersDto lire(String famille, UUID id) {
        return fiche(trouver(famille(famille), id));
    }

    @Transactional
    public FicheTiersDto creer(String famille, EcritureTiersRequest corps) {
        FamilleTiers f = famille(famille);
        CfgTier t = new CfgTier();
        ecriture.appliquer(t, f, corps);
        CfgTierType type = types.findByCode(f.codeType())
                .orElseThrow(() -> new ConfigurationManquanteException("Type de tiers absent : " + f.codeType()));
        TracePatrimoine.Trace trace = traces.maintenant();
        t.setId(UUID.randomUUID());
        if (!f.fiche().codeSaisi()) {
            t.setCode(codes.suivant(GenerateurCodeSequence.SEQUENCE_TIER, 2, 5));
        }
        t.setCfgTierType(type);
        t.setIsProspect(false);
        t.setIsOldCustomer(false);
        t.setIsRntCustomer(false);
        t.setIsVisibleInList(false);
        t.setIsFundReader(false);
        t.setUserCreate(trace.auteur());
        t.setUserCreateDateTime(trace.le());
        em.persist(t);
        return fiche(t);
    }

    @Transactional
    public FicheTiersDto modifier(String famille, UUID id, EcritureTiersRequest corps) {
        FamilleTiers f = famille(famille);
        CfgTier t = trouver(f, id);
        try {
            ecriture.appliquer(t, f, corps);
        } catch (InvalidRequestException e) {
            em.detach(t);
            throw e;
        }
        TracePatrimoine.Trace trace = traces.maintenant();
        t.setUserUpdate(trace.auteur());
        t.setUserUpdateDateTime(trace.le());
        return fiche(t);
    }

    /** Les CASCADE de la base partent avec le tiers ; une FK NO_ACTION le bloque, sans purge (décision du spec). */
    @Transactional
    public void supprimer(String famille, UUID id) {
        em.remove(trouver(famille(famille), id));
        try {
            em.flush();
        } catch (ConstraintViolationException e) {
            em.clear();
            throw new ConflictException("Suppression impossible : ce tiers est encore référencé ailleurs.");
        }
    }

    private static FamilleTiers famille(String segment) {
        return FamilleTiers.depuis(segment)
                .orElseThrow(() -> new ResourceNotFoundException("Famille de tiers inconnue : " + segment));
    }

    private CfgTier trouver(FamilleTiers famille, UUID id) {
        return tiers.findById(id)
                .filter(t -> t.getCfgTierType() != null && famille.codeType().equals(t.getCfgTierType().getCode()))
                .orElseThrow(() -> new ResourceNotFoundException("Tiers introuvable : " + id));
    }

    private static Specification<CfgTier> specification(FamilleTiers famille, FiltresTiers filtres) {
        return (racine, requete, cb) -> {
            if (requete.getResultType() != Long.class) {
                LIBELLES_LISTE.forEach(a -> racine.fetch(a, JoinType.LEFT));
            }
            List<Predicate> conditions = new ArrayList<>();
            conditions.add(cb.equal(racine.get("cfgTierType").get("code"), famille.codeType()));
            if (StringUtils.hasText(filtres.recherche())) {
                conditions.add(recherche(racine, cb, famille, filtres.recherche()));
            }
            if (famille.filtreNature() && filtres.natureId() != null) {
                conditions.add(cb.equal(racine.get("cfgTierKind").get("id"), filtres.natureId()));
            }
            return cb.and(conditions.toArray(Predicate[]::new));
        };
    }

    private static Predicate recherche(Root<CfgTier> racine, CriteriaBuilder cb, FamilleTiers famille, String recherche) {
        String motif = Objects.requireNonNull(HistoriqueService.motif(recherche.strip())).toLowerCase(Locale.ROOT);
        return cb.or(famille.fiche().colonnesCherchees().stream().map(c -> cb.like(cb.lower(racine.get(c)), motif))
                .toArray(Predicate[]::new));
    }

    private Map<UUID, String> contacts(List<CfgTier> lignes) {
        if (lignes.isEmpty()) {
            return Map.of();
        }
        return em.createQuery("""
                        select l.cfgTier.id, c.description from CfgTierContact l join l.cfgContact c
                        where l.cfgTier.id in :ids and c.description is not null order by c.description""", Object[].class)
                .setParameter("ids", lignes.stream().map(CfgTier::getId).toList()).getResultList().stream()
                .collect(Collectors.groupingBy(l -> (UUID) l[0], Collectors.mapping(l -> (String) l[1], Collectors.joining(", "))));
    }

    private static LigneTiersDto ligne(CfgTier t, String contacts) {
        return new LigneTiersDto(t.getId(), t.getCode(), t.getDescription(), t.getSocialReason(), contacts, t.getPhone(),
                t.getFax(), t.getMobile(), t.getEMail(), t.getWebSite(), t.getAddress(), t.getNotes(), t.getIsExtern(),
                t.getIce(), libelle(t.getCfgTierKind(), CfgTierKind::getDescription),
                libelle(t.getAdmCountry(), AdmCountry::getDescription),
                libelle(t.getAdmCountryResidence(), AdmCountry::getDescription), t.getBankAgency(), t.getRib(), t.getIban(),
                t.getFiscalCode(), t.getFiscalRegistry(), t.getFiscalDouane(), t.getCodeCountable(),
                t.getCodeCountableCollective(), libelle(t.getAdmLiability(), CodeLibelleOmran::getDescription),
                t.getPhone1(), t.getPhone2());
    }

    private static FicheTiersDto fiche(CfgTier t) {
        return new FicheTiersDto(t.getId(), t.getUserCreate(), t.getUserCreateDateTime(),
                t.getUserUpdate(), t.getUserUpdateDateTime(), EcritureTiers.champs(t),
                libelle(t.getCfgTierKind(), CfgTierKind::getDescription),
                libelle(t.getAdmCountry(), AdmCountry::getDescription),
                libelle(t.getAdmSocialForm(), CodeLibelleOmran::getDescription),
                libelle(t.getStlCurrency(), StlCurrency::getDescription));
    }

    private static <T> String libelle(T objet, Function<T, String> lecture) {
        return Optional.ofNullable(objet).map(lecture).orElse(null);
    }
}
