package com.tsix.immocrm.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Le cookie porte des attributs qu'un navigateur applique en silence : mal combinés, il n'y a ni
 * exception ni log, seulement une session qui ne s'établit pas. Ces assertions portent donc sur
 * l'en-tête réellement émis, pas sur l'intention.
 *
 * <p>Test unitaire pur : aucun contexte Spring, aucune base.
 */
class CookieJwtTest {

    @Test
    @DisplayName("SameSite=None sans Secure est refusé au démarrage plutôt qu'en production")
    void noneSansSecure_echoueAuDemarrage() {
        assertThatThrownBy(() -> new CookieJwt(false, "None"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cookie-secure=true");
    }

    @Test
    @DisplayName("La casse ne doit pas ouvrir une faille : 'none' est traité comme 'None'")
    void noneEnMinuscules_estAussiRefuse() {
        assertThatThrownBy(() -> new CookieJwt(false, "none"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Configuration de production : le cookie part en SameSite=None; Secure")
    void configurationProduction_emetNoneEtSecure() {
        String entete = new CookieJwt(true, "None").poser("jeton", 480 * 60).toString();

        assertThat(entete)
                .contains(CookieJwt.NOM + "=jeton")
                .contains("SameSite=None")
                .contains("Secure")
                .contains("HttpOnly")
                .contains("Path=/")
                .contains("Max-Age=28800");
    }

    @Test
    @DisplayName("Configuration locale : Lax sans Secure reste permise, pour du HTTP en dev")
    void configurationLocale_laxSansSecure() {
        String entete = new CookieJwt(false, "Lax").poser("jeton", 60).toString();

        assertThat(entete).contains("SameSite=Lax").doesNotContain("Secure");
    }

    @Test
    @DisplayName("L'effacement rejoue les mêmes attributs, sinon le navigateur n'écrase rien")
    void effacer_memesAttributsEtMaxAgeZero() {
        CookieJwt cookie = new CookieJwt(true, "None");
        String pose = cookie.poser("jeton", 3600).toString();
        String efface = cookie.effacer().toString();

        assertThat(efface)
                .contains("Max-Age=0")
                .contains("SameSite=None")
                .contains("Secure")
                .contains("HttpOnly")
                .contains("Path=/");
        // Les attributs qui identifient le cookie à écraser doivent être identiques.
        assertThat(attributs(efface)).isEqualTo(attributs(pose));
    }

    @Test
    @DisplayName("La réémission de /api/mon-compte reste un cookie de session, sans Max-Age")
    void poserSession_sansMaxAge() {
        assertThat(new CookieJwt(true, "None").poserSession("jeton").toString())
                .doesNotContain("Max-Age")
                .contains("SameSite=None");
    }

    /** Attributs hors valeur et hors Max-Age — ce que le navigateur compare pour écraser. */
    private static String attributs(String entete) {
        return entete.replaceFirst("^[^=]+=[^;]*", "")
                .replaceFirst("; Max-Age=\\d+", "")
                .replaceFirst("; Expires=[^;]+", "");
    }
}
