package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.domain.rapport.TypeParametre;
import com.tsix.immocrm.services.rapport.vente.flux.FluxMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.FluxVenteMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles;
import com.tsix.immocrm.services.rapport.vente.flux.MesureFlux;
import com.tsix.immocrm.services.rapport.vente.flux.SeuilsStatutsVente;
import com.tsix.immocrm.services.rapport.vente.flux.ValorisationTranche;
import com.tsix.immocrm.services.rapport.vente.flux.ValorisationTranche.Cumul;
import com.tsix.immocrm.services.rapport.vente.flux.ValorisationTranche.Population;
import com.tsix.immocrm.services.rapport.vente.flux.ValorisationTranche.Valorisation;
import com.tsix.immocrm.services.rapport.vente.flux.ValorisationTranche.Valorisations;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * N° 11 SITUATION GLOBALE DES VENTES (FEATURE-tdb-vente-rapports-flux.md §4.6), modèle {@code ComTdbSalesSituationGlobal} :
 * situation du jour par tranche en vente visible, sous-totaux par programme ; versements = encaissements toutes années.
 */
@Component
public class SituationGlobaleVentesRapport extends RapportFluxVente {

    static final String CODE = "ComTdbSalesSituationGlobal";
    private static final String NOMBRES = "Nombre d'articles";
    private static final String CA = "Chiffre d'affaires TTC";
    private static final String TRESORERIE = "Trésorerie";

    private final ValorisationTranche valorisation;

    public SituationGlobaleVentesRapport(FluxVenteMensuel flux, ValorisationTranche valorisation, Clock clock) {
        super(CODE, flux, clock);
        this.valorisation = valorisation;
    }

    /** {@code annee} accepté pour la visionneuse commune mais sans effet : la situation est celle du jour. */
    @Override
    public List<ParametreRapport> parametres() {
        return List.of(ParametreRapport.tranches(), new ParametreRapport(LignesMensuelles.ANNEE,
                "Année (sans effet : situation du jour)", TypeParametre.ANNEE, false, false, null, List.of(), null, null));
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        Valorisations vs = valorisation.pour(contexte.tranchesVisibles());
        FluxMensuel f = calculer(contexte, null, null, EnumSet.of(MesureFlux.ENCAISSEMENTS));
        boolean reservation = !vs.parametresAbsents().contains(SeuilsStatutsVente.RESERVATION);
        List<Map<String, Object>> lignes = vs.tranches().stream()
                .map(v -> ligne(v, reservation, f.tranche(MesureFlux.ENCAISSEMENTS, v.tranche()))).toList();
        List<String> avertissements = new ArrayList<>(LignesMensuelles.avertissements(f));
        vs.parametresAbsents().forEach(p -> avertissements.add(
                "Paramètre %s absent : les colonnes qui en dépendent sont vides ou à 0.".formatted(p)));
        avertissements.add(ActiviteParProjetRapport.NOTE_VERSEMENTS);
        return new ResultatRapport(List.of(new SectionRapport("situation", "Situation globale des ventes", colonnes(),
                lignes, PROGRAMME, null, List.of())), avertissements);
    }

    /** Libres = sans dossier + réservés ; sans le paramètre de réservation, leurs nombre et C.A. sont vides. */
    private static Map<String, Object> ligne(Valorisation v, boolean reservation, BigDecimal versements) {
        Cumul reserves = v.de(Population.RESERVES);
        BigDecimal resteLibres = v.prixLibres().add(reserves.reste());
        BigDecimal reliquat = v.de(Population.ENGAGES).reste();
        Map<String, Object> l = ligneTranche(v);
        l.put("nbArticles", v.articles());
        l.put("nbLibres", reservation ? v.libres() + reserves.nombre() : null);
        l.put("nbConcretises", v.de(Population.CONCRETISES).nombre());
        l.put("nbVendus", v.de(Population.VENDUS).nombre());
        l.put("nbLivres", v.de(Population.LIVRES).nombre());
        l.put("valorisationStock", v.prixTotal());
        l.put("caLibre", reservation ? v.prixLibres().add(reserves.prix()) : null);
        l.put("caConcretise", v.de(Population.CONCRETISES).prix());
        l.put("caVendu", v.de(Population.VENDUS).prix());
        l.put("resteAEncaisserLibres", resteLibres);
        l.put("reliquatConcretises", reliquat);
        l.put("resteAEncaisser", resteLibres.add(reliquat));
        l.put("versements", versements);
        return l;
    }

    private static List<ColonneRapport> colonnes() {
        List<ColonneRapport> colonnes = new ArrayList<>(colonnesTranche());
        colonnes.addAll(List.of(ColonneRapport.effectif("nbArticles", "Articles", NOMBRES),
                ColonneRapport.effectif("nbLibres", "Libres", NOMBRES),
                ColonneRapport.effectif("nbConcretises", "Concrétisés", NOMBRES),
                ColonneRapport.effectif("nbVendus", "Vendus", NOMBRES),
                ColonneRapport.effectif("nbLivres", "Livrés", NOMBRES),
                ColonneRapport.montant("valorisationStock", "Valorisation du stock", "Valorisation"),
                ColonneRapport.montant("caLibre", "Libre", CA),
                ColonneRapport.montant("caConcretise", "Concrétisé", CA),
                ColonneRapport.montant("caVendu", "Vendu", CA),
                ColonneRapport.montant("resteAEncaisserLibres", "Reste à encaisser (libres)", TRESORERIE),
                ColonneRapport.montant("reliquatConcretises", "Reliquat des concrétisés", TRESORERIE),
                ColonneRapport.montant("resteAEncaisser", "Reste à encaisser", TRESORERIE),
                ColonneRapport.montant("versements", "Versements (*)", TRESORERIE)));
        return colonnes;
    }
}
