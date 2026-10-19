package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.entite.ActionnaireDto;
import com.tsix.immocrm.dto.entite.ActionnaireEcritureDto;
import com.tsix.immocrm.dto.entite.GroupeActionnairesDto;
import com.tsix.immocrm.dto.entite.GroupeActionnairesEcritureDto;
import com.tsix.immocrm.dto.entite.LigneGroupeDto;
import com.tsix.immocrm.entities.AuditOmran;
import com.tsix.immocrm.entities.CodeLibelleOmran;
import com.tsix.immocrm.entities.cfg.CfgGroupShareholder;
import com.tsix.immocrm.entities.cfg.CfgGroupShareholderLine;
import com.tsix.immocrm.entities.cfg.CfgShareholder;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.utils.Champs;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.IntStream;

import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.fraction;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.obligatoire;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.texte;

/** Actionnaires et groupes d'actionnaires (FEATURE-entites-juridiques.md §2) ; écrans OMRAN ACTIONNAIRES / GROUPES. */
@Service
@Transactional(readOnly = true)
public class ActionnairesService {

    private static final String SOMME = "La somme des pourcentages doit être égale à 100 % !";
    private static final String CODE = "Le code";
    private static final String DESCRIPTION = "La description";
    private static final Sort PAR_CODE = Sort.by("code");

    private final DepotsEntites depots;
    private final TracePatrimoine trace;

    public ActionnairesService(DepotsEntites depots, TracePatrimoine trace) {
        this.depots = depots;
        this.trace = trace;
    }

    // ------------------------------------------------------------------ actionnaires

    public List<ActionnaireDto> actionnaires() {
        return depots.actionnaires().findAll(PAR_CODE).stream().map(ActionnairesService::toDto).toList();
    }

    public ActionnaireDto actionnaire(UUID id) {
        return toDto(depots.actionnaire(id));
    }

    @Transactional
    public ActionnaireDto creerActionnaire(ActionnaireEcritureDto saisie) {
        CfgShareholder a = new CfgShareholder();
        a.setId(UUID.randomUUID());
        tracer(a, true);
        return toDto(depots.actionnaires().save(appliquer(a, saisie)));
    }

    @Transactional
    public ActionnaireDto modifierActionnaire(UUID id, ActionnaireEcritureDto saisie) {
        CfgShareholder a = depots.actionnaire(id);
        tracer(a, false);
        return toDto(depots.actionnaires().save(appliquer(a, saisie)));
    }

    /** Cité par une répartition (NO_ACTION en base) ou une ligne de groupe (CASCADE en base, refusé ici) → 409. */
    @Transactional
    public void supprimerActionnaire(UUID id) {
        CfgShareholder a = depots.actionnaire(id);
        if (depots.repartitions().existsByCfgShareholder_Id(id) || depots.lignesGroupe().existsByCfgShareholder_Id(id)) {
            throw new ConflictException("Suppression impossible : l'actionnaire %s figure dans une répartition ou un groupe."
                    .formatted(a.getCode()));
        }
        depots.actionnaires().delete(a);
    }

    private CfgShareholder appliquer(CfgShareholder a, ActionnaireEcritureDto d) {
        a.setCode(codeLibre(obligatoire(d.code(), a.getCode(), CODE), a.getId(), depots.actionnaires()::findFirstByCode,
                "l'actionnaire"));
        a.setDescription(obligatoire(d.description(), a.getDescription(), DESCRIPTION));
        a.setNotes(texte(d.notes(), a.getNotes(), 500, "Les notes"));
        return a;
    }

    // ------------------------------------------------------------------ groupes

    public List<GroupeActionnairesDto> groupes() {
        return depots.groupes().findAll(PAR_CODE).stream().map(this::toDto).toList();
    }

    public GroupeActionnairesDto groupe(UUID id) {
        return toDto(depots.groupe(id));
    }

    @Transactional
    public GroupeActionnairesDto creerGroupe(GroupeActionnairesEcritureDto saisie) {
        CfgGroupShareholder g = new CfgGroupShareholder();
        g.setId(UUID.randomUUID());
        tracer(g, true);
        return enregistrerGroupe(g, saisie, Objects.requireNonNullElse(saisie.lignes(), List.of()));
    }

    @Transactional
    public GroupeActionnairesDto modifierGroupe(UUID id, GroupeActionnairesEcritureDto saisie) {
        CfgGroupShareholder g = depots.groupe(id);
        tracer(g, false);
        return enregistrerGroupe(g, saisie, saisie.lignes());
    }

