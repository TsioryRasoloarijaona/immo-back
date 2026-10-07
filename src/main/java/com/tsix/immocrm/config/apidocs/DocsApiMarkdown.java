package com.tsix.immocrm.config.apidocs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.beans.BeanUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;

/**
 * Textes OpenAPI longs lus dans {@code src/main/resources/api-docs/**.md} et injectés dans le
 * document springdoc. Voir instructions/FEATURE-commentaires-et-docs-api.md.
 */
public class DocsApiMarkdown {

    static final String DOSSIER_SCHEMAS = "schemas";
    private static final String PARAM = "param ";
    private static final String REPONSE = "reponse ";

    private record DocOperation(String fichier, String description,
                                Map<String, String> params, Map<String, String> reponses) {
    }

    private record DocSchema(String fichier, String nom, String description, Map<String, String> champs) {
    }

    /** Classe du contrôleur → nom de méthode → textes. */
    private final Map<Class<?>, Map<String, DocOperation>> operations = new HashMap<>();
    /** Nom du tag → texte. */
    private final Map<String, String> tags = new LinkedHashMap<>();
    private final List<DocSchema> schemas = new ArrayList<>();
    /** Tags de chaque opération dans l'ordre où springdoc les traite (voir ordonnerTags). */
    private final List<List<String>> tagsParOperation = new ArrayList<>();

    /**
     * @param fichiers  chemin relatif à {@code api-docs/} → contenu Markdown
     * @param controleurs classes (non proxifiées) des contrôleurs REST
     * @param classesParNom résout des noms simples de classe en classes candidates
     * @throws IllegalStateException si une section est orpheline ou si un texte a deux sources
     */
    public DocsApiMarkdown(Map<String, String> fichiers, Collection<Class<?>> controleurs,
                           Function<Set<String>, Map<String, List<Class<?>>>> classesParNom) {
        List<String> erreurs = new ArrayList<>();
        List<FichierDocMarkdown> docsControleurs = new ArrayList<>();
        List<FichierDocMarkdown> docsSchemas = new ArrayList<>();
        fichiers.forEach((chemin, contenu) -> {
            FichierDocMarkdown f = FichierDocMarkdown.lire(chemin, contenu);
            f.erreurs.forEach(e -> erreurs.add(chemin + " : " + e));
            if (!f.erreurs.isEmpty()) return;
            String[] parties = chemin.split("/");
            String base = parties[parties.length - 1].replaceFirst("\\.md$", "");
            if (parties.length != 2) {
                erreurs.add(chemin + " : attendu <domaine>/<Nom>.md ou schemas/<Nom>.md");
            } else if (!base.equals(f.titre)) {
                erreurs.add(chemin + " : le titre « # " + f.titre + " » doit être le nom du fichier");
            } else if (parties[0].equals(DOSSIER_SCHEMAS)) {
                docsSchemas.add(f);
            } else {
                docsControleurs.add(f);
            }
        });

        Map<String, List<Class<?>>> controleursParNom = new HashMap<>();
        for (Class<?> c : controleurs) controleursParNom.computeIfAbsent(c.getSimpleName(), k -> new ArrayList<>()).add(c);
        for (FichierDocMarkdown f : docsControleurs) lireControleur(f, controleursParNom, erreurs);

        Set<String> noms = new HashSet<>();
        docsSchemas.forEach(f -> noms.add(f.titre));
        Map<String, List<Class<?>>> classes = noms.isEmpty() ? Map.of() : classesParNom.apply(noms);
        for (FichierDocMarkdown f : docsSchemas) lireSchema(f, classes.getOrDefault(f.titre, List.of()), erreurs);

        if (!erreurs.isEmpty()) {
            throw new IllegalStateException("Documentation OpenAPI Markdown invalide :\n  - "
                    + String.join("\n  - ", erreurs));
        }
    }

    private static String ou(FichierDocMarkdown f, String section) {
        return f.chemin + ", section « " + section + " »";
    }

