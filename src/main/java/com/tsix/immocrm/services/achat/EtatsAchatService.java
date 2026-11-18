package com.tsix.immocrm.services.achat;

import com.tsix.immocrm.domain.TypeExport;
import com.tsix.immocrm.domain.achat.SuiviAchat;
import com.tsix.immocrm.domain.achat.SuiviAchat.Montants;
import com.tsix.immocrm.dto.achat.EtatTvaDeductibleDto;
import com.tsix.immocrm.dto.achat.EtatTvaDeductibleDto.LigneTvaDeductibleDto;
import com.tsix.immocrm.dto.achat.EtatTvaDeductibleDto.MoisTvaDeductibleDto;
import com.tsix.immocrm.dto.achat.EtatTvaDeductibleDto.TotauxTvaDeductibleDto;
import com.tsix.immocrm.dto.achat.FiltresEtatsAchat;
import com.tsix.immocrm.dto.achat.SuiviAvancesDto;
import com.tsix.immocrm.dto.achat.SuiviAvancesDto.LigneSuiviAvanceDto;
import com.tsix.immocrm.dto.achat.SuiviAvancesDto.TotauxSuiviAvancesDto;
import com.tsix.immocrm.dto.achat.SuiviRetenuesDto;
import com.tsix.immocrm.dto.achat.SuiviRetenuesDto.LigneSuiviRetenueDto;
import com.tsix.immocrm.dto.achat.SuiviRetenuesDto.TotauxSuiviRetenuesDto;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.services.achat.RequetesEtatsAchat.LigneSuivi;
import com.tsix.immocrm.services.achat.RequetesEtatsAchat.LigneTva;
import com.tsix.immocrm.services.droits.DroitsOmran;
import com.tsix.immocrm.services.perimetre.PerimetreAchat;
import com.tsix.immocrm.services.perimetre.PerimetreProjetService;
import com.tsix.immocrm.web.export.ColonneFixe;
import com.tsix.immocrm.web.export.ExportTableur;
import com.tsix.immocrm.web.export.FeuilleExport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/** États Achat en lecture (FEATURE-n7-etats-achat.md N7a) : totaux = Σ des lignes non annulées, calculés ici seulement. */
@Service
@Transactional(readOnly = true)
public class EtatsAchatService {

    public static final String DROIT_TVA = "Yes.Buy.Front.Tdb.VatStatement";

    private static final String MONTANT_HT = "montantHT";
    private static final String MONTANT_TVA = "montantTva";
    private static final String PREMIER = "montants.premier";
    private static final String DEUXIEME = "montants.deuxieme";
    private static final String TROISIEME = "montants.troisieme";
    private static final List<ColonneFixe> COLONNES_TVA = List.of(ColonneFixe.texte("factureCode", "Facture"),
            ColonneFixe.de("dateFacture", "Date", TypeExport.DATE), ColonneFixe.texte("fournisseur", "Fournisseur"),
            ColonneFixe.texte("description", "Description"), ColonneFixe.de(MONTANT_HT, "Montant HT", TypeExport.MONTANT),
            ColonneFixe.de("tauxTvaPct", "TVA %", TypeExport.NOMBRE),
            ColonneFixe.de(MONTANT_TVA, "Montant TVA", TypeExport.MONTANT), ColonneFixe.texte("statut", "Statut"),
            ColonneFixe.de("annulee", "Annulée", TypeExport.BOOLEEN),
            ColonneFixe.de("sansLignes", "Sans lignes", TypeExport.BOOLEEN), ColonneFixe.texte("projet", "Projet"),
            ColonneFixe.texte("societe", "Entité"));
    private static final List<ColonneFixe> COLONNES_MOIS = List.of(ColonneFixe.de("mois", "Mois", TypeExport.DATE),
            ColonneFixe.de("factures", "Factures", TypeExport.NOMBRE),
            ColonneFixe.de(MONTANT_HT, "Base HT", TypeExport.MONTANT),
            ColonneFixe.de(MONTANT_TVA, "TVA déductible", TypeExport.MONTANT));

    private final RequetesEtatsAchat requetes;
    private final PerimetreProjetService perimetres;
    private final DroitsOmran droits;

    public EtatsAchatService(RequetesEtatsAchat requetes, PerimetreProjetService perimetres, DroitsOmran droits) {
        this.requetes = requetes;
        this.perimetres = perimetres;
        this.droits = droits;
    }

    public EtatTvaDeductibleDto tvaDeductible(FiltresEtatsAchat f) {
        droits.exiger(DROIT_TVA);
        List<LigneTva> lignes = requetes.tva(perimetre(f), f);
        List<LigneTva> comptees = lignes.stream().filter(l -> !l.ligne().annulee()).toList();
        Map<LocalDate, List<LigneTva>> parMois = new TreeMap<>();
        comptees.forEach(l -> parMois.computeIfAbsent(l.ligne().dateFacture().withDayOfMonth(1), m -> new ArrayList<>())
                .add(l));
        BigDecimal tva = somme(comptees, LigneTvaDeductibleDto::montantTva);
        BigDecimal validee = somme(comptees.stream().filter(LigneTva::validee).toList(), LigneTvaDeductibleDto::montantTva);
        return new EtatTvaDeductibleDto(lignes.stream().map(LigneTva::ligne).toList(),
                parMois.entrySet().stream().map(e -> new MoisTvaDeductibleDto(e.getKey(), factures(e.getValue()),
                        somme(e.getValue(), LigneTvaDeductibleDto::montantHT),
                        somme(e.getValue(), LigneTvaDeductibleDto::montantTva))).toList(),
                new TotauxTvaDeductibleDto(factures(comptees), somme(comptees, LigneTvaDeductibleDto::montantHT), tva,
                        validee, tva.subtract(validee)));
    }

