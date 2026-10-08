package com.tsix.immocrm.config.apidocs;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/** Branche {@link DocsApiMarkdown} sur springdoc ; un fichier invalide fait échouer le démarrage. */
@Configuration
public class DocsApiMarkdownConfig {

    private static final String RACINE = "api-docs/";

    @Bean
    DocsApiMarkdown docsApiMarkdown(ListableBeanFactory beanFactory) throws IOException {
        List<Class<?>> controleurs = new ArrayList<>();
        for (String nom : beanFactory.getBeanNamesForAnnotation(RestController.class)) {
            Class<?> type = beanFactory.getType(nom);
            if (type != null) controleurs.add(ClassUtils.getUserClass(type));
        }
        List<String> paquets = AutoConfigurationPackages.has(beanFactory)
                ? AutoConfigurationPackages.get(beanFactory) : List.of();
        return new DocsApiMarkdown(lireFichiers(), controleurs, noms -> classesNommees(paquets, noms));
    }

    @Bean
    OperationCustomizer docsApiMarkdownOperations(DocsApiMarkdown docs) {
        return docs::personnaliserOperation;
    }

    @Bean
    OpenApiCustomizer docsApiMarkdownOpenApi(DocsApiMarkdown docs) {
        return docs::personnaliserOpenApi;
    }

    /** Chemin relatif à {@code api-docs/} → contenu, pour chaque {@code .md} du classpath. */
    static Map<String, String> lireFichiers() throws IOException {
        Map<String, String> fichiers = new TreeMap<>();
        for (Resource r : new PathMatchingResourcePatternResolver().getResources("classpath*:" + RACINE + "**/*.md")) {
            String url = URLDecoder.decode(r.getURL().toString(), StandardCharsets.UTF_8);
            String chemin = url.substring(url.lastIndexOf(RACINE) + RACINE.length());
            fichiers.put(chemin, r.getContentAsString(StandardCharsets.UTF_8));
        }
        return fichiers;
    }

    private static Map<String, List<Class<?>>> classesNommees(List<String> paquets, Set<String> noms) {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(
                    org.springframework.beans.factory.annotation.AnnotatedBeanDefinition definition) {
                return true;
            }
        };
        scanner.addIncludeFilter((lecteur, fabrique) ->
                noms.contains(nomSimple(lecteur.getClassMetadata().getClassName())));
        Map<String, List<Class<?>>> out = new HashMap<>();
        for (String paquet : paquets) {
            scanner.findCandidateComponents(paquet).stream()
                    .map(d -> ClassUtils.resolveClassName(Objects.requireNonNull(d.getBeanClassName()), null))
                    .forEach(c -> out.computeIfAbsent(c.getSimpleName(), k -> new ArrayList<>()).add(c));
        }
        return out;
    }

    /** {@code a.b.Externe$Interne} → {@code Interne} : ce qui suit le dernier point du nom court. */
    static String nomSimple(String nomDeClasse) {
        String court = ClassUtils.getShortName(nomDeClasse);
        return court.substring(court.lastIndexOf('.') + 1);
    }
}
