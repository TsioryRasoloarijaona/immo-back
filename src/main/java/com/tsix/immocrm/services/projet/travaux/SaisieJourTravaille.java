package com.tsix.immocrm.services.projet.travaux;

import com.tsix.immocrm.dto.projet.travaux.JourTravailleEcritureDto;
import com.tsix.immocrm.entities.buy.BuyItem;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProjectZone;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.grh.GrhWorkedDay;
import com.tsix.immocrm.entities.grh.GrhWorkedDayType;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.perimetre.PerimetreProjetService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Contrôles (R1, D3, références) puis écriture d'un jour travaillé (D8, R3, R4) ; tous les contrôles avant le 1er setter. */
@Component
class SaisieJourTravaille {

    private final EntityManager em;
    private final PerimetreProjetService perimetre;
    private final UtilisateurAffiche utilisateur;
    private final Clock clock;

    SaisieJourTravaille(EntityManager em, PerimetreProjetService perimetre, UtilisateurAffiche utilisateur, Clock clock) {
        this.em = em;
        this.perimetre = perimetre;
        this.utilisateur = utilisateur;
        this.clock = clock;
    }

    void verifier(JourTravailleEcritureDto c) {
        Map<String, Object> obligatoires = new LinkedHashMap<>();
        obligatoires.put("société (societeId)", c.societeId());
        obligatoires.put("type (typeId)", c.typeId());
        obligatoires.put("date (date)", c.date());
        obligatoires.put("quantité (quantite)", c.quantite());
        obligatoires.put("quantité 1 (quantite1)", c.quantite1());
        obligatoires.forEach((champ, valeur) -> {
            if (valeur == null) {
                throw new InvalidRequestException("Champ obligatoire manquant : " + champ + ".");
            }
        });
        if (c.projetId() != null && !perimetre.estVisible(c.projetId())) {
            throw new ResourceNotFoundException("Projet introuvable : " + c.projetId());
        }
        CfgCompany societe = em.find(CfgCompany.class, c.societeId());
        if (societe == null || !Boolean.TRUE.equals(societe.getIsActive())) {
            throw new InvalidRequestException("Société inconnue ou inactive : " + c.societeId());
        }
        exister(GrhWorkedDayType.class, c.typeId(), "Type de travaux");
        exister(CfgProjectZone.class, c.zoneId(), "Zone");
        exister(CfgTier.class, c.employeId(), "Personnel");
        exister(CfgTier.class, c.clientId(), "Client");
        exister(BuyItem.class, c.articleId(), "Article");
        if (c.niveauId() != null && ((Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM CfgTrancheLevelingDefinition WHERE PKey = :id")
                .setParameter("id", c.niveauId()).getSingleResult()).intValue() == 0) {
            throw new InvalidRequestException("Niveau inconnu : " + c.niveauId());
        }
    }

    /** D8 / R3 : le type recopie ses valeurs à la création et quand il change ; sinon la saisie prime sur l'existant. */
    void appliquer(GrhWorkedDay j, JourTravailleEcritureDto c) {
        boolean nouveauType = !c.typeId().equals(j.getGrhWorkedDayTypeId());
        boolean creation = j.getId() == null;
        if (nouveauType) {
            GrhWorkedDayType t = em.find(GrhWorkedDayType.class, c.typeId());
            j.setUnity(t.getUnity());
            j.setUnitPriceHT(t.getUnitPriceHT());
            j.setVatRatio(t.getVatRatio());
            j.setUnity1(t.getUnity1());
            j.setUnitPriceHT1(t.getUnitPriceHT1());
            j.setVatRatio1(t.getVatRatio1());
        }
        if (creation || !nouveauType) {
            j.setUnity(ou(c.unite(), j.getUnity()));
            j.setUnitPriceHT(ou(c.prixUnitaireHT(), j.getUnitPriceHT()));
            j.setVatRatio(ou(c.tva(), j.getVatRatio()));
            j.setUnity1(ou(c.unite1(), j.getUnity1()));
            j.setUnitPriceHT1(ou(c.prixUnitaireHT1(), j.getUnitPriceHT1()));
            j.setVatRatio1(ou(c.tva1(), j.getVatRatio1()));
        }
        // R4 (GrhWorkedDay.cs:SetCfgCompany) : changer de société vide le personnel, même s'il est envoyé.
        boolean nouvelleSociete = !creation && !c.societeId().equals(j.getCfgCompanyId());
        j.setCfgEmployeeId(nouvelleSociete ? null : c.employeId());
        j.setCfgCompanyId(c.societeId());
        j.setGrhWorkedDayTypeId(c.typeId());
        j.setPrjProjectId(c.projetId());
        j.setCfgCustomerId(c.clientId());
        j.setCfgTrancheLevelingDefinitionId(c.niveauId());
        j.setCfgProjectZoneId(c.zoneId());
        j.setBuyItemId(c.articleId());
        j.setDocDate(c.date().atStartOfDay());
        j.setTimeStart(moment(c.date(), c.heureDebut()));
        j.setTimeEnd(moment(c.date(), c.heureFin()));
        j.setQuantity(c.quantite());
        j.setQuantity1(c.quantite1());
        j.setCode(c.code());
        j.setDescription(c.description());
        j.setNotes(c.notes());
    }

    void persister(GrhWorkedDay j) {
        String auteur = utilisateur.courant().nom();
        LocalDateTime maintenant = LocalDateTime.now(clock);
        j.setId(UUID.randomUUID());
        j.setUserCreate(auteur);
        j.setUserCreateDateTime(maintenant);
        j.setUserUpdate(auteur);
        j.setUserUpdateDateTime(maintenant);
        em.persist(j);
    }

    void marquer(GrhWorkedDay j) {
        j.setUserUpdate(utilisateur.courant().nom());
        j.setUserUpdateDateTime(LocalDateTime.now(clock));
    }

    private void exister(Class<?> classe, UUID id, String libelle) {
        if (id != null && em.find(classe, id) == null) {
            throw new InvalidRequestException(libelle + " inconnu(e) : " + id);
        }
    }

    private static <T> T ou(T saisie, T actuelle) {
        return saisie != null ? saisie : actuelle;
    }

    private static LocalDateTime moment(LocalDate date, LocalTime heure) {
        return heure == null ? null : date.atTime(heure);
    }
}
