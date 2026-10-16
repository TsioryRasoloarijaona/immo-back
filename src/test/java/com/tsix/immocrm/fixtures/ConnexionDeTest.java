package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.security.AuthenticatedUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/**
 * Appel MockMvc au nom d'un compte {@code AdmUser} réel : {@code UtilisateurAffiche.courant()} en tire
 * l'auteur ({@code Description}) des colonnes {@code UserCreate}/{@code UserUpdate}.
 */
public final class ConnexionDeTest {

    private ConnexionDeTest() {
    }

    public static RequestPostProcessor connecte(AdmUser compte) {
        AuthenticatedUser principal = new AuthenticatedUser(compte.getId(), compte.getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Administrateur"))));
    }
}