    /** Groupe choisi par une entité → 409 : en base, {@code CfgCompany → CfgGroupShareholder} est ON DELETE CASCADE. */
    @Transactional
    public void supprimerGroupe(UUID id) {
        CfgGroupShareholder g = depots.groupe(id);
        if (depots.entites().existsByCfgGroupShareholder_Id(id)) {
            throw new ConflictException("Suppression impossible : le groupe %s est choisi par une entité juridique."
                    .formatted(g.getCode()));
        }
        depots.lignesGroupe().deleteAll(lignes(id));
        depots.groupes().delete(g);
    }

    /** {@code lignes} null = inchangées ; sinon contrôlées ({@code CfgGroupShareholder.Verif}) puis remplacées. */
    private GroupeActionnairesDto enregistrerGroupe(CfgGroupShareholder g, GroupeActionnairesEcritureDto d,
                                                   List<LigneGroupeDto> lignes) {
        g.setCode(codeLibre(obligatoire(d.code(), g.getCode(), CODE), g.getId(), depots.groupes()::findFirstByCode,
                "le groupe"));
        g.setDescription(obligatoire(d.description(), g.getDescription(), DESCRIPTION));
        List<CfgGroupShareholderLine> nouvelles = lignes == null ? null : nouvellesLignes(g, lignes);
        depots.groupes().save(g);
        if (nouvelles != null) {
            depots.lignesGroupe().deleteAll(lignes(g.getId()));
            depots.lignesGroupe().saveAll(nouvelles);
        }
        return toDto(g);
    }

    private List<CfgGroupShareholderLine> nouvellesLignes(CfgGroupShareholder g, List<LigneGroupeDto> saisies) {
        EntiteJuridiqueService.sansDoublon(saisies.stream().map(LigneGroupeDto::actionnaireId).toList());
        // decimal(26,6) en base : la somme se contrôle sur les valeurs arrondies comme elles seront stockées.
        List<BigDecimal> pourcentages = saisies.stream()
                .map(s -> Objects.requireNonNullElse(fraction(s.pourcentage(), "pourcentage"), BigDecimal.ZERO)
                        .setScale(6, RoundingMode.HALF_UP))
                .toList();
        if (pourcentages.stream().reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(BigDecimal.ONE) != 0) {
            throw new InvalidRequestException(SOMME);
        }
        TracePatrimoine.Trace t = trace.maintenant();
        return IntStream.range(0, saisies.size()).mapToObj(i -> {
            CfgGroupShareholderLine l = new CfgGroupShareholderLine();
            l.setId(UUID.randomUUID());
            l.setCfgGroupShareholder(g);
            l.setCfgShareholder(ReferencesPatrimoine.trouver(depots.actionnaires()::findById, saisies.get(i).actionnaireId(),
                    EntiteJuridiqueService.ACTIONNAIRE_ID));
            l.setSharingNumber(pourcentages.get(i));
            EntiteJuridiqueService.tracer(l, t);
            return l;
        }).toList();
    }

    private List<CfgGroupShareholderLine> lignes(UUID groupeId) {
        return depots.lignesGroupe().findByCfgGroupShareholder_IdOrderByCfgShareholder_CodeAsc(groupeId);
    }

    // ------------------------------------------------------------------ commun

    /** Code unique de la table : déjà pris par une autre ligne → 409. */
    static <T extends CodeLibelleOmran> String codeLibre(String code, UUID id, Function<String, Optional<T>> parCode,
                                                         String objet) {
        parCode.apply(code).filter(existant -> !existant.getId().equals(id)).ifPresent(existant -> {
            throw new ConflictException("Le code %s est déjà pris par %s %s.".formatted(code, objet, existant.getDescription()));
        });
        return code;
    }

    private void tracer(AuditOmran objet, boolean creation) {
        TracePatrimoine.Trace t = trace.maintenant();
        if (creation) {
            objet.setUserCreate(t.auteur());
            objet.setUserCreateDateTime(t.le());
        }
        objet.setUserUpdate(t.auteur());
        objet.setUserUpdateDateTime(t.le());
    }

    private static ActionnaireDto toDto(CfgShareholder a) {
        return new ActionnaireDto(a.getId(), a.getCode(), a.getDescription(), a.getNotes(), a.getUserCreate(),
                a.getUserCreateDateTime(), a.getUserUpdate(), a.getUserUpdateDateTime());
    }

    private GroupeActionnairesDto toDto(CfgGroupShareholder g) {
        List<LigneGroupeDto> lignes = lignes(g.getId()).stream()
                .map(l -> new LigneGroupeDto(Champs.de(l.getCfgShareholder(), CfgShareholder::getId),
                        Champs.de(l.getCfgShareholder(), CfgShareholder::getCode),
                        Champs.de(l.getCfgShareholder(), CfgShareholder::getDescription), l.getSharingNumber()))
                .toList();
        return new GroupeActionnairesDto(g.getId(), g.getCode(), g.getDescription(), lignes, g.getUserCreate(),
                g.getUserCreateDateTime(), g.getUserUpdate(), g.getUserUpdateDateTime());
    }
}
