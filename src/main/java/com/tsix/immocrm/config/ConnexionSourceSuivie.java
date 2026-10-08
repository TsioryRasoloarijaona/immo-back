package com.tsix.immocrm.config;

import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Note, pour la requête HTTP en cours, qu'une connexion à la source a été obtenue : une instruction a pu partir,
 * un échec ultérieur ne peut plus promettre « rien n'a été enregistré » (FEATURE-stabilite-connexion-source.md).
 * Hors requête (tâches planifiées), sans effet.
 */
public class ConnexionSourceSuivie extends DelegatingDataSource {

    static final String ATTRIBUT = ConnexionSourceSuivie.class.getName() + ".obtenue";

    public ConnexionSourceSuivie(DataSource cible) {
        super(cible);
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connexion = super.getConnection();
        marquer();
        return connexion;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection connexion = super.getConnection(username, password);
        marquer();
        return connexion;
    }

    public static boolean obtenueDansLaRequete() {
        RequestAttributes requete = RequestContextHolder.getRequestAttributes();
        return requete != null && requete.getAttribute(ATTRIBUT, RequestAttributes.SCOPE_REQUEST) != null;
    }

    static void marquer() {
        RequestAttributes requete = RequestContextHolder.getRequestAttributes();
        if (requete != null) {
            requete.setAttribute(ATTRIBUT, Boolean.TRUE, RequestAttributes.SCOPE_REQUEST);
        }
    }
}
