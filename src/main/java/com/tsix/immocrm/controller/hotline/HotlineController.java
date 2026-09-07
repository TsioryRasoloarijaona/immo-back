package com.tsix.immocrm.controller.hotline;

import com.tsix.immocrm.dto.hotline.HotlineResultDto;
import com.tsix.immocrm.services.hotline.HotlineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * "Fiche hotline" — a single-call, phone-number-first lookup used when someone calls in: is this
 * caller already known, and if so, what does the system know about them (identity, past
 * prospections, past dossiers de vente, règlements, interactions...)? See {@link HotlineService}.
 */
@RestController
@RequestMapping("/api/hotline")
@Tag(name = "Hotline", description = "Recherche d'un correspondant par numéro de téléphone, avec "
        + "tout son historique d'activité (prospections, dossiers de vente, règlements, "
        + "interactions...) en une seule requête. Pensé pour un agent hotline qui doit "
        + "identifier un appelant et voir instantanément son passé dans le CRM.\n\n"
        + "**Entités juridiques en exploitation.** Les blocs prospections, dossiers, règlements et "
        + "interactions ne remontent que ce qui relève d'une entité juridique active "
        + "(`CfgCompany.IsActive`) — aujourd'hui ADMOD et GHRIZA. Un correspondant dont toute "
        + "l'activité porte sur une entité hors exploitation est donc trouvé avec un historique "
        + "vide, ce qui n'est pas une erreur.")
public class HotlineController {

    private final HotlineService hotlineService;

    public HotlineController(HotlineService hotlineService) {
        this.hotlineService = hotlineService;
    }

    @GetMapping("/recherche")
    @Operation(
            summary = "Recherche complète par numéro de téléphone",
            description = "Recherche, en une seule requête, tout ce que le système sait sur un "
                    + "numéro de téléphone donné :\n\n"
                    + "1. **Fiches existantes** — toute personne (CfgTier : prospect, client, "
                    + "contact...) dont un des numéros enregistrés (Phone, Phone1, Phone2, "
                    + "Mobile) correspond. Pour chacune : informations générales, prospections, "
                    + "dossiers de vente, règlements, interactions et historique de statut.\n\n"
                    + "2. **Appels sans fiche** — pré-prospections (premiers contacts jamais "
                    + "convertis en fiche) dont le numéro renseigné à l'époque correspond, "
                    + "signalant un appelant déjà contacté par le passé mais jamais créé "
                    + "formellement.\n\n"
                    + "La comparaison ignore les espaces, tirets, parenthèses et l'indicatif "
                    + "pays : \"+212 6 12 34 56 78\", \"212612345678\" et \"0612345678\" sont "
                    + "considérés comme le même numéro. Ne renvoie jamais 404 : si personne ne "
                    + "correspond, `connu` vaut `false` et les listes sont vides — c'est au "
                    + "front de proposer la création d'une nouvelle fiche dans ce cas.\n\n"
                    + "**Périmètre commercial** : si l'appelant est restreint "
                    + "(`UtilisateurCourantDto.perimetreRestreint = true`), les listes imbriquées "
                    + "de chaque fiche (prospections, dossiers, règlements, interactions) ainsi "
                    + "que `appelsSansFiche` ne contiennent que les lignes de ses projets "
                    + "affectés. La **fiche elle-même n'est pas masquée** : le tier reste "
                    + "identifiable — c'est le but de cet écran, un agent doit pouvoir dire qui "
                    + "appelle — mais son activité sur les autres projets ne lui est pas montrée. "
                    + "Un appelant peut donc apparaître `connu` avec des listes vides."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recherche effectuée (que quelqu'un "
                    + "ait été trouvé ou non — voir le champ `connu`).",
                    content = @Content(schema = @Schema(implementation = HotlineResultDto.class))),
            @ApiResponse(responseCode = "400", description = "Le paramètre `telephone` est "
                    + "manquant, vide, ou ne contient aucun chiffre exploitable.")
    })
    public HotlineResultDto rechercher(
            @Parameter(
                    description = "Numéro de téléphone à rechercher, dans n'importe quel format "
                            + "utilisé par les agents ou l'ancien système (avec ou sans "
                            + "indicatif pays, avec ou sans espaces).",
                    required = true,
                    example = "06 12 34 56 78")
            @RequestParam String telephone) {
        return hotlineService.rechercherParTelephone(telephone);
    }
}
