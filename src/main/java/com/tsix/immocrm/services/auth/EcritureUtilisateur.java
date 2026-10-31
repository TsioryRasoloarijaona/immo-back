package com.tsix.immocrm.services.auth;

import com.tsix.immocrm.dto.auth.UtilisateurEcritureDto;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmRoleRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.adm.AdmWebCulturesRepository;
import com.tsix.immocrm.repositories.app.AppUserCredentialRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.services.patrimoine.SaisiePatrimoine;
import com.tsix.immocrm.services.patrimoine.TracePatrimoine;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.ou;

/** Saisie d'un compte : les contrôles passent avant le premier setter, un refus ne laisse rien à flusher. */
@Component
record EcritureUtilisateur(TracePatrimoine trace, AdmUserRepository comptes, AdmWebCulturesRepository cultures,
                           AdmRoleRepository roles, CfgTierRepository tiers, AppUserCredentialRepository identifiants,
                           Clock clock) {

    private static final Saisie DEFAUTS = new Saisie(null, null, null, null, null, true, false, true, false, null, 0, false,
            null, null);

    /** État comparable d'un compte : les textes vides de la base valent null. */
    private record Saisie(String description, String email, String telephone, String mobile, UUID personnelId,
                          boolean inactif, boolean chat, boolean masquer, boolean accesMobile, String smtp, Integer port,
                          boolean ssl, UUID culture, UUID roleWeb) {
    }

    /** Ligne verrouillée : toute écriture sur un compte passe par ici, 404 si inconnu. */
    AdmUser compte(UUID id) {
        return comptes.findPourEcriture(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable: " + id));
    }

    void creer(AdmUser u, String code, UtilisateurEcritureDto d) {
        description(d);
        Saisie s = apres(DEFAUTS, d);
        CfgTier personnel = verifier(s, DEFAUTS, null);
        u.setCode(code);
        u.setLinesPerPage(50);
        u.setConfirmDelete(true);
        u.setLockDelay(3000);
        ecrire(u, s, personnel);
        TracePatrimoine.Trace maintenant = trace.maintenant();
        u.setUserCreate(maintenant.auteur());
        u.setUserCreateDateTime(maintenant.le());
    }

    /** Sans changement effectif, la ligne n'est pas touchée : rien à tracer. */
    void modifier(AdmUser u, UtilisateurEcritureDto d) {
        Saisie actuelle = actuelle(u);
        Saisie s = apres(actuelle, d);
        if (s.equals(actuelle)) {
            return;
        }
        ecrire(u, s, verifier(s, actuelle, u.getCfgEmployee()));
        marquerModifie(u);
    }

    void marquerModifie(AdmUser u) {
        TracePatrimoine.Trace maintenant = trace.maintenant();
        u.setUserUpdate(maintenant.auteur());
        u.setUserUpdateDateTime(maintenant.le());
    }

    /** Le filtre JWT compare ce numéro à chaque requête ; sans identifiants Arpent, il n'y a pas de session. */
    void couperSession(UUID id) {
        identifiants.findByAdmUserId(id).ifPresent(c -> {
            c.setTokenVersion(c.getTokenVersion() + 1);
            c.setUserUpdateDateTime(LocalDateTime.now(clock));
        });
    }

    private static Saisie actuelle(AdmUser u) {
        return new Saisie(u.getDescription(), vide(u.getEmail()), vide(u.getPhone()), vide(u.getMobile()),
                Champs.de(u.getCfgEmployee(), CfgTier::getId), Boolean.TRUE.equals(u.getDisabled()),
                Boolean.TRUE.equals(u.getVisibleForChat()), Boolean.TRUE.equals(u.getCanDisapearNotification()),
                Boolean.TRUE.equals(u.getMobileUser()), vide(u.getEmailSmtpServer()), u.getEmailPortNumber(),
                Boolean.TRUE.equals(u.getEmailSslActive()), u.getWebCultureId(), u.getWebRoleId());
    }

    private static Saisie apres(Saisie a, UtilisateurEcritureDto d) {
        boolean inactif = ou(d.inactif(), a.inactif());
        String description = d.description() == null ? a.description() : description(d);
        String smtp = d.smtpServeur() == null ? a.smtp()
                : vide(SaisiePatrimoine.texte(d.smtpServeur(), null, SaisiePatrimoine.COURT, "smtpServeur"));
        return new Saisie(description, texte(d.email(), a.email()), texte(d.telephone(), a.telephone()),
                texte(d.mobile(), a.mobile()), lien(d.viderPersonnel(), d.personnelId(), a.personnelId()), inactif,
                acces(d.avecNotification(), a.chat(), inactif, d.inactif()), ou(d.peutMasquerNotification(), a.masquer()),
                acces(d.accesMobile(), a.accesMobile(), inactif, d.inactif()), smtp, ou(d.smtpPort(), a.port()),
                ou(d.smtpSsl(), a.ssl()), ou(d.cultureWebId(), a.culture()),
                lien(d.viderRoleWeb(), d.roleWebId(), a.roleWeb()));
    }

    private static String description(UtilisateurEcritureDto d) {
        return SaisiePatrimoine.obligatoire(SaisiePatrimoine.sansBlancsAuxBords(d.description()), null, "description");
    }

    private static UUID lien(Boolean vider, UUID saisie, UUID actuel) {
        return Boolean.TRUE.equals(vider) ? null : ou(saisie, actuel);
    }

    /** Coupé par {@code inactif: true}, ou demandé sur un compte inactif ; un PATCH muet ne retouche rien. */
    private static boolean acces(Boolean saisie, boolean actuel, boolean inactif, Boolean inactifSaisi) {
        if (saisie == null && !Boolean.TRUE.equals(inactifSaisi)) {
            return actuel;
        }
        return !inactif && ou(saisie, actuel);
    }

    private static String texte(String saisie, String actuel) {
        return saisie == null ? actuel : vide(saisie);
    }

    static String vide(String valeur) {
        return StringUtils.hasText(valeur) ? valeur : null;
    }

    /** Une référence n'est contrôlée que si elle change (R11) : renvoyer la valeur actuelle passe toujours. */
    private CfgTier verifier(Saisie s, Saisie actuelle, CfgTier personnelActuel) {
        if (!Objects.equals(s.culture(), actuelle.culture()) && !cultures.existsById(s.culture())) {
            throw new InvalidRequestException("Culture web inconnue.");
        }
        if (s.roleWeb() != null && !s.roleWeb().equals(actuelle.roleWeb()) && !roles.existsById(s.roleWeb())) {
            throw new InvalidRequestException("Rôle web inconnu.");
        }
        if (s.personnelId() == null) {
            return null;
        }
        if (s.personnelId().equals(actuelle.personnelId())) {
            return personnelActuel;
        }
        return tiers.findEmploye(s.personnelId())
                .orElseThrow(() -> new InvalidRequestException("Personnel inconnu ou hors type Employé."));
    }

    private static void ecrire(AdmUser u, Saisie s, CfgTier personnel) {
        u.setDescription(s.description());
        u.setEmail(s.email());
        u.setPhone(s.telephone());
        u.setMobile(s.mobile());
        u.setCfgEmployee(personnel);
        u.setDisabled(s.inactif());
        u.setVisibleForChat(s.chat());
        u.setCanDisapearNotification(s.masquer());
        u.setMobileUser(s.accesMobile());
        u.setEmailSmtpServer(s.smtp());
        u.setEmailPortNumber(s.port());
        u.setEmailSslActive(s.ssl());
        u.setWebCultureId(s.culture());
        u.setWebRoleId(s.roleWeb());
    }
}