    private void lireControleur(FichierDocMarkdown f, Map<String, List<Class<?>>> parNom, List<String> erreurs) {
        List<Class<?>> candidats = parNom.getOrDefault(f.titre, List.of());
        if (candidats.size() != 1) {
            erreurs.add(ou(f, "# " + f.titre) + " : " + (candidats.isEmpty()
                    ? "aucun contrôleur REST de ce nom" : "plusieurs contrôleurs de ce nom"));
            return;
        }
        Class<?> classe = candidats.get(0);
        String domaine = f.chemin.substring(0, f.chemin.indexOf('/'));
        String paquet = classe.getPackageName();
        if (!paquet.substring(paquet.lastIndexOf('.') + 1).equals(domaine)) {
            erreurs.add(ou(f, "# " + f.titre) + " : le contrôleur est dans " + paquet
                    + ", le fichier devrait être sous api-docs/" + paquet.substring(paquet.lastIndexOf('.') + 1) + "/");
        }

        if (f.texteTete != null) {
            List<Tag> tagsClasse = tagsDe(classe);
            if (tagsClasse.size() != 1) {
                erreurs.add(ou(f, "# " + f.titre) + " : texte de tag, mais le contrôleur porte "
                        + tagsClasse.size() + " @Tag (un seul attendu)");
            } else if (!tagsClasse.get(0).description().isBlank()) {
                erreurs.add(ou(f, "# " + f.titre) + " : description déjà présente dans @Tag");
            } else if (tags.putIfAbsent(tagsClasse.get(0).name(), f.texteTete) != null) {
                erreurs.add(ou(f, "# " + f.titre) + " : tag « " + tagsClasse.get(0).name() + " » documenté deux fois");
            }
        }

        Map<String, DocOperation> parMethode = new HashMap<>();
        for (FichierDocMarkdown.Section s : f.sections.values()) {
            String ici = ou(f, "## " + s.titre());
            List<Method> methodes = Arrays.stream(classe.getMethods())
                    .filter(m -> m.getName().equals(s.titre()))
                    .filter(m -> AnnotatedElementUtils.hasAnnotation(m, RequestMapping.class))
                    .toList();
            if (methodes.size() != 1) {
                erreurs.add(ici + " : " + (methodes.isEmpty()
                        ? "aucune méthode de handler de ce nom" : "nom de méthode surchargé"));
                continue;
            }
            Method methode = methodes.get(0);
            Operation op = AnnotatedElementUtils.findMergedAnnotation(methode, Operation.class);
            if (s.texte() != null && op != null && !op.description().isBlank()) {
                erreurs.add(ici + " : description déjà présente dans @Operation");
            }
            Map<String, String> params = new LinkedHashMap<>();
            Map<String, String> reponses = new LinkedHashMap<>();
            Map<String, Parameter> parametres = parametres(methode);
            Map<String, ApiResponse> apiReponses = reponses(methode, classe, op);
            s.sousSections().forEach((titre, texte) -> {
                String sous = ou(f, "## " + s.titre() + " / ### " + titre);
                if (texte == null) {
                    erreurs.add(sous + " : section vide");
                } else if (titre.startsWith(PARAM)) {
                    String nom = titre.substring(PARAM.length()).strip();
                    if (!parametres.containsKey(nom)) erreurs.add(sous + " : aucun paramètre « " + nom + " »");
                    else if (parametres.get(nom) != null && !parametres.get(nom).description().isBlank())
                        erreurs.add(sous + " : description déjà présente dans @Parameter");
                    else params.put(nom, texte);
                } else if (titre.startsWith(REPONSE)) {
                    String code = titre.substring(REPONSE.length()).strip();
                    if (!apiReponses.containsKey(code)) erreurs.add(sous + " : aucune @ApiResponse « " + code + " »");
                    else if (!apiReponses.get(code).description().isBlank())
                        erreurs.add(sous + " : description déjà présente dans @ApiResponse");
                    else reponses.put(code, texte);
                } else {
                    erreurs.add(sous + " : attendu « ### param <nom> » ou « ### reponse <code> »");
                }
            });
            parMethode.put(methode.getName(), new DocOperation(f.chemin, s.texte(), params, reponses));
        }
        operations.put(classe, parMethode);
    }

