package com.tsix.immocrm.services.patrimoine.prospectionterrain;

import com.tsix.immocrm.domain.foncier.CalculSurfacesTerrain;
import com.tsix.immocrm.domain.foncier.SurfacesTerrain;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.ApercuCalculDto;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.ExecutionTransitionDto;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.FiltresProspectionsTerrain;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.HistoriqueStatutProspectionDto;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.ProspectionTerrainDetailDto;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.ProspectionTerrainEcritureDto;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.ProspectionTerrainListeDto;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.TransitionProspectionTerrainDto;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.ZoneTerrainDto;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.ZoneTerrainEcritureDto;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.ZonesTerrainEcritureDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.entities.CodeLibelleOmran;
import com.tsix.immocrm.entities.adm.AdmCity;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.lnd.LndLand;
import com.tsix.immocrm.entities.lnd.LndLandZone;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.entities.str.StrWorkFlow;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.lnd.LndLandRepository;
import com.tsix.immocrm.repositories.lnd.LndLandZoneRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.services.patrimoine.SuppressionProjet;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Prospections terrain (FEATURE-patrimoine-prospections-terrain.md §5) ; aucune visibilité par société (§0). */
@Service
@Transactional(readOnly = true)
public class ProspectionTerrainService {

    private static final String ID = "id";
    private static final String CODE = "code";
    private static final String DESCRIPTION = "description";

    private final LndLandRepository prospections;
    private final LndLandZoneRepository zones;
    private final StkItemRepository items;
    private final EcritureProspectionTerrain ecriture;
    private final TransitionsProspectionTerrain transitions;
    private final SuppressionProjet suppression;

    ProspectionTerrainService(LndLandRepository prospections, LndLandZoneRepository zones, StkItemRepository items,
                              EcritureProspectionTerrain ecriture, TransitionsProspectionTerrain transitions,
                              SuppressionProjet suppression) {
        this.prospections = prospections;
        this.zones = zones;
        this.items = items;
        this.ecriture = ecriture;
        this.transitions = transitions;
        this.suppression = suppression;
    }

    public Page<ProspectionTerrainListeDto> lister(FiltresProspectionsTerrain f, Pageable pageable) {
        Page<LndLand> page = prospections.findAll(filtre(f), pageable);
        Map<UUID, Long> etudes = new HashMap<>();
        if (page.hasContent()) {
            prospections.etudesParProspection(page.map(LndLand::getId).getContent())
                    .forEach(l -> etudes.put((UUID) l[0], (Long) l[1]));
        }
        Map<UUID, String> types = typesDeStatut();
        return page.map(p -> ligne(p, etudes.getOrDefault(p.getId(), 0L), types));
    }

    public ProspectionTerrainDetailDto lire(UUID id) {
        return detail(exiger(id));
    }

    /** Lecture par id de toutes les routes de la prospection (notes, audit, pièces jointes comprises) : 404 sinon. */
    public LndLand exiger(UUID id) {
        return prospections.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prospection terrain introuvable: " + id));
    }

    @Transactional
    public ProspectionTerrainDetailDto creer(ProspectionTerrainEcritureDto saisie) {
        return detail(ecriture.creer(saisie, LocalDateTime.now(transitions.clock())));
    }

    @Transactional
    public ProspectionTerrainDetailDto modifier(UUID id, ProspectionTerrainEcritureDto saisie) {
        LndLand p = exiger(id);
        try {
            ecriture.modifier(p, saisie);
        } catch (RuntimeException e) {
            // Un refus ne doit rien laisser dans la session : un flush ultérieur de la transaction l'écrirait.
            ecriture.em().detach(p);
            throw e;
        }
        return detail(p);
    }

    /** {@code RessourceWithWorkFlow.cs:BeforeDelete} ; FK de yesRemsTest absentes : terrain et études contrôlés ici. */
    @Transactional
    public void supprimer(UUID id) {
        LndLand p = exiger(id);
        if (!Boolean.TRUE.equals(Champs.de(p.getStrStatus(), StrStatus::getSuppressionPermise))) {
            throw new ConflictException("Vous ne pouvez pas supprimer cette entité dans le statut : [%s] !"
                    .formatted(Champs.de(p.getStrStatus(), StrStatus::getDescription)));
        }
        items.findFirstByLndLandId(id).ifPresent(t -> {
            throw new ConflictException("Suppression impossible : le terrain %s est issu de cette prospection."
                    .formatted(t.getCode()));
        });
        suppression.prospectionTerrain(p);
    }

    @Transactional
    public List<ZoneTerrainDto> modifierZones(UUID id, ZonesTerrainEcritureDto saisie) {
        return ecriture.remplacerZones(exiger(id), saisie).stream().map(ProspectionTerrainService::zone).toList();
    }

    public SurfacesTerrain apercu(ApercuCalculDto saisie) {
        return CalculSurfacesTerrain.prospection(saisie.surfaces(), saisie.champModifie(), ecriture.maroc());
    }

