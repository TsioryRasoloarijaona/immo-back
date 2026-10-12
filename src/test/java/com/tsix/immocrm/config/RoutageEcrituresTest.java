package com.tsix.immocrm.config;

import com.tsix.immocrm.services.note.NoteService;
import com.tsix.immocrm.services.patrimoine.ActionArticleService;
import com.tsix.immocrm.services.patrimoine.ArticleEcritureService;
import com.tsix.immocrm.services.patrimoine.ProjetService;
import com.tsix.immocrm.services.patrimoine.TrancheService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Interdit qu'une méthode d'écriture se retrouve annotée {@code readOnly = true}.
 *
 * <p>Depuis {@link DataSourceConfig}, ce drapeau ne règle plus seulement une optimisation
 * Hibernate : <b>il choisit la base</b>. Et les deux erreurs possibles n'ont pas du tout le
 * même coût.
 *
 * <table>
 *   <tr><th>Erreur</th><th>Conséquence</th></tr>
 *   <tr><td>lecture sans {@code readOnly}</td>
 *       <td>part sur la source de vérité : <b>lent, mais correct</b></td></tr>
 *   <tr><td>écriture avec {@code readOnly = true}</td>
 *       <td>écrit dans la <b>copie</b>, où la ligne sera écrasée au prochain rapatriement.
 *           <b>Perdue, sans exception et sans trace.</b></td></tr>
 * </table>
 *
 * <p>Seul le second cas est surveillé ici — le premier se corrige tout seul par la mesure, le
 * second ne se voit jamais. Aucune occurrence n'existait à l'écriture de ce test : il protège
 * le code futur, il ne répare rien.
 *
 * <p>Test par réflexion plutôt qu'avec une bibliothèque de règles d'architecture : la règle 1
 * du {@code CLAUDE.md} interdit de toucher {@code pom.xml} sans accord préalable.
 */
class RoutageEcrituresTest {

    /**
     * Les verbes qui désignent une écriture dans ce dépôt. {@code get}/{@code find}/
     * {@code lister}/{@code rechercher} sont volontairement absents : une lecture mal annotée
     * est seulement lente.
     */
    private static final Pattern ECRITURE = Pattern.compile(
            "^(creer|create|modifier|update|supprimer|delete|ajouter|add|enregistrer|"
                    + "affecter|desaffecter|cloturer|valider|annuler|generer|envoyer|importer|initialiser|changer)[A-Z].*"
                    + "|^(creer|create|modifier|update|supprimer|delete|ajouter|add|affecter|desaffecter)$");

    /** FEATURE-patrimoine-ecriture.md §11 : chacun de ces services doit exposer des écritures reconnues, toutes routées vers la source. */
    @Test
    void ecrituresPatrimoine_reconnuesEtJamaisEnLectureSeule() {
        // + FEATURE-patrimoine-audit-actions.md §8 : actions d'article et notes.
        List<Class<?>> patrimoine = List.of(ProjetService.class, TrancheService.class, ArticleEcritureService.class,
                ActionArticleService.class, NoteService.class);
        for (Class<?> service : patrimoine) {
            List<Method> ecritures = java.util.Arrays.stream(service.getDeclaredMethods())
                    .filter(m -> Modifier.isPublic(m.getModifiers()) && ECRITURE.matcher(m.getName()).matches())
                    .toList();
            assertThat(ecritures).as(service.getSimpleName()).isNotEmpty()
                    .noneMatch(m -> lectureSeuleEffective(service, m));
        }
    }

    @Test
    void aucuneMethodeDEcritureNEstMarqueeEnLectureSeule() {
        List<String> fautives = new ArrayList<>();
        int inspectees = 0;

        for (Class<?> service : services()) {
            for (Method methode : service.getDeclaredMethods()) {
                if (!Modifier.isPublic(methode.getModifiers()) || methode.isSynthetic()) {
                    continue;
                }
                if (!ECRITURE.matcher(methode.getName()).matches()) {
                    continue;
                }
                inspectees++;
                if (lectureSeuleEffective(service, methode)) {
                    fautives.add(service.getSimpleName() + "#" + methode.getName());
                }
            }
        }

        // Sans ce plancher, une retouche malheureuse du motif ECRITURE rendrait le test vide —
        // donc vert — alors qu'il ne verifierait plus rien. Un garde-fou muet est pire qu'absent.
        // 27 methodes reconnues au moment d'ecrire ceci ; le plancher laisse de la marge pour
        // qu'un renommage ne casse pas la construction, sans laisser le test devenir vacant.
        assertThat(inspectees)
                .as("Le motif ECRITURE ne reconnait quasiment plus de methode d ecriture : le test "
                        + "ne verifie plus grand-chose.")
                .isGreaterThanOrEqualTo(20);

        assertThat(fautives)
                .as("Ces methodes ecrivent mais sont annotees readOnly : elles ecriraient dans la "
                        + "copie de lecture, ou leur ligne serait ecrasee au prochain rapatriement. "
                        + "Poser @Transactional (sans readOnly) sur la methode.")
                .isEmpty();
    }

    /**
     * Reproduit la résolution de Spring : l'annotation portée par la méthode l'emporte sur celle
     * de la classe. C'est exactement ce que lira {@code determineCurrentLookupKey()}.
     */
    private static boolean lectureSeuleEffective(Class<?> service, Method methode) {
        Transactional surMethode = AnnotatedElementUtils.findMergedAnnotation(methode, Transactional.class);
        if (surMethode != null) {
            return surMethode.readOnly();
        }
        Transactional surClasse = AnnotatedElementUtils.findMergedAnnotation(service, Transactional.class);
        return surClasse != null && surClasse.readOnly();
    }

    /**
     * Filtre sur {@code @Component} et non sur {@code @Service} : {@code @Service} en est une
     * méta-annotation, donc les deux sont couverts. La nuance n'est pas théorique —
     * {@code prospect/NumeroTelephoneSupport} porte {@code @Component} et une méthode
     * {@code valider} transactionnelle, que filtrer sur {@code @Service} laissait passer.
     */
    private static List<Class<?>> services() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Component.class));

        List<Class<?>> classes = new ArrayList<>();
        for (BeanDefinition definition : scanner.findCandidateComponents("com.tsix.immocrm.services")) {
            try {
                classes.add(Class.forName(definition.getBeanClassName()));
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Classe de service introuvable : " + definition, e);
            }
        }
        // Garde-fou du garde-fou : un scan qui ne trouve rien passerait le test sans rien verifier.
        assertThat(classes).as("Aucun composant trouve sous com.tsix.immocrm.services").isNotEmpty();
        return classes;
    }
}
