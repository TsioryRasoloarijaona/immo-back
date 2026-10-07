package com.tsix.immocrm.services.auth;

import com.tsix.immocrm.dto.auth.ReinitialiserMotDePasseResponse;
import com.tsix.immocrm.dto.auth.UtilisateurListItemDto;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppUserCredential;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierKind;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.mail.AdresseMasquee;
import com.tsix.immocrm.mail.EnvoiEmail;
import com.tsix.immocrm.mail.MotDePasseTemporaireEmis;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRoleRepository;
import com.tsix.immocrm.repositories.app.AppUserCredentialRepository;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Backs the admin-only endpoints of {@code UtilisateurAdminController} (see its
 * {@code @PreAuthorize}): listing the accounts and resetting a password. Accounts
 * themselves are created in Omran, never here (FEATURE-acces-arpent-depuis-omran.md).
 */
@Service
@Transactional(readOnly = true)
public class UtilisateurAdminService {

    /** Unambiguous charset (no 0/O/1/I/l) — the temporary password is read aloud/typed by hand. */
    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final int TEMP_PASSWORD_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AdmUserRepository userRepository;
    private final AppUserCredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdmUserRoleRepository userRoleRepository;
    private final PerimetreCommercialService perimetreService;
    private final ApplicationEventPublisher evenements;
    private final EnvoiEmail envoiEmail;

    public UtilisateurAdminService(AdmUserRepository userRepository, AppUserCredentialRepository credentialRepository,
                                    PasswordEncoder passwordEncoder, AdmUserRoleRepository userRoleRepository,
                                    PerimetreCommercialService perimetreService,
                                    ApplicationEventPublisher evenements, EnvoiEmail envoiEmail) {
        this.perimetreService = perimetreService;
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.userRoleRepository = userRoleRepository;
        this.evenements = evenements;
        this.envoiEmail = envoiEmail;
    }

    /**
     * {@code GET /api/utilisateurs} — every account (AdmUser) with its details, linked employee
     * (CfgTier) info and assigned roles (AdmRole), for the admin user-management screen.
     * {@code search} matches (case-insensitive, substring) username/description/email; disabled
     * accounts are included unless {@code disabled=false} is passed explicitly.
     */
    public Page<UtilisateurListItemDto> findAll(String search, Boolean disabled, Pageable pageable) {
        Specification<AdmUser> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(search)) {
                // code/email are mapped @Lob (CLOB) — Hibernate's lower() rejects CLOB arguments,
                // so these two rely on SQL Server's default case-insensitive collation instead.
                String pattern = "%" + search + "%";
                predicates.add(cb.or(
                        cb.like(root.get("code"), pattern),
                        cb.like(cb.lower(root.get("description")), pattern.toLowerCase()),
                        cb.like(root.get("email"), pattern)
                ));
            }
            if (disabled != null) {
                predicates.add(cb.equal(cb.coalesce(root.get("disabled"), false), disabled));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<AdmUser> page = userRepository.findAll(spec, pageable);
        List<UUID> userIds = page.getContent().stream().map(AdmUser::getId).toList();

        Map<UUID, List<String>> rolesByUserId = userRoleRepository.findByAdmUserIdIn(userIds).stream()
                .filter(ur -> ur.getAdmUser() != null && ur.getAdmRole() != null)
                .collect(Collectors.groupingBy(ur -> ur.getAdmUser().getId(),
                        Collectors.mapping(ur -> ur.getAdmRole().getCode(), Collectors.toList())));

        Map<UUID, AppUserCredential> credentialByUserId = credentialRepository.findByAdmUserIdIn(userIds).stream()
                .collect(Collectors.toMap(c -> c.getAdmUser().getId(), c -> c));

        // Périmètre commercial : un seul agrégat pour toute la page, pas une requête par ligne.
        Map<UUID, Integer> projetsByUserId = perimetreService.nombreProjetsParUtilisateur(userIds);

        return page.map(user -> toListItem(user,
                rolesByUserId.getOrDefault(user.getId(), List.of()),
                credentialByUserId.get(user.getId()),
                projetsByUserId.getOrDefault(user.getId(), 0)));
    }

    private static UtilisateurListItemDto toListItem(AdmUser user, List<String> roles, AppUserCredential credential,
                                                      int nombreProjets) {
        CfgTier employee = user.getCfgEmployee();
        CfgTierKind employeeKind = Champs.de(employee, CfgTier::getCfgTierKind);
        return new UtilisateurListItemDto(
                user.getId(),
                user.getCode(),
                user.getDescription(),
                user.getEmail(),
                user.getPhone(),
                user.getMobile(),
                Boolean.TRUE.equals(user.getDisabled()),
                Champs.de(employee, CfgTier::getId),
                Champs.de(employee, CfgTier::getFirstName),
                Champs.de(employee, CfgTier::getLastName),
                Champs.de(employeeKind, CfgTierKind::getCode),
                Champs.de(employeeKind, CfgTierKind::getDescription),
                roles,
                credential != null,
                credential != null && Boolean.TRUE.equals(credential.getMustChangePassword()),
                credential != null && Boolean.TRUE.equals(credential.getProfileCompleted()),
                user.getUserCreateDateTime(),
                nombreProjets
        );
    }