    public List<TransitionProspectionTerrainDto> transitions(UUID id) {
        List<StrWorkFlow> sortantes = transitions.sortantes(exiger(id));
        Map<UUID, StrStatus> cibles = transitions.cibles(sortantes);
        return sortantes.stream().map(w -> new TransitionProspectionTerrainDto(w.getId(), w.getCode(), w.getDescription(),
                statut(cibles.get(w.getStatusToId())), Boolean.TRUE.equals(w.getWithComment()),
                Boolean.TRUE.equals(w.getWithUserDate()), Boolean.TRUE.equals(w.getWithPassword()))).toList();
    }

    @Transactional
    public ProspectionTerrainDetailDto executerTransition(UUID id, ExecutionTransitionDto saisie) {
        LndLand p = exiger(id);
        transitions.executer(p, saisie);
        ecriture.tracer(p, false);
        return detail(p);
    }

    public List<HistoriqueStatutProspectionDto> historique(UUID id) {
        LndLand p = exiger(id);
        return transitions.historiques().findByObjectIdAndStrConfigEntityIdOrderByDocDateAsc(p.getId(), p.getStrEntityId())
                .stream()
                .map(h -> new HistoriqueStatutProspectionDto(h.getId(), h.getDocDate(), h.getDescription(),
                        h.getAdmUserDescription(), h.getStrWorkflowDescription(), h.getStrStatusId(),
                        h.getStrStatusDescription(), h.getNotes(), h.getUserDate()))
                .toList();
    }

    /**
     * Motif du refus de « Transformer en terrain » ({@code Prospection.cs} front l.230-238), null si possible ; seule règle,
     * lue par la fiche et par la transformation.
     */
    public String motifNonTransformable(LndLand p, Optional<StkItem> terrain) {
        if (terrain.isPresent()) {
            return "Cette prospection est déjà transformée en terrain (%s).".formatted(terrain.get().getCode());
        }
        if (!Boolean.TRUE.equals(Champs.de(p.getStrStatus(), StrStatus::getVenteEffective))) {
            return "Le statut [%s] n'autorise pas la transformation en terrain."
                    .formatted(Champs.de(p.getStrStatus(), StrStatus::getDescription));
        }
        if (p.getArea() == null || p.getArea().signum() <= 0) {
            return "La valeur de la supérficie doit être strictement supérieure à 0 !";
        }
        return null;
    }

    // ------------------------------------------------------------------ lecture

    /** {@code ProspectionItem.GetList} l.512-523 : recherche sur code, description, ville, catégorie, type, vocation, niveau. */
    private static Specification<LndLand> filtre(FiltresProspectionsTerrain f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f.search() != null && !f.search().isBlank()) {
                String motif = "%" + f.search().trim().toLowerCase(Locale.ROOT) + "%";
                p.add(cb.or(contient(cb, root, CODE, motif), contient(cb, root, DESCRIPTION, motif),
                        contient(cb, root.join("admCity", JoinType.LEFT), DESCRIPTION, motif),
                        contient(cb, root.join("categorie", JoinType.LEFT), DESCRIPTION, motif),
                        contient(cb, root.join("type", JoinType.LEFT), DESCRIPTION, motif),
                        contient(cb, root.join("lndVocation", JoinType.LEFT), DESCRIPTION, motif),
                        contient(cb, root.join("lndLevel", JoinType.LEFT), DESCRIPTION, motif)));
            }
            egal(p, cb, root.get("isInArchive"), f.archivee());
            egal(p, cb, root.get("isTransformedToLand"), f.transformee());
            egal(p, cb, root.get("isFinished"), f.amenage());
            egal(p, cb, root.get("strStatus").get(ID), f.statutId());
            egal(p, cb, root.get("lndVocation").get(ID), f.vocationId());
            egal(p, cb, root.get("lndLevel").get(ID), f.niveauId());
            egal(p, cb, root.get("categorie").get(ID), f.categorieId());
            egal(p, cb, root.get("admCity").get(ID), f.villeId());
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    private static Predicate contient(CriteriaBuilder cb, From<?, ?> from, String champ, String motif) {
        return cb.like(cb.lower(from.get(champ)), motif);
    }

    /** Booléen faux : NULL compris (colonnes OMRAN sans défaut). */
    private static void egal(List<Predicate> p, CriteriaBuilder cb, Path<Object> champ, Object valeur) {
        if (Boolean.FALSE.equals(valeur)) {
            p.add(cb.or(cb.isNull(champ), cb.equal(champ, false)));
        } else if (valeur != null) {
            p.add(cb.equal(champ, valeur));
        }
    }

    private Map<UUID, String> typesDeStatut() {
        Map<UUID, String> types = new HashMap<>();
        prospections.typesDeStatut().forEach(l -> types.put(UUID.fromString(l[0].toString()), (String) l[1]));
        return types;
    }

