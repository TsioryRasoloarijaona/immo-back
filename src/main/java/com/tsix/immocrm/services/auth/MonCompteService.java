package com.tsix.immocrm.services.auth;

import com.tsix.immocrm.dto.auth.ChangerMotDePasseRequest;
import com.tsix.immocrm.dto.auth.CompleterProfilRequest;
import com.tsix.immocrm.dto.auth.UtilisateurCourantDto;
import com.tsix.immocrm.dto.perimetre.ProjetPerimetreDto;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppUserCredential;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRoleRepository;
import com.tsix.immocrm.repositories.app.AppUserCredentialRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.security.JwtService;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Backs {@code GET /api/mon-compte}, {@code POST /api/mon-compte/changer-mot-de-passe} and
 * {@code PATCH /api/mon-compte/profil}. Since FEATURE-acces-arpent-depuis-omran.md there is no
 * onboarding gate any more: the password change is voluntary, and the profile form is no longer
 * shown by the frontend (the route stays in place).
 */
@Service
@Transactional(readOnly = true)
public class MonCompteService {

    private final AdmUserRepository userRepository;
    private final AppUserCredentialRepository credentialRepository;
    private final AdmUserRoleRepository userRoleRepository;
    private final CfgTierRepository tierRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PerimetreCommercialService perimetreService;

    public MonCompteService(AdmUserRepository userRepository, AppUserCredentialRepository credentialRepository,
                             AdmUserRoleRepository userRoleRepository, CfgTierRepository tierRepository,
                             PasswordEncoder passwordEncoder, JwtService jwtService,
                             PerimetreCommercialService perimetreService) {
        this.perimetreService = perimetreService;
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.userRoleRepository = userRoleRepository;
        this.tierRepository = tierRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UtilisateurCourantDto moi(AuthenticatedUser principal) {
        AdmUser user = findUser(principal);
        AppUserCredential credential = findCredential(principal);
        return toDto(user, roles(user), credential);
    }

    /**
     * Verifies {@code ancienMotDePasse}, hashes and stores the new one, clears
     * {@code mustChangePassword}, bumps {@code TokenVersion} (invalidates every token issued
     * before this call — including the one used to make this very request, so a fresh cookie is
     * returned) and returns a freshly signed token to replace it.
     */
    @Transactional
    public AuthService.LoginResult changerMotDePasse(AuthenticatedUser principal, ChangerMotDePasseRequest request) {
        if (!StringUtils.hasText(request.ancienMotDePasse()) || !StringUtils.hasText(request.nouveauMotDePasse())) {
            throw new InvalidRequestException("ancienMotDePasse et nouveauMotDePasse sont requis");
        }
        if (request.nouveauMotDePasse().length() < 8) {
            throw new InvalidRequestException("Le nouveau mot de passe doit contenir au moins 8 caractères");
        }

        AdmUser user = findUser(principal);
        AppUserCredential credential = findCredential(principal);
        if (!passwordEncoder.matches(request.ancienMotDePasse(), credential.getPasswordHash())) {
            throw new InvalidRequestException("Ancien mot de passe incorrect");
        }

        credential.setPasswordHash(passwordEncoder.encode(request.nouveauMotDePasse()));
        credential.setMustChangePassword(false);
        credential.setTokenVersion(credential.getTokenVersion() + 1);
        credential.setUserUpdateDateTime(LocalDateTime.now());
        credentialRepository.save(credential);

        List<String> roles = roles(user);
        String token = jwtService.generate(user.getId(), user.getCode(), roles, principal.employeeId(),
                false, credential.getProfileCompleted(), credential.getTokenVersion());
        return new AuthService.LoginResult(token, toDto(user, roles, credential));
    }

    /**
     * Fills the linked {@code CfgTier}'s personal fields and marks {@code ProfileCompleted}.
     * 400 if the account has no linked employee (nowhere to store the info) — a known limitation
     * for legacy accounts like "notaire" that were never tied to a CfgTier.
     */
    @Transactional
    public AuthService.LoginResult completerProfil(AuthenticatedUser principal, CompleterProfilRequest request) {
        if (!StringUtils.hasText(request.prenom()) || !StringUtils.hasText(request.nom())
                || !StringUtils.hasText(request.telephone()) || !StringUtils.hasText(request.email())
                || request.dateNaissance() == null) {
            throw new InvalidRequestException("prenom, nom, telephone, email et dateNaissance sont requis");
        }

        AdmUser user = findUser(principal);
        CfgTier employee = user.getCfgEmployee();
        if (employee == null) {
            throw new InvalidRequestException(
                    "Ce compte n'est lié à aucune fiche employé — impossible de compléter le profil.");
        }
        employee.setFirstName(request.prenom());
        employee.setLastName(request.nom());
        employee.setMobile(request.telephone());
        employee.setEMail(request.email());
        employee.setBirthDate(request.dateNaissance());
        if (StringUtils.hasText(request.adresse())) {
            employee.setAddress(request.adresse());
        }
        tierRepository.save(employee);

        AppUserCredential credential = findCredential(principal);
        credential.setProfileCompleted(true);
        credential.setUserUpdateDateTime(LocalDateTime.now());
        credentialRepository.save(credential);

        List<String> roles = roles(user);
        String token = jwtService.generate(user.getId(), user.getCode(), roles, principal.employeeId(),
                credential.getMustChangePassword(), true, credential.getTokenVersion());
        return new AuthService.LoginResult(token, toDto(user, roles, credential));
    }

    private AdmUser findUser(AuthenticatedUser principal) {
        return userRepository.findById(principal.admUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable: " + principal.admUserId()));
    }

    private AppUserCredential findCredential(AuthenticatedUser principal) {
        return credentialRepository.findByAdmUserId(principal.admUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Identifiants introuvables pour: " + principal.admUserId()));
    }

    private List<String> roles(AdmUser user) {
        return userRoleRepository.findByAdmUserId(user.getId()).stream()
                .map(ur -> ur.getAdmRole() != null ? ur.getAdmRole().getCode() : null)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Le périmètre commercial est relu en base à chaque appel — jamais depuis le JWT : c'est
     * précisément ce qui permet à {@code GET /api/mon-compte} de refléter immédiatement une
     * affectation modifiée par un administrateur, sans reconnexion.
     */
    private UtilisateurCourantDto toDto(AdmUser user, List<String> roles, AppUserCredential credential) {
        List<ProjetPerimetreDto> projets = perimetreService.projetsDuPerimetre(user.getId());
        return new UtilisateurCourantDto(
                user.getId(),
                user.getCode(),
                user.getDescription(),
                user.getEmail(),
                user.getCfgEmployee() != null ? user.getCfgEmployee().getId() : null,
                roles,
                Boolean.TRUE.equals(credential.getMustChangePassword()),
                Boolean.TRUE.equals(credential.getProfileCompleted()),
                !projets.isEmpty(),
                projets
        );
    }
}
