package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Detail shape for {@code GET /api/prospections/{id}}.
 *
 * NOTE: field naming is not yet backed by a shared API spec doc
 * (API-endpoints-vente-prospection-patrimoine.md is not present in this repo) — derived
 * directly from {@code ComProspection}. Adjust once a spec is available.
 *
 * <p>Les champs ajoutés par {@code FEATURE-enrichissement-fiche-prospection.md} viennent tous des
 * <strong>associations JPA</strong> de {@code ComProspection}, jamais de {@code ComSaleProspectionView} :
 * mapper une colonne de plus sur l'entité de vue dégraderait l'élagage SQL qui fait tenir la fiche
 * à 26 ms. Ils restent donc renseignés sur une <strong>pré-prospection</strong>, contrairement au
 * bloc {@code activite}.
 */
public record ProspectionDetailDto(
        UUID id,
        String code,
        LocalDateTime dateDocument,
        String tempDescription,
        String tempTel,
        String tempEmail,

        // -- Prospect (CfgTier). Le nom est déjà servi ; le reste vient des colonnes que la vue
        //    légataire affiche sur la fiche et que nous n'exposions pas.
        UUID prospectId,
        String prospectNom,

        @Schema(description = "Code du prospect dans le référentiel tiers. Renseigné sur "
                + "**100 %** des prospections réelles.", example = "26/14520")
        String prospectCode,

        @Schema(description = "Numéro de mobile **brut**, sans indicatif — 99,8 %. Le logiciel de "
                + "bureau affiche `(indicatif) numéro` ; ce formatage est laissé au front. Même "
                + "champ que `mobileProspect` de la liste.", example = "6 61 83 60 90")
        String mobileProspect,

        @Schema(description = "Indicatif pays du mobile, **stocké sans `+`** — 100 %. Composer "
                + "l'affichage côté front (`+212 6 61 83 60 90`).", example = "212")
        String indicatifMobile,

        @Schema(description = "Email porté par la fiche tiers — à distinguer de `tempEmail`, saisi "
                + "sur la prospection elle-même. 59,1 %.")
        String emailFiche,

        @Schema(description = "Identifiant du type de tiers. 100 %.")
        UUID typeProspectId,

        @Schema(description = "Code du type de tiers — **c'est ce champ qu'il faut tester**, "
                + "jamais le libellé. 100 %.\n\n"
                + "⚠️ **Ne pas confondre avec `typeProspectionCode`** : celui-ci qualifie le "
                + "*tiers* (`PRO`, `PRORNT`), l'autre qualifie le *document* (`ComProspection`, "
                + "`ComPreProspection`). Même distinction que sur la liste.", example = "PRO")
        String typeProspect,

        @Schema(description = "Libellé du type de tiers. 100 %.")
        String typeProspectLibelle,

        UUID commercialId,
        String commercialNom,

        @Schema(description = "Code du commercial dans le référentiel tiers. 99,3 %.")
        String commercialCode,

        @Schema(description = "Identifiant du proposeur. **Mesuré mort : 1 ligne sur 14 118.** "
                + "Conservé pour compatibilité — ne pas afficher.")
        UUID proposeurId,

        @Schema(description = "Nom du proposeur. **Mort : 1 ligne sur 14 118.** Ne pas afficher.")
        String proposeurNom,

        UUID origineId,
        String origine,

        @Schema(description = "Code de l'origine. 100 %.")
        String origineCode,

        UUID origineSupportId,
        String origineSupport,

        @Schema(description = "Code du support d'origine. 50,2 %.")
        String origineSupportCode,

        UUID etapeId,
        String etape,

        @Schema(description = "Code de l'étape de prospection. 100 %.", example = "3 QUALIFIÉ")
        String etapeCode,

        UUID vocationId,
        String vocation,

        @Schema(description = "Code de la vocation. 100 %.", example = "VENTE")
        String vocationCode,

        UUID typeContactId,
        String typeContact,

        @Schema(description = "Code du type de contact. 100 %.")
        String typeContactCode,

        UUID actionCommercialeId,
        String actionCommerciale,

        @Schema(description = "Code de l'action commerciale (campagne). 40,2 %.")
        String actionCommercialeCode,

        String notes,

        // -- Bloc « qualification budgétaire » : MESURÉ MORT sur les 14 114 prospections réelles.
        //    Price est renseigné sur 4 lignes et vaut 0 — jamais null — partout ailleurs ; Area,
        //    AreaTo, PriceTo, AdvanceAmount et les trois financements : 0 ligne. Les champs sont
        //    conservés pour ne pas rompre le contrat existant, mais le front ne doit pas les
        //    afficher : ils annonceraient un budget de « 0,00 DH » sur tout le portefeuille.
        //    Retirés de la sélection par défaut de la liste (ColonneProspection).
        //    Voir instructions/INVENTAIRE-colonnes-prospection.md §1.
        @Schema(description = "Surface recherchée. **Mesurée morte : 0 ligne renseignée sur "
                + "14 114.** Conservée pour compatibilité — ne pas afficher.")
        BigDecimal surface,

        @Schema(description = "Borne haute de surface. **Morte : 0 ligne.** Ne pas afficher.")
        BigDecimal surfaceJusqua,

        @Schema(description = "Budget déclaré. **Mesuré mort : 4 lignes sur 14 114**, et `0` — "
                + "jamais `null` — ailleurs. Conservé pour compatibilité ; ne pas afficher.")
        BigDecimal prix,

        @Schema(description = "Borne haute de budget. **Morte : 0 ligne.** Ne pas afficher.")
        BigDecimal prixJusqua,

        @Schema(description = "Avance envisagée. **Morte : 0 ligne.** Ne pas afficher.")
        BigDecimal avance,

        @Schema(description = "Financement sur fonds propres. **Mort : 0 ligne.** Ne pas afficher.")
        BigDecimal financementFondsPropres,

        @Schema(description = "Financement par crédit. **Mort : 0 ligne.** Ne pas afficher.")
        BigDecimal financementCredit,

        @Schema(description = "Autre financement. **Mort : 0 ligne.** Ne pas afficher.")
        BigDecimal financementAutre,

        @Schema(description = "Prospection archivée (`ComProspection.IsClosed`). **Seul indicateur "
                + "fiable de l'état archivé** : après un désarchivage, `dateFermeture` et "
                + "`raisonFermeture*` restent renseignés (88 prospections vente ouvertes en base "
                + "réelle en portent). `null` équivaut à `false` (saisie OMRAN ancienne). Bascule "
                + "par `PATCH .../archiver` et `PATCH .../desarchiver`.")
        Boolean fermee,

        @Schema(description = "Date du dernier archivage. **Conservée après désarchivage** — ne pas "
                + "s'en servir pour déduire `fermee`. 62,9 %.")
        LocalDateTime dateFermeture,

        @Schema(description = "Id du motif de clôture (`ComProspectionClosingReason`). Conservé "
                + "après désarchivage.")
        UUID raisonFermetureId,

        @Schema(description = "Libellé du motif de clôture. 60,5 %. Conservé après désarchivage.",
                example = "Hors budget")
        String raisonFermeture,

        @Schema(description = "Code du motif de clôture. 60,5 % — ne vaut quelque chose que sur "
                + "une prospection fermée.")
        String raisonFermetureCode,

        @Schema(description = "Motif de clôture saisi librement (`ClosingReasonText`), complément "
                + "ou substitut du motif référentiel. Conservé après désarchivage.")
        String raisonFermetureTexte,
        Boolean ancienClient,
        // -- Tranche principale et article principal (ComProspection.StkHierarchyId/StkItemId).
        // Toujours cohérents entre eux quand articleId est renseigné (l'article appartient
        // forcément à trancheId) — garanti par PATCH /api/prospections/{id}/tranche-principale,
        // le seul endpoint qui les modifie. Ne pas confondre avec les "autres tranches"
        // (GET/POST /api/prospections/{id}/autres-tranches) qui sont des intérêts secondaires
        // indépendants, sur d'autres tranches/articles.
        UUID trancheId,
        String trancheCode,
        String trancheDescription,
        UUID articleId,
        String articleCode,
        String articleDescription,

        @Schema(description = "Catégorie de l'article principal. Renseignée sur **100 % des "
                + "prospections qui portent un article** — soit 5,7 % du total (804 sur 14 118), "
                + "l'article lui-même étant rare.")
        String articleCategorie,

        @Schema(description = "Type de l'article principal. Même couverture que la catégorie : "
                + "100 % des prospections portant un article.")
        String articleType,

        // -- Société : dérivée de la tranche (CfgTranche → CfgProject → CfgCompany), exactement
        //    comme le fait la vue légataire. Elle n'est jamais saisie sur la prospection.
        @Schema(description = "Identifiant de la société / entité juridique. **99,8 %.** "
                + "Déjà servi par la liste depuis le lot B ; la fiche ne le portait pas.")
        UUID societeId,

        @Schema(description = "Code de la société. 99,8 %.", example = "ADMOD")
        String societeCode,

        @Schema(description = "Libellé de la société. **Dérivé de la tranche** "
                + "(`CfgTranche → CfgProject → CfgCompany`), jamais saisi sur la prospection : "
                + "une prospection sans tranche n'a pas de société, et ce n'est pas une anomalie.")
        String societeLibelle,

        @Schema(description = "Identifiant du pays du prospect. 27,3 %.")
        UUID paysId,

        @Schema(description = "Code du pays. 27,3 %.")
        String paysCode,

        @Schema(description = "Libellé du pays. 27,3 % — `null` sur les deux tiers des fiches.")
        String pays,

        @Schema(description = "Auteur de la création. **34,3 % des prospections, 27,1 % des "
                + "pré-prospections** : les lignes créées par notre API ne posent pas "
                + "`UserCreate` (défaut connu). Un front qui affiche l'auteur verra deux fiches "
                + "sur trois sans auteur.")
        String creePar,

        @Schema(description = "Date de création. 100 % sur les deux populations.")
        LocalDateTime creeLe,

        @Schema(description = "Auteur de la dernière modification. **100 % des prospections, mais "
                + "28,1 % des pré-prospections** — un lead injecté automatiquement et jamais "
                + "rouvert n'a pas d'auteur de modification.")
        String modifiePar,

        @Schema(description = "Date de la dernière modification. **100 % des prospections, 28,1 % "
                + "des pré-prospections.**")
        LocalDateTime modifieLe,

        // -- Tri manuel et déduplication (ComProspection.TempField1 / TempField10).
        //    Voir instructions/FEATURE-marqueurs-tri-pre-prospection.md.
        @Schema(description = "Contenu du champ libre de qualification (`TempField1`), servi tel "
                + "quel — seuls les espaces de bordure sont retirés, et une valeur vide devient "
                + "`null`. Ni la casse ni l'orthographe ne sont corrigées : c'est justement ce "
                + "qu'on ne sait pas interpréter.\n\n"
                + "**Son sens dépend de la population.** Sur une pré-prospection c'est un "
                + "marqueur de tri (« Doublon », « spam »). Sur une prospection c'est le plus "
                + "souvent la typologie recherchée — `salon_+_2_chambres` (518 lignes), "
                + "`salon_+_1_chambre` (213) — parfois une ville ou un budget. Servi brut "
                + "précisément parce qu'il n'est pas normalisé ; utiliser `marqueurTri` quand on "
                + "cherche un marqueur.", example = "salon_+_2_chambres")
        String noteQualification,

        @Schema(description = "Marqueur de tri **normalisé**, déduit de `noteQualification` par "
                + "correspondance exacte (casse et accents ignorés).\n\n"
                + "`null` dès que le texte n'est pas un marqueur reconnu — **c'est le cas le plus "
                + "fréquent** (1 239 lignes de texte libre, dont les 1 200 typologies). `null` "
                + "signifie « pas un marqueur connu », jamais « pas de note » : regarder "
                + "`noteQualification`.\n\n"
                + "Volumes réels : `DOUBLON` 921, `SPAM` 32, `TRAITE` 10.",
                allowableValues = {"DOUBLON", "SPAM", "TRAITE"}, example = "DOUBLON")
        String marqueurTri,

        @Schema(description = "Code du tiers que cette fiche duplique (`TempField10`). 849 lignes "
                + "réelles le portent, et **les 849 résolvent vers un `CfgTier` existant**. "
                + "Toujours renseigné quand le pointeur existe, **même si la résolution "
                + "échoue**.", example = "25/11847")
        String doublonDeCode,

        @Schema(description = "Identifiant du tiers dupliqué, une fois `doublonDeCode` résolu.\n\n"
                + "`null` si le code ne correspond à **aucun** tiers ou à **plusieurs** : "
                + "`CfgTier.Code` n'est pas unique (14 516 tiers pour 14 504 codes), et désigner "
                + "un tiers au hasard serait pire que ne rien désigner. Dans ce cas "
                + "`doublonDeCode` reste affichable, sans lien.")
        UUID doublonDeId,

        @Schema(description = "Nom du tiers dupliqué. `null` dans les mêmes cas que `doublonDeId`.")
        String doublonDeNom,

        // -- Type de document du légataire (StrEntity) : le discriminant réel entre prospection
        //    de vente, pré-prospection et prospection de location.
        //    Voir instructions/FEATURE-strentity-prospection.md §3.1.
        @Schema(description = "Identifiant du type de document légataire (`StrEntity`). `null` sur "
                + "les 4 prospections dont `StrEntityId` n'est pas renseigné.")
        UUID typeProspectionId,

        @Schema(description = "Code du type de document — **c'est ce champ qu'il faut tester**, "
                + "jamais le libellé. Valeurs observées : `ComProspection` (14 114 lignes), "
                + "`ComPreProspection` (3 817), `RntProspection` (4, prospection de location).\n\n"
                + "`null` signifie que la prospection ne porte aucun type : elle est alors "
                + "**invisible dans le logiciel de bureau**, dont toutes les vues font un "
                + "`INNER JOIN` sur `StrEntity`. Ce n'est pas une donnée manquante mais une "
                + "anomalie de création — 4 lignes en base réelle.",
                example = "ComProspection")
        String typeProspectionCode,

        @Schema(description = "Libellé d'affichage du type, traduit — « Prospection vente », "
                + "« Pré-Prospection ». Ne pas s'en servir pour tester le type.",
                example = "Prospection vente")
        String typeProspection,

        @Schema(description = "Bloc d'indicateurs d'activité calculés par la base légataire "
                + "(nombre d'interactions, inactivité, dernière communication, visites, relance "
                + "en cours, projet, et le lien vers le dossier issu de la prospection).\n\n"
                + "**Vaut `null` en entier sur une pré-prospection** : ces indicateurs viennent "
                + "de `ComSaleProspectionView`, qui exclut les pré-prospections. L'absence du "
                + "bloc signifie « non disponible », jamais « zéro ». Les autres champs de cette "
                + "fiche restent, eux, renseignés sur une pré-prospection.")
        ActiviteProspectionDto activite
) {
}
