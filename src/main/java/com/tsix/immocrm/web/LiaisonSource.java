package com.tsix.immocrm.web;

import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.util.ClassUtils;

import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Seul endroit qui décide si une erreur d'accès à la base vient de la liaison (503) ou de la requête (inchangé).
 * Voir FEATURE-stabilite-connexion-source.md §2.
 */
public enum LiaisonSource {

    SOURCE_INJOIGNABLE("La base OMRAN est injoignable : rien n'a été enregistré. "
            + "Réessayez dans quelques minutes."),
    LIAISON_COUPEE("La liaison avec la base OMRAN a été coupée pendant l'opération. "
            + "Vérifiez si la saisie a été enregistrée avant de la refaire.");

    // mssql-jdbc est en scope runtime : classe résolue au chargement, pas importée ; égalité stricte, pas de sous-classes.
    private static final Class<?> SQL_SERVER_EXCEPTION = classeOuNull("com.microsoft.sqlserver.jdbc.SQLServerException");

    private final String detail;

    LiaisonSource(String detail) {
        this.detail = detail;
    }

    public String code() {
        return name();
    }

    public String detail() {
        return detail;
    }

    /**
     * @param connexionDejaObtenue une connexion à la source a servi plus tôt dans la requête : une instruction a pu
     *                             partir, donc jamais {@link #SOURCE_INJOIGNABLE} (R7)
     * @return {@code null} si l'erreur ne vient pas de la liaison
     */
    public static LiaisonSource classer(Throwable erreur, boolean connexionDejaObtenue) {
        boolean aucuneConnexion = false;
        boolean connexionPerdue = false;
        for (Throwable t : chaine(erreur)) {
            if (t instanceof SQLTransientConnectionException
                    || t instanceof CannotGetJdbcConnectionException
                    || t instanceof CannotCreateTransactionException) {
                aucuneConnexion = true;
            } else if (estPerteDeConnexion(t)) {
                connexionPerdue = true;
            }
        }
        if (aucuneConnexion && !connexionDejaObtenue) {
            return SOURCE_INJOIGNABLE;
        }
        return aucuneConnexion || connexionPerdue ? LIAISON_COUPEE : null;
    }

    public static Throwable racine(Throwable erreur) {
        List<Throwable> chaine = chaine(erreur);
        return chaine.getLast();
    }

    // Classe 08 = exception de connexion (SQL:2003) ; mssql-jdbc signale ainsi reset, fermeture et timeout socket.
    private static boolean estPerteDeConnexion(Throwable t) {
        if (t instanceof SQLException sql && sql.getSQLState() != null && sql.getSQLState().startsWith("08")) {
            return true;
        }
        if (t.getClass() == SQL_SERVER_EXCEPTION && t.getMessage() != null) {
            String message = t.getMessage().toLowerCase(Locale.ROOT);
            return message.contains("connection is closed") || message.contains("connection reset");
        }
        return false;
    }

    private static Class<?> classeOuNull(String nom) {
        try {
            return ClassUtils.forName(nom, LiaisonSource.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }

    private static List<Throwable> chaine(Throwable erreur) {
        Set<Throwable> vues = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Throwable> liste = new ArrayList<>();
        for (Throwable t = erreur; t != null && vues.add(t); t = t.getCause()) {
            liste.add(t);
        }
        return liste;
    }
}
