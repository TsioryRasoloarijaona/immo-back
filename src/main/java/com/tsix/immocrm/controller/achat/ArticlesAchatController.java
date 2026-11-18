package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.dto.achat.config.ArticleAchatDto;
import com.tsix.immocrm.dto.achat.config.ArticleAchatEcritureDto;
import com.tsix.immocrm.dto.achat.config.ArticlesAchatDto;
import com.tsix.immocrm.dto.achat.config.SousFamilleAchatDto;
import com.tsix.immocrm.services.achat.config.ArticlesAchatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Achat — articles (nomenclature)")
@RestController
@RequestMapping("/api/achat/config/articles")
public class ArticlesAchatController {

    private static final String ID = "BuyItem.PKey.";
    private static final String ID_INCONNU = "Article inconnu.";
    private static final String INVALIDE = "Obligatoire manquant, référence inconnue, texte trop long, ou sous-famille hors "
            + "de la famille envoyée ; le message le nomme.";

    private final ArticlesAchatService service;

    public ArticlesAchatController(ArticlesAchatService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Articles d'achat triés par code, avec la recherche multi-critère")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Les articles retenus et les actions permises."),
            @ApiResponse(responseCode = "403", description = "Sans le droit Yes.Buy.Front.Config.BuyItemList.",
                    content = @Content)})
    public ArticlesAchatDto lister(
            @Parameter(description = "Texte cherché, casse ignorée, dans code, description, unité, codes famille et "
                    + "sous-famille.") @RequestParam(required = false) String search) {
        return service.lister(search);
    }

    @GetMapping("/sous-familles")
    @Operation(summary = "Sous-familles proposées sur la fiche Article, triées par code de famille puis code")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Toutes les sous-familles, avec leur famille."),
            @ApiResponse(responseCode = "403", description = "Sans le droit Yes.Buy.Front.Config.BuyItemList.",
                    content = @Content)})
    public List<SousFamilleAchatDto> sousFamilles() {
        return service.sousFamilles();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fiche d'un article d'achat")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "L'article."),
            @ApiResponse(responseCode = "403", description = "Sans le droit Yes.Buy.Front.Config.BuyItemList.",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = ID_INCONNU, content = @Content)})
    public ArticleAchatDto lire(@Parameter(description = ID) @PathVariable UUID id) {
        return service.lire(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crée un article d'achat")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "L'article créé, famille dérivée comprise."),
            @ApiResponse(responseCode = "400", description = INVALIDE, content = @Content),
            @ApiResponse(responseCode = "403", description = "Sans le droit Yes.Buy.Front.Config.BuyItemList.toolNew.",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "« Ce code existe déjà. »", content = @Content)})
    public ArticleAchatDto creer(@RequestBody ArticleAchatEcritureDto corps) {
        return service.creer(corps);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Modifie un article d'achat (champ absent ou null = inchangé)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "L'article à jour."),
            @ApiResponse(responseCode = "400", description = INVALIDE, content = @Content),
            @ApiResponse(responseCode = "403", description = "Sans le droit Yes.Buy.Front.Config.BuyItemList.toolNew.",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = ID_INCONNU, content = @Content),
            @ApiResponse(responseCode = "409", description = "« Ce code existe déjà. »", content = @Content)})
    public ArticleAchatDto modifier(@Parameter(description = ID) @PathVariable UUID id,
                                    @RequestBody ArticleAchatEcritureDto corps) {
        return service.modifier(id, corps);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprime un article d'achat que rien ne cite")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Article supprimé."),
            @ApiResponse(responseCode = "403", description = "Sans le droit Yes.Buy.Front.Config.BuyItemList.toolDelete.",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = ID_INCONNU, content = @Content),
            @ApiResponse(responseCode = "409", description = "Article cité par une ligne de pièce, de lot, de décompte, "
                    + "de sélection ou de journée ; le message donne les nombres.", content = @Content)})
    public void supprimer(@Parameter(description = ID) @PathVariable UUID id) {
        service.supprimer(id);
    }
}
