package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.RepartitionActionnaires;
import com.tsix.immocrm.dto.patrimoine.EntiteJuridiqueDto;
import com.tsix.immocrm.dto.patrimoine.LigneRepartitionEcritureDto;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgCompanyShareholder;
import com.tsix.immocrm.entities.cfg.CfgGroupShareholder;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Onglet « Répartition » (FEATURE-entites-juridiques.md §2) : saisie directe et chargement depuis un groupe. */
@Service
public class RepartitionEntiteService {

    private final CfgCompanyRepository repository;
    private final EntiteJuridiqueVisible visible;
    private final RepartitionEntite repartition;
    private final GroupeActionnairesService groupes;
    private final UtilisateurAffiche utilisateurAffiche;
    private final Clock clock;

    public RepartitionEntiteService(CfgCompanyRepository repository, EntiteJuridiqueVisible visible,
                                    RepartitionEntite repartition, GroupeActionnairesService groupes,
                                    UtilisateurAffiche utilisateurAffiche, Clock clock) {
        this.repository = repository;
        this.visible = visible;
        this.repartition = repartition;
        this.groupes = groupes;
        this.utilisateurAffiche = utilisateurAffiche;
        this.clock = clock;
    }

    /** Remplace toutes les lignes ; contrôlées contre le nombre d'actionnaires et de parts déjà enregistrés. */
    @Transactional
    public EntiteJuridiqueDto modifierRepartition(UUID id, List<LigneRepartitionEcritureDto> demande) {
        CfgCompany entite = visible.trouver(id);
        List<CfgCompanyShareholder> lignes = repartition.lignesValides(demande == null ? List.of() : demande);
        RepartitionEntite.verifier(entite, lignes);
        return enregistrer(entite, lignes);
    }

    /**
     * {@code SetRepartitions} : groupe posé sur la fiche, lignes recalculées sur le nombre de parts saisi
     * ({@code nombreParts}, sinon celui enregistré), nombre d'actionnaires aligné sur le groupe — sinon {@code Verif}
     * refuserait aussitôt la fiche.
     */
    @Transactional
    public EntiteJuridiqueDto modifierRepartitionDepuisGroupe(UUID id, UUID groupeId, Integer nombreParts) {
        CfgCompany entite = visible.trouver(id);
        CfgGroupShareholder groupe = groupes.trouver(groupeId);
        if (nombreParts != null) {
            entite.setSharingNumber(ReglesSaisie.positifOuNul(nombreParts, RepartitionEntite.CHAMP_NOMBRE_PARTS));
        }
        List<RepartitionActionnaires.Parts> parts = RepartitionActionnaires.depuisGroupe(
                groupes.pourcentages(groupeId), RepartitionEntite.zeroSiNul(entite.getSharingNumber()));
        List<CfgCompanyShareholder> lignes = repartition.depuisParts(parts);
        entite.setCfgGroupShareholder(groupe);
        entite.setTotalSharing(lignes.size());
        RepartitionEntite.verifier(entite, lignes);
        return enregistrer(entite, lignes);
    }

    private EntiteJuridiqueDto enregistrer(CfgCompany entite, List<CfgCompanyShareholder> lignes) {
        String auteur = utilisateurAffiche.courant().nom();
        LocalDateTime maintenant = LocalDateTime.now(clock);
        entite.setUserUpdate(auteur);
        entite.setUserUpdateDateTime(maintenant);
        CfgCompany enregistree = repository.save(entite);
        repartition.remplacer(enregistree, lignes, auteur, maintenant);
        return EntiteJuridiqueService.versDto(enregistree, RepartitionEntite.versDto(lignes));
    }
}
