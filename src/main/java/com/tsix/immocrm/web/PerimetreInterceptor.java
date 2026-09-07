package com.tsix.immocrm.web;

import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * <b>Le</b> point de contrôle du périmètre commercial sur l'accès à une ressource par id.
 *
 * <p>Le filtrage des listes se fait par {@code Specification} dans les services ; mais sans
 * contrôle sur l'accès direct, le périmètre se contournerait simplement en énumérant les ids. Or
 * un dossier est chargé par id par une trentaine de routes imbriquées (échéancier, règlements,
 * pièces jointes, historique, et une dizaine de générateurs de documents PDF/Word) — recopier la
 * vérification dans chaque méthode de service garantissait d'en oublier une. Cet intercepteur
 * applique donc la règle <b>une seule fois</b>, quelle que soit la profondeur de la route :
 *
 * <pre>
 *   /api/dossiers/{id}/…            → {@code verifierAccesDossier}
 *   /api/prospections/{id}/…        → {@code verifierAccesProspection}
 *   /api/pre-prospections/{id}/…    → {@code verifierAccesProspection}
 *   /api/encaissements/{id}         → {@code verifierAccesEncaissement}
 * </pre>
 *
 * <h2>Deux règles apprises à la dure — ne pas les défaire</h2>
 *
 * <p><b>1. L'id ne se lit pas dans l'URL.</b> Une première version découpait
 * {@code request.getRequestURI()}, qui n'est <b>pas décodé</b> alors que Spring décode chaque
 * segment : {@code /api/dossiers/1635ee6c%2Dbcd9%2D…} échouait au parse de l'intercepteur —
 * contrôle sauté — pendant que le contrôleur recevait un UUID valide et servait le dossier. On lit
 * donc les variables de chemin <b>déjà résolues par Spring</b>
 * ({@link HandlerMapping#URI_TEMPLATE_VARIABLES_ATTRIBUTE}).
 *
 * <p><b>2. Lire la même chaîne ne suffit pas : il faut aussi échouer en fermeture.</b> La version
 * suivante lisait bien la valeur de Spring, mais la <i>convertissait</i> autrement :
 * {@code UUID.fromString(valeur)} ici contre {@code UUID.fromString(source.trim())} dans le
 * {@code StringToUUIDConverter} de Spring. Un simple {@code %20} devant l'id rejouait la faille —
 * l'intercepteur n'y voyait pas un UUID et sautait, le contrôleur trimait et servait la ressource.
 * L'argument « si je n'arrive pas à le parser, le contrôleur n'y arrivera pas non plus, donc 400
 * sans rien servir » était donc <b>faux</b>, et tout raisonnement de cette forme l'est : il parie
 * sur le comportement d'une couche en aval.
 *
 * <p>La règle retenue ne parie plus : sur une collection soumise au périmètre, si la variable d'id
 * est présente et qu'aucun UUID ne s'en résout, on <b>refuse</b> (404). Recopier le {@code trim()}
 * de Spring aurait corrigé ce cas précis tout en reconduisant le contrat fragile — il resterait à
 * parier qu'aucune autre divergence de conversion n'existe, aujourd'hui ou après une montée de
 * version. Un id légitime est toujours un UUID valide : refuser le reste ne coûte aucun cas
 * d'usage. Le refus est émis en positionnant le statut et en renvoyant {@code false}, jamais en
 * levant une exception (qui deviendrait un 500).
 *
 * <p>Les sauts légitimes sont <b>structurels</b>, décidés sur la forme de la requête et jamais sur
 * la valeur de l'id : un motif sans variable d'id au segment 3 ({@code /api/dossiers/documents/…},
 * {@code /api/dossiers/simulation-financement}) ne désigne aucune ressource à contrôler, et une
 * <b>préflight CORS</b> ne demande pas une ressource mais l'autorisation d'une méthode.
 *
 * <p>Hors visibilité ⇒ {@code 404}, jamais {@code 403} : un 403 confirmerait l'existence de la
 * ressource.
 *
 * <p><b>Ce contrôle s'exécute désormais pour tout appelant</b>, et non plus seulement pour un
 * compte restreint par un périmètre : la visibilité par entité juridique
 * ({@code VisibiliteEntites}) est globale. Il en coûte une requête d'existence par route portant un
 * id — c'est le prix de la règle, sans quoi elle ne tiendrait que sur les listes.
 *
 * <p>Angle mort assumé et couvert ailleurs : les générateurs de documents <i>en lot</i>
 * ({@code POST /api/dossiers/documents/…}) reçoivent leurs ids dans le corps de la requête, hors
 * de portée d'un intercepteur ; ils sont contrôlés explicitement dans {@code DocumentLotService}.
 */
@Component
public class PerimetreInterceptor implements HandlerInterceptor {

    /** Les collections soumises au périmètre commercial — cf. §6.5 et §9bis.2. */
    private static final Set<String> COLLECTIONS_SCOPEES =
            Set.of("dossiers", "prospections", "pre-prospections", "encaissements");

    private final PerimetreCommercialService perimetreService;
    private final ObjectProvider<RequestMappingHandlerAdapter> handlerAdapter;
    private volatile HttpMessageConverter<Object> convertisseurProbleme;

    public PerimetreInterceptor(PerimetreCommercialService perimetreService,
                                 ObjectProvider<RequestMappingHandlerAdapter> handlerAdapter) {
        this.perimetreService = perimetreService;
        this.handlerAdapter = handlerAdapter;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws IOException {
        // Une préflight CORS ne désigne aucune ressource : elle demande « ai-je le droit d'émettre
        // un PATCH depuis cette origine ? », question à laquelle une règle de visibilité n'a rien à
        // répondre. Le navigateur l'envoie d'ailleurs sans cookie, donc sans appelant identifiable.
        // La refuser casserait TOUTE requête cross-origin non simple du frontend, pas seulement
        // celles portant un id masqué — y compris sur des ressources parfaitement visibles.
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }

        String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        Object variables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (pattern == null || !(variables instanceof Map)) {
            return true;
        }

        // Le motif — pas l'URL — dit quelle collection est visée et sous quel nom la variable d'id
        // a été liée : ["", "api", "<collection>", "{id}", ...]. Un motif sans variable au segment
        // 3 ne désigne aucune ressource : saut structurel, le seul autorisé.
        String[] segments = pattern.split("/");
        if (segments.length < 4 || !segments[3].startsWith("{")) {
            return true;
        }
        String collection = segments[2];
        if (!COLLECTIONS_SCOPEES.contains(collection)) {
            return true;
        }

        UUID id = parseUuid(((Map<String, String>) variables).get(nomVariable(segments[3])));
        if (id == null) {
            // Aucun UUID résoluble sur une route scopée : on refuse. Ne JAMAIS transformer ceci en
            // « return true » au motif que le contrôleur échouerait de toute façon à lier la
            // valeur — c'est exactement le pari qui a produit les contournements %2D puis %20.
            refuser(collection, response);
            return false;
        }

        switch (collection) {
            case "dossiers" -> perimetreService.verifierAccesDossier(id);
            case "prospections", "pre-prospections" -> perimetreService.verifierAccesProspection(id);
            case "encaissements" -> perimetreService.verifierAccesEncaissement(id);
            default -> throw new IllegalStateException("Collection scopée non traitée: " + collection);
        }
        return true;
    }

    /**
     * Écrit un 404 RFC 7807 et interrompt la chaîne, sans lever d'exception (qui deviendrait un
     * 500 — voir la note 2 ci-dessus).
     *
     * <p>Le corps est un vrai {@link ProblemDetail} sérialisé par le <b>convertisseur de messages
     * de Spring lui-même</b>, celui qu'utilise {@link GlobalExceptionHandler} pour tous les autres
     * 404 de l'API. Une première version écrivait le JSON à la main et produisait un enveloppe
     * différente : elle sérialisait {@code "type":"about:blank"} (que Spring omet, étant la valeur
     * par défaut) et omettait {@code instance} (que Spring renseigne avec le chemin de la requête).
     * Passer par le convertisseur supprime la possibilité même d'une dérive : il n'y a plus deux
     * représentations à garder synchronisées.
     *
     * <p>{@code instance} vaut la <b>collection</b> ({@code /api/dossiers}) et non le chemin de la
     * requête : ici l'identifiant est par définition une chaîne arbitraire fournie par l'appelant,
     * et un corps d'erreur ne doit jamais la renvoyer telle quelle. C'est la seule différence
     * assumée avec {@link GlobalExceptionHandler}, dont l'{@code instance} contient le chemin
     * complet — inoffensif là-bas, l'identifiant y étant toujours un UUID valide. Le motif de route
     * lui-même ({@code /api/dossiers/&#123;id&#125;}) n'est pas utilisable : les accolades ne sont
     * pas des caractères d'URI légaux.
     */
    private void refuser(String collection, HttpServletResponse response) throws IOException {
        ProblemDetail probleme = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, "Ressource introuvable: identifiant invalide.");
        // Spring pose lui-même `instance` quand un @ExceptionHandler renvoie un ProblemDetail
        // (AbstractMessageConverterMethodProcessor) ; hors de ce chemin, c'est à nous de le faire.
        probleme.setInstance(URI.create("/api/" + collection));

        response.setStatus(HttpStatus.NOT_FOUND.value());
        convertisseurProbleme().write(probleme, MediaType.APPLICATION_PROBLEM_JSON,
                new ServletServerHttpResponse(response));
    }

    /**
     * Le convertisseur que {@code RequestMappingHandlerAdapter} utilise pour écrire un
     * {@link ProblemDetail} — donc littéralement celui qui sérialise les réponses de
     * {@link GlobalExceptionHandler}. Résolu paresseusement via un {@link ObjectProvider} : une
     * injection directe créerait un cycle au démarrage, cet intercepteur étant enregistré depuis un
     * {@code WebMvcConfigurer}. Mémorisé après la première résolution.
     */
    @SuppressWarnings("unchecked")
    private HttpMessageConverter<Object> convertisseurProbleme() {
        HttpMessageConverter<Object> convertisseur = this.convertisseurProbleme;
        if (convertisseur == null) {
            convertisseur = handlerAdapter.getObject().getMessageConverters().stream()
                    .filter(c -> c.canWrite(ProblemDetail.class, MediaType.APPLICATION_PROBLEM_JSON))
                    .map(c -> (HttpMessageConverter<Object>) c)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Aucun HttpMessageConverter ne sait écrire un ProblemDetail en application/problem+json"));
            this.convertisseurProbleme = convertisseur;
        }
        return convertisseur;
    }

    /** {@code "{id}"} ou {@code "{id:regex}"} → {@code "id"}. */
    private static String nomVariable(String segmentDuMotif) {
        String nom = segmentDuMotif.substring(1, segmentDuMotif.endsWith("}")
                ? segmentDuMotif.length() - 1 : segmentDuMotif.length());
        int separateurRegex = nom.indexOf(':');
        return separateurRegex >= 0 ? nom.substring(0, separateurRegex) : nom;
    }

    private static UUID parseUuid(String valeur) {
        if (valeur == null) {
            return null;
        }
        try {
            return UUID.fromString(valeur);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
