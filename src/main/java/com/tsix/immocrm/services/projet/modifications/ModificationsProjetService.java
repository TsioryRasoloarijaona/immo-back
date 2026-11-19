package com.tsix.immocrm.services.projet.modifications;

import com.tsix.immocrm.domain.EtatModification;
import com.tsix.immocrm.dto.dossier.modifications.ModificationRequest;
import com.tsix.immocrm.dto.projet.modifications.EtatModificationProjetDto;
import com.tsix.immocrm.dto.projet.modifications.FiltresModifications;
import com.tsix.immocrm.dto.projet.modifications.ModificationProjetLigneDto;
import com.tsix.immocrm.dto.projet.modifications.ReferentielsModificationsDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.services.dossier.modifications.ModificationsService;
import com.tsix.immocrm.services.droits.DroitsOmran;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.services.projet.travaux.RequeteNative;
import com.tsix.immocrm.services.projet.travaux.ValeursNatives;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Modifications (FEATURE-n2-travaux-chantier.md) : liste et fiche R7, PUT délégué à l'onglet du dossier (D6). */
@Service
@Transactional(readOnly = true)
public class ModificationsProjetService {

    static final String LIRE = "Yes.Prj.Front.Modification.ModificationList";
    static final String MODIFIER = LIRE + ".toolNew";

    // Modification.cs:GetList : article racine relié par StkItem.ComFolderId (lien retour), tranche habilitée.
    private static final String SELECT = """
            SELECT m.PKey, m.ComFolderId, m.Status, m.DemandDate, COALESCE(tr.Description, tr.Code),
                COALESCE(cat.Description, cat.Code), COALESCE(ty.Description, ty.Code), i.Code,
                COALESCE(b.Description, b.Code), f.Sorting, m.Description, %s,
                COALESCE(NULLIF(c.Mobile, N''), NULLIF(c.Phone, N''), NULLIF(c.Phone1, N''), NULLIF(c.Phone2, N'')),
                st.Description,
                m.TechnicalValidationDate, m.CustomerValidationDate, m.Cost, m.CommercialAmountHT, m.CommercialAmount,
                m.Notes
            FROM ComModification m
            JOIN ComFolder d ON d.PKey = m.ComFolderId
            JOIN StkItem i ON i.ComFolderId = d.PKey AND i.IsUnderItem = 0
            JOIN CfgTranche tr ON tr.PKey = i.StkHierarchyId
            LEFT JOIN CfgProject pr ON pr.PKey = tr.CfgProjectId
            LEFT JOIN CfgCompany soc ON soc.PKey = pr.CfgCompanyId
            LEFT JOIN StkItemCategory cat ON cat.PKey = i.StkItemCategoryId
            LEFT JOIN StkItemType ty ON ty.PKey = i.StkItemTypeId
            LEFT JOIN StkBuilding b ON b.PKey = i.StkBuildingId
            LEFT JOIN StkFloor f ON f.PKey = i.StkFloorId
            LEFT JOIN CfgTier c ON c.PKey = d.CfgCustomerId
            LEFT JOIN StrStatus st ON st.PKey = d.StrStatusId
            WHERE (soc.IsActive = 1 OR soc.PKey IS NULL)""".formatted(ValeursNatives.libelleTiers("c"));
    private static final String RECHERCHE = " AND (m.Description LIKE :recherche OR m.Notes LIKE :recherche"
            + " OR i.Code LIKE :recherche" + ValeursNatives.rechercheTiers("c") + ")";

    private final EntityManager em;
    private final DroitsOmran droits;
    private final AdmUserHabilitationRepository habilitations;
    private final PerimetreCommercialService perimetreCommercial;
    private final ModificationsService modifications;

    public ModificationsProjetService(EntityManager em, DroitsOmran droits, AdmUserHabilitationRepository habilitations,
                                      PerimetreCommercialService perimetreCommercial,
                                      ModificationsService modifications) {
        this.em = em;
        this.droits = droits;
        this.habilitations = habilitations;
        this.perimetreCommercial = perimetreCommercial;
        this.modifications = modifications;
    }

