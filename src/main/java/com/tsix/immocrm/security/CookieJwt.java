package com.tsix.immocrm.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Fabrique unique du cookie portant le JWT.
 *
 * <p>La construction était dupliquée dans {@code AuthController} et {@code MonCompteController} ;
 * les deux copies devaient rester d'accord sur cinq attributs, dont ceux qui décident si le
 * navigateur accepte le cookie. Une seule définition ferme cette classe de bug.
 *
 * <h2>Pourquoi {@code SameSite} est configurable</h2>
 *
 * <p>Le frontend n'est plus servi depuis le même site que l'API : {@code immo-front-six.vercel.app}
 * et {@code api.tsiory.tech} sont deux domaines enregistrables distincts, donc toute requête de
 * l'un vers l'autre est <b>cross-site</b>. Avec {@code SameSite=Strict}, le navigateur n'attache
 * jamais le cookie à ces requêtes : la connexion réussirait, poserait le cookie, et l'appel suivant
 * repartirait anonyme — un « 401 après login réussi » sans trace côté serveur.
 *
 * <p>{@code SameSite=None} lève cette restriction, mais les navigateurs <b>rejettent</b> un cookie
 * {@code None} qui n'est pas également {@code Secure}. La combinaison est donc vérifiée au
 * démarrage : mal configurée, elle ne produirait aucune erreur, juste un cookie silencieusement
 * ignoré — la panne la plus coûteuse à diagnostiquer.
 *
 * <p>En développement local, frontend et API partagent {@code localhost} : {@code Lax} suffit et
 * évite d'exiger HTTPS.
 */
@Component
public class CookieJwt {

    /** Nom du cookie httpOnly portant le JWT. */
    public static final String NOM = "access_token";

    private final boolean secure;
    private final String sameSite;

    public CookieJwt(@Value("${app.jwt.cookie-secure}") boolean secure,
                     @Value("${app.jwt.cookie-same-site}") String sameSite) {
        if ("None".equalsIgnoreCase(sameSite) && !secure) {
            throw new IllegalStateException(
                    "app.jwt.cookie-same-site=None exige app.jwt.cookie-secure=true : les navigateurs "
                            + "rejettent un cookie SameSite=None non Secure, et le rejet est silencieux.");
        }
        this.secure = secure;
        this.sameSite = sameSite;
    }

    /** Cookie persistant, à poser après une connexion réussie. */
    public ResponseCookie poser(String token, long dureeSecondes) {
        return base(token).maxAge(dureeSecondes).build();
    }

    /**
     * Cookie de session, effacé à la fermeture du navigateur (pas de {@code Max-Age}).
     *
     * <p>C'est la forme qu'utilisent les réémissions de {@code /api/mon-compte} depuis toujours,
     * là où la connexion pose un cookie de 8 h. Comportement conservé tel quel, mais nommé :
     * l'écart tenait à un {@code maxAge} simplement absent, et rien ne disait s'il était voulu.
     */
    public ResponseCookie poserSession(String token) {
        return base(token).build();
    }

    /**
     * Cookie d'effacement (valeur vide, {@code Max-Age=0}).
     *
     * <p>Il doit porter <b>les mêmes attributs</b> que celui posé à la connexion : un navigateur
     * n'écrase un cookie que si le triplet nom/domaine/chemin correspond, et refuse un
     * {@code SameSite=None} non {@code Secure} même pour une suppression. Un effacement construit
     * à part finirait par diverger et laisserait la session ouverte.
     */
    public ResponseCookie effacer() {
        return base("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String valeur) {
        return ResponseCookie.from(NOM, valeur)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/");
    }
}
