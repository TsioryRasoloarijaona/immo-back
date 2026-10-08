package com.tsix.immocrm.controller.passerelle;

import com.tsix.immocrm.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Caddy {@code forward_auth} devant {@code /svc/<nom>/*} (FEATURE-passerelle-auth.md). Hors de
 * {@code /api/auth/**} (permitAll) pour hériter de toute la chaîne de sécurité. Les en-têtes sont
 * le contrat entre passerelle et services : ne pas les renommer.
 */
@Tag(name = "Passerelle")
@RestController
@RequestMapping("/api/passerelle")
public class PasserelleController {

    public static final String EN_TETE_USER_ID = "X-User-Id";
    public static final String EN_TETE_USERNAME = "X-Username";
    public static final String EN_TETE_ROLES = "X-User-Roles";
    public static final String EN_TETE_EMPLOYEE_ID = "X-Employee-Id";

    private static final String PREFIXE_ROLE = "ROLE_";

    @GetMapping("/verifier")
    @Operation(summary = "Vérifie l'appelant pour la passerelle")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Appelant authentifié ; identité dans les en-têtes.",
                    headers = {
                            @Header(name = EN_TETE_USER_ID, description = "Identifiant AdmUser (UUID)."),
                            @Header(name = EN_TETE_USERNAME, description = "Identifiant de connexion (AdmUser.Code)."),
                            @Header(name = EN_TETE_ROLES, description = "Codes de rôle séparés par des virgules, chacun encodé en pourcentage UTF-8."),
                            @Header(name = EN_TETE_EMPLOYEE_ID, description = "Fiche employé (CfgTier, UUID) ; absent si aucune.")
                    }),
            @ApiResponse(responseCode = "401"),
            @ApiResponse(responseCode = "403", description = "Clé d'API dont la portée ne couvre pas cette route (HORS_PORTEE).")
    })
    public ResponseEntity<Void> verifier(@AuthenticationPrincipal AuthenticatedUser principal,
                                         Authentication authentication) {
        ResponseEntity.HeadersBuilder<?> reponse = ResponseEntity.noContent()
                .cacheControl(CacheControl.noStore())
                .header(EN_TETE_USER_ID, principal.admUserId().toString())
                .header(EN_TETE_USERNAME, principal.username())
                .header(EN_TETE_ROLES, roles(authentication.getAuthorities()));
        if (principal.employeeId() != null) {
            reponse.header(EN_TETE_EMPLOYEE_ID, principal.employeeId().toString());
        }
        return reponse.build();
    }

    /** {@code ROLE_Direction Générale} → {@code Direction%20G%C3%A9n%C3%A9rale} ; une virgule dans un code devient {@code %2C}. */
    static String roles(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith(PREFIXE_ROLE))
                .map(a -> a.substring(PREFIXE_ROLE.length()))
                .map(code -> URLEncoder.encode(code, StandardCharsets.UTF_8).replace("+", "%20"))
                .collect(Collectors.joining(","));
    }
}
