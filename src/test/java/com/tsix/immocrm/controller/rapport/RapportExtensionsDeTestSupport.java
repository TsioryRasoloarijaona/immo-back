package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.services.equipe.CommerciauxVisibles;
import com.tsix.immocrm.services.equipe.MembreEquipe;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rapport de test des ajouts B2–B5 du socle (FEATURE-tdb-vente-rapports-socle.md, « Ajouts au socle… »), déclaré
 * comme le demande §9.1 (« une Rapport de test en @TestConfiguration »). Construit par réflexion sur les noms du spec
 * ({@code domain/rapport/*}, composantes des records) : la classe compile avant le socle ; tant que
 * {@code Rapport} n'existe pas, aucun bean n'est déclaré et les routes répondent 404.
 * <pre>
 *   paramètres : commercialId (COMMERCIAUX, multiple), etat (CHOIX, multiple, options lues en base : A, B)
 *   section « commerciaux » : une ligne par élément de ContexteRapport.commerciauxVisibles()
 *   section « dynamique »   : colonne « serie » puis une colonne NOMBRE (SOMME) par valeur de choix("etat"),
 *                             graphique LIGNES sur ces colonnes
 * </pre>
 */
@TestConfiguration
class RapportExtensionsDeTestSupport {

    static final String CODE = "ComTestSocleExtensions";
    static final String COMMERCIAL_ID = "commercialId";
    static final String ETAT = "etat";
    static final String SERIE = "serie";
    private static final String PAQUET = "com.tsix.immocrm.domain.rapport.";
    private static final String NOM = "nom";
    private static final String LIBELLE = "libelle";
    private static final String TYPE = "type";
    private static final String MULTIPLE = "multiple";

    @Bean
    static BeanDefinitionRegistryPostProcessor rapportExtensionsDeTest() {
        return new Enregistrement();
    }

    /** Déclare le bean avec le type {@code Rapport} : le registre le reçoit dans sa {@code List<Rapport>}. */
    static final class Enregistrement implements BeanDefinitionRegistryPostProcessor {

        @Override
        public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
            Class<?> rapport = classeOuNull("Rapport");
            if (rapport == null) {
                return;
            }
            BeanFactory usine = (BeanFactory) registry;
            RootBeanDefinition definition = new RootBeanDefinition();
            definition.setTargetType(rapport);
            definition.setInstanceSupplier(() -> Proxy.newProxyInstance(rapport.getClassLoader(),
                    new Class<?>[]{rapport}, new Comportement(usine)));
            registry.registerBeanDefinition("rapportExtensionsDeTestBean", definition);
        }

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
            // rien : tout se joue à l'enregistrement
        }
    }

    private record Comportement(BeanFactory usine) implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return switch (method.getName()) {
                case "code" -> CODE;
                case "module" -> enumere(method.getReturnType(), "VENTE");
                case "parametres" -> parametres();
                case "executer" -> executer(args[0]);
                case "toString" -> CODE;
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> InvocationHandler.invokeDefault(proxy, method, args);
            };
        }

        private List<Object> parametres() {
            Class<?> parametre = classe("ParametreRapport");
            // B4 : options d'un CHOIX chargées en base par une dépendance du rapport.
            List<String> valeurs = usine.getBean(JdbcTemplate.class)
                    .queryForList("SELECT v FROM (VALUES ('A'), ('B')) AS t(v) ORDER BY v ASC", String.class);
            Class<?> option = typeDesElements(parametre, "options");
            List<Object> options = new ArrayList<>();
            for (String v : valeurs) {
                options.add(construire(option, Map.of("valeur", v, LIBELLE, "Etat " + v)));
            }
            Map<String, Object> commerciaux = new HashMap<>(Map.of(NOM, COMMERCIAL_ID, LIBELLE, "Commerciaux",
                    TYPE, "COMMERCIAUX", MULTIPLE, true, "options", List.of()));
            Map<String, Object> etat = new HashMap<>(Map.of(NOM, ETAT, LIBELLE, "Etat", TYPE, "CHOIX",
                    MULTIPLE, true, "options", options));
            return List.of(construire(parametre, commerciaux), construire(parametre, etat));
        }

        @SuppressWarnings("unchecked")
        private Object executer(Object contexte) throws ReflectiveOperationException {
            Class<?> type = classe("ContexteRapport");
            List<String> choix = (List<String>) type.getMethod("choix", String.class).invoke(contexte, ETAT);
            List<?> commerciaux = (List<?>) type.getMethod("commerciauxVisibles").invoke(contexte);
            return construire(classe("ResultatRapport"), Map.of("sections",
                    List.of(sectionCommerciaux(commerciaux), sectionDynamique(choix)), "avertissements", List.of()));
        }

        private Object sectionCommerciaux(List<?> commerciaux) {
            List<Map<String, Object>> lignes = new ArrayList<>();
            for (Object c : commerciaux) {
                Object cle = c instanceof MembreEquipe m ? CommerciauxVisibles.cle(m) : c;
                lignes.add(Map.of(COMMERCIAL_ID, String.valueOf(cle)));
            }
            return section("commerciaux", List.of(colonne(COMMERCIAL_ID, "TEXTE", "AUCUNE")), lignes, List.of());
        }

        // B5 : colonnes propres à ce résultat ; B3 : graphique LIGNES.
        private Object sectionDynamique(List<String> choix) {
            List<Object> colonnes = new ArrayList<>(List.of(colonne(SERIE, "TEXTE", "AUCUNE")));
            Map<String, Object> ligne = new LinkedHashMap<>(Map.of(SERIE, "S1"));
            for (String c : choix) {
                colonnes.add(colonne(c, "NOMBRE", "SOMME"));
                ligne.put(c, 1);
            }
            Object graphique = construire(classe("GraphiqueRapport"), Map.of(TYPE, "LIGNES", "titre", "Evolution",
                    "colonneCategorie", SERIE, "colonnesValeurs", List.copyOf(choix)));
            return section("dynamique", colonnes, List.of(ligne), List.of(graphique));
        }

        private static Object section(String id, List<Object> colonnes, List<Map<String, Object>> lignes,
                                      List<Object> graphiques) {
            Map<String, Object> valeurs = new HashMap<>();
            valeurs.put("id", id);
            valeurs.put("titre", "Section " + id);
            valeurs.put("colonnes", colonnes);
            valeurs.put("lignes", lignes);
            valeurs.put("graphiques", graphiques);
            return construire(classe("SectionRapport"), valeurs);
        }

        private static Object colonne(String nom, String type, String total) {
            return construire(classe("ColonneRapport"), Map.of(NOM, nom, LIBELLE, "Colonne " + nom, TYPE, type,
                    "fonctionTotal", total, "parDefaut", true));
        }
    }

    // ------------------------------------------------------------------ réflexion sur les noms du spec

    private static Class<?> classeOuNull(String nom) {
        try {
            return Class.forName(PAQUET + nom);
        } catch (ClassNotFoundException absent) {
            return null;
        }
    }

    private static Class<?> classe(String nom) {
        Class<?> c = classeOuNull(nom);
        if (c == null) {
            throw new IllegalStateException("classe du socle absente : " + nom);
        }
        return c;
    }

    private static Class<?> typeDesElements(Class<?> typeRecord, String composante) {
        for (RecordComponent c : typeRecord.getRecordComponents()) {
            if (c.getName().equals(composante) && c.getGenericType() instanceof ParameterizedType p) {
                return (Class<?>) p.getActualTypeArguments()[0];
            }
        }
        throw new IllegalStateException(typeRecord.getSimpleName() + "." + composante + " : liste typée attendue");
    }

    /** Record construit par nom de composante ; texte → constante d'énumération ; composante non fournie → défaut. */
    private static Object construire(Class<?> typeRecord, Map<String, Object> valeurs) {
        RecordComponent[] composantes = typeRecord.getRecordComponents();
        Class<?>[] types = new Class<?>[composantes.length];
        Object[] arguments = new Object[composantes.length];
        for (int i = 0; i < composantes.length; i++) {
            types[i] = composantes[i].getType();
            arguments[i] = valeur(types[i], valeurs.get(composantes[i].getName()));
        }
        try {
            return typeRecord.getDeclaredConstructor(types).newInstance(arguments);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(typeRecord.getSimpleName(), e);
        }
    }

    private static Object valeur(Class<?> type, Object fournie) {
        if (fournie instanceof String texte && type.isEnum()) {
            return enumere(type, texte);
        }
        if (fournie == null && type == boolean.class) {
            return false;
        }
        return fournie;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumere(Class<?> type, String nom) {
        return Enum.valueOf((Class<? extends Enum>) type, nom);
    }
}
