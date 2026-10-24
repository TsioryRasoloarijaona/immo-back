package com.tsix.immocrm.config;

import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import java.util.List;

/**
 * Registres sans liste centrale : chaque domaine ajoute sa classe (publique, constructeur sans argument) qui implémente
 * l'interface du registre, et ce balayage la trouve. Pas de bean Spring : les registres sont lus en statique, tests
 * unitaires compris.
 */
public final class Contributions {

    private Contributions() {
    }

    /**
     * Une instance de chaque classe concrète de {@code type}, triées par nom de classe. Piège : seul le paquet de
     * {@code type} et ses sous-paquets sont balayés ; une contribution rangée ailleurs est ignorée.
     */
    public static <T> List<T> trouver(Class<T> type) {
        ClassLoader chargeur = Contributions.class.getClassLoader();
        ClassPathScanningCandidateComponentProvider balayage = new ClassPathScanningCandidateComponentProvider(false);
        balayage.setResourceLoader(new DefaultResourceLoader(chargeur));
        balayage.addIncludeFilter(new AssignableTypeFilter(type));
        return balayage.findCandidateComponents(type.getPackageName()).stream()
                .map(d -> instancier(type, d.getBeanClassName(), chargeur))
                .sorted(Comparator.comparing(c -> c.getClass().getName()))
                .toList();
    }

    private static <T> T instancier(Class<T> type, String nom, ClassLoader chargeur) {
        try {
            return ClassUtils.forName(nom, chargeur).asSubclass(type).getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException
                 | InvocationTargetException e) {
            throw new IllegalStateException("Contribution " + nom + " non instanciable", e);
        }
    }
}
