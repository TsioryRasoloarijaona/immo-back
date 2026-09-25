package com.tsix.immocrm.controller.auth;

import com.tsix.immocrm.dto.auth.CleApiCreeeDto;
import com.tsix.immocrm.dto.auth.CleApiDto;
import com.tsix.immocrm.dto.auth.CreerCleApiRequest;
import com.tsix.immocrm.services.auth.CleApiAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Clés d'API (admin)", description = "Clés d'API des clients machine (n8n, agents), "
        + "réservées au rôle Administrateur. Une clé s'envoie dans l'en-tête `X-Api-Key` ; elle "
        + "authentifie un compte de service et n'autorise que les routes de ses portées. Le secret "
        + "n'est montré qu'à la création et à la régénération. Voir `FEATURE-cle-api-n8n.md`.")
@RestController
@RequestMapping("/api/cles-api")
public class CleApiController {

    private final CleApiAdminService service;

    public CleApiController(CleApiAdminService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(
            summary = "Liste les clés d'API",
            description = "Toutes les clés, actives et révoquées, les plus récentes d'abord. Le "
                    + "secret n'est **jamais** renvoyé : `clePrefixe` (`ak_` + 8 caractères) suffit "
                    + "à reconnaître la clé configurée côté client. `dernierUsage` est tracé à la "
                    + "minute près."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste, éventuellement vide."),
            @ApiResponse(responseCode = "403", description = "L'appelant n'a pas le rôle Administrateur "
                    + "— ou appelle avec une clé d'API, qui n'a jamais cette portée.")
    })
    public List<CleApiDto> lister() {
        return service.lister();
    }

    @PostMapping
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(
            summary = "Crée une clé d'API et son compte de service",
            description = "Génère un secret aléatoire (`ak_` + 43 caractères), n'en stocke que le "
                    + "hash SHA-256, et le renvoie **une seule fois** dans `cle`. Sauf `compteCode`, "
                    + "un compte `AdmUser` est créé au nom de la clé, sans employé ni rôle : c'est "
                    + "lui qui apparaît en auteur (`UserCreate`) de tout ce que la clé écrit. Un tel "
                    + "compte n'a **aucune restriction de périmètre** (règle existante : pas "
                    + "d'habilitation = tout voir) — la portée est sa seule limite, d'où l'exigence "
                    + "d'au moins une portée.\n\n"
                    + "**Pour n8n** : `{\"nom\":\"n8n\",\"portees\":[\"PRE_PROSPECTIONS_ECRIRE\","
                    + "\"REFERENTIELS_LIRE\"]}`, puis credential Header Auth `X-Api-Key` dans n8n."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Clé créée ; `cle` est le secret, à copier maintenant."),
            @ApiResponse(responseCode = "400", description = "`nom` vide ou trop long ; `portees` vide "
                    + "ou portant un code inconnu ; `compteCode` inconnu."),
            @ApiResponse(responseCode = "403", description = "L'appelant n'a pas le rôle Administrateur."),
            @ApiResponse(responseCode = "409", description = "Une clé de ce nom existe déjà ; ou un "
                    + "compte `AdmUser` de ce code existe déjà sans que `compteCode` le désigne.")
    })
    public ResponseEntity<CleApiCreeeDto> creer(@RequestBody CreerCleApiRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creer(request));
    }

    @PostMapping("/{id}/regenerer")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(
            summary = "Régénère le secret d'une clé",
            description = "Nouveau secret, même compte de service, mêmes portées. L'ancien secret "
                    + "cesse d'être accepté immédiatement. À utiliser après une fuite ou pour une "
                    + "rotation ; une clé révoquée ne se régénère pas, on en crée une nouvelle."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nouveau secret dans `cle`, montré une seule fois."),
            @ApiResponse(responseCode = "403", description = "L'appelant n'a pas le rôle Administrateur."),
            @ApiResponse(responseCode = "404", description = "Aucune clé avec cet id."),
            @ApiResponse(responseCode = "409", description = "La clé est révoquée.")
    })
    public CleApiCreeeDto regenerer(
            @Parameter(description = "Identifiant de la clé (AppApiClient.PKey).") @PathVariable UUID id) {
        return service.regenerer(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(
            summary = "Révoque une clé",
            description = "La clé est refusée en 401 dès le commit ; la ligne reste listée avec "
                    + "`actif = false`, `revoqueLe` et `revoquePar`. Idempotent : révoquer une clé "
                    + "déjà révoquée ne change rien. Le compte de service n'est pas touché."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Révoquée (ou déjà révoquée)."),
            @ApiResponse(responseCode = "403", description = "L'appelant n'a pas le rôle Administrateur."),
            @ApiResponse(responseCode = "404", description = "Aucune clé avec cet id.")
    })
    public ResponseEntity<Void> revoquer(
            @Parameter(description = "Identifiant de la clé (AppApiClient.PKey).") @PathVariable UUID id) {
        service.revoquer(id);
        return ResponseEntity.noContent().build();
    }
}
