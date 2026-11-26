package com.tsix.immocrm.services.achat.demandes;

import com.tsix.immocrm.dto.achat.demandes.ImportOffreDemandeAchatDto;
import com.tsix.immocrm.entities.buy.BuyDocument;
import com.tsix.immocrm.entities.buy.BuyDocumentLine;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.services.patrimoine.importation.LectureClasseur;
import com.tsix.immocrm.services.patrimoine.importation.LectureClasseur.LigneLue;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.CODE;
import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.FODEC;
import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.PU_HT;
import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.REMISE;
import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.UNITE;
import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.UNITE_GROUPE;
import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.code;
import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.nombre;
import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.texte;

/** R7 : prix d'une offre repris d'un classeur au format de l'export du bordereau, rapprochés par le code. */
final class ImportOffreDemandeAchat {

    private ImportOffreDemandeAchat() {
    }

    private record Prix(List<BuyDocumentLine> lignes, BigDecimal pu, BigDecimal remise, BigDecimal fodec) {
    }

    static ImportOffreDemandeAchatDto importer(OffresDemandeAchat offres, UUID dia, UUID oid, MultipartFile fichier) {
        BuyDocument o = offres.offre(dia, oid);
        LectureClasseur.Feuille feuille = LectureClasseur.lire(fichier, null);
        if (!feuille.entetes().contains(CODE)) {
            throw new InvalidRequestException("La colonne « %s » est absente du classeur".formatted(CODE));
        }
        List<BuyDocumentLine> lignes = offres.lignes(oid);
        Map<String, List<BuyDocumentLine>> parCode = lignes.stream().filter(l -> l.getCode() != null)
                .collect(Collectors.groupingBy(BuyDocumentLine::getCode));
        List<Prix> aEcrire = new ArrayList<>();
        int ignores = 0;
        for (LigneLue l : feuille.lignes()) {
            String c = code(l, CODE);
            if (c == null || UNITE_GROUPE.equals(texte(l, UNITE))) {
                continue;
            }
            List<BuyDocumentLine> cibles = parCode.get(c);
            if (cibles == null) {
                ignores++;
            } else {
                aEcrire.add(prix(l, cibles));
            }
        }
        OffresDemandeAchat.verifierNonEcartee(o);
        aEcrire.forEach(ImportOffreDemandeAchat::appliquer);
        return new ImportOffreDemandeAchatDto(offres.recalculer(dia, o, lignes), aEcrire.size(), ignores);
    }

    private static Prix prix(LigneLue l, List<BuyDocumentLine> cibles) {
        BuyDocumentLine actuelle = cibles.getFirst();
        Prix p = new Prix(cibles, nombre(l, PU_HT), pourcentage(nombre(l, REMISE)), pourcentage(nombre(l, FODEC)));
        OffresDemandeAchat.verifierPrix(ou(p.pu(), actuelle.getUnitPriceHT()), ou(p.remise(), actuelle.getDiscountRatio()));
        return p;
    }

    private static BigDecimal ou(BigDecimal lue, BigDecimal actuelle) {
        if (lue != null) {
            return lue;
        }
        return Objects.requireNonNullElse(actuelle, BigDecimal.ZERO);
    }

    private static BigDecimal pourcentage(BigDecimal valeur) {
        return valeur == null ? null : valeur.movePointLeft(2);
    }

    private static void appliquer(Prix p) {
        p.lignes().forEach(l -> {
            if (p.pu() != null) {
                l.setUnitPriceHT(p.pu());
            }
            if (p.remise() != null) {
                l.setDiscountRatio(p.remise());
            }
            if (p.fodec() != null) {
                l.setFodecRatio(p.fodec());
            }
        });
    }
}