    private void lireSchema(FichierDocMarkdown f, List<Class<?>> candidats, List<String> erreurs) {
        if (candidats.size() != 1) {
            erreurs.add(ou(f, "# " + f.titre) + " : " + (candidats.isEmpty()
                    ? "aucune classe de ce nom" : "plusieurs classes de ce nom"));
            return;
        }
        Class<?> classe = candidats.get(0);
        Schema annotation = classe.getAnnotation(Schema.class);
        if (annotation != null && !annotation.name().isBlank() && !annotation.name().equals(f.titre)) {
            erreurs.add(ou(f, "# " + f.titre) + " : le schéma OpenAPI s'appelle « " + annotation.name()
                    + " » (@Schema(name)), non pris en charge");
            return;
        }
        if (f.texteTete != null && aUneDescription(classe)) {
            erreurs.add(ou(f, "# " + f.titre) + " : description déjà présente dans @Schema");
        }
        Map<String, String> champs = new LinkedHashMap<>();
        for (FichierDocMarkdown.Section s : f.sections.values()) {
            String ici = ou(f, "## " + s.titre());
            List<AnnotatedElement> elements = elementsDuChamp(classe, s.titre());
            if (!s.sousSections().isEmpty()) erreurs.add(ici + " : pas de « ### » dans un schéma");
            if (s.texte() == null) erreurs.add(ici + " : section vide");
            else if (elements.isEmpty()) erreurs.add(ici + " : aucun champ publié « " + s.titre() + " » dans " + classe.getName()
                    + " (absent, static, @JsonIgnore ou @Schema(hidden = true))");
            else if (elements.stream().anyMatch(DocsApiMarkdown::aUneDescription))
                erreurs.add(ici + " : description déjà présente dans @Schema");
            else champs.put(s.titre(), s.texte());
        }
        schemas.add(new DocSchema(f.chemin, f.titre, f.texteTete, champs));
    }

    private static boolean aUneDescription(AnnotatedElement e) {
        Schema s = e.getAnnotation(Schema.class);
        return s != null && !s.description().isBlank();
    }

