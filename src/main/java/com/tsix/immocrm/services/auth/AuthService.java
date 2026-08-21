package com.tsix.immocrm.services.auth;

import com.tsix.immocrm.dto.auth.UtilisateurCourantDto;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppUserCredential;
import com.tsix.immocrm.exceptions.UnauthorizedException;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRoleRepository;
import com.tsix.immocrm.repositories.app.AppUserCredentialRepository;
import com.tsix.immocrm.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/** Backs {@code POST /api/auth/connexion} / {@code /api/auth/deconnexion}. */
@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final String IDENTIFIANTS_INVALIDES = "Identifiants invalides";

    private final AdmUserRepository userRepository;
    private final AppUserCredentialRepository credentialRepository;
    private final AdmUserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AdmUserRepository userRepository, AppUserCredentialRepository credentialRepository,
                        AdmUserRoleRepository userRoleRepository, PasswordEncoder passwordEncoder,
                        JwtService jwtService) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Validates username/password and issues a signed JWT (for the caller to put in the
     * {@code access_token} httpOnly cookie). Deliberately throws the exact same
     * {@link UnauthorizedException} message whether the username is unknown, the account is
     * disabled, no web credential has been set up for it yet, or the password is wrong — never
     * reveals which case it was.
     */
    public LoginResult connexion(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new UnauthorizedException(IDENTIFIANTS_INVALIDES);
        }

        AdmUser user = userRepository.findByCode(username).orElseThrow(() -> new UnauthorizedException(IDENTIFIANTS_INVALIDES));
        if (Boolean.TRUE.equals(user.getDisabled())) {
            throw new UnauthorizedException(IDENTIFIANTS_INVALIDES);
        }
        AppUserCredential credential = credentialRepository.findByAdmUserId(user.getId())
                .orElseThrow(() -> new UnauthorizedException(IDENTIFIANTS_INVALIDES));
        if (!passwordEncoder.matches(password, credential.getPasswordHash())) {
            throw new UnauthorizedException(IDENTIFIANTS_INVALIDES);
        }

        List<String> roles = userRoleRepository.findByAdmUserId(user.getId()).stream()
                .map(ur -> ur.getAdmRole() != null ? ur.getAdmRole().getCode() : null)
                .filter(Objects::nonNull)
                .toList();
        boolean mustChangePassword = Boolean.TRUE.equals(credential.getMustChangePassword());
        boolean profileCompleted = Boolean.TRUE.equals(credential.getProfileCompleted());

        String token = jwtService.generate(
                user.getId(), user.getCode(), roles,
                user.getCfgEmployee() != null ? user.getCfgEmployee().getId() : null,
                mustChangePassword, profileCompleted, credential.getTokenVersion());

        return new LoginResult(token, toUtilisateurCourantDto(user, roles, mustChangePassword, profileCompleted));
    }

    private static UtilisateurCourantDto toUtilisateurCourantDto(AdmUser user, List<String> roles,
                                                                   boolean mustChangePassword, boolean profileCompleted) {
        return new UtilisateurCourantDto(
                user.getId(),
                user.getCode(),
                user.getDescription(),
                user.getEmail(),
                user.getCfgEmployee() != null ? user.getCfgEmployee().getId() : null,
                roles,
                mustChangePassword,
                profileCompleted
        );
    }

    /** Result of a successful login: the signed JWT plus the profile to hand back in the response body. */
    public record LoginResult(String token, UtilisateurCourantDto utilisateur) {
    }
}
