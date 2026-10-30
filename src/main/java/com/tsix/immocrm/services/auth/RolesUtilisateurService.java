package com.tsix.immocrm.services.auth;

import com.tsix.immocrm.config.audit.EcouteurAuditOmran;
import com.tsix.immocrm.dto.auth.ComptesDepotEcritureDto;
import com.tsix.immocrm.dto.auth.RolesUtilisateurEcritureDto;
import com.tsix.immocrm.dto.auth.UtilisateurFicheDto;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserRole;
import com.tsix.immocrm.entities.stl.StlDepositUser;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.adm.AdmRoleRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRoleRepository;
import com.tsix.immocrm.repositories.stl.StlDepositRepository;
import com.tsix.immocrm.repositories.stl.StlDepositUserRepository;
import com.tsix.immocrm.services.patrimoine.TracePatrimoine;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

/** R9, R10, R12 : seule la différence est écrite, et elle se résume en une ligne d'audit sur le compte. */
@Service
public class RolesUtilisateurService {

    /** Regroupés pour rester sous S107. */
    @Component
    record Depots(StlDepositRepository depots, StlDepositUserRepository lignes) {
    }

    /** Regroupés pour rester sous S107. */
    @Component
    record Suivi(EcouteurAuditOmran audit, TracePatrimoine trace) {
    }

    private final AdmRoleRepository roles;
    private final AdmUserRoleRepository userRoles;
    private final Depots depots;
    private final EcritureUtilisateur ecriture;
    private final FicheUtilisateur fiche;
    private final GardeAdministrateurs garde;
    private final Suivi suivi;
    private final EntityManager em;

    public RolesUtilisateurService(AdmRoleRepository roles, AdmUserRoleRepository userRoles, Depots depots,
                                   EcritureUtilisateur ecriture, FicheUtilisateur fiche, GardeAdministrateurs garde,
                                   Suivi suivi, EntityManager em) {
        this.roles = roles;
        this.userRoles = userRoles;
        this.depots = depots;
        this.ecriture = ecriture;
        this.fiche = fiche;
        this.garde = garde;
        this.suivi = suivi;
        this.em = em;
    }

    @Transactional
    public UtilisateurFicheDto remplacerRoles(UUID id, RolesUtilisateurEcritureDto corps) {
        Set<UUID> voulus = etatFinal(corps.roleIds(), "roleIds");
        garde.verrouiller();
        AdmUser compte = ecriture.compte(id);
        List<AdmUserRole> lignes = userRoles.findByAdmUserId(id).stream().filter(l -> l.getAdmRole() != null).toList();
        Set<UUID> ajouts = new TreeSet<>(voulus);
        ajouts.removeAll(lignes.stream().map(l -> l.getAdmRole().getId()).collect(Collectors.toSet()));
        List<AdmUserRole> retraits = lignes.stream().filter(l -> !voulus.contains(l.getAdmRole().getId())).toList();
        List<String> codesRetires = retraits.stream().map(l -> l.getAdmRole().getCode()).toList();
        if (codesRetires.contains(GardeAdministrateurs.ADMINISTRATEUR)) {
            garde.avantRetraitRole(compte);
        }
        // Verrou de RolesService.supprimer, en ordre d'id : une suppression parallèle attend, ou le rôle a disparu (400).
        List<AdmRole> ajoutes = ajouts.stream().map(r -> roles.findPourEcriture(r)
                .orElseThrow(() -> new InvalidRequestException("Rôle inconnu : " + r))).toList();
        if (ajoutes.isEmpty() && retraits.isEmpty()) {
            return fiche.dto(compte);
        }
        retraits.forEach(em::remove);
        ajoutes.forEach(r -> ajouter(compte, r));
        ecriture.couperSession(id);
        tracer(compte, "Rôles : %d ajoutés (%s), %d retirés (%s)".formatted(ajoutes.size(),
                String.join(", ", ajoutes.stream().map(AdmRole::getCode).toList()), retraits.size(),
                String.join(", ", codesRetires)));
        return fiche.dto(compte);
    }

    @Transactional
    public UtilisateurFicheDto remplacerComptesDepot(UUID id, ComptesDepotEcritureDto corps) {
        Set<UUID> voulus = etatFinal(corps.compteIds(), "compteIds");
        AdmUser compte = ecriture.compte(id);
        List<StlDepositUser> lignes = depots.lignes().findByAdmUserId(id);
        Set<UUID> ajouts = new HashSet<>(voulus);
        ajouts.removeAll(lignes.stream().map(StlDepositUser::getStlDepositId).collect(Collectors.toSet()));
        List<StlDepositUser> retraits = lignes.stream().filter(l -> !voulus.contains(l.getStlDepositId())).toList();
        if (depots.depots().findAllById(ajouts).size() != ajouts.size()) {
            throw new InvalidRequestException("Compte de dépôt inconnu.");
        }
        if (ajouts.isEmpty() && retraits.isEmpty()) {
            return fiche.dto(compte);
        }
        retraits.forEach(em::remove);
        ajouts.forEach(d -> ajouterDepot(id, d));
        tracer(compte, "Comptes de dépôt : %d ajoutés, %d retirés".formatted(ajouts.size(), retraits.size()));
        return fiche.dto(compte);
    }

    private static Set<UUID> etatFinal(Collection<UUID> ids, String champ) {
        if (ids == null || ids.stream().anyMatch(Objects::isNull)) {
            throw new InvalidRequestException(champ + " est obligatoire : envoyez [] pour tout retirer.");
        }
        return new LinkedHashSet<>(ids);
    }

    private void ajouter(AdmUser compte, AdmRole role) {
        AdmUserRole ligne = new AdmUserRole();
        ligne.setId(UUID.randomUUID());
        ligne.setAdmUser(compte);
        ligne.setAdmRole(role);
        em.persist(ligne);
    }

    private void ajouterDepot(UUID compte, UUID depot) {
        TracePatrimoine.Trace maintenant = suivi.trace().maintenant();
        StlDepositUser ligne = new StlDepositUser();
        ligne.setId(UUID.randomUUID());
        ligne.setAdmUserId(compte);
        ligne.setStlDepositId(depot);
        ligne.setUserCreate(maintenant.auteur());
        ligne.setUserCreateDateTime(maintenant.le());
        em.persist(ligne);
    }

    private void tracer(AdmUser compte, String resume) {
        ecriture.marquerModifie(compte);
        em.persist(suivi.audit().resume(compte, compte.getId(), resume));
    }
}
