package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.GraphiqueRapport;
import com.tsix.immocrm.domain.rapport.ModuleRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.TypeColonne;
import com.tsix.immocrm.domain.rapport.TypeGraphique;
import com.tsix.immocrm.dto.rapport.ParametreRapportDto;
import com.tsix.immocrm.dto.rapport.ResultatRapportDto;
import com.tsix.immocrm.services.rapport.ServiceRapports;
import com.tsix.immocrm.services.rapport.vente.TableauPrevisionsVente.Definition;
import com.tsix.immocrm.services.rapport.vente.TableauPrevisionsVente.Prevus;
import com.tsix.immocrm.services.rapport.vente.flux.DemandeFlux;
import com.tsix.immocrm.services.rapport.vente.flux.FluxMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.FluxVenteMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.services.rapport.vente.TableauPrevisionsVente.graphique;
import static com.tsix.immocrm.services.rapport.vente.flux.MesureFlux.*;

/** Écran « Suivi des prévisions de vente » (FEATURE-tdb-vente-previsions.md §4), par le socle sans AdmReportData. */
@Service
public class SuiviPrevisionsVente {

    private static final String CODE = "SuiviPrevisionsVente";
    private static final String LIBELLE = "Suivi des prévisions de vente";
    private static final String REEL = "Réel";
    static final String MENTION = "(*) Encaissements sans les pièces non comptabilisées ni les règlements annulés ; "
            + "frais selon le filtre.";

    private static final Definition QUANTITES = new Definition("quantites", "Concrétisations (quantité)",
            TypeColonne.NOMBRE, new Prevus(BASE_CONCRETISATION_QUANTITE, ANCIEN_PREVU_CONCRETISATION_QUANTITE,
            PREVU_CONCRETISATION_QUANTITE), REEL, List.of(CONCRETISATIONS_NETTES), List.of());

    private static final List<GraphiqueRapport> POURCENTAGES = List.of(
            graphique(TypeGraphique.COLONNES, "Pourcentage de réalisation", TableauPrevisionsVente.POURCENTAGE_REALISE),
            graphique(TypeGraphique.LIGNES, "Pourcentage annuel", TableauPrevisionsVente.POURCENTAGE_CUMULE),
            graphique(TypeGraphique.COLONNES, "Rentabilité", TableauPrevisionsVente.RENTABILITE));

    private final ServiceRapports socle;
    private final FluxVenteMensuel flux;
    private final DetailPrevisionsVente detail;
    private final Clock clock;

    SuiviPrevisionsVente(ServiceRapports socle, FluxVenteMensuel flux, DetailPrevisionsVente detail, Clock clock) {
        this.socle = socle;
        this.flux = flux;
        this.detail = detail;
        this.clock = clock;
    }

    public List<ParametreRapportDto> parametres() {
        return socle.parametresEcran(ecran());
    }

    public ResultatRapportDto resultat(Map<String, List<String>> recus) {
        return socle.resultatEcran(ecran(), recus);
    }

    public ResultatRapportDto detail(Map<String, List<String>> recus) {
        List<ParametreRapport> parametres = new ArrayList<>(parametresEcran());
        parametres.add(DetailPrevisionsVente.MOIS_PARAMETRE);
        return socle.resultatEcran(new ServiceRapports.Ecran(ModuleRapport.VENTE, CODE + "Mois",
                LIBELLE + " — détail du mois", parametres, detail::executer), recus);
    }

    public ServiceRapports.Export exporter(String format, Map<String, List<String>> recus) {
        return socle.exporterEcran(ecran(), format, recus);
    }

    private ServiceRapports.Ecran ecran() {
        return new ServiceRapports.Ecran(ModuleRapport.VENTE, CODE, LIBELLE, parametresEcran(), this::executer);
    }

    private List<ParametreRapport> parametresEcran() {
        return List.of(LignesMensuelles.annee(clock), ParametreRapport.tranches(), FraisEcran.parametre());
    }

    private static Definition montants(FraisEcran frais) {
        return new Definition("montants", "Encaissements (montant) (*)", TypeColonne.MONTANT,
                new Prevus(BASE_ENCAISSEMENT, ANCIEN_PREVU_ENCAISSEMENT, PREVU_ENCAISSEMENT), REEL, frais.reel(),
                frais.schema());
    }

    private ResultatRapport executer(ContexteRapport contexte) {
        int annee = RapportFluxVente.annee(contexte);
        Definition montants = montants(FraisEcran.de(contexte));
        FluxMensuel f = flux.calculer(new DemandeFlux(contexte.tranchesVisibles(), annee, annee,
                TableauPrevisionsVente.mesures(QUANTITES, montants)));
        List<String> avertissements = new ArrayList<>(LignesMensuelles.avertissements(f));
        avertissements.add(MENTION);
        return new ResultatRapport(List.of(TableauPrevisionsVente.section(QUANTITES, f, annee, POURCENTAGES),
                TableauPrevisionsVente.section(montants, f, annee, POURCENTAGES)), avertissements);
    }
}