    /**
     * Les éléments portant la propriété OpenAPI {@code nom} : composant de record et accesseur, ou
     * champ. Vide si la propriété n'existe pas ou si OpenAPI l'omet (static, @JsonIgnore,
     * {@code @Schema(hidden = true)}). Le nom suit {@code @JsonProperty} s'il est présent.
     */
    private static List<AnnotatedElement> elementsDuChamp(Class<?> classe, String nom) {
        List<List<AnnotatedElement>> candidats = new ArrayList<>();
        if (classe.isRecord()) {
            for (RecordComponent rc : classe.getRecordComponents()) {
                List<AnnotatedElement> e = new ArrayList<>(List.of(rc, rc.getAccessor()));
                try {
                    e.add(classe.getDeclaredField(rc.getName()));
                } catch (NoSuchFieldException ignore) {
                    // un record a toujours son champ privé
                }
                candidats.add(e);
            }
        } else {
            for (Class<?> c = classe; c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field champ : c.getDeclaredFields()) {
                    if (!Modifier.isStatic(champ.getModifiers()) && !champ.isSynthetic()) candidats.add(List.of(champ));
                }
            }
        }
        for (List<AnnotatedElement> e : candidats) {
            String nomJava = e.get(0) instanceof RecordComponent rc ? rc.getName() : ((Field) e.get(0)).getName();
            String nomJson = e.stream().map(x -> valeurJackson(x, "JsonProperty")).filter(v -> v instanceof String t && !t.isEmpty())
                    .map(String.class::cast).findFirst().orElse(nomJava);
            if (!nomJson.equals(nom)) continue;
            boolean omis = e.stream().anyMatch(x -> Boolean.TRUE.equals(valeurJackson(x, "JsonIgnore"))
                    || (x.getAnnotation(Schema.class) != null && x.getAnnotation(Schema.class).hidden()));
            return omis ? List.of() : e;
        }
        return List.of();
    }

    // Par nom simple : évite de lier ce code à une version précise de jackson-annotations.
    private static Object valeurJackson(AnnotatedElement e, String annotation) {
        for (var a : e.getAnnotations()) {
            if (a.annotationType().getSimpleName().equals(annotation)) {
                try {
                    return a.annotationType().getMethod("value").invoke(a);
                } catch (ReflectiveOperationException ex) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Nom OpenAPI → @Parameter éventuel, pour chaque argument que springdoc publie en paramètre
     * (chemin, requête, en-tête, cookie). Le corps, les parties multipart, les types techniques
     * (requête servlet, principal…) et les objets non annotés n'en sont pas.
     */
    private static Map<String, Parameter> parametres(Method methode) {
        Map<String, Parameter> out = new LinkedHashMap<>();
        DefaultParameterNameDiscoverer noms = new DefaultParameterNameDiscoverer();
        for (int i = 0; i < methode.getParameterCount(); i++) {
            MethodParameter mp = new MethodParameter(methode, i);
            mp.initParameterNameDiscovery(noms);
            Parameter p = mp.getParameterAnnotation(Parameter.class);
            if (p != null && p.hidden()) continue;
            String nom = nomPublie(mp);
            if (nom == null) continue;
            out.put(p != null && !p.name().isBlank() ? p.name() : nom, p);
        }
        AnnotatedElementUtils.findMergedRepeatableAnnotations(methode, Parameter.class, Parameters.class).stream()
                .filter(p -> !p.name().isBlank() && !p.hidden())
                .forEach(p -> out.put(p.name(), p));
        return out;
    }

    private static String nomPublie(MethodParameter mp) {
        Class<?> type = mp.getParameterType();
        if (mp.hasParameterAnnotation(RequestBody.class) || mp.hasParameterAnnotation(RequestPart.class)) return null;
        if (Arrays.stream(mp.getParameterAnnotations())
                .anyMatch(a -> a.annotationType().getSimpleName().equals("AuthenticationPrincipal"))) return null;
        RequestParam rp = mp.getParameterAnnotation(RequestParam.class);
        if (rp != null) {
            Class<?> element = type.isArray() ? type.getComponentType()
                    : Collection.class.isAssignableFrom(type) ? mp.nested().getNestedParameterType() : type;
            if (MultipartFile.class.isAssignableFrom(element) || Map.class.isAssignableFrom(type)) return null;
            return nom(rp.name(), rp.value(), mp);
        }
        PathVariable pv = mp.getParameterAnnotation(PathVariable.class);
        if (pv != null) return Map.class.isAssignableFrom(type) ? null : nom(pv.name(), pv.value(), mp);
        RequestHeader rh = mp.getParameterAnnotation(RequestHeader.class);
        if (rh != null) return Map.class.isAssignableFrom(type) ? null : nom(rh.name(), rh.value(), mp);
        CookieValue cv = mp.getParameterAnnotation(CookieValue.class);
        if (cv != null) return nom(cv.name(), cv.value(), mp);
        // Sans annotation : springdoc publie Pageable en un seul paramètre « pageable » (pas de
        // @ParameterObject ici) et un type simple en paramètre de requête ; le reste est ignoré
        // ou éclaté en champs, qu'on ne documente pas ici.
        if (Pageable.class.isAssignableFrom(type)) return mp.getParameterName();
        if (BeanUtils.isSimpleValueType(type) && !Locale.class.equals(type) && !TimeZone.class.equals(type)
                && !ZoneId.class.equals(type)) return mp.getParameterName();
        return null;
    }

    // Annotations lues brutes : l'alias name/value n'est pas résolu, on regarde les deux.
    private static String nom(String name, String value, MethodParameter mp) {
        return !name.isBlank() ? name : !value.isBlank() ? value : mp.getParameterName();
    }

    /** Code → @ApiResponse déclarée sur la méthode, dans @Operation, ou sur la classe. */
    private static Map<String, ApiResponse> reponses(Method methode, Class<?> classe, Operation op) {
        Map<String, ApiResponse> out = new LinkedHashMap<>();
        AnnotatedElementUtils.findMergedRepeatableAnnotations(classe, ApiResponse.class, ApiResponses.class)
                .forEach(r -> out.put(r.responseCode(), r));
        if (op != null) for (ApiResponse r : op.responses()) out.put(r.responseCode(), r);
        AnnotatedElementUtils.findMergedRepeatableAnnotations(methode, ApiResponse.class, ApiResponses.class)
                .forEach(r -> out.put(r.responseCode(), r));
        return out;
    }

    /** Les @Tag d'un élément, dans l'ordre où springdoc les lit (OpenAPIService.buildTagsFrom*). */
    private static List<Tag> tagsDe(AnnotatedElement e) {
        Set<Tag> out = new LinkedHashSet<>();
        AnnotatedElementUtils.findAllMergedAnnotations(e, Tags.class).forEach(t -> out.addAll(List.of(t.value())));
        out.addAll(AnnotatedElementUtils.findAllMergedAnnotations(e, Tag.class));
        return new ArrayList<>(out);
    }

    // ---------------------------------------------------------------- injection

    /** {@code OperationCustomizer} : appelé par springdoc pour chaque opération, dans l'ordre de traitement. */
    public io.swagger.v3.oas.models.Operation personnaliserOperation(
            io.swagger.v3.oas.models.Operation operation, HandlerMethod handler) {
        List<String> nomsTags = new ArrayList<>();
        tagsDe(handler.getMethod()).forEach(t -> nomsTags.add(t.name()));
        tagsDe(handler.getBeanType()).forEach(t -> nomsTags.add(t.name()));
        synchronized (tagsParOperation) {
            tagsParOperation.add(nomsTags);
        }

        DocOperation doc = operations.getOrDefault(handler.getBeanType(), Map.of()).get(handler.getMethod().getName());
        if (doc == null) return operation;
        String ici = doc.fichier() + ", section « ## " + handler.getMethod().getName() + " »";
        if (doc.description() != null) operation.setDescription(doc.description());
        doc.params().forEach((nom, texte) -> {
            io.swagger.v3.oas.models.parameters.Parameter p = operation.getParameters() == null ? null
                    : operation.getParameters().stream().filter(x -> nom.equals(x.getName())).findFirst().orElse(null);
            if (p == null) throw new IllegalStateException(ici + " : paramètre « " + nom + " » absent du document OpenAPI");
            p.setDescription(texte);
        });
        doc.reponses().forEach((code, texte) -> {
            io.swagger.v3.oas.models.responses.ApiResponse r =
                    operation.getResponses() == null ? null : operation.getResponses().get(code);
            if (r == null) throw new IllegalStateException(ici + " : réponse « " + code + " » absente du document OpenAPI");
            r.setDescription(texte);
        });
        return operation;
    }

    /** {@code OpenApiCustomizer} : tags et schémas, une fois le document construit. */
    public void personnaliserOpenApi(OpenAPI openApi) {
        List<List<String>> sequence;
        synchronized (tagsParOperation) {
            sequence = new ArrayList<>(tagsParOperation);
            tagsParOperation.clear();
        }
        if (!tags.isEmpty()) ordonnerTags(openApi, sequence);

        for (DocSchema doc : schemas) {
            io.swagger.v3.oas.models.media.Schema<?> schema = openApi.getComponents() == null
                    || openApi.getComponents().getSchemas() == null
                    ? null : openApi.getComponents().getSchemas().get(doc.nom());
            if (schema == null) throw new IllegalStateException(doc.fichier() + " : schéma « " + doc.nom() + " » absent du document OpenAPI");
            if (doc.description() != null) schema.setDescription(doc.description());
            doc.champs().forEach((champ, texte) -> {
                io.swagger.v3.oas.models.media.Schema<?> propriete =
                        schema.getProperties() == null ? null : schema.getProperties().get(champ);
                if (propriete == null) throw new IllegalStateException(doc.fichier() + ", section « ## " + champ
                        + " » : propriété absente du schéma OpenAPI");
                propriete.setDescription(texte);
            });
        }
    }

    /**
     * Pose les descriptions de tag et rétablit l'ordre de la liste globale. Springdoc la reconstruit
     * à chaque opération en itérant un {@code HashSet} de tags dont le hash inclut la description,
     * et omet un @Tag sans description : on rejoue donc cette construction avec les descriptions
     * finales, pour obtenir l'ordre qu'aurait donné le texte resté dans l'annotation. Piège : un
     * retour à la ligne change le hash, donc l'ordre — un texte de tag garde ses paragraphes sur
     * une ligne, identique à l'ancien texte d'annotation.
     */
    private void ordonnerTags(OpenAPI openApi, List<List<String>> sequence) {
        Map<String, io.swagger.v3.oas.models.tags.Tag> finaux = new LinkedHashMap<>();
        if (openApi.getTags() != null) openApi.getTags().forEach(t -> finaux.put(t.getName(), t));
        tags.forEach((nom, texte) -> {
            io.swagger.v3.oas.models.tags.Tag t = finaux.get(nom);
            if (t == null) {
                if (sequence.stream().noneMatch(s -> s.contains(nom)))
                    throw new IllegalStateException("tag « " + nom + " » : aucune opération du document OpenAPI");
                t = new io.swagger.v3.oas.models.tags.Tag().name(nom);
                finaux.put(nom, t);
            }
            t.setDescription(texte);
        });

        List<io.swagger.v3.oas.models.tags.Tag> courant = new ArrayList<>();
        for (List<String> nomsOperation : sequence) {
            Set<io.swagger.v3.oas.models.tags.Tag> ensemble = new HashSet<>();
            for (String nom : nomsOperation) {
                io.swagger.v3.oas.models.tags.Tag t = finaux.get(nom);
                if (t != null && ensemble.stream().noneMatch(x -> x.getName().equals(nom))) ensemble.add(t);
            }
            if (ensemble.isEmpty()) continue;
            ensemble.addAll(courant);
            courant = new ArrayList<>(ensemble);
        }
        if (!new HashSet<>(courant).equals(new HashSet<>(finaux.values()))) {
            throw new IllegalStateException("tags OpenAPI : l'ordre rejoué ne couvre pas les tags du document "
                    + courant.stream().map(io.swagger.v3.oas.models.tags.Tag::getName).toList() + " ≠ " + finaux.keySet());
        }
        openApi.setTags(courant);
    }
}
