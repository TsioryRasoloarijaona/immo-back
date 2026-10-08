package com.tsix.immocrm.controller.encaissement;

import com.tsix.immocrm.domain.ChampDateEncaissement;
import com.tsix.immocrm.domain.ColonneEncaissement;
import com.tsix.immocrm.domain.Granularite;
import com.tsix.immocrm.domain.TriGroupe;
import com.tsix.immocrm.dto.common.GroupeDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.common.PageGroupesDto;
import com.tsix.immocrm.dto.encaissement.EncaissementDetailDto;
import com.tsix.immocrm.dto.encaissement.EncaissementDto;
import com.tsix.immocrm.services.encaissement.EncaissementService;
import com.tsix.immocrm.web.Groupements;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Encaissements")
@RestController
@RequestMapping("/api/encaissements")
public class EncaissementController {

    private final EncaissementService encaissementService;

    public EncaissementController(EncaissementService encaissementService) {
        this.encaissementService = encaissementService;
    }

    @GetMapping
    @Operation(summary = "Liste paginée des encaissements (règlements de Dossiers de vente)")
    public PageDto<EncaissementDto> findAll(
            @Parameter(description = "Id(s) d'entité juridique (CfgCompany). Répétable, OR. Absent = toutes.")
            @RequestParam(required = false) List<UUID> entiteId,
            @Parameter(description = "Id(s) de type de règlement (StlSettlementType, voir GET /api/referentiels/types-reglement). Répétable, OR.")
            @RequestParam(required = false) List<UUID> typeId,
            @Parameter(description = "État(s) (StlSettlement.Status : IN_WORK/POINTED/CANCELED confirmés). Répétable, OR, insensible à la casse.", example = "IN_WORK")
            @RequestParam(required = false) List<String> etat,
            @Parameter(description = "Id du compte bancaire de destination (StlAccount, voir GET /api/referentiels/comptes).")
            @RequestParam(required = false) UUID compteId,
            @Parameter(description = "Id de la catégorie de dépôt (StlDeposit, voir GET /api/referentiels/depots).")
            @RequestParam(required = false) UUID depotId,
            @Parameter(description = "Restreint aux règlements d'un seul Dossier (ComFolder.id).")
            @RequestParam(required = false) UUID dossierId,
            @RequestParam(required = false) List<UUID> trancheId,
            @Parameter(description = "Id du tiers réglant (CfgTier.id).")
            @RequestParam(required = false) UUID tierId,
            @Parameter(description = "Sous-chaîne sur la référence (StlSettlement.Code, ex. n° de chèque).")
            @RequestParam(required = false) String reference,
            @Parameter(description = "Recherche libre : référence OU nom du tiers réglant.")
            @RequestParam(required = false) String search,
            @Parameter(description = "true = uniquement comptabilisés, false = uniquement non comptabilisés, absent = tous.")
            @RequestParam(required = false) Boolean comptabilise,
            @Parameter(description = "Montant brut minimum (inclus).")
            @RequestParam(required = false) BigDecimal montantDe,
            @Parameter(description = "Montant brut maximum (inclus).")
            @RequestParam(required = false) BigDecimal montantA,
            @Parameter(description = "Jalon sur lequel appliquer dateDebut/dateFin. Ignoré si "
                    + "aucun des deux n'est fourni. Défaut : DATE_PIECE.", example = "DATE_ECHEANCE")
            @RequestParam(required = false) ChampDateEncaissement champDate,
            @Parameter(description = "Borne inférieure (incluse) de la plage sur champDate, format ISO-8601.", example = "2026-08-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @Parameter(description = "Borne supérieure (incluse) de la plage sur champDate, format ISO-8601.", example = "2026-08-31T23:59:59")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @Parameter(example = "code,description,montantBrut,typeLibelle,creePar")
            @RequestParam(required = false) List<String> colonnes,
            @Parameter(example = "typeLibelle")
            @RequestParam(required = false) String groupeColonne,
            @Parameter(example = "CHEQUE")
            @RequestParam(required = false) String groupeValeur,
            @Parameter(example = "MOIS")
            @RequestParam(required = false) Granularite granularite,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "docDate", direction = Sort.Direction.DESC) Pageable pageable) {
        ColonneEncaissement groupe = Groupements.colonneDeDepliage(
                groupeColonne, groupeValeur, ColonneEncaissement::resoudreGroupement);
        return PageDto.of(encaissementService.findAll(entiteId, typeId, etat, compteId, depotId, dossierId, trancheId,
                tierId, reference, search, comptabilise, montantDe, montantA, champDate, dateDebut, dateFin,
                groupe, groupeValeur, Groupements.granularite(groupe, granularite), pageable,
                ColonneEncaissement.resoudre(colonnes)));
    }

    @GetMapping("/groupes")
    @Operation(
            summary = "Liste des encaissements repliée en groupes, avec effectifs et totaux",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Groupes, paginés comme une liste"),
                    @ApiResponse(responseCode = "400", content = @Content)
            })
    public PageGroupesDto grouper(
            @Parameter(description = "Colonne de groupement. **Requis.** Un seul niveau de "
                    + "groupement est supporté.", example = "typeLibelle", required = true)
            @RequestParam String groupBy,
            @Parameter(example = "montantBrutRestant,montantNet,montantBrut:AVG")
            @RequestParam(required = false) List<String> agregats,
            @Parameter(example = "MOIS")
            @RequestParam(required = false) Granularite granularite,
            @Parameter(description = "Ordre des groupes : LIBELLE (croissant, défaut) ou NOMBRE "
                    + "(effectif décroissant).", example = "NOMBRE")
            @RequestParam(required = false) TriGroupe tri,
            @Parameter(example = "false")
            @RequestParam(defaultValue = "false") boolean inclureSansValeur,
            @RequestParam(required = false) List<UUID> entiteId,
            @RequestParam(required = false) List<UUID> typeId,
            @RequestParam(required = false) List<String> etat,
            @RequestParam(required = false) UUID compteId,
            @RequestParam(required = false) UUID depotId,
            @RequestParam(required = false) UUID dossierId,
            @RequestParam(required = false) List<UUID> trancheId,
            @RequestParam(required = false) UUID tierId,
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean comptabilise,
            @RequestParam(required = false) BigDecimal montantDe,
            @RequestParam(required = false) BigDecimal montantA,
            @RequestParam(required = false) ChampDateEncaissement champDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        ColonneEncaissement groupe = ColonneEncaissement.resoudreGroupement(groupBy);
        return encaissementService.grouper(entiteId, typeId, etat, compteId, depotId,
                dossierId, trancheId, tierId, reference, search, comptabilise, montantDe, montantA,
                champDate, dateDebut, dateFin, groupe,
                Groupements.granularite(groupe, granularite),
                ColonneEncaissement.resoudreAgregats(agregats),
                tri != null ? tri : TriGroupe.DEFAUT, inclureSansValeur, pageable).versPage();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fiche d'un encaissement (règlement) et ses affectations")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Encaissement trouvé."),
            @ApiResponse(responseCode = "404")
    })
    public EncaissementDetailDto findById(
            @Parameter(description = "Id du règlement (StlSettlement.id).")
            @PathVariable UUID id) {
        return encaissementService.findById(id);
    }
}
