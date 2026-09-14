package com.tsix.immocrm.controller.perimetre;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garde structurelle sur l'unique hypothèse que {@code PerimetreInterceptor} ne peut pas vérifier
 * lui-même.
 *
 * <p>L'intercepteur décide d'agir sur le <b>motif de route</b> : il exige une variable de chemin au
 * segment 3 ({@code /api/<collection>/{id}/…}) et refuse tout ce qui n'y résout pas un UUID. Le saut
 * « structurel » — motif sans variable au segment 3 — est le seul saut autorisé, et il est correct
 * <em>tant que</em> aucune route scopée ne place son identifiant de ressource ailleurs. C'est une
 * propriété des <b>routes</b>, pas de l'intercepteur : rien dans le code de l'intercepteur ne casse
 * le jour où quelqu'un ajoute {@code GET /api/dossiers/par-code/{code}/echeancier}, qui serait
 * silencieusement non gardée.
 *
 * <p>Ce test remplace la revue humaine par un échec de build : il énumère toutes les routes réellement
 * publiées par Spring et vérifie l'invariant. Ajouter une route scopée à segment littéral fait
 * échouer {@link #touteRouteScopeePorteSaVariableDIdAuSegment3} ; la lier en {@code String} plutôt
 * qu'en {@code UUID} fait échouer {@link #laVariableDIdDesRoutesScopeesEstToujoursUnUuid}.
 */
@SpringBootTest
class PerimetreInterceptorInvarianteTest {

    /** Les collections que {@code PerimetreInterceptor} contrôle (cf. {@code COLLECTIONS_SCOPEES}). */
    private static final Set<String> COLLECTIONS_SCOPEES =
            Set.of("dossiers", "prospections", "pre-prospections", "encaissements");

    /**
     * Les seules routes scopées légitimement dépourvues d'identifiant de ressource dans l'URL.
     * Toute nouvelle entrée ici doit être justifiée : soit la route ne charge aucune ressource
     * scopée (simulation), soit elle porte ses ids dans le corps et est contrôlée explicitement
     * ({@code DocumentLotService#genererLot}).
     */
    private static final Set<String> ROUTES_SANS_ID_AUTORISEES = Set.of(
            "/api/dossiers",
            "/api/dossiers/simulation-financement",
            "/api/dossiers/documents/fiche-reservation",
            "/api/dossiers/documents/acte-reservation",
            "/api/dossiers/documents/fiche-bancaire",
            "/api/dossiers/documents/fiche-visite-technique",
            "/api/dossiers/documents/fiche-dossier-vente",
            "/api/dossiers/documents/fiche-trop-percu",
            "/api/prospections",
            "/api/pre-prospections",
            "/api/encaissements",
            // Endpoints de groupement (FEATURE-groupement-listes.md). Routes de COLLECTION, sans
            // identifiant de ressource : elles ne chargent aucune ressource scopée à l'unité, elles
            // agrègent. Leur périmètre n'est donc pas l'affaire de l'intercepteur mais de la
            // requête elle-même — chacune applique PerimetreCommercialService#filtreDossiers /
            // #filtreProspections à la Specification, exactement comme la liste qu'elle replie.
            //
            // C'est vérifié, pas supposé : DossierGroupementTest#sommeDesEffectifs_egaleLeTotalDeLaListe
            // compare l'effectif des groupes au total de la liste à filtres identiques. Un périmètre
            // appliqué d'un seul côté ferait diverger les deux.
            "/api/dossiers/groupes",
            "/api/prospections/groupes",
            "/api/pre-prospections/groupes",
            // Même justification, pour les encaissements
            // (FEATURE-colonnes-groupement-encaissements.md) : EncaissementService#grouper compose
            // specEncaissements avec PerimetreCommercialService#filtreEncaissements, la même
            // Specification que la liste. Vérifié par
            // EncaissementGroupementTest#sommeDesEffectifs_egaleLeTotalDeLaListe.
            "/api/encaissements/groupes");

    // Qualifié : le contexte porte aussi le controllerEndpointHandlerMapping d'Actuator.
    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void touteRouteScopeePorteSaVariableDIdAuSegment3() {
        List<String> violations = new ArrayList<>();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entree : handlerMapping.getHandlerMethods().entrySet()) {
            for (String motif : motifs(entree.getKey())) {
                if (!estScopee(motif) || segment(motif, 3).startsWith("{")) {
                    continue;
                }
                if (!ROUTES_SANS_ID_AUTORISEES.contains(motif)) {
                    violations.add(motif + " → " + entree.getValue().getMethod().getName());
                }
            }
        }

        assertThat(violations)
                .as("""
                        Route(s) sous une collection soumise au périmètre commercial dont le motif ne \
                        porte pas de variable de chemin au segment 3. PerimetreInterceptor les saute \
                        (saut structurel) : elles ne sont donc PAS gardées. Soit la route ne charge \
                        aucune ressource scopée et doit être ajoutée à ROUTES_SANS_ID_AUTORISEES avec \
                        justification, soit elle doit être contrôlée explicitement dans son service, \
                        comme DocumentLotService#genererLot.""")
                .isEmpty();
    }

    /**
     * Le refus en fermeture repose sur « un id légitime est toujours un UUID valide ». Une route
     * scopée qui lierait son id en {@code String} (ou tout autre type) casserait l'équivalence :
     * l'intercepteur refuserait une valeur que le contrôleur, lui, accepterait parfaitement.
     */
    @Test
    void laVariableDIdDesRoutesScopeesEstToujoursUnUuid() {
        List<String> violations = new ArrayList<>();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entree : handlerMapping.getHandlerMethods().entrySet()) {
            for (String motif : motifs(entree.getKey())) {
                if (!estScopee(motif)) {
                    continue;
                }
                String segment = segment(motif, 3);
                if (!segment.startsWith("{")) {
                    continue;
                }
                String nom = nomVariable(segment);
                Class<?> type = typeDeLaVariable(entree.getValue(), nom);
                if (type == null) {
                    violations.add(motif + " : aucune @PathVariable nommée '" + nom + "' sur "
                            + entree.getValue().getMethod().getName());
                } else if (!UUID.class.equals(type)) {
                    violations.add(motif + " : '" + nom + "' est lié en " + type.getSimpleName()
                            + ", pas en UUID");
                }
            }
        }

        assertThat(violations)
                .as("""
                        La variable d'id d'une route scopée doit être liée en UUID. Sinon \
                        PerimetreInterceptor, qui refuse tout ce qui ne se résout pas en UUID, \
                        rejetterait (404) une valeur que le contrôleur accepterait — ou pire, \
                        laisserait passer une clé fonctionnelle non contrôlée.""")
                .isEmpty();
    }

    /** Recense les collections scopées effectivement publiées — garde-fou sur une faute de frappe. */
    @Test
    void lesQuatreCollectionsScopeesExistentBienDansLesRoutesPubliees() {
        Set<String> publiees = new TreeSet<>();
        for (RequestMappingInfo info : handlerMapping.getHandlerMethods().keySet()) {
            for (String motif : motifs(info)) {
                String collection = segment(motif, 2);
                if (COLLECTIONS_SCOPEES.contains(collection)) {
                    publiees.add(collection);
                }
            }
        }
        assertThat(publiees)
                .as("une collection listée dans COLLECTIONS_SCOPEES qui n'existe plus (ou renommée) "
                        + "rendrait le filtrage silencieusement inopérant")
                .containsExactlyInAnyOrderElementsOf(COLLECTIONS_SCOPEES);
    }

    // ------------------------------------------------------------------ utilitaires

    private static Set<String> motifs(RequestMappingInfo info) {
        if (info.getPathPatternsCondition() != null) {
            return info.getPathPatternsCondition().getPatternValues();
        }
        return info.getPatternsCondition() != null ? info.getPatternsCondition().getPatterns() : Set.of();
    }

    private static boolean estScopee(String motif) {
        return COLLECTIONS_SCOPEES.contains(segment(motif, 2));
    }

    private static String segment(String motif, int index) {
        String[] segments = motif.split("/");
        return segments.length > index ? segments[index] : "";
    }

    private static String nomVariable(String segmentDuMotif) {
        String nom = segmentDuMotif.substring(1, segmentDuMotif.endsWith("}")
                ? segmentDuMotif.length() - 1 : segmentDuMotif.length());
        int separateurRegex = nom.indexOf(':');
        return separateurRegex >= 0 ? nom.substring(0, separateurRegex) : nom;
    }

    private static Class<?> typeDeLaVariable(HandlerMethod methode, String nom) {
        for (MethodParameter parametre : methode.getMethodParameters()) {
            PathVariable annotation = parametre.getParameterAnnotation(PathVariable.class);
            if (annotation == null) {
                continue;
            }
            parametre.initParameterNameDiscovery(new org.springframework.core.DefaultParameterNameDiscoverer());
            String nomLie = !annotation.value().isEmpty() ? annotation.value()
                    : (!annotation.name().isEmpty() ? annotation.name() : parametre.getParameterName());
            if (nom.equals(nomLie)) {
                return parametre.getParameterType();
            }
        }
        return null;
    }
}