    public SuiviAvancesDto avances(FiltresEtatsAchat f) {
        List<LigneSuivi> lignes = suivi(SuiviAchat.AVANCES, f);
        Montants t = totaux(lignes);
        return new SuiviAvancesDto(lignes.stream().map(l -> new LigneSuiviAvanceDto(l.piece(), l.montants().premier(),
                l.montants().deuxieme(), l.montants().troisieme())).toList(),
                new TotauxSuiviAvancesDto(t.premier(), t.deuxieme(), t.troisieme()));
    }

    public SuiviRetenuesDto retenues(FiltresEtatsAchat f) {
        List<LigneSuivi> lignes = suivi(SuiviAchat.RETENUES, f);
        Montants t = totaux(lignes);
        return new SuiviRetenuesDto(lignes.stream().map(l -> new LigneSuiviRetenueDto(l.piece(), l.montants().premier(),
                l.montants().deuxieme(), l.montants().troisieme())).toList(),
                new TotauxSuiviRetenuesDto(t.premier(), t.deuxieme(), t.troisieme()));
    }

    /** Classeur d'un état : préfixe du fichier, titre, feuilles. */
    public record Export(String fichier, String titre, List<FeuilleExport> feuilles) {
    }

    /** Export de l'état {@code etat} (segment de route) ; 404 si inconnu. */
    public Export export(String etat, FiltresEtatsAchat f) {
        return switch (etat) {
            case "tva-deductible" -> new Export(etat, "État de la TVA déductible", exportTva(f));
            case "avances" -> new Export("suivi-avances", "Suivi des avances", exportSuivi(SuiviAchat.AVANCES, f));
            case "retenues-garantie" -> new Export("suivi-retenues-garantie", "Suivi des retenues de garantie",
                    exportSuivi(SuiviAchat.RETENUES, f));
            default -> throw new ResourceNotFoundException("État inconnu : " + etat);
        };
    }

    /** Feuilles DONNEES (lignes + Total) et PAR_MOIS de l'export de la TVA déductible. */
    private List<FeuilleExport> exportTva(FiltresEtatsAchat f) {
        EtatTvaDeductibleDto etat = tvaDeductible(f);
        TotauxTvaDeductibleDto t = etat.totaux();
        return List.of(new FeuilleExport(ExportTableur.FEUILLE, COLONNES_TVA, etat.lignes(),
                        Map.of(MONTANT_HT, t.montantHT(), MONTANT_TVA, t.montantTva())),
                new FeuilleExport("PAR_MOIS", COLONNES_MOIS, etat.parMois(), null));
    }

    /** Feuille DONNEES (lignes + Total) de l'export d'un suivi ; exige aussi le droit d'export. */
    private List<FeuilleExport> exportSuivi(SuiviAchat suivi, FiltresEtatsAchat f) {
        droits.exiger(suivi.droitExport());
        List<LigneSuivi> lignes = suivi(suivi, f);
        Montants t = totaux(lignes);
        List<String> libelles = suivi.libelles();
        List<ColonneFixe> colonnes = List.of(ColonneFixe.de("piece.date", "Date", TypeExport.DATE),
                ColonneFixe.texte("piece.code", "Code"), ColonneFixe.texte("piece.description", "Description"),
                ColonneFixe.texte("piece.nature", "Entité fonctionnelle"),
                ColonneFixe.de(PREMIER, libelles.get(0), TypeExport.MONTANT),
                ColonneFixe.de(DEUXIEME, libelles.get(1), TypeExport.MONTANT),
                ColonneFixe.de(TROISIEME, libelles.get(2), TypeExport.MONTANT),
                ColonneFixe.texte("piece.fournisseur", "Tier"), ColonneFixe.texte("piece.projet", "Projet"),
                ColonneFixe.texte("piece.societe", "Entité"), ColonneFixe.texte("piece.lot", "Lot"),
                ColonneFixe.texte("piece.statut", "Statut"));
        return List.of(new FeuilleExport(ExportTableur.FEUILLE, colonnes, lignes,
                Map.of(PREMIER, t.premier(), DEUXIEME, t.deuxieme(), TROISIEME, t.troisieme())));
    }

    private List<LigneSuivi> suivi(SuiviAchat suivi, FiltresEtatsAchat f) {
        droits.exiger(suivi.droit());
        return requetes.suivi(suivi, perimetre(f), f);
    }

    /** Périmètre achat (D3) restreint à la société demandée ; exige la période (R8). */
    private PerimetreAchat perimetre(FiltresEtatsAchat f) {
        f.exigerPeriode();
        return perimetres.achat(f.projetIds())
                .deSocietes(f.societeId() == null ? List.of() : List.of(f.societeId()));
    }

    private static Montants totaux(List<LigneSuivi> lignes) {
        return lignes.stream().filter(l -> !l.piece().annulee()).map(LigneSuivi::montants)
                .reduce(Montants.ZERO, Montants::plus);
    }

    private static long factures(List<LigneTva> lignes) {
        return lignes.stream().map(l -> l.ligne().factureId()).distinct().count();
    }

    private static BigDecimal somme(List<LigneTva> lignes, Function<LigneTvaDeductibleDto, BigDecimal> montant) {
        return lignes.stream().map(l -> montant.apply(l.ligne())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
