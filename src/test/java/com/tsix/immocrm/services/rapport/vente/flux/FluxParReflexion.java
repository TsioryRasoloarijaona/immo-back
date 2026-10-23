package com.tsix.immocrm.services.rapport.vente.flux;

import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Appelle {@code FluxVenteMensuel} par réflexion sur les noms du contrat Java du spec (FEATURE-tdb-vente-rapports-flux.md
 * §3.1) : les tests compilent avant le service. Tant qu'une classe du contrat manque, chaque appel échoue en
 * {@link IllegalStateException} « contrat absent » (jamais en {@link IllegalArgumentException}, réservée au service).
 */
final class FluxParReflexion {

    static final String PAQUET = "com.tsix.immocrm.services.rapport.vente.flux.";

    private final ApplicationContext contexte;

    FluxParReflexion(ApplicationContext contexte) {
        this.contexte = contexte;
    }

    /** {@code calculer(new DemandeFlux(tranches, debut, fin, mesures))} ; l'exception du service remonte telle quelle. */
    Flux calculer(List<UUID> tranches, Integer debut, Integer fin, String... mesures) {
        Class<?> service = classe("FluxVenteMensuel");
        Class<?> demande = classe("DemandeFlux");
        Set<Object> ensemble = new HashSet<>();
        for (String m : mesures) {
            ensemble.add(mesure(m));
        }
        try {
            Object d = demande.getDeclaredConstructor(List.class, Integer.class, Integer.class, Set.class)
                    .newInstance(tranches, debut, fin, ensemble);
            Object bean = contexte.getBean(service);
            return new Flux(service.getMethod("calculer", demande).invoke(bean, d));
        } catch (InvocationTargetException e) {
            throw relancer(e);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("contrat absent : DemandeFlux / calculer", e);
        }
    }

    /** Valeurs des constantes {@code static final String} d'une classe du paquet (ex. {@code RegleEncaissement}). */
    static List<String> constantes(String nomClasse) {
        List<String> valeurs = new ArrayList<>();
        for (Field f : classe(nomClasse).getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) && f.getType() == String.class) {
                f.setAccessible(true);
                try {
                    valeurs.add((String) f.get(null));
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(f.getName(), e);
                }
            }
        }
        return valeurs;
    }

    static Class<?> classe(String nom) {
        try {
            return Class.forName(PAQUET + nom);
        } catch (ClassNotFoundException absent) {
            throw new IllegalStateException("contrat absent : " + PAQUET + nom, absent);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static Object mesure(String nom) {
        return Enum.valueOf((Class<? extends Enum>) classe("MesureFlux"), nom);
    }

    private static RuntimeException relancer(InvocationTargetException e) {
        Throwable cause = e.getCause();
        if (cause instanceof RuntimeException r) {
            return r;
        }
        return new IllegalStateException(cause);
    }

    private static Object appeler(Object cible, String nom, Object... arguments) {
        for (Method m : cible.getClass().getMethods()) {
            if (m.getName().equals(nom) && m.getParameterCount() == arguments.length) {
                try {
                    return m.invoke(cible, arguments);
                } catch (InvocationTargetException e) {
                    throw relancer(e);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(nom, e);
                }
            }
        }
        throw new IllegalStateException("contrat absent : " + cible.getClass().getSimpleName() + "." + nom);
    }

    /** Une cellule de {@code FluxMensuel.cles()}. */
    record Cellule(UUID tranche, int annee, int mois) {
    }

    /** Vue sur un {@code FluxMensuel}. */
    static final class Flux {

        private final Object cible;

        Flux(Object cible) {
            this.cible = cible;
        }

        BigDecimal valeur(String m, UUID tranche, int annee, int mois) {
            return (BigDecimal) appeler(cible, "valeur", mesure(m), tranche, annee, mois);
        }

        BigDecimal mois(String m, int annee, int mois) {
            return (BigDecimal) appeler(cible, "mois", mesure(m), annee, mois);
        }

        BigDecimal annee(String m, int annee) {
            return (BigDecimal) appeler(cible, "annee", mesure(m), annee);
        }

        BigDecimal tranche(String m, UUID tranche, int annee) {
            return (BigDecimal) appeler(cible, "tranche", mesure(m), tranche, annee);
        }

        BigDecimal total(String m) {
            return (BigDecimal) appeler(cible, "total", mesure(m));
        }

        List<Cellule> cles() {
            List<Cellule> cellules = new ArrayList<>();
            for (Object c : (Collection<?>) appeler(cible, "cles")) {
                cellules.add(new Cellule((UUID) appeler(c, "trancheId"), ((Number) appeler(c, "annee")).intValue(),
                        ((Number) appeler(c, "mois")).intValue()));
            }
            return cellules;
        }

        Anomalies anomalies() {
            return new Anomalies(appeler(cible, "anomalies"));
        }
    }

    /** Vue sur {@code AnomaliesFlux}. */
    static final class Anomalies {

        private final Object cible;

        Anomalies(Object cible) {
            this.cible = cible;
        }

        long dossiersSansStatut() {
            return ((Number) appeler(cible, "dossiersSansStatut")).longValue();
        }

        /** Nombres portés par {@code reglementsSansDate()} (nombre et montant), quel que soit leur emballage. */
        List<BigDecimal> reglementsSansDate() {
            Object valeur = appeler(cible, "reglementsSansDate");
            List<BigDecimal> nombres = new ArrayList<>();
            if (valeur instanceof Number n) {
                nombres.add(new BigDecimal(n.toString()));
            } else if (valeur != null && valeur.getClass().isRecord()) {
                for (RecordComponent c : valeur.getClass().getRecordComponents()) {
                    Object v = appeler(valeur, c.getName());
                    if (v instanceof Number n) {
                        nombres.add(new BigDecimal(n.toString()));
                    }
                }
            }
            return nombres;
        }

        List<String> parametresAbsents() {
            List<String> noms = new ArrayList<>();
            for (Object o : (Collection<?>) appeler(cible, "parametresAbsents")) {
                noms.add(String.valueOf(o));
            }
            return noms;
        }

        boolean prevuNonSaisi() {
            return Boolean.TRUE.equals(appeler(cible, "prevuNonSaisi"));
        }
    }
}
