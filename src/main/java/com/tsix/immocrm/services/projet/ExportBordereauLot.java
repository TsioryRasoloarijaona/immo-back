package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.TypeExport;
import com.tsix.immocrm.domain.projet.MontantsLot;
import com.tsix.immocrm.domain.projet.TypeBordereau;
import com.tsix.immocrm.dto.projet.BordereauDto;
import com.tsix.immocrm.dto.projet.BordereauEstimatifDto;
import com.tsix.immocrm.dto.projet.GroupeBordereauDto;
import com.tsix.immocrm.dto.projet.LigneBordereauDto;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.web.export.ColonneFixe;
import com.tsix.immocrm.web.export.ExportTableur;
import com.tsix.immocrm.web.export.FeuilleExport;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.CODE;
import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.CODE_GROUPE;
import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.DESCRIPTION;
import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.FODEC;
import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.NOTES;
import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.ODT;
import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.PU_HT;
import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.PU_TTC;
import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.QUANTITE;
import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.TVA;
import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.UNITE;
import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.UNITE_GROUPE;

/** Export du bordereau du lot aux colonnes OMRAN (FEATURE-lc2-bordereau-excel.md D2, D3) : groupes puis lignes. */
@Service
public class ExportBordereauLot {

    private static final List<ColonneFixe> COLONNES = List.of(ColonneFixe.de("odt", ODT, TypeExport.NOMBRE),
            ColonneFixe.texte("code", CODE), ColonneFixe.texte("description", DESCRIPTION), ColonneFixe.texte("unite", UNITE),
            ColonneFixe.de("quantite", QUANTITE, TypeExport.NOMBRE), ColonneFixe.de("prixUnitaireHT", PU_HT, TypeExport.NOMBRE),
            ColonneFixe.de("tva", TVA, TypeExport.NOMBRE), ColonneFixe.texte("remise", "Remise"),
            ColonneFixe.de("fodec", FODEC, TypeExport.NOMBRE), ColonneFixe.de("prixUnitaireTTC", PU_TTC, TypeExport.NOMBRE),
            ColonneFixe.texte("notes", NOTES), ColonneFixe.texte("codeGroupe", CODE_GROUPE));

    private final LotsProjetService lots;
    private final ExportTableur tableur;

    public ExportBordereauLot(LotsProjetService lots, ExportTableur tableur) {
        this.lots = lots;
        this.tableur = tableur;
    }

    /** Une rangée ; {@code remise} reste vide (aucune colonne en base). Publique : lue par réflexion par ExportTableur. */
    public record Rangee(Integer odt, String code, String description, String unite, BigDecimal quantite,
                          BigDecimal prixUnitaireHT, BigDecimal tva, String remise, BigDecimal fodec, BigDecimal prixUnitaireTTC,
                          String notes, String codeGroupe) {
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> exporter(UUID id, TypeBordereau type) {
        PrjMarket lot = lots.visible(id);
        List<GroupeBordereauDto> groupes;
        List<LigneBordereauDto> lignes;
        if (type == TypeBordereau.ESTIMATIF) {
            BordereauEstimatifDto b = lots.bordereauEstimatif(id);
            groupes = b.groupes();
            lignes = b.lignes();
        } else {
            BordereauDto b = lots.bordereau(id);
            groupes = b.groupes();
            lignes = b.lignes();
        }
        Map<UUID, String> codes = new HashMap<>();
        groupes.forEach(g -> codes.put(g.id(), g.code()));
        List<Rangee> rangees = Stream.concat(
                groupes.stream().filter(g -> !g.stock()).map(g -> new Rangee(g.ordre(), g.code(), g.description(), UNITE_GROUPE,
                        null, null, null, null, null, null, g.notes(), codes.get(g.parentId()))),
                lignes.stream().map(l -> rangee(l, codes.get(l.groupeId())))).toList();
        return tableur.reponseEnMemoire("BDP_" + lot.getCode(), "Bordereau du lot",
                List.of(new FeuilleExport(ExportTableur.FEUILLE, COLONNES, rangees, null)));
    }

    private static Rangee rangee(LigneBordereauDto l, String codeGroupe) {
        BigDecimal tva = Objects.requireNonNullElse(l.tauxTVA(), BigDecimal.ZERO);
        BigDecimal fodec = Objects.requireNonNullElse(l.tauxFODEC(), BigDecimal.ZERO);
        BigDecimal pu = Objects.requireNonNullElse(l.prixUnitaireHT(), BigDecimal.ZERO);
        return new Rangee(l.ordre(), l.code(), l.designation(), l.unite(), l.quantite(), pu, tva, null, fodec,
                MontantsLot.prixTTC(pu, fodec.movePointLeft(2), tva.movePointLeft(2)), l.notes(), codeGroupe);
    }
}
