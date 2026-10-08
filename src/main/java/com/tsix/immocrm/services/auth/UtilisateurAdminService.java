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

/** Les comptes se créent dans Omran, jamais ici ({@code FEATURE-acces-arpent-depuis-omran.md}). */
@Service
@Transactional(readOnly = true)
public class UtilisateurAdminService {

    /** Sans 0/O/1/I/l : le mot de passe est lu et retapé à la main. */
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

    public Page<UtilisateurListItemDto> findAll(String search, Boolean disabled, Pageable pageable) {
        Specification<AdmUser> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(search)) {
                // code/email sont des @Lob : lower() refuse les CLOB, la collation insensible à la
                // casse de SQL Server fait le travail.
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
     * Voir {@code FEATURE-acces-arpent-depuis-omran.md} B1. Adresse invalide ⇒ 400 avant toute écriture ;
     * sans destinataire, pas de mail et le mot de passe n'existe que dans la réponse. Adresse du
     * corps jamais enregistrée : Omran reste la source des comptes.
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

    /** Repli sur la fiche employé : il gagne des destinataires réels ({@code FEATURE-service-envoi-email.md} §2). */
    private static String resoudreDestinataire(AdmUser user) {
        String direct = adressePlausible(user.getEmail());
        if (direct != null) {
            return direct;
        }
        return adressePlausible(Champs.de(user.getCfgEmployee(), CfgTier::getEMail));
    }

    /**
     * Volontairement permissif : écarte ce qui ne peut pas être une adresse, notamment les chaînes
     * vides que la base contient à la place de {@code NULL}.
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
