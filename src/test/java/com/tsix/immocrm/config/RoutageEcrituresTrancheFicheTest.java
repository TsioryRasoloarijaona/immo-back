package com.tsix.immocrm.config;

import com.tsix.immocrm.services.patrimoine.ProjetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Extension de {@link RoutageEcrituresTest} (FEATURE-patrimoine-tranche-fiche.md §6) : la suppression de programme et
 * les écritures de zones sont nommées comme des écritures et jamais en {@code readOnly} (elles partiraient sur la copie).
 */
class RoutageEcrituresTrancheFicheTest {

    @Test
    void projetService_aUneSuppressionJamaisEnLectureSeule() {
        List<Method> suppressions = Arrays.stream(ProjetService.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()) && !m.isSynthetic())
                .filter(m -> m.getName().startsWith("supprimer"))
                .toList();

        assertThat(suppressions).as("ProjetService#supprimer…").isNotEmpty()
                .noneMatch(m -> lectureSeuleEffective(ProjetService.class, m));
    }

    @Test
    void servicesPatrimoine_ecrituresDeZonesNommeesEtJamaisEnLectureSeule() {
        List<Method> zones = servicesPatrimoine().stream()
                .flatMap(c -> Arrays.stream(c.getDeclaredMethods()))
                .filter(m -> Modifier.isPublic(m.getModifiers()) && !m.isSynthetic())
                .filter(m -> m.getName().contains("Zone"))
                .filter(m -> m.getName().matches("^(creer|ajouter|modifier|supprimer)[A-Z].*"))
                .toList();

        assertThat(zones).extracting(Method::getName)
                .anyMatch(n -> n.startsWith("creer") || n.startsWith("ajouter"))
                .anyMatch(n -> n.startsWith("modifier"))
                .anyMatch(n -> n.startsWith("supprimer"));
        assertThat(zones).isNotEmpty()
                .noneMatch(m -> lectureSeuleEffective(m.getDeclaringClass(), m));
    }

    private static boolean lectureSeuleEffective(Class<?> service, Method methode) {
        Transactional surMethode = AnnotatedElementUtils.findMergedAnnotation(methode, Transactional.class);
        if (surMethode != null) {
            return surMethode.readOnly();
        }
        Transactional surClasse = AnnotatedElementUtils.findMergedAnnotation(service, Transactional.class);
        return surClasse != null && surClasse.readOnly();
    }

    private static List<Class<?>> servicesPatrimoine() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Component.class));
        List<Class<?>> classes = scanner.findCandidateComponents("com.tsix.immocrm.services.patrimoine").stream()
                .map(BeanDefinition::getBeanClassName)
                .<Class<?>>map(RoutageEcrituresTrancheFicheTest::charger)
                .toList();
        assertThat(classes).as("Aucun composant sous services.patrimoine").isNotEmpty();
        return classes;
    }

    private static Class<?> charger(String nom) {
        try {
            return Class.forName(nom);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Classe de service introuvable : " + nom, e);
        }
    }
}
