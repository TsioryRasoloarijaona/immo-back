package com.tsix.immocrm.services.achat.document;

import com.tsix.immocrm.domain.TypeExport;
import com.tsix.immocrm.dto.achat.DocumentAchatDetailDto;
import com.tsix.immocrm.dto.achat.DocumentAchatDetailDto.LigneDocumentAchatDto;
import com.tsix.immocrm.dto.achat.DocumentAchatListItemDto;
import com.tsix.immocrm.dto.achat.RecapTvaAchatDto;
import com.tsix.immocrm.dto.achat.RetenuesAvancesAchatDto;
import com.tsix.immocrm.dto.achat.RetenuesAvancesAchatDto.RetenueAchatDto;
import com.tsix.immocrm.services.achat.RequetesDocumentsAchat.LigneReglementImprime;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import com.tsix.immocrm.web.export.MiseEnPagePdf;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static com.tsix.immocrm.web.export.MiseEnPagePdf.GRAS;
import static com.tsix.immocrm.web.export.MiseEnPagePdf.NORMAL;
import static com.tsix.immocrm.web.export.MiseEnPagePdf.SECTION;
import static com.tsix.immocrm.web.export.MiseEnPagePdf.TITRE;
import static com.tsix.immocrm.web.export.MiseEnPagePdf.ajouter;
import static com.tsix.immocrm.web.export.MiseEnPagePdf.entete;
import static com.tsix.immocrm.web.export.MiseEnPagePdf.nouvelleCellule;
import static com.tsix.immocrm.web.export.MiseEnPagePdf.paragraphe;
import static com.tsix.immocrm.web.export.MiseEnPagePdf.texte;

/** Rapport OMRAN « Paiement Approuvé » d'une facture d'achat, bande par bande (FEATURE-lc8b-paiement-approuve.md R4-R9). */
@Component
public class PaiementApprouvePdfRenderer {

    private static final String VIDE = "—";
    private static final String[] COLONNES_LIGNES = {"Code", "Description", "Unité", "Quantité", "PU HT", "TVA (%)",
            "Total HT", "Rem. (%)", "Total TTC"};
    private static final RetenuesAvancesAchatDto SANS_RETENUE =
            new RetenuesAvancesAchatDto(null, null, null, null, null, null, null, null, null, null, null);

    /** Ligne du pied : libellé, taux en % (null sans taux), montant. */
    private record LignePied(String libelle, BigDecimal taux, BigDecimal montant) {

        static LignePied de(String libelle, BigDecimal montant) {
            return new LignePied(libelle, null, montant);
        }

        static LignePied de(String libelle, RetenueAchatDto retenue) {
            if (retenue == null) {
                return de(libelle, (BigDecimal) null);
            }
            BigDecimal ratio = retenue.ratio();
            return new LignePied(libelle, ratio == null ? null : ratio.movePointRight(2), retenue.montant());
        }
    }

    public byte[] rendre(DocumentAchatDetailDto fiche, List<LigneReglementImprime> reglements) {
        DocumentAchatListItemDto d = fiche.document();
        return MiseEnPagePdf.rendre(PageSize.A4, doc -> {
            Paragraph titre = paragraphe("Paiement Approuvé", TITRE);
            titre.setAlignment(Element.ALIGN_CENTER);
            ajouter(doc, titre);
            ajouter(doc, paragraphe(vide(d.description()), SECTION));
            ajouter(doc, enTete(d));
            section(doc, "Lignes");
            ajouter(doc, lignes(fiche.lignes()));
            section(doc, "Notes");
            ajouter(doc, paragraphe(vide(fiche.notes()), NORMAL));
            section(doc, "Totaux");
            ajouter(doc, pied(pied(fiche)));
            section(doc, "Récapitulatif TVA");
            ajouter(doc, recapTva(fiche.recapTva()));
            section(doc, "Règlements");
            ajouter(doc, reglements(reglements));
            ajouter(doc, paragraphe("Reste à régler : " + montant(d.resteARegler()), GRAS));
            Paragraph signature = paragraphe("Signature", SECTION);
            signature.setAlignment(Element.ALIGN_RIGHT);
            signature.setSpacingBefore(24);
            ajouter(doc, signature);
        });
    }

    private static void section(Document doc, String titre) {
        Paragraph p = paragraphe(titre, SECTION);
        p.setSpacingBefore(8);
        p.setSpacingAfter(4);
        ajouter(doc, p);
    }

    private static PdfPTable enTete(DocumentAchatListItemDto d) {
        PdfPTable table = tableau(4);
        String[][] champs = {{"Code", d.code()}, {"Date", date(d.dateDocument())}, {"Échéance", date(d.dateEcheance())},
                {"Module", d.moduleLibelle()}, {"Entité juridique", d.societeNom()}, {"Fournisseur", d.fournisseurNom()},
                {"Réf. interne", d.reference()}, {"Personne à contacter", d.personneAContacter()},
                {"Projet", d.projetCode()}, {"Dossier de groupement", d.dossierGroupement()}, {"Lot", d.lotCode()},
                {"", ""}};
        for (String[] champ : champs) {
            table.addCell(nouvelleCellule(champ[0], false, true));
            table.addCell(nouvelleCellule(champ[0].isEmpty() ? "" : vide(champ[1]), false, false));
        }
        return table;
    }

