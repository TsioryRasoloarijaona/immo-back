package com.tsix.immocrm.security;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * Libellé d'auteur des colonnes d'audit légataires ({@code UserName}, {@code UserCreate}…) : {@code AdmUser.Description},
 * jamais demandé au front. En deux bases, le compte peut manquer dans la base d'écriture : repli sur sa description
 * lue sur la copie, puis sur le login, sans {@code AdmUserId} (clé étrangère) — voir FEATURE-suivi-actions.md §6.
 */
@Component
public class UtilisateurAffiche {

    private static final Logger log = LoggerFactory.getLogger(UtilisateurAffiche.class);

    private final AdmUserRepository userRepository;
    /** Absent en mono-base. */
    private final ObjectProvider<JdbcTemplate> jdbcLecture;

    public UtilisateurAffiche(AdmUserRepository userRepository,
                              @Qualifier("jdbcLecture") ObjectProvider<JdbcTemplate> jdbcLecture) {
        this.userRepository = userRepository;
        this.jdbcLecture = jdbcLecture;
    }

    /** {@code compte} est nul s'il n'existe pas dans la base de la transaction : ne poser {@code AdmUserId} que depuis lui. */
    public record Auteur(AdmUser compte, String nom) {

        static final Auteur ANONYME = new Auteur(null, null);

        public UUID compteId() {
            return compte != null ? compte.getId() : null;
        }
    }

    /** Un principal d'un autre type ({@code @WithMockUser}, route ouverte) = pas d'auteur, pas une erreur. */
    public Auteur courant() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser utilisateur) {
            return resoudre(utilisateur);
        }
        return Auteur.ANONYME;
    }

    public String nom(AuthenticatedUser principal) {
        return principal == null ? null : resoudre(principal).nom();
    }

    private Auteur resoudre(AuthenticatedUser principal) {
        if (principal.admUserId() == null) {
            return new Auteur(null, principal.username());
        }
        AdmUser compte = userRepository.findById(principal.admUserId()).orElse(null);
        if (compte != null) {
            return new Auteur(compte, StringUtils.hasText(compte.getDescription())
                    ? compte.getDescription() : principal.username());
        }
        String surLaCopie = descriptionSurLaCopie(principal.admUserId());
        log.warn("Compte {} ({}) absent de la base d'écriture : action enregistrée sans AdmUserId, "
                        + "au nom de « {} ». Synchroniser les comptes vers la source.",
                principal.admUserId(), principal.username(),
                surLaCopie != null ? surLaCopie : principal.username());
        return new Auteur(null, surLaCopie != null ? surLaCopie : principal.username());
    }

    private String descriptionSurLaCopie(UUID admUserId) {
        JdbcTemplate copie = jdbcLecture.getIfAvailable();
        if (copie == null) {
            return null;
        }
        List<String> descriptions = copie.queryForList(
                "SELECT Description FROM dbo.AdmUser WHERE PKey = ?", String.class, admUserId);
        return descriptions.stream().filter(StringUtils::hasText).findFirst().orElse(null);
    }
}
