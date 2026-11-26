package com.tsix.immocrm.services.perimetre;

import com.tsix.immocrm.domain.RoleAgence;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRoleRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierContactRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Chaîne compte → {@code AdmUser.CfgEmployeeId} (vendeur) → lien « Vendeur d'agence » → agence → ses vendeurs,
 * relue en base à chaque appel (FEATURE-agences-vente.md R-V2c, R-INS4).
 */
@Component
public class AgenceDuCompte {

    /** Id qui n'existe nulle part : un {@code IN} sur lui ne rend rien (état « restreint, vide », R-V5). */
    public static final UUID AUCUN = new UUID(0, 0);

    private final AdmUserRoleRepository userRoles;
    private final AdmUserRepository users;
    private final CfgTierContactRepository liens;

    public AgenceDuCompte(AdmUserRoleRepository userRoles, AdmUserRepository users, CfgTierContactRepository liens) {
        this.userRoles = userRoles;
        this.users = users;
        this.liens = liens;
    }

    /** Codes de rôle lus dans {@code AdmUserRole}, jamais dans le JWT. */
    public List<String> roles(UUID admUserId) {
        return userRoles.findByAdmUserId(admUserId).stream()
                .map(ur -> ur.getAdmRole() != null ? ur.getAdmRole().getCode() : null)
                .filter(Objects::nonNull)
                .toList();
    }

    /** Vendeurs de l'agence du compte ; absent si le compte n'a pas le rôle {@code Agence}. */
    public Optional<List<UUID>> vendeurs(UUID admUserId) {
        if (admUserId == null || !RoleAgence.porte(roles(admUserId))) {
            return Optional.empty();
        }
        return Optional.of(vendeursDeLAgence(admUserId));
    }

    /** Compte incomplet (pas de vendeur, vendeur sans agence) : {@code [AUCUN]}, jamais une liste vide (R-INS3). */
    List<UUID> vendeursDeLAgence(UUID admUserId) {
        UUID vendeur = users.findById(admUserId).map(AdmUser::getCfgEmployee).map(CfgTier::getId).orElse(null);
        if (vendeur == null) {
            return List.of(AUCUN);
        }
        List<UUID> agences = liens.tiersDuContact(vendeur, RoleAgence.RELATION_VENDEUR);
        if (agences.isEmpty()) {
            return List.of(AUCUN);
        }
        List<UUID> vendeurs = liens.contactsDuTiers(agences.getFirst(), RoleAgence.RELATION_VENDEUR);
        return vendeurs.isEmpty() ? List.of(AUCUN) : vendeurs;
    }
}
