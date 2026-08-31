package com.tsix.immocrm.services.auth;

import com.tsix.immocrm.dto.auth.CreerEmployeRequest;
import com.tsix.immocrm.dto.auth.CreerEmployeResponse;
import com.tsix.immocrm.dto.auth.ReinitialiserMotDePasseResponse;
import com.tsix.immocrm.dto.auth.UtilisateurListItemDto;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserRole;
import com.tsix.immocrm.entities.app.AppUserCredential;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierKind;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmRoleRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRoleRepository;
import com.tsix.immocrm.repositories.app.AppUserCredentialRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierKindRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import jakarta.persistence.criteria.Predicate;
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Backs the admin-only endpoints of {@code UtilisateurAdminController} (see its
 * {@code @PreAuthorize}): resetting a password and onboarding a brand-new employee.
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
    private final CfgTierRepository tierRepository;
    private final CfgTierKindRepository tierKindRepository;
    private final AdmRoleRepository roleRepository;
    private final AdmUserRoleRepository userRoleRepository;

    public UtilisateurAdminService(AdmUserRepository userRepository, AppUserCredentialRepository credentialRepository,
                                    PasswordEncoder passwordEncoder, CfgTierRepository tierRepository,
                                    CfgTierKindRepository tierKindRepository, AdmRoleRepository roleRepository,
                                    AdmUserRoleRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.tierRepository = tierRepository;
        this.tierKindRepository = tierKindRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
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

        return page.map(user -> toListItem(user,
                rolesByUserId.getOrDefault(user.getId(), List.of()),
                credentialByUserId.get(user.getId())));
    }

    private static UtilisateurListItemDto toListItem(AdmUser user, List<String> roles, AppUserCredential credential) {
        CfgTier employee = user.getCfgEmployee();
        CfgTierKind employeeKind = employee != null ? employee.getCfgTierKind() : null;
        return new UtilisateurListItemDto(
                user.getId(),
                user.getCode(),
                user.getDescription(),
                user.getEmail(),
                user.getPhone(),
                user.getMobile(),
                Boolean.TRUE.equals(user.getDisabled()),
                employee != null ? employee.getId() : null,
                employee != null ? employee.getFirstName() : null,
                employee != null ? employee.getLastName() : null,
                employeeKind != null ? employeeKind.getCode() : null,
                employeeKind != null ? employeeKind.getDescription() : null,
                roles,
                credential != null,
                credential != null && Boolean.TRUE.equals(credential.getMustChangePassword()),
                credential != null && Boolean.TRUE.equals(credential.getProfileCompleted()),
                user.getUserCreateDateTime()
        );
    }

    /**
     * Generates a fresh temporary password, hashes it, sets {@code MustChangePassword=true} and
     * bumps {@code TokenVersion} (invalidates every token already issued to this account, on
     * every device). Creates the {@code AppUserCredential} row if the account has never logged
     * into the web app before — an admin can bootstrap any legacy {@code AdmUser} this way.
     */
    @Transactional
    public ReinitialiserMotDePasseResponse reinitialiserMotDePasse(UUID admUserId) {
        AdmUser user = userRepository.findById(admUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable: " + admUserId));

        AppUserCredential credential = credentialRepository.findByAdmUserId(admUserId).orElseGet(() -> {
            AppUserCredential c = new AppUserCredential();
            c.setId(UUID.randomUUID());
            c.setAdmUser(user);
            c.setTokenVersion(0);
            c.setUserCreateDateTime(LocalDateTime.now());
            return c;
        });

        String motDePasseTemporaire = genererMotDePasseTemporaire();
        credential.setPasswordHash(passwordEncoder.encode(motDePasseTemporaire));
        credential.setMustChangePassword(true);
        credential.setTokenVersion(credential.getTokenVersion() + 1);
        credential.setUserUpdateDateTime(LocalDateTime.now());
        credentialRepository.save(credential);

        return new ReinitialiserMotDePasseResponse(user.getId(), user.getCode(), motDePasseTemporaire);
    }

    /**
     * Onboards a brand-new employee in one call: creates the person record ({@code CfgTier}),
     * the login account ({@code AdmUser}) linked to it, the requested role assignments
     * ({@code AdmUserRole}), and a fresh {@code AppUserCredential} with a one-time temporary
     * password (same "must change on first login" flow as {@link #reinitialiserMotDePasse}).
     */
    @Transactional
    public CreerEmployeResponse creerEmploye(CreerEmployeRequest request) {
        if (!StringUtils.hasText(request.identifiant())) {
            throw new InvalidRequestException("identifiant est requis");
        }
        if (userRepository.findByCode(request.identifiant()).isPresent()) {
            throw new ConflictException("Identifiant déjà utilisé: " + request.identifiant());
        }
        if (request.roles() == null || request.roles().isEmpty()) {
            throw new InvalidRequestException("Au moins un rôle est requis");
        }

        CfgTierKind tierKind = tierKindRepository.findByCode(request.typeEmployeCode())
                .orElseThrow(() -> new InvalidRequestException(
                        "typeEmployeCode inconnu: " + request.typeEmployeCode()));

        List<AdmRole> roles = roleRepository.findByCodeIn(request.roles());
        if (roles.size() != Set.copyOf(request.roles()).size()) {
            Set<String> found = roles.stream().map(AdmRole::getCode).collect(Collectors.toSet());
            List<String> unknown = request.roles().stream().filter(code -> !found.contains(code)).toList();
            throw new InvalidRequestException("Rôle(s) inconnu(s): " + unknown);
        }

        LocalDateTime now = LocalDateTime.now();

        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setFirstName(request.prenom());
        tier.setLastName(request.nom());
        tier.setEMail(request.email());
        tier.setMobile(request.mobile());
        tier.setCfgTierKind(tierKind);
        tier.setUserCreateDateTime(now);
        tier = tierRepository.save(tier);

        AdmUser user = new AdmUser();
        user.setId(UUID.randomUUID());
        user.setCode(request.identifiant());
        user.setDescription(request.prenom() + " " + request.nom());
        user.setEmail(request.email());
        user.setMobile(request.mobile());
        user.setDisabled(false);
        user.setCfgEmployee(tier);
        user.setUserCreateDateTime(now);
        user = userRepository.save(user);

        for (AdmRole role : roles) {
            AdmUserRole userRole = new AdmUserRole();
            userRole.setId(UUID.randomUUID());
            userRole.setAdmUser(user);
            userRole.setAdmRole(role);
            userRoleRepository.save(userRole);
        }

        String motDePasseTemporaire = genererMotDePasseTemporaire();
        AppUserCredential credential = new AppUserCredential();
        credential.setId(UUID.randomUUID());
        credential.setAdmUser(user);
        credential.setPasswordHash(passwordEncoder.encode(motDePasseTemporaire));
        credential.setMustChangePassword(true);
        credential.setProfileCompleted(false);
        credential.setTokenVersion(0);
        credential.setUserCreateDateTime(now);
        credentialRepository.save(credential);

        List<String> roleCodes = roles.stream().map(AdmRole::getCode).toList();
        return new CreerEmployeResponse(tier.getId(), user.getId(), user.getCode(), roleCodes, motDePasseTemporaire);
    }

    private static String genererMotDePasseTemporaire() {
        StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            sb.append(TEMP_PASSWORD_CHARS.charAt(RANDOM.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}
