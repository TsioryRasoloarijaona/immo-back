package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.entities.act.ActAccount;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTrancheStanding;
import com.tsix.immocrm.entities.lnd.LndLevel;
import com.tsix.immocrm.entities.prj.PrjProject;
import com.tsix.immocrm.entities.stk.StkTrancheHeading;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.act.ActAccountRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheStandingRepository;
import com.tsix.immocrm.repositories.lnd.LndLevelRepository;
import com.tsix.immocrm.repositories.prj.PrjProjectRepository;
import com.tsix.immocrm.repositories.stk.StkTrancheHeadingRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Référentiels de la fiche programme / tranche (FEATURE-patrimoine-tranche-fiche.md §2) : listes de choix et
 * résolution d'une clé étrangère saisie (inconnue → 400 nommant le champ).
 */
@Component
public record ReferencesPatrimoine(CfgTierRepository tiers, CfgTrancheStandingRepository standingsRepository,
                                   LndLevelRepository niveaux, StkTrancheHeadingRepository rubriques,
                                   ActAccountRepository comptes, PrjProjectRepository projets) {

    /** PKey de {@code CfgTierType} EMP (constante OMRAN {@code CfgTierType.Employee}). */
    public static final UUID TYPE_EMPLOYE = UUID.fromString("EEB3FCFF-15BE-484D-B785-923D7587E649");
    /** PKey de {@code CfgTierType} FOU : le sélecteur « Tiers autrui » est {@code IsSupplier}. */
    public static final UUID TYPE_FOURNISSEUR = UUID.fromString("00685105-DFD9-497B-8830-DBF0FBE91668");

    private static final Sort PAR_CODE = Sort.by("code");

    public List<ReferentielItemDto> standings() {
        return standingsRepository.findAll(PAR_CODE).stream()
                .map(s -> new ReferentielItemDto(s.getId(), s.getCode(), s.getDescription(), null)).toList();
    }

    public List<ReferentielItemDto> niveauxTerrain() {
        return niveaux.findAll(PAR_CODE).stream()
                .map(n -> new ReferentielItemDto(n.getId(), n.getCode(), n.getDescription(), null)).toList();
    }

    public List<ReferentielItemDto> rubriquesTranche() {
        return rubriques.findAll(Sort.by("sorting", "code")).stream()
                .map(r -> new ReferentielItemDto(r.getId(), r.getCode(), r.getDescription(), r.getSorting())).toList();
    }

    public List<ReferentielItemDto> plansComptables() {
        return comptes.findAll(PAR_CODE).stream()
                .map(a -> new ReferentielItemDto(a.getId(), a.getCode(), a.getDescription(), null)).toList();
    }

    public List<ReferentielItemDto> projetsFacturation(UUID entiteId) {
        return projets.desEntitesActives(entiteId).stream()
                .map(p -> new ReferentielItemDto(p.getId(), p.getCode(), p.getDescription(), null)).toList();
    }

    public List<ReferentielItemDto> personnels() {
        return tiersDuType(TYPE_EMPLOYE);
    }

    public List<ReferentielItemDto> tiersAutrui() {
        return tiersDuType(TYPE_FOURNISSEUR);
    }

    private List<ReferentielItemDto> tiersDuType(UUID type) {
        return tiers.findByCfgTierType_IdOrderByDescriptionAsc(type).stream()
                .map(t -> new ReferentielItemDto(t.getId(), t.getCode(), t.getDescription(), null)).toList();
    }

    CfgTier tiers(UUID id, String champ) {
        return trouver(tiers, id, champ);
    }

    CfgTrancheStanding standing(UUID id, String champ) {
        return trouver(standingsRepository, id, champ);
    }

    LndLevel niveau(UUID id, String champ) {
        return trouver(niveaux, id, champ);
    }

    StkTrancheHeading rubrique(UUID id, String champ) {
        return trouver(rubriques, id, champ);
    }

    ActAccount compte(UUID id, String champ) {
        return trouver(comptes, id, champ);
    }

    PrjProject projet(UUID id, String champ) {
        return trouver(projets, id, champ);
    }

    private static <T> T trouver(CrudRepository<T, UUID> depot, UUID id, String champ) {
        return trouver(depot::findById, id, champ);
    }

    /** Clé étrangère saisie : inconnue → 400 nommant le champ. */
    static <T> T trouver(Function<UUID, Optional<T>> lecture, UUID id, String champ) {
        return lecture.apply(id)
                .orElseThrow(() -> new InvalidRequestException("%s : référence inconnue (%s).".formatted(champ, id)));
    }
}
