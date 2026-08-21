package com.tsix.immocrm.services.auth;

import com.tsix.immocrm.dto.auth.ReinitialiserMotDePasseResponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppUserCredential;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.app.AppUserCredentialRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Backs {@code POST /api/utilisateurs/{admUserId}/reinitialiser-mot-de-passe} — admin-only (see
 * {@code UtilisateurAdminController}'s {@code @PreAuthorize}).
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

    public UtilisateurAdminService(AdmUserRepository userRepository, AppUserCredentialRepository credentialRepository,
                                    PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.passwordEncoder = passwordEncoder;
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

    private static String genererMotDePasseTemporaire() {
        StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            sb.append(TEMP_PASSWORD_CHARS.charAt(RANDOM.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}
