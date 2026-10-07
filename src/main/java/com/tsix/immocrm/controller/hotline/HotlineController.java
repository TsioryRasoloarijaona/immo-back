package com.tsix.immocrm.controller.hotline;

import com.tsix.immocrm.dto.hotline.HotlineResultDto;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.services.hotline.HotlineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hotline")
@Tag(name = "Hotline")
public class HotlineController {

    private final HotlineService hotlineService;

    public HotlineController(HotlineService hotlineService) {
        this.hotlineService = hotlineService;
    }

    @GetMapping("/recherche")
    @Operation(summary = "Recherche complète par numéro de téléphone ou par nom")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recherche effectuée (que quelqu'un "
                    + "ait été trouvé ou non — voir le champ `connu`).",
                    content = @Content(schema = @Schema(implementation = HotlineResultDto.class))),
            @ApiResponse(responseCode = "400")
    })
    public HotlineResultDto rechercher(
            @Parameter(example = "06 12 34 56 78")
            @RequestParam(required = false) String telephone,
            @Parameter(
                    description = "Nom à rechercher (sous-chaîne, insensible à la casse ET aux "
                            + "accents), 2 caractères minimum. Exclusif avec `telephone`.",
                    example = "alami")
            @RequestParam(required = false) String nom) {

        boolean parTelephone = StringUtils.hasText(telephone);
        boolean parNom = StringUtils.hasText(nom);

        // Exactement un critère. Accepter les deux imposerait de décider lequel prime, et cette
        // décision serait invisible pour l'agent.
        if (parTelephone == parNom) {
            throw new InvalidRequestException(parTelephone
                    ? "Fournissez soit 'telephone', soit 'nom', mais pas les deux"
                    : "Fournissez soit 'telephone', soit 'nom'");
        }

        return parTelephone
                ? hotlineService.rechercherParTelephone(telephone)
                : hotlineService.rechercherParNom(nom);
    }
}
