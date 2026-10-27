package com.tsix.immocrm.services.patrimoine.fiche;

import com.tsix.immocrm.domain.TypeExport;
import com.tsix.immocrm.dto.patrimoine.FicheArticleDto;
import com.tsix.immocrm.dto.patrimoine.LotissementDetailDto;
import com.tsix.immocrm.services.patrimoine.fiche.FichePdf.Champ;
import com.tsix.immocrm.services.patrimoine.fiche.FichePdf.Fiche;
import com.tsix.immocrm.services.patrimoine.fiche.FichePdf.Section;
import com.tsix.immocrm.web.export.MiseEnPagePdf;

import java.math.BigDecimal;
import java.util.List;

/** Fiche PDF d'un lot (FEATURE-patrimoine-lotissements.md §1.8) : onglets de la fiche, valeurs lues du détail. */
public final class FicheLotissement {

    private FicheLotissement() {
    }

    public static Fiche fiche(LotissementDetailDto d) {
        FicheArticleDto f = d.fiche();
        return new Fiche("fiche-lotissement_" + d.code(), "Fiche lot " + d.code(), d.entiteId(), List.of(
                Section.champs("Identification", List.of(new Champ("Code", d.code()),
                        new Champ("Libellé", d.description()), new Champ("Tranche", d.trancheCode()),
                        new Champ("Programme", d.programmeDescription()), new Champ("Nature", "Terrain"),
                        new Champ("Vocation", d.vocationLibelle()), new Champ("Catégorie", d.categorieLibelle()),
                        new Champ("Type", d.typeLibelle()), new Champ("Présentation", d.presentationTypeLibelle()),
                        new Champ("Orientation", d.orientationLibelle()),
                        new Champ("Catégorie de charge", d.categorieChargeLibelle()),
                        new Champ("Syndic", d.entiteSyndicLibelle()), new Champ("Adresse", d.adresse()),
                        new Champ("Titré", d.estTitre()), new Champ("Occupé", d.estOccupe()))),
                Section.champs("Superficies / valorisation", List.of(
                        new Champ("Superficie provisoire", d.surfaceProvisoire()),
                        new Champ("Prix m² provisoire TTC", d.prixM2ProvisoireTTC()),
                        new Champ("Prix provisoire TTC", d.prixProvisoireTTC()),
                        new Champ("Superficie définitive", d.surfaceDefinitive()),
                        new Champ("Prix m² définitif TTC", d.prixM2DefinitifTTC()),
                        new Champ("Prix définitif TTC", d.prixDefinitifTTC()), new Champ("TVA", pourcentage(d.tauxTva())),
                        new Champ("Remise niveau 1", pourcentage(d.remiseMax1())),
                        new Champ("Remise niveau 2", pourcentage(d.remiseMax2())),
                        new Champ("Avance minimale", f.avanceMinimum()), new Champ("Loyer mensuel HT", f.loyerMensuelHT()),
                        new Champ("Nombre de cautions", f.nombreCautions()), new Champ("Emprise au sol (COS)", d.cos()),
                        new Champ("Plancher couvert (CUF)", d.cuf()),
                        new Champ("Arrêté d'alignement (m)", d.arreteAlignement()),
                        new Champ("Retrait voisin (m)", d.retraitVoisin()), new Champ("Hauteur max (m)", d.hauteurMax()),
                        new Champ("Superficie nette", f.superficieNette()))),
                Section.champs("Identification foncière", List.of(new Champ("Titre foncier", d.titreFoncier()),
                        new Champ("Titre foncier mère", f.titreFoncierMere()),
                        new Champ("Intitulé du titre", f.intituleTitreFoncier()),
                        new Champ("Date du titre", f.dateTitreFoncier()), new Champ("Notes du titre", f.notesTitreFoncier()),
                        new Champ("Date d'enregistrement", f.dateEnregistrement()),
                        new Champ("N° d'enregistrement", f.numeroEnregistrement()),
                        new Champ("Lieu d'enregistrement", f.lieuEnregistrement()),
                        new Champ("C.P.F. immatriculation", f.cpfImmatriculation()),
                        new Champ("Date d'immatriculation", f.dateImmatriculation()),
                        new Champ("Propriété dite", f.proprieteDite()), new Champ("Fraction divise", f.fractionDivise()),
                        new Champ("Fraction divise terrain", f.fractionDiviseTerrain()),
                        new Champ("Parties divises", f.partiesDivises()), new Champ("Parties indivises", f.partiesIndivises()),
                        new Champ("Quota terrain groupe", f.quotaTerrainGroupe()))),
                Section.champs("Compteurs", List.of(new Champ("Référence électricité", f.referenceElectricite()),
                        new Champ("Police électricité", f.policeElectricite()),
                        new Champ("Compteur électricité", f.compteurElectricite()),
                        new Champ("Arrêté électricité", f.releveElectricite()), new Champ("Référence eau", f.referenceEau()),
                        new Champ("Police eau", f.policeEau()), new Champ("Compteur eau", f.compteurEau()),
                        new Champ("Arrêté eau", f.releveEau()), new Champ("Référence gaz", f.referenceGaz()),
                        new Champ("Police gaz", f.policeGaz()), new Champ("Compteur gaz", f.compteurGaz()),
                        new Champ("Arrêté gaz", f.releveGaz()))),
                Section.champs("Autres", List.of(new Champ("Contenance", f.contenanceLibre()),
                        new Champ("Notes", d.notes()), new Champ("Anomalie", f.anomalies()))),
                Section.champs("Audit", List.of(new Champ("Créé par", d.creePar()), new Champ("Créé le", d.creeLe()),
                        new Champ("Modifié par", d.modifiePar()), new Champ("Modifié le", d.modifieLe())))));
    }

    private static String pourcentage(BigDecimal fraction) {
        return fraction == null ? null : MiseEnPagePdf.texte(fraction.movePointRight(2), TypeExport.NOMBRE) + " %";
    }
}
