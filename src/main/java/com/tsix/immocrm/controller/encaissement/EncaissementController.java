package com.tsix.immocrm.controller.encaissement;

import com.tsix.immocrm.domain.ChampDateEncaissement;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.encaissement.EncaissementDto;
import com.tsix.immocrm.services.encaissement.EncaissementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

@Tag(name = "Encaissements", description = "Règlements (StlSettlement) rattachés à un Dossier de "
        + "vente — reproduit l'écran legacy \"Encaissements Vente\" (module Vente d'OMRAN Estate). "
        + "Pour l'historique des règlements d'une seule échéance, voir plutôt "
        + "`GET /api/dossiers/{id}/echeancier/{echeanceId}/reglements`, endpoint plus léger et "
        + "déjà scopé à un dossier — celui-ci est la vue transverse \"tous les encaissements\" "
        + "avec les mêmes filtres que le tableau legacy (entité, type, état, période, compte, "
        + "dépôt...).\n\n"
        + "Collection soumise au périmètre commercial : sur **toutes** les routes portant un "
        + "identifiant, un encaissement hors du périmètre de l'appelant renvoie 404 (jamais 403 — "
        + "un 403 confirmerait son existence), et un identifiant qui n'est pas un UUID valide "
        + "renvoie 404 lui aussi, plutôt que le 400 habituel : il est refusé avant d'atteindre le "
        + "contrôleur. Voir `perimetreRestreint` sur GET /api/mon-compte.\n\n"
        + "**Entités juridiques en exploitation.** Ne sont renvoyés que les règlements portés par "
        + "une entité juridique active (`StlSettlement.CfgCompanyId` → `CfgCompany.IsActive`) — "
        + "aujourd'hui ADMOD et GHRIZA. Restriction **globale et permanente**, distincte du "
        + "périmètre commercial : elle s'applique à tout appelant, rôles exemptés compris. Un "
        + "règlement masqué renvoie 404 sur toutes les routes portant son identifiant.")
@RestController
@RequestMapping("/api/encaissements")
public class EncaissementController {

    private final EncaissementService encaissementService;

    public EncaissementController(EncaissementService encaissementService) {
        this.encaissementService = encaissementService;
    }

    @GetMapping
    @Operation(
            summary = "Liste paginée des encaissements (règlements de Dossiers de vente)",
            description = "Toujours restreint aux règlements liés à au moins une échéance de "
                    + "Dossier de vente (les règlements d'achat/location, s'il y en a, ne "
                    + "remontent jamais ici), et aux **encaissements** : un décaissement "
                    + "(`StlSettlement.IsCredit` faux, typiquement un remboursement à montant "
                    + "négatif) ne remonte jamais, comme sur l'écran legacy. "
                    + "`montantBrutRestant` (calculé : montant brut moins "
                    + "la part déjà affectée à une échéance) et `montantNet` (montant brut moins "
                    + "retenue source/timbre) sont calculés côté serveur, jamais stockés. "
                    + "`etat` accepte les 3 valeurs confirmées sur données réelles (IN_WORK, "
                    + "POINTED, CANCELED) — l'écran legacy propose aussi \"Impayé(e)\"/\"Perdu(e)\" "
                    + "comme filtres mais aucune ligne réelle ne les porte à ce jour ; passés tel "
                    + "quel (comparaison insensible à la casse), donc utilisables dès qu'un vrai "
                    + "code sera confirmé, sans changement d'API.\n\n"
                    + "**Périmètre commercial** : si l'appelant est restreint "
                    + "(`UtilisateurCourantDto.perimetreRestreint = true`), cette liste ne "
                    + "contient que les règlements portant au moins une ligne rattachée à un "
                    + "dossier d'un de ses projets affectés. Les règlements dont aucune ligne ne "
                    + "résout de projet restent visibles de tous.\n\n"
                    + "**Encaissements non affectés : volontairement absents.** Un règlement "
                    + "encaissé mais pas encore imputé à une échéance n'a aucune ligne, donc ne "
                    + "remonte pas ici. L'écran legacy les affiche (il les reconnaît sous "
                    + "`StlSettlementRelationView.Origin = 'StlSettlement-NonAffecte'`) : 16 "
                    + "pièces pour 6 435 400 DH sur les données réelles, toutes à l'état IN_WORK. "
                    + "Les servir suppose de changer le critère \"Vente\" — décision produit en "
                    + "attente, voir `instructions/FEATURE-encaissements-conformite-legataire.md` "
                    + "§4. Ne pas confondre avec un filtre trop strict : c'est un choix assumé."
    )
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
            @Parameter(description = "Id(s) de tranche (CfgTranche.id, voir GET /api/patrimoine/tranches). "
                    + "Répétable, OR. Un règlement remonte dès qu'une de ses échéances porte sur un "
                    + "lot de l'une des tranches citées. Absent = toutes.")
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
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "docDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(encaissementService.findAll(entiteId, typeId, etat, compteId, depotId, dossierId, trancheId,
                tierId, reference, search, comptabilise, montantDe, montantA, champDate, dateDebut, dateFin, pageable));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Détail d'un encaissement (règlement)",
            description = "Même forme que les lignes de `GET /api/encaissements` — alimente le "
                    + "panneau détail \"Pièce de règlement\" côté front (`EncaissementDetail.tsx`). "
                    + "`montantBrutRestant` et `montantNet` sont recalculés côté serveur, jamais "
                    + "stockés, comme sur la liste.\n\n"
                    + "**Périmètre commercial** : un encaissement hors du périmètre de l'appelant "
                    + "renvoie **404**, et non 403 — un 403 confirmerait son existence et "
                    + "permettrait d'énumérer les identifiants."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Encaissement trouvé."),
            @ApiResponse(responseCode = "404", description = "Aucun StlSettlement pour cet id, ou "
                    + "encaissement hors du périmètre commercial de l'appelant — les deux cas sont "
                    + "volontairement indiscernables. Également renvoyé (plutôt qu'un 400) lorsque "
                    + "l'identifiant fourni n'est pas un UUID valide.")
    })
    public EncaissementDto findById(
            @Parameter(description = "Id du règlement (StlSettlement.id).")
            @PathVariable UUID id) {
        return encaissementService.findById(id);
    }
}