    private static PdfPTable lignes(List<LigneDocumentAchatDto> lignes) {
        PdfPTable table = tableau(COLONNES_LIGNES.length);
        table.setHeaderRows(1);
        for (String colonne : COLONNES_LIGNES) {
            table.addCell(entete(colonne));
        }
        if (lignes.isEmpty()) {
            PdfPCell aucune = nouvelleCellule("Aucune ligne", false, false);
            aucune.setColspan(COLONNES_LIGNES.length);
            table.addCell(aucune);
        }
        for (LigneDocumentAchatDto l : lignes) {
            table.addCell(nouvelleCellule(vide(l.code()), false, false));
            table.addCell(nouvelleCellule(vide(l.designation()), false, false));
            table.addCell(nouvelleCellule(vide(l.unite()), false, false));
            table.addCell(nouvelleCellule(nombre(l.quantite()), true, false));
            table.addCell(nouvelleCellule(montant(l.prixUnitaireHT()), true, false));
            table.addCell(nouvelleCellule(taux(l.tauxTVA()), true, false));
            table.addCell(nouvelleCellule(montant(l.totalHT()), true, false));
            table.addCell(nouvelleCellule(taux(l.remisePourcent()), true, false));
            table.addCell(nouvelleCellule(montant(l.totalTTC()), true, false));
        }
        return table;
    }

    /** Pied long (pièce de lot) : les 10 retenues dans l'ordre OMRAN, même nulles ; pied court sinon. */
    private static List<LignePied> pied(DocumentAchatDetailDto fiche) {
        DocumentAchatListItemDto d = fiche.document();
        RetenuesAvancesAchatDto r = fiche.retenuesAvances() == null ? SANS_RETENUE : fiche.retenuesAvances();
        List<LignePied> totaux = List.of(LignePied.de("Total HT", d.montantHT()),
                LignePied.de("Total TVA", d.montantTVA()), LignePied.de("Total TTC", d.montantTTC()),
                LignePied.de("Montant Régul.", fiche.regularisation()), LignePied.de("Timbre fiscal", fiche.timbreFiscal()));
        List<LignePied> retenues = d.lotId() == null
                ? List.of(LignePied.de("Amortissement d'avance", r.amortissementAvance()))
                : List.of(LignePied.de("Amortissement d'avance sur marché", r.amortissementAvance()),
                LignePied.de("Retenue d'assurance décennale", r.assuranceDecennale()),
                LignePied.de("Retenue d'assurance tous risques chantier", r.assuranceTRC()),
                LignePied.de("Retenue de garantie", r.retenueGarantie()),
                LignePied.de("Retenue de compte prorata", r.compteProrata()),
                LignePied.de("Amortissement d'avance sur décompte", r.avanceSurDecompte()),
                LignePied.de("Remboursement approvisionnement", r.approvisionnement()),
                LignePied.de("Retenues forfaitaires", r.retenuesForfaitaires()),
                LignePied.de("Remboursement compte prorata", r.remboursementProrata()),
                LignePied.de("Fluctuation", r.remboursementFluctuations()));
        return Stream.of(totaux, retenues, List.of(LignePied.de("Net à payer", d.montantAPayer())))
                .flatMap(List::stream).toList();
    }

    private static PdfPTable pied(List<LignePied> lignes) {
        PdfPTable table = tableau(3);
        table.setWidthPercentage(60);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        for (LignePied l : lignes) {
            table.addCell(nouvelleCellule(l.libelle(), false, true));
            table.addCell(nouvelleCellule(l.taux() == null ? "" : taux(l.taux()), true, false));
            table.addCell(nouvelleCellule(montant(l.montant()), true, false));
        }
        return table;
    }

    private static PdfPTable recapTva(List<RecapTvaAchatDto> recap) {
        PdfPTable table = tableau(3);
        table.setWidthPercentage(50);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);
        for (String colonne : new String[]{"Base", "TVA (%)", "Montant"}) {
            table.addCell(entete(colonne));
        }
        for (RecapTvaAchatDto r : recap) {
            table.addCell(nouvelleCellule(montant(r.base()), true, false));
            table.addCell(nouvelleCellule(taux(r.tauxTVA()), true, false));
            table.addCell(nouvelleCellule(montant(r.montantTVA()), true, false));
        }
        return table;
    }

    private static PdfPTable reglements(List<LigneReglementImprime> reglements) {
        PdfPTable table = tableau(4);
        for (String colonne : new String[]{"Date", "Description", "Montant", "Affectation"}) {
            table.addCell(entete(colonne));
        }
        for (LigneReglementImprime r : reglements) {
            table.addCell(nouvelleCellule(date(r.date()), false, false));
            table.addCell(nouvelleCellule(vide(r.description()), false, false));
            table.addCell(nouvelleCellule(montant(r.montant()), true, false));
            table.addCell(nouvelleCellule(montant(r.affectation()), true, false));
        }
        return table;
    }

    private static PdfPTable tableau(int colonnes) {
        PdfPTable table = new PdfPTable(colonnes);
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);
        return table;
    }

    private static String vide(String valeur) {
        return valeur == null || valeur.isBlank() ? VIDE : valeur;
    }

    private static String date(LocalDate valeur) {
        return valeur == null ? VIDE : texte(valeur);
    }

    private static String montant(BigDecimal valeur) {
        return texte(valeur == null ? BigDecimal.ZERO : valeur) + " DH";
    }

    private static String nombre(BigDecimal valeur) {
        return valeur == null ? VIDE : texte(valeur, TypeExport.NOMBRE);
    }

    private static String taux(BigDecimal valeur) {
        return valeur == null ? VIDE : nombre(valeur) + " %";
    }
}
