package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Filtres ajoutés à {@code GET /api/prospections} par le lot C — voir
 * {@code instructions/INVENTAIRE-colonnes-prospection.md} §3.
 *
 * <p>Regroupés dans un objet plutôt qu'ajoutés à la signature : avec les huit filtres
 * historiques, la sélection de colonnes et la pagination, la méthode dépasserait vingt
 * paramètres. Spring lie chaque composant depuis la chaîne de requête, et springdoc les
 * documente un par un.
 *
 * <p><strong>Deux familles, deux coûts.</strong> Les filtres portés par
 * {@code ComProspection} et ses associations sont gratuits. Les cinq derniers portent sur des
 * colonnes calculées par {@code ComSaleProspectionView} et se traduisent par une sous-requête :
 * mesuré à <strong>231 ms page 1 et 302 ms à l'{@code OFFSET 5000}</strong>, contre 100/134 ms
 * sans. Le surcoût n'est payé que si l'un d'eux est actif, et reste plat en profondeur — servir
 * la liste depuis la vue coûtait 515 ms dès la première page.
 *
 * <p>Tous les champs sont facultatifs ; {@code null} signifie « pas de filtre ». Les listes sont
 * en OU entre elles et en ET avec les autres critères, comme les filtres historiques.
 */
public record FiltresAvancesProspection(

        // ---------------- Portés par l'entité — coût nul ----------------

        @Schema(description = "Id(s) de projet (`CfgProject`). Répétable. Renseigné sur 99,8 % "
                + "des prospections — une prospection sans tranche n'a pas de projet et sort "
                + "donc du résultat quand ce filtre est actif.")
        List<UUID> projetId,

        @Schema(description = "Id(s) de société / entité juridique (`CfgCompany`). Répétable. "
                + "99,8 %.")
        List<UUID> societeId,

        @Schema(description = "Id(s) de vocation (`StkVocation`). Répétable. 100 %.")
        List<UUID> vocationId,

        @Schema(description = "Id(s) de type de contact (`ComActionMessageType`). Répétable. "
                + "100 %. C'est aussi ce que la vue légataire appelle « type d'origine » — les "
                + "deux colonnes sont identiques sur les 14 118 lignes réelles.")
        List<UUID> typeContactId,

        @Schema(description = "Id(s) de motif de clôture. Répétable. Renseigné sur 60,5 %.")
        List<UUID> raisonFermetureId,

        @Schema(description = "Id(s) de pays du prospect. Répétable. 27,3 %.")
        List<UUID> paysId,

        @Schema(description = "Id(s) d'action commerciale (campagne). Répétable. 40,2 %.")
        List<UUID> actionId,

        @Schema(description = "Id(s) de support d'origine. Répétable. 50,2 %.")
        List<UUID> origineSupportId,

        @Schema(description = "Filtre sur « le prospect est un ancien client ». 100 % renseigné.")
        Boolean ancienClient,

        @Schema(description = "Code(s) de type de document légataire (`StrEntity.Code`). "
                + "Répétable. Filtre sur le **discriminant réel** du logiciel de bureau, et non "
                + "sur l'heuristique que cette liste applique par ailleurs.\n\n"
                + "C'est ce qui rend l'écart mesurable : `?typeProspection=ComPreProspection` "
                + "isole les **830 pré-prospections** que l'heuristique laisse dans la liste de "
                + "vente, et `RntProspection` les 4 prospections de location qui s'y trouvent "
                + "aussi. Valeurs connues : `ComProspection`, `ComPreProspection`, "
                + "`RntProspection`, `PrmProspection`, `RomProspection`.\n\n"
                + "Un code inconnu ne provoque pas d'erreur : il ne correspond à aucune "
                + "prospection et le résultat est vide. Les 4 lignes sans `StrEntityId` sont "
                + "exclues dès que ce filtre est actif — elles n'ont aucun type.",
                example = "ComPreProspection")
        List<String> typeProspection,

        // ---------------- Portés par la vue — sous-requête ----------------

        @Schema(description = "Prospections inactives depuis **au moins** N jours. Renseigné sur "
                + "100 % des lignes : c'est le critère de relance du logiciel de bureau, et le "
                + "plus exploitable des filtres de cette famille.",
                example = "90")
        Integer inactiviteMin,

        @Schema(description = "Au moins N interactions journalisées. 97 %.", example = "3")
        Integer nbInteractionsMin,

        @Schema(description = "Au moins N visites. Attention : seules 20,8 % des prospections ont "
                + "atteint l'étape visite — un seuil ≥ 1 réduit fortement le résultat.",
                example = "1")
        Integer nbVisitesMin,

        @Schema(description = "Dernier échange à partir de cette date (incluse), ISO-8601. 90,4 %.",
                example = "2026-01-01T00:00:00")
        LocalDateTime communicationDepuis,

        @Schema(description = "Dernier échange jusqu'à cette date (incluse), ISO-8601.",
                example = "2026-12-31T23:59:59")
        LocalDateTime communicationJusqua,

        @Schema(description = "`true` : uniquement celles portant une relance active. `false` : "
                + "uniquement celles sans. Absent : pas de filtre. 25,2 % en portent une.")
        Boolean avecAlerteActive
) {

    /** Vrai si au moins un filtre exige la sous-requête sur la vue — donc le surcoût mesuré. */
    public boolean exigeLaVue() {
        return inactiviteMin != null || nbInteractionsMin != null || nbVisitesMin != null
                || communicationDepuis != null || communicationJusqua != null
                || avecAlerteActive != null;
    }
}
