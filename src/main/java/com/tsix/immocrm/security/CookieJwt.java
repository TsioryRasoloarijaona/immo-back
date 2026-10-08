package com.tsix.immocrm.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Fabrique unique du cookie JWT. {@code SameSite=Strict} suffit et tient lieu de CSRF tant que front et API
 * partagent le domaine enregistrable ; un front sur un autre domaine exige {@code None} (cookie tiers, que les
 * navigateurs rejettent sans erreur s'il n'est pas aussi {@code Secure} : d'où le contrôle au démarrage).
 */
@Component
public class CookieJwt {

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

    public ResponseCookie poser(String token, long dureeSecondes) {
        return base(token).maxAge(dureeSecondes).build();
    }

    /** Sans {@code Max-Age} (cookie de session), contrairement à la connexion : forme des réémissions de mon-compte. */
    public ResponseCookie poserSession(String token) {
        return base(token).build();
    }

    /**
     * Mêmes attributs que le cookie posé, sinon le navigateur ne l'écrase pas (et refuse un {@code None} non
     * {@code Secure} même pour effacer) : la session resterait ouverte.
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
