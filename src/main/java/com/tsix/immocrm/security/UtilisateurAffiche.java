package com.tsix.immocrm.security;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Résout le nom affiché de l'utilisateur connecté pour les colonnes d'audit legacy
 * {@code UserCreate}/{@code UserUpdate}.
 *
 * <p>Ces colonnes stockent un <b>libellé</b>, pas un identifiant : en données réelles elles
 * contiennent « Dounya SAFSAF », soit {@code AdmUser.Description}. On retombe sur le
 * {@code username} du token si la description est vide, et sur {@code null} si l'appel n'est pas
 * authentifiable (contexte de test). Jamais demandé au front — toujours déduit du token.
 *
 * <p>Extrait de {@code PieceJointeService} parce qu'un deuxième appelant en a besoin
 * ({@code EcheancierService}, qui renseigne enfin {@code UserCreate} sur les règlements créés par
 * l'API).
 */
@Component
public class UtilisateurAffiche {

    private final AdmUserRepository userRepository;

    public UtilisateurAffiche(AdmUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String nom(AuthenticatedUser principal) {
        if (principal == null) {
            return null;
        }
        return userRepository.findById(principal.admUserId())
                .map(AdmUser::getDescription)
                .filter(StringUtils::hasText)
                .orElse(principal.username());
    }
}
