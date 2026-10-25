package com.tsix.immocrm.services.patrimoine.fiche;

import com.tsix.immocrm.domain.TypeExport;
import com.tsix.immocrm.dto.patrimoine.LignePrevisionDto;
import com.tsix.immocrm.dto.patrimoine.PrevisionsBaseDto;
import com.tsix.immocrm.dto.patrimoine.TrancheDetailDto;
import com.tsix.immocrm.services.patrimoine.fiche.FichePdf.Champ;
import com.tsix.immocrm.services.patrimoine.fiche.FichePdf.Fiche;
import com.tsix.immocrm.services.patrimoine.fiche.FichePdf.Section;
import com.tsix.immocrm.web.export.MiseEnPagePdf;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** Fiche « prévision » de la tranche (gabarit OMRAN {@code CfgTranchePrevision}) : champs de {@link PrevisionsBaseDto}. */
public final class FichePrevisionsBasePdf {

    private FichePrevisionsBasePdf() {
    }

    public static Fiche fiche(TrancheDetailDto t, PrevisionsBaseDto p) {
        List<Object[]> lignes = new ArrayList<>(Objects.requireNonNullElse(p.lignes(), List.<LignePrevisionDto>of())
                .stream().map(l -> new Object[]{l.annee(), l.mois(), l.quantiteConcretisation(), l.quantiteFinalisation(),
                        l.versementTotal(), l.chiffreAffaires(), l.notes()}).toList());
        lignes.add(new Object[]{"Total", null, somme(p, LignePrevisionDto::quantiteConcretisation),
                somme(p, LignePrevisionDto::quantiteFinalisation), null, null, null});
        return new Fiche("fiche-prevision_" + t.code(), "Prévisions de base " + t.code() + " — " + t.description(),
                t.entiteId(), List.of(
                Section.champs("Hypothèses", List.of(
                        new Champ("Nombre d'articles", p.nombreArticles()),
                        new Champ("Valorisation du stock", p.valorisationStock()),
                        new Champ("% concrétisation", pourcentage(p.pourcentageConcretisation())),
                        new Champ("Début concrétisation", p.debutConcretisation()),
                        new Champ("Fin concrétisation", p.finConcretisation()),
                        new Champ("Écart type concrétisation", nombre(p.ecartTypeConcretisation())),
                        new Champ("Début finalisation", p.debutFinalisation()),
                        new Champ("Fin finalisation", p.finFinalisation()),
                        new Champ("Écart type finalisation", nombre(p.ecartTypeFinalisation())))),
                Section.tableau("Prévisions mensuelles", List.of("Année", "Mois", "Qté concrétisation",
                        "Qté finalisation", "Montant", "Chiffre d'affaires", "Notes"), lignes, Function.identity())));
    }

    private static int somme(PrevisionsBaseDto p, Function<LignePrevisionDto, Integer> quantite) {
        return Objects.requireNonNullElse(p.lignes(), List.<LignePrevisionDto>of()).stream()
                .map(quantite).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
    }

    private static String pourcentage(BigDecimal fraction) {
        return fraction == null ? null : nombre(fraction.movePointRight(2)) + " %";
    }

    private static String nombre(BigDecimal valeur) {
        return valeur == null ? null : MiseEnPagePdf.texte(valeur, TypeExport.NOMBRE);
    }
}
