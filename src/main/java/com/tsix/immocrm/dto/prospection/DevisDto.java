package com.tsix.immocrm.dto.prospection;

import com.tsix.immocrm.dto.dossier.LigneSchemaFinancementDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Une ligne {@code StlQuote}. */
@Schema(description = "Une offre de prix (\"devis\") faite à un prospect sur une prospection, "
        + "avec son détail HT/TTC complet.")
public record DevisDto(
        UUID id,
        String code,
        LocalDateTime dateDocument,

        UUID prospectionId,
        String prospectionCode,

        UUID trancheId,
        @Schema(description = "Code de la tranche.")
        String tranche,
        @Schema(description = "Libellé (description) de la tranche.")
        String trancheDescription,

        UUID articleId,
        @Schema(description = "Code de l'article.")
        String article,
        @Schema(description = "Libellé (description) de l'article.")
        String articleDescription,
        @Schema(description = "Superficie de l'article, en m². null si l'offre n'a pas d'article.")
        BigDecimal articleSuperficie,
        @Schema(description = "Nature du bien (StkItemCategory), ex. \"APPARTEMENT\". null si "
                + "l'offre n'a pas d'article.")
        String articleCategorie,
        @Schema(description = "Typologie du bien (StkItemType), ex. \"1 CHAMBRES + SALON\". null "
                + "si l'offre n'a pas d'article.")
        String articleType,

        UUID prospectId,
        @Schema(description = "Nom (raison sociale ou prénom+nom) du prospect.")
        String prospect,

        UUID commercialId,
        @Schema(description = "Nom du commercial.")
        String commercial,

        UUID modePaiementId,
        @Schema(description = "Libellé du mode de paiement.")
        String modePaiement,

        @Schema(description = "Détail HT/TTC complet de l'offre — voir MontantsOffreDto.")
        MontantsOffreDto montants,

        List<LigneSchemaFinancementDto> schemaFinancement,

        String notes,

        @Schema(description = "Auteur de l'offre (StlQuote.UserCreate), nom affiché de l'utilisateur.")
        String creePar,
        @Schema(description = "Date de création de l'offre (StlQuote.UserCreateDateTime).")
        LocalDateTime creeLe,
        @Schema(description = "Auteur de la dernière modification (StlQuote.UserUpdate).")
        String modifiePar,
        @Schema(description = "Date de la dernière modification (StlQuote.UserUpdateDateTime).")
        LocalDateTime modifieLe
) {
}
