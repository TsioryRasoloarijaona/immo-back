package com.tsix.immocrm.services.patrimoine.importation;

import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkPresentationType;
import com.tsix.immocrm.services.patrimoine.importation.ChampsArticle.ChampImport;
import com.tsix.immocrm.services.patrimoine.importation.ChampsArticle.Portee;
import com.tsix.immocrm.services.patrimoine.importation.ChampsArticle.Reference;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static com.tsix.immocrm.services.patrimoine.importation.ChampsArticle.champ;

/**
 * En-têtes d'{@code ImportLotLand} (FEATURE-patrimoine-lotissements.md §1.5) → champ de {@code LotissementEcritureDto} :
 * ceux de l'article sans les colonnes du bâti, plus la présentation et les règles d'urbanisme du lot.
 */
final class ChampsLotissement {

    private static final Set<String> BATI = Set.of("Immeuble", "Etage", "Type présentation", "Superficie intérieure (m²)",
            "Superficie murs (m²)", "Superficie parties communes (m²)");

    static final List<ChampImport> CHAMPS = Stream.concat(
            ChampsArticle.du(false).stream().filter(c -> c.rang() == 0 && !BATI.contains(c.entete())),
            Stream.of(new ChampImport("Présentation", "presentationId", TypeCellule.REFERENCE,
                            new Reference(StkPresentationType.class, false), StkItem::getStkPresentationType, Portee.TOUS, 0),
                    champ("Arrêté d'alignement(m)", "arreteAlignement", TypeCellule.ENTIER, StkItem::getAlinmentStop),
                    champ("Retrait/voisin(m)", "retraitVoisin", TypeCellule.ENTIER, StkItem::getNeighborStop),
                    champ("Hauteur max autorisée(m)", "hauteurMax", TypeCellule.ENTIER, StkItem::getMaxHeight),
                    champ("Emprise au sol Batiments (COS)", "cos", TypeCellule.NOMBRE, StkItem::getCos),
                    champ("Plancher Couvert (CUF)", "cuf", TypeCellule.NOMBRE, StkItem::getCuf))).toList();

    private ChampsLotissement() {
    }
}