    public List<ModificationProjetLigneDto> liste(FiltresModifications filtres) {
        droits.exiger(LIRE);
        return lire(filtres, null);
    }

    public ReferentielsModificationsDto referentiels() {
        droits.exiger(LIRE);
        Set<UUID> ids = tranchesHabilitees();
        List<ReferentielItemDto> tranches = ids.isEmpty() ? List.of()
                : em.createQuery("SELECT t FROM CfgTranche t WHERE t.id IN :ids", CfgTranche.class)
                .setParameter("ids", ids).getResultList().stream()
                .map(t -> new ReferentielItemDto(t.getId(), t.getCode(), t.getDescription(), null))
                .sorted(Comparator.comparing(t -> String.valueOf(t.description())))
                .toList();
        return new ReferentielsModificationsDto(tranches,
                Arrays.stream(EtatModification.values()).map(e -> etat(e.name())).toList());
    }

    public ModificationProjetLigneDto fiche(UUID id) {
        droits.exiger(LIRE);
        return visible(id);
    }

    /** Réponse 4 : mêmes contrôles que le chemin dossier (périmètre commercial), puis la seule règle d'écriture (D6). */
    @Transactional
    public ModificationProjetLigneDto modifier(UUID id, ModificationRequest corps) {
        droits.exiger(MODIFIER);
        UUID dossierId = visible(id).dossierId();
        perimetreCommercial.verifierAccesDossier(dossierId);
        modifications.modifier(dossierId, id, corps);
        em.flush();
        return visible(id);
    }

    private ModificationProjetLigneDto visible(UUID id) {
        return lire(FiltresModifications.aucun(), id).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Demande de modification introuvable : " + id));
    }

    /** R7 (Modification.cs:280) : tranches de AdmUserHabilitation.CfgTrancheId, sans exemption de rôle. */
    private Set<UUID> tranchesHabilitees() {
        return PerimetreCommercialService.admUserIdCourant().map(habilitations::findTrancheIdsByAdmUserId)
                .map(Set::copyOf).orElseGet(Set::of);
    }

    private List<ModificationProjetLigneDto> lire(FiltresModifications f, UUID id) {
        Set<UUID> tranches = tranchesHabilitees();
        if (f.trancheId() != null) {
            tranches = tranches.contains(f.trancheId()) ? Set.of(f.trancheId()) : Set.of();
        }
        return new RequeteNative(SELECT)
                .parmi(" AND i.StkHierarchyId IN (:tranches)", "tranches", tranches)
                .filtre(" AND m.PKey = :id", "id", id)
                .filtre(" AND m.Status IN (:etats)", "etats",
                        f.etats().isEmpty() ? null : f.etats().stream().map(Enum::name).toList())
                .filtre(" AND CAST(m.DemandDate AS date) >= :du", "du", f.du())
                .filtre(" AND CAST(m.DemandDate AS date) <= :au", "au", f.au())
                .contient(RECHERCHE, f.recherche())
                .lignes(em, " ORDER BY m.DemandDate DESC, i.Code ASC").stream()
                .map(ModificationsProjetService::ligne).toList();
    }

    private static EtatModificationProjetDto etat(String code) {
        if (code == null) {
            return null;
        }
        String libelle = Arrays.stream(EtatModification.values()).filter(e -> e.name().equals(code)).findFirst()
                .map(EtatModification::libelle).orElse(code);
        return new EtatModificationProjetDto(code, libelle);
    }

    private static ModificationProjetLigneDto ligne(Object[] r) {
        return new ModificationProjetLigneDto(ValeursNatives.uuid(r[0]), ValeursNatives.uuid(r[1]), etat((String) r[2]),
                ValeursNatives.jour(r[3]), (String) r[4], (String) r[5], (String) r[6], (String) r[7], (String) r[8],
                ValeursNatives.entier(r[9]), (String) r[10], (String) r[11], (String) r[12], (String) r[13],
                ValeursNatives.jour(r[14]), ValeursNatives.jour(r[15]), ValeursNatives.decimal(r[16]),
                ValeursNatives.decimal(r[17]), ValeursNatives.decimal(r[18]), (String) r[19]);
    }
}