    private ProspectionTerrainDetailDto detail(LndLand p) {
        Optional<StkItem> terrain = items.findFirstByLndLandId(p.getId());
        long etudes = prospections.etudesParProspection(List.of(p.getId())).stream()
                .mapToLong(l -> (Long) l[1]).sum();
        String motif = estAdministrateur() ? motifNonTransformable(p, terrain) : "Réservé à l'Administrateur.";
        return new ProspectionTerrainDetailDto(ligne(p, etudes, typesDeStatut()),
                Champs.de(p.getCategorie(), CodeLibelleOmran::getId), Champs.de(p.getType(), CodeLibelleOmran::getId),
                Champs.de(p.getLndLevel(), CodeLibelleOmran::getId), Champs.de(p.getLndVocation(), CodeLibelleOmran::getId),
                Champs.de(p.getAdmCountry(), AdmCountry::getId), Champs.de(p.getAdmCity(), AdmCity::getId),
                Champs.de(p.getCfgMiddleman(), CfgTier::getId), Champs.de(p.getCfgLawyer(), CfgTier::getId),
                tiers(p.getCfgLawyer()), Champs.de(p.getStkOrientation(), CodeLibelleOmran::getId),
                ref(p.getStkOrientation()), p.getGmapLink(), p.getZoneIdentification(),
                p.getDealerConnectionConditions(), p.getLandBase(), p.getIsTopographicSurvey(),
                zones.findByProspectionIdOrderByIdentificationAsc(p.getId()).stream()
                        .map(ProspectionTerrainService::zone).toList(),
                terrain.map(StkItem::getId).orElse(null), terrain.map(StkItem::getCode).orElse(null),
                motif == null, motif);
    }

    private static ProspectionTerrainListeDto ligne(LndLand p, long etudes, Map<UUID, String> types) {
        StrStatus s = p.getStrStatus();
        return new ProspectionTerrainListeDto(p.getId(), p.getCode(), p.getDescription(), jour(p.getDocDate()),
                ref(p.getCategorie()), ref(p.getType()), ref(p.getLndLevel()), ref(p.getLndVocation()),
                p.getAdmCountry() == null ? null : new ReferentielItemDto(p.getAdmCountry().getId(),
                        p.getAdmCountry().getCode(), p.getAdmCountry().getDescription(), null),
                p.getAdmCity() == null ? null : new ReferentielItemDto(p.getAdmCity().getId(), p.getAdmCity().getCode(),
                        p.getAdmCity().getDescription(), null),
                statut(s), Champs.de(s, StrStatus::getColorName), types.get(Champs.de(s, StrStatus::getStrStatusTypeId)),
                jour(p.getStatusValidityDate()), tiers(p.getCfgMiddleman()), p.getAdress(), p.getOwner(), p.getNotes(),
                p.getAnomalies(), p.getIsInArchive(), p.getIsTransformedToLand(), p.getIsFinished(), surfaces(p),
                p.getAlinmentStop(), p.getMaxHeight(), p.getProspectionPriceProposed(), jour(p.getRegistryDate()),
                p.getRegistryNumber(), p.getRegistryPlace(), p.getImmatriculationCPF(), jour(p.getImmatriculationDate()),
                etudes, p.getUserCreate(), p.getUserCreateDateTime(), p.getUserUpdate(), p.getUserUpdateDateTime());
    }

    private static SurfacesTerrain surfaces(LndLand p) {
        return new SurfacesTerrain(p.getArea(), p.getEquipementsArea(), p.getAreaEquipmentTransferred(),
                p.getAreaRegularized(), p.getAreaCeded(), p.getEquipementsCoef(), p.getAreaNet(), p.getCos(),
                p.getAreaCos(), p.getCuf(), p.getAreaCuf(), p.getCoefSaleableArea(), p.getSalableArea(),
                p.getCommerceCoef(), p.getCommerceArea(), p.getOfficeCoef(), p.getOfficeArea(), p.getHabitatArea(),
                p.getUndergroundParkingCoef(), p.getUndergroundParkingArea(), p.getProspectionUnitCost(),
                p.getProspectionPrice());
    }

    private static ZoneTerrainDto zone(LndLandZone z) {
        return new ZoneTerrainDto(new ZoneTerrainEcritureDto(z.getId(), z.getIdentification(), z.getArea(), z.getCos(),
                z.getAreaCos(), z.getCuf(), z.getAreaCuf(), z.getCoefSaleableArea(), z.getSalableArea(),
                z.getCommerceCoef(), z.getCommerceArea(), z.getOfficeCoef(), z.getOfficeArea(),
                z.getUndergroundParkingCoef(), z.getUndergroundParkingArea(), z.getAlinmentStop(), z.getMaxHeight(),
                z.getNotes()), z.getHabitatArea());
    }

    private static ReferentielItemDto ref(CodeLibelleOmran r) {
        return r == null ? null : new ReferentielItemDto(r.getId(), r.getCode(), r.getDescription(), null);
    }

    private static ReferentielItemDto tiers(CfgTier t) {
        return t == null ? null : new ReferentielItemDto(t.getId(), t.getCode(), t.getDescription(), null);
    }

    private static ReferentielItemDto statut(StrStatus s) {
        return s == null ? null : new ReferentielItemDto(s.getId(), s.getCode(), s.getDescription(), s.getSorting());
    }

    private static LocalDate jour(LocalDateTime t) {
        return t == null ? null : t.toLocalDate();
    }

    private static boolean estAdministrateur() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_Administrateur"::equals);
    }
}
