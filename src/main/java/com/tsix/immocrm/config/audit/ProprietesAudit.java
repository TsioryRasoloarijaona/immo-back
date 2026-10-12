package com.tsix.immocrm.config.audit;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import org.hibernate.persister.entity.EntityPersister;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Propriétés tracées d'une entité, dans l'ordre de déclaration : nom = colonne pour un scalaire, nom de navigation
 * OMRAN (colonne sans le suffixe {@code Id}) pour une clé étrangère. Exclues : colonnes {@code User*}, colonnes en
 * lecture seule, listes et formules (FEATURE-patrimoine-audit-actions.md §2.2 r3).
 */
final class ProprietesAudit {

    record Propriete(int index, String nom, boolean association) {
    }

    private static final String PREFIXE_EXCLU = "User";
    private static final String SUFFIXE_CLE = "Id";

    private static final Map<Class<?>, List<Propriete>> PAR_CLASSE = new ConcurrentHashMap<>();

    private ProprietesAudit() {
    }

    static List<Propriete> de(EntityPersister persister) {
        return PAR_CLASSE.computeIfAbsent(persister.getMappedClass(),
                classe -> construire(classe, persister.getPropertyNames()));
    }

    private static List<Propriete> construire(Class<?> classe, String[] noms) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < noms.length; i++) {
            index.put(noms[i], i);
        }
        List<Propriete> proprietes = new ArrayList<>();
        for (Class<?> c = classe; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field champ : c.getDeclaredFields()) {
                Integer i = index.get(champ.getName());
                if (i != null) {
                    propriete(champ, i).ifPresent(proprietes::add);
                }
            }
        }
        return List.copyOf(proprietes);
    }

    private static Optional<Propriete> propriete(Field champ, int index) {
        JoinColumn jointure = champ.getAnnotation(JoinColumn.class);
        if (jointure != null) {
            return ecrite(jointure.insertable(), jointure.updatable(), jointure.name())
                    ? Optional.of(new Propriete(index, navigation(jointure.name()), true))
                    : Optional.empty();
        }
        Column colonne = champ.getAnnotation(Column.class);
        if (colonne == null || !ecrite(colonne.insertable(), colonne.updatable(), colonne.name())) {
            return Optional.empty();
        }
        return Optional.of(new Propriete(index, sansGuillemets(colonne.name()), false));
    }

    private static boolean ecrite(boolean insertable, boolean updatable, String nom) {
        return (insertable || updatable) && !sansGuillemets(nom).startsWith(PREFIXE_EXCLU);
    }

    /** {@code CfgProjectId} → {@code CfgProject}, comme la navigation EF qu'OMRAN écrit. */
    static String navigation(String colonne) {
        String nom = sansGuillemets(colonne);
        return nom.endsWith(SUFFIXE_CLE) && nom.length() > SUFFIXE_CLE.length()
                ? nom.substring(0, nom.length() - SUFFIXE_CLE.length()) : nom;
    }

    private static String sansGuillemets(String nom) {
        return nom.replace("\"", "");
    }
}
