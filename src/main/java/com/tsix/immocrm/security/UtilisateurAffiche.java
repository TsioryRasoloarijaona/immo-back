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
 * Résout l'auteur de l'utilisateur connecté pour les colonnes d'audit legacy
 * ({@code UserCreate}, {@code UserName}, {@code AdmUserId}…).
 *
 * <p>Ces colonnes stockent un <b>libellé</b>, pas un identifiant : en données réelles elles
 * contiennent « Dounya SAFSAF », soit {@code AdmUser.Description}. Jamais demandé au front —
 * toujours déduit du token.
 *
 * <h2>Le compte peut manquer dans la base où l'on écrit</h2>
 *
 * <p>En deux bases, les écritures partent vers la source de vérité alors que la connexion
 * (`AppUserCredential`) est vérifiée sur la copie. Un compte qui n'existe que dans la copie — le
 * cas des comptes du banc, et de tout compte créé sur la base du VPS avant le passage en deux
 * bases — n'était alors <b>pas trouvé</b> : l'interaction partait sans {@code UserName}, sans
 * {@code AdmUserId}, avec un libellé sans nom. 13 lignes constatées du 18 au 22/09, invisibles
 * sous tout commercial dans le « Suivi des actions » d'OMRAN, qui filtre sur {@code UserName}.
 * Voir {@code instructions/FEATURE-suivi-actions.md} §6.
 *
 * <p>Résolution, dans l'ordre :
 * <ol>
 *   <li>le compte dans la base de la transaction courante — {@link Auteur#compte()} renseigné, donc
 *       {@code AdmUserId} posable sans violer la clé étrangère ;</li>
 *   <li>à défaut, sa description <b>lue sur la copie</b> (deux bases seulement) — le même libellé
 *       qu'OMRAN écrirait, donc regroupé avec ses autres actions ; {@code compte} reste nul, la clé
 *       étrangère l'interdit ;</li>
 *   <li>à défaut, l'identifiant de connexion du token.</li>
 * </ol>
 */
@Component
public class UtilisateurAffiche {

    private static final Logger log = LoggerFactory.getLogger(UtilisateurAffiche.class);

    private final AdmUserRepository userRepository;
    /** Absent en mono-base : source et copie ne font qu'une, il n'y a rien d'autre à lire. */
    private final ObjectProvider<JdbcTemplate> jdbcLecture;

    public UtilisateurAffiche(AdmUserRepository userRepository,
                              @Qualifier("jdbcLecture") ObjectProvider<JdbcTemplate> jdbcLecture) {
        this.userRepository = userRepository;
        this.jdbcLecture = jdbcLecture;
    }

    /**
     * L'auteur résolu.
     *
     * @param compte le compte dans la base de la transaction courante, ou {@code null} s'il n'y
     *               existe pas — ne poser {@code AdmUserId} que depuis ce champ
     * @param nom    le libellé à écrire dans {@code UserName}/{@code UserCreate}, ou {@code null}
     *               hors contexte authentifié
     */
    public record Auteur(AdmUser compte, String nom) {

        static final Auteur ANONYME = new Auteur(null, null);

        public UUID compteId() {
            return compte != null ? compte.getId() : null;
        }
    }

    /**
     * L'auteur de la requête en cours. {@code @WithMockUser} et les endpoints ouverts portent un
     * principal d'un autre type : traités comme « pas d'auteur », jamais comme une erreur.
     */
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