    /**
     * {@code POST /api/utilisateurs/{admUserId}/reinitialiser-mot-de-passe} — ouvre (ou rouvre)
     * l'accès Arpent d'un compte Omran. Voir {@code instructions/FEATURE-acces-arpent-depuis-omran.md} B1.
     *
     * <p>Génère toujours un mot de passe, le hache, rend le compte immédiatement utilisable
     * ({@code MustChangePassword=false}, {@code ProfileCompleted=true} — tout est déjà saisi sur
     * Omran) et incrémente {@code TokenVersion} (déconnecte toutes les sessions déjà ouvertes).
     * Crée la ligne {@code AppUserCredential} si le compte ne s'était jamais connecté au web.
     *
     * <p>Destinataire : {@code emailCorps} s'il est fourni, sinon {@link #resoudreDestinataire}.
     * Une adresse fournie mais invalide est refusée en 400 <b>avant toute écriture</b>. Sans
     * destinataire, la réinitialisation a lieu quand même et aucun événement mail n'est publié :
     * le mot de passe n'existe alors que dans la réponse. L'adresse du corps n'est jamais
     * enregistrée — Omran reste la source des comptes.
     */
    @Transactional
    public ReinitialiserMotDePasseResponse reinitialiserMotDePasse(UUID admUserId, String emailCorps) {
        AdmUser user = userRepository.findById(admUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable: " + admUserId));

        String destinataire;
        if (StringUtils.hasText(emailCorps)) {
            destinataire = adressePlausible(emailCorps);
            if (destinataire == null) {
                throw new InvalidRequestException("L'adresse email fournie n'est pas valide : "
                        + "corrigez-la, ou laissez le champ vide pour seulement afficher les "
                        + "identifiants.");
            }
        } else {
            destinataire = resoudreDestinataire(user);
        }

        AppUserCredential credential = credentialRepository.findByAdmUserId(admUserId).orElseGet(() -> {
            AppUserCredential c = new AppUserCredential();
            c.setId(UUID.randomUUID());
            c.setAdmUser(user);
            c.setTokenVersion(0);
            c.setUserCreateDateTime(LocalDateTime.now());
            return c;
        });

        String motDePasse = genererMotDePasseTemporaire();
        credential.setPasswordHash(passwordEncoder.encode(motDePasse));
        credential.setMustChangePassword(false);
        credential.setProfileCompleted(true);
        credential.setTokenVersion(credential.getTokenVersion() + 1);
        credential.setUserUpdateDateTime(LocalDateTime.now());
        credentialRepository.save(credential);

        if (destinataire != null) {
            evenements.publishEvent(new MotDePasseTemporaireEmis(
                    destinataire, nomAffiche(user), user.getCode(), motDePasse));
        }

        return new ReinitialiserMotDePasseResponse(user.getId(), user.getCode(), motDePasse,
                destinataire != null && envoiEmail.estActif(), AdresseMasquee.de(destinataire));
    }

    private static String genererMotDePasseTemporaire() {
        StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            sb.append(TEMP_PASSWORD_CHARS.charAt(RANDOM.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * Adresse à laquelle expédier le mot de passe : {@code AdmUser.Email}, à défaut l'email de
     * la fiche employé liée ({@code CfgTier.EMail}). {@code null} si le compte n'en a aucune.
     *
     * <p>Le repli n'est pas décoratif — mesuré sur {@code yesRemsProd} : 24 comptes portent une
     * adresse sur {@code AdmUser}, 17 sur {@code CfgTier}, et 25 sur l'une ou l'autre. Il gagne
     * donc un destinataire réel, ce qui suffit à le justifier sur une population de 15 comptes
     * actifs.
     */
    private static String resoudreDestinataire(AdmUser user) {
        String direct = adressePlausible(user.getEmail());
        if (direct != null) {
            return direct;
        }
        return adressePlausible(Champs.de(user.getCfgEmployee(), CfgTier::getEMail));
    }

    /**
     * Normalise et filtre grossièrement une adresse : {@code null} si elle est vide, blanche,
     * ou dépourvue de structure {@code local@domaine.tld}.
     *
     * <p>Volontairement permissif — le but n'est pas de valider une adresse (seul un envoi
     * réussi le fait) mais d'écarter ce qui ne peut manifestement pas en être une, en
     * particulier les chaînes vides que la base contient à la place de {@code NULL}.
     */
    private static String adressePlausible(String valeur) {
        if (!StringUtils.hasText(valeur)) {
            return null;
        }
        String nettoyee = valeur.trim();
        int arobase = nettoyee.indexOf('@');
        if (arobase < 1 || nettoyee.indexOf('@', arobase + 1) >= 0) {
            return null;
        }
        String domaine = nettoyee.substring(arobase + 1);
        int point = domaine.indexOf('.');
        return (point > 0 && point < domaine.length() - 1) ? nettoyee : null;
    }

    /** Nom lisible pour l'en-tête du message, avec repli sur l'identifiant du compte. */
    private static String nomAffiche(AdmUser user) {
        if (StringUtils.hasText(user.getDescription())) {
            return user.getDescription().trim();
        }
        CfgTier employe = user.getCfgEmployee();
        String prenom = Champs.de(employe, CfgTier::getFirstName);
        String nom = Champs.de(employe, CfgTier::getLastName);
        String complet = ((prenom == null ? "" : prenom) + " " + (nom == null ? "" : nom)).trim();
        return StringUtils.hasText(complet) ? complet : user.getCode();
    }
}
