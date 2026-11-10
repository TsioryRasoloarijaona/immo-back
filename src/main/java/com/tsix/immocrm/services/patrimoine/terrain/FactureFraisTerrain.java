package com.tsix.immocrm.services.patrimoine.terrain;

import com.tsix.immocrm.dto.patrimoine.FactureFraisTerrainDto;
import com.tsix.immocrm.entities.buy.BuyDocument;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.stk.StkExpense;
import com.tsix.immocrm.entities.stk.StkExpenseType;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stl.StlCategory;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.services.achat.ecriture.EcriturePieceAchat;
import com.tsix.immocrm.services.achat.ecriture.PieceAchatACreer;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * « Valider pour règlement » (FEATURE-patrimoine-terrains.md §3) : facture écrite par {@link EcriturePieceAchat}, puis
 * {@code IsValidated}, dans une seule transaction — OMRAN ({@code LandProject.cs:ValidateExpense}) les sépare. Tout est
 * contrôlé et lu avant la première écriture.
 */
@Component
public class FactureFraisTerrain {

    static final String DEJA_VALIDE = "Vous ne pouvez pas ajouter une facture pour un frais qui est déjà validé !";

    private final TerrainsVisibles terrains;
    private final FraisFactures factures;
    private final EcriturePieceAchat ecriture;
    private final Clock clock;

    FactureFraisTerrain(TerrainsVisibles terrains, FraisFactures factures, EcriturePieceAchat ecriture, Clock clock) {
        this.terrains = terrains;
        this.factures = factures;
        this.ecriture = ecriture;
        this.clock = clock;
    }

    @Transactional
    public FactureFraisTerrainDto valider(UUID terrainId, UUID fraisId) {
        StkItem terrain = terrains.exiger(terrainId);
        StkExpense frais = factures.verrouiller(fraisId, terrainId);
        if (FraisFactures.valide(frais, factures.parFrais(List.of(frais)))) {
            throw new ConflictException(DEJA_VALIDE);
        }
        CfgTier fournisseur = frais.getCfgSupplier() != null ? frais.getCfgSupplier() : terrain.getCfgSupplier();
        if (fournisseur == null) {
            throw new InvalidRequestException(FraisTerrainService.SANS_FOURNISSEUR);
        }
        StkExpenseType type = frais.getStkExpenseType();
        if (type == null || type.getStlCategory() == null || type.getPrjMarketType() == null) {
            throw new InvalidRequestException("Le type de frais %s n'a pas de catégorie comptable ou de type de marché."
                    .formatted(Champs.de(type, StkExpenseType::getDescription)));
        }
        BuyDocument facture = ecriture.creer(facture(terrain, frais, fournisseur, type));
        frais.setIsValidated(true);
        frais.setUserUpdate(facture.getUserCreate());
        frais.setUserUpdateDateTime(facture.getUserCreateDateTime());
        return new FactureFraisTerrainDto(facture.getId(), facture.getCode(), facture.getAmountTTC());
    }

    /** Facture mesurée (§3.2) : une ligne ({@code Invoice.cs:940-970}), une ventilation catégorie à 100 %. */
    private PieceAchatACreer facture(StkItem terrain, StkExpense frais, CfgTier fournisseur, StkExpenseType type) {
        String jour = LocalDate.now(clock).atStartOfDay().format(SuppressionTerrain.JOUR);
        UUID categorie = Champs.de(type.getStlCategory(), StlCategory::getId);
        return new PieceAchatACreer("STL", "StkInvoiceDebitLand",
                "Aucun statut de départ dans le workflow des factures de frais.", "INVOICE", "LAND_EXPENSE",
                fournisseur, Champs.de(terrain.getCfgCompany(), CfgCompany::getId), type.getPrjMarketType().getId(),
                frais.getId(), code -> "Facture N° %s du %s".formatted(code, jour),
                "Frais : %s sur terrain : %s".formatted(type.getDescription(), terrain.getDescription()),
                List.of(new PieceAchatACreer.Ligne(type.getCode(), "Frais : " + type.getDescription(), frais.getAmount(), "",
                        categorie)),
                List.of(new PieceAchatACreer.Part(categorie, frais.getAmount())), List.of());
    }
}
