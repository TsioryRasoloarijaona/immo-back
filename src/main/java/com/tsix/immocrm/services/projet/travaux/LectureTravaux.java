package com.tsix.immocrm.services.projet.travaux;

import com.tsix.immocrm.dto.projet.travaux.FiltresTravaux;
import com.tsix.immocrm.dto.projet.travaux.JourTravailleLigneDto;
import com.tsix.immocrm.dto.projet.travaux.StatutJourTravailleDto;
import com.tsix.immocrm.entities.grh.GrhWorkedDay;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.services.perimetre.PerimetreProjetService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Seule règle de visibilité d'un jour travaillé (D2 statut, D3 projet, société active) : liste et lecture par id. */
@Component
class LectureTravaux {

    private static final String SELECT = """
            SELECT d.Pkey, st.PKey, st.Description, st.ColorName, d.DocDate, d.TimeStart, d.TimeEnd, %s, niv.Code,
                COALESCE(t.Description, t.Code), COALESCE(bi.Description, bi.Code), t.GroupDescription,
                COALESCE(p.Description, p.Code), %s, d.Quantity, d.Unity, d.UnitPriceHT, d.Code, d.Description, d.Notes
            FROM GrhWorkedDay d
            LEFT JOIN StrStatus st ON st.PKey = d.StrStatusId
            LEFT JOIN CfgCompany soc ON soc.PKey = d.CfgCompanyId
            LEFT JOIN CfgTier emp ON emp.PKey = d.CfgEmployeeId
            LEFT JOIN CfgTier cli ON cli.PKey = d.CfgCustomerId
            LEFT JOIN CfgTrancheLevelingDefinition niv ON niv.PKey = d.CfgTrancheLevelingDefinitionId
            LEFT JOIN GrhWorkedDayType t ON t.PKey = d.GrhWorkedDayTypeId
            LEFT JOIN BuyItem bi ON bi.PKey = d.BuyItemId
            LEFT JOIN PrjProject p ON p.PKey = d.PrjProjectId
            WHERE (soc.IsActive = 1 OR soc.PKey IS NULL)""".formatted(ValeursNatives.libelleTiers("emp"),
            ValeursNatives.libelleTiers("cli"));
    private static final String RECHERCHE = " AND (d.Code LIKE :recherche OR d.Description LIKE :recherche"
            + " OR d.Notes LIKE :recherche" + ValeursNatives.rechercheTiers("emp") + ValeursNatives.rechercheTiers("cli") + ")";
    private static final String PROJETS = "projets";

    private final EntityManager em;
    private final PerimetreProjetService perimetre;
    private final StatutsJourTravaille statuts;

    LectureTravaux(EntityManager em, PerimetreProjetService perimetre, StatutsJourTravaille statuts) {
        this.em = em;
        this.perimetre = perimetre;
        this.statuts = statuts;
    }

    List<JourTravailleLigneDto> lignes(FiltresTravaux f) {
        return lire(f, null);
    }

    /** Jour visible de l'appelant, sinon 404 (inconnu, statut masqué, projet non habilité, société inactive). */
    GrhWorkedDay visible(UUID id) {
        if (lire(FiltresTravaux.aucun(), id).isEmpty()) {
            throw new ResourceNotFoundException("Jour travaillé introuvable : " + id);
        }
        return em.find(GrhWorkedDay.class, id);
    }

    static BigDecimal total(BigDecimal quantite, BigDecimal prix) {
        return quantite == null || prix == null ? null : quantite.multiply(prix);
    }

    static LocalTime heure(LocalDateTime moment) {
        return moment == null ? null : moment.toLocalTime();
    }

    private List<JourTravailleLigneDto> lire(FiltresTravaux f, UUID id) {
        RequeteNative requete = visibilite(new RequeteNative(SELECT), f.projetIds())
                .filtre(" AND d.Pkey = :id", "id", id)
                .filtre(" AND d.CfgCompanyId = :societe", "societe", f.societeId())
                .filtre(" AND d.DocDate >= :du", "du", f.du() == null ? null : f.du().atStartOfDay())
                .filtre(" AND d.DocDate < :au", "au", f.au() == null ? null : f.au().plusDays(1).atStartOfDay())
                .filtre(" AND d.StrStatusId IN (:coches)", "coches", f.statutIds().isEmpty() ? null : f.statutIds())
                .filtre(" AND d.GrhWorkedDayTypeId = :type", "type", f.typeId())
                .filtre(" AND d.CfgEmployeeId = :employe", "employe", f.employeId())
                .contient(RECHERCHE, f.recherche());
        return requete.lignes(em, " ORDER BY d.DocDate DESC, d.Code ASC").stream().map(LectureTravaux::ligne).toList();
    }

    /** D2 : statut parmi les visibles (aucun rôle restreint = tous) ; D3 : projet habilité ou jour sans projet. */
    private RequeteNative visibilite(RequeteNative requete, List<UUID> projetsDemandes) {
        Optional<Set<UUID>> visibles = statuts.visibles();
        visibles.ifPresent(v -> requete.parmi(" AND d.StrStatusId IN (:visibles)", "visibles", v));
        Set<UUID> projets = perimetre.restreindre(projetsDemandes);
        if (!projetsDemandes.isEmpty()) {
            return requete.parmi(" AND d.PrjProjectId IN (:projets)", PROJETS, projets);
        }
        return projets.isEmpty() ? requete.condition(" AND d.PrjProjectId IS NULL")
                : requete.filtre(" AND (d.PrjProjectId IS NULL OR d.PrjProjectId IN (:projets))", PROJETS, projets);
    }

    private static JourTravailleLigneDto ligne(Object[] r) {
        StatutJourTravailleDto statut = r[1] == null ? null
                : new StatutJourTravailleDto(ValeursNatives.uuid(r[1]), (String) r[2], (String) r[3]);
        BigDecimal quantite = ValeursNatives.decimal(r[14]);
        BigDecimal prix = ValeursNatives.decimal(r[16]);
        return new JourTravailleLigneDto(ValeursNatives.uuid(r[0]), statut, ValeursNatives.jour(r[4]),
                heure(ValeursNatives.moment(r[5])), heure(ValeursNatives.moment(r[6])), (String) r[7], (String) r[8],
                (String) r[9], (String) r[10], (String) r[11], (String) r[12], (String) r[8], (String) r[13], quantite,
                (String) r[15], prix, total(quantite, prix), (String) r[17], (String) r[18], (String) r[19]);
    }
}
