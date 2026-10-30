package com.tsix.immocrm.services.auth;

import com.tsix.immocrm.dto.auth.FiltresUtilisateurs;
import com.tsix.immocrm.dto.auth.ReinitialiserMotDePasseResponse;
import com.tsix.immocrm.dto.auth.UtilisateurListItemDto;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserRole;
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
import com.tsix.immocrm.web.export.LignesAplaties;
import com.tsix.immocrm.web.export.LignesExport;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Liste des comptes et mot de passe Arpent ; création et fiche : {@link UtilisateursService}. */
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
    private final Clock clock;

    public UtilisateurAdminService(AdmUserRepository userRepository, AppUserCredentialRepository credentialRepository,
                                    PasswordEncoder passwordEncoder, AdmUserRoleRepository userRoleRepository,
                                    PerimetreCommercialService perimetreService,
                                    ApplicationEventPublisher evenements, EnvoiEmail envoiEmail, Clock clock) {
        this.clock = clock;
        this.perimetreService = perimetreService;
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.userRoleRepository = userRoleRepository;
        this.evenements = evenements;
        this.envoiEmail = envoiEmail;
    }

    public Page<UtilisateurListItemDto> findAll(FiltresUtilisateurs filtres, Pageable pageable) {
        Page<AdmUser> page = userRepository.findAll(specification(filtres), pageable);
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

    public List<Map<String, Object>> lignesExport(FiltresUtilisateurs filtres) {
        return findAll(filtres, PageRequest.of(0, LignesExport.LIGNES_EXPORT_MAX, Sort.by("description"))).stream()
                .map(LignesAplaties::de).toList();
    }

    private static Specification<AdmUser> specification(FiltresUtilisateurs f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(f.search())) {
                String motif = "%" + f.search().trim().toUpperCase(Locale.ROOT) + "%";
                predicates.add(cb.or(cb.like(root.get("codeMajuscule"), motif), cb.like(root.get("emailMajuscule"), motif),
                        cb.like(cb.upper(root.get("description")), motif)));
            }
            if (f.disabled() != null) {
                predicates.add(cb.equal(cb.coalesce(root.get("disabled"), false), f.disabled()));
            }
            if (f.personnelId() != null) {
                predicates.add(cb.equal(root.get("cfgEmployee").get("id"), f.personnelId()));
            }
            if (f.roleId() != null) {
                Subquery<UUID> lien = query.subquery(UUID.class);
                Root<AdmUserRole> ligne = lien.from(AdmUserRole.class);
                lien.select(ligne.get("id")).where(cb.equal(ligne.get("admUser"), root),
                        cb.equal(ligne.get("admRole").get("id"), f.roleId()));
                predicates.add(cb.exists(lien));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
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
                nombreProjets,
                Boolean.TRUE.equals(user.getMobileUser()),
                Boolean.TRUE.equals(user.getVisibleForChat()),
                user.getUserCreate(),
                user.getUserUpdate(),
                user.getUserUpdateDateTime()
        );
    }

    /**
     * Voir {@code FEATURE-acces-arpent-depuis-omran.md} B1. Adresse invalide ⇒ 400 avant toute écriture ;
     * sans destinataire, pas de mail et le mot de passe n'existe que dans la réponse. Adresse du
     * corps jamais enregistrée.
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
            c.setUserCreateDateTime(LocalDateTime.now(clock));
            return c;
        });

        String motDePasse = genererMotDePasseTemporaire();
        credential.setPasswordHash(passwordEncoder.encode(motDePasse));
        credential.setMustChangePassword(false);
        credential.setProfileCompleted(true);
        credential.setTokenVersion(credential.getTokenVersion() + 1);
        credential.setUserUpdateDateTime(LocalDateTime.now(clock));
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
