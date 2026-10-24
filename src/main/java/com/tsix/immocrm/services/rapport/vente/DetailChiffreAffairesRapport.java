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
import com.tsix.immocrm.services.rapport.vente.flux.ValorisationTranche;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * N° 7 DÉTAILS CHIFFRE D'AFFAIRE PAR PROJET et PAR ANNÉE (FEATURE-tdb-vente-rapports-flux.md §4.5), modèle
 * {@code ComTdbDetailsChiffreAffaire} : une ligne par tranche en vente visible, même sans mouvement.
 */
@Component
public class DetailChiffreAffairesRapport extends RapportFluxVente {

    static final String CODE = "ComTdbDetailsChiffreAffaire";
    private static final String COEFFICIENT = "coefficient";
    private static final String SOUS_ARTICLES = "Le C.A. de base compte aussi les sous-articles.";
    private static final BigDecimal CENT = BigDecimal.valueOf(100);

    private final ValorisationTranche valorisation;

    public DetailChiffreAffairesRapport(FluxVenteMensuel flux, ValorisationTranche valorisation, Clock clock) {
        super(CODE, flux, clock);
        this.valorisation = valorisation;
    }

    @Override
    public List<ParametreRapport> parametres() {
        return List.of(LignesMensuelles.annee(clock), ParametreRapport.tranches(),
                new ParametreRapport(COEFFICIENT, "Coefficient de prévision (%)", TypeParametre.ENTIER, false, false,
                        "100", List.of(), 0, 1000));
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        int annee = annee(contexte);
        BigDecimal coefficient = BigDecimal.valueOf(Objects.requireNonNull(contexte.entier(COEFFICIENT),
                "Paramètre coefficient absent"));
        FluxMensuel f = calculer(contexte, null, annee, EnumSet.of(MesureFlux.CA_TTC, MesureFlux.PREVU_CA_TTC));
        List<Map<String, Object>> lignes = new ArrayList<>();
        for (ValorisationTranche.Valorisation v : valorisation.pour(contexte.tranchesVisibles()).tranches()) {
            BigDecimal prevision = f.tranche(MesureFlux.PREVU_CA_TTC, v.tranche(), annee).multiply(coefficient)
                    .divide(CENT, 6, RoundingMode.HALF_UP);
            BigDecimal reel = f.tranche(MesureFlux.CA_TTC, v.tranche(), annee);
            BigDecimal dejaFacture = f.tranche(MesureFlux.CA_TTC, v.tranche()).subtract(reel);
            Map<String, Object> ligne = ligneTranche(v);
            ligne.put("caBase", v.prixTotal());
            ligne.put("caPrevisionnel", prevision);
            ligne.put("caReel", reel);
            ligne.put("ecart", reel.subtract(prevision));
            ligne.put("dejaFacture", dejaFacture);
            ligne.put("resteAFacturer", v.prixTotal().subtract(dejaFacture));
            lignes.add(ligne);
        }
        List<ColonneRapport> colonnes = new ArrayList<>(colonnesTranche());
        colonnes.addAll(List.of(ColonneRapport.montant("caBase", "C.A. de base par projet TTC", null),
                ColonneRapport.montant("caPrevisionnel", "C.A. prévisionnel " + annee + " TTC", null),
                ColonneRapport.montant("caReel", "C.A. réel " + annee + " TTC", null),
                ColonneRapport.montant("ecart", "Écart C.A.", null),
                ColonneRapport.montant("dejaFacture", "Déjà facturé", null),
                ColonneRapport.montant("resteAFacturer", "Reste à facturer", null)));
        List<String> avertissements = new ArrayList<>(LignesMensuelles.avertissements(f));
        avertissements.add(SOUS_ARTICLES);
        return new ResultatRapport(List.of(new SectionRapport("projets",
                "Détails du chiffre d'affaires par projet pour l'année " + annee, colonnes, lignes, null, null,
                List.of())), avertissements);
    }
}
