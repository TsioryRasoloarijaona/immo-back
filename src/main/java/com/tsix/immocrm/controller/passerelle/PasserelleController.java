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
 * Vérification d'identité pour la passerelle (Caddy {@code forward_auth}) placée devant les
 * services annexes {@code /svc/<nom>/*}. Voir {@code instructions/FEATURE-passerelle-auth.md}.
 *
 * <p><b>Pourquoi hors de {@code /api/auth/**}.</b> Ce préfixe est {@code permitAll} dans
 * {@code SecurityConfig}. Ici, au contraire, la route
 * hérite de toute la chaîne : 401 sans cookie valide (signature, expiration, {@code tokenVersion}
 * révoqué), et {@code X-Api-Key} accepté si la portée de la clé couvre cette route. Le contrôleur n'a donc
 * qu'une chose à faire : traduire le principal en en-têtes.
 *
 * <p>Ces en-têtes sont <b>le contrat</b> entre la passerelle et les services. Un passage futur à
 * une validation locale du JWT par la passerelle (RS256 + JWKS) devra produire exactement les
 * mêmes ; ne pas les renommer.
 */
@Tag(name = "Passerelle", description = "Vérification d'identité appelée par la passerelle (Caddy "
        + "forward_auth) avant de relayer une requête vers un service annexe /svc/<nom>/*. "
        + "Pas destinée au frontend.")
@RestController
@RequestMapping("/api/passerelle")
public class PasserelleController {

    public static final String EN_TETE_USER_ID = "X-User-Id";
    public static final String EN_TETE_USERNAME = "X-Username";
    public static final String EN_TETE_ROLES = "X-User-Roles";
    public static final String EN_TETE_EMPLOYEE_ID = "X-Employee-Id";

    private static final String PREFIXE_ROLE = "ROLE_";

    @GetMapping("/verifier")
    @Operation(
            summary = "Vérifie l'appelant pour la passerelle",
            description = "Appelée par Caddy (forward_auth) avec les en-têtes de la requête d'origine, "
                    + "dont le cookie access_token. Ne renvoie aucun corps : en cas de succès, "
                    + "l'identité est portée par les en-têtes X-User-Id, X-Username, X-User-Roles et "
                    + "X-Employee-Id, que la passerelle recopie sur la requête relayée au service. "
                    + "X-User-Roles liste les codes de rôle (AdmRole.Code) séparés par des virgules, "
                    + "chacun encodé en pourcentage UTF-8 (« Direction Générale » → "
                    + "« Direction%20G%C3%A9n%C3%A9rale ») : les codes réels portent accents et "
                    + "espaces, qu'un en-tête HTTP ne peut pas transporter tels quels. X-Employee-Id "
                    + "est absent si le compte n'est lié à aucune fiche employé. Les mêmes contrôles "
                    + "que sur le reste de l'API s'appliquent : révocation par changement de mot de "
                    + "passe, portée des clés d'API. Plus aucun blocage de première connexion : "
                    + "mustChangePassword/profileCompleted ne sont pas consultés."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Appelant authentifié ; identité dans les en-têtes.",
                    headers = {
                            @Header(name = EN_TETE_USER_ID, description = "Identifiant AdmUser (UUID)."),
                            @Header(name = EN_TETE_USERNAME, description = "Identifiant de connexion (AdmUser.Code)."),
                            @Header(name = EN_TETE_ROLES, description = "Codes de rôle séparés par des virgules, chacun encodé en pourcentage UTF-8."),
                            @Header(name = EN_TETE_EMPLOYEE_ID, description = "Fiche employé (CfgTier, UUID) ; absent si aucune.")
                    }),
            @ApiResponse(responseCode = "401", description = "Cookie absent, expiré, mal signé ou révoqué (TokenVersion), ou clé d'API invalide. La passerelle renvoie ce 401 tel quel au client."),
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
