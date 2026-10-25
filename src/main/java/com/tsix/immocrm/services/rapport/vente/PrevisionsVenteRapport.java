package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.GraphiqueRapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.TypeColonne;
import com.tsix.immocrm.domain.rapport.TypeGraphique;
import com.tsix.immocrm.services.rapport.vente.TableauPrevisionsVente.Definition;
import com.tsix.immocrm.services.rapport.vente.TableauPrevisionsVente.Prevus;
import com.tsix.immocrm.services.rapport.vente.flux.FluxMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.FluxVenteMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles;
import com.tsix.immocrm.services.rapport.vente.flux.MesureFlux;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import static com.tsix.immocrm.services.rapport.vente.flux.MesureFlux.*;

/**
 * N° 18 SUIVI DES PRÉVISIONS DE VENTE (FEATURE-tdb-vente-previsions.md §5), modèle {@code ComTdbPrevisionState} : quatre
 * gabarits de 12 mois ; chaque pourcentage est divisé par son propre prévu (fautes (a)–(c) du modèle corrigées).
 */
@Component
public class PrevisionsVenteRapport extends RapportFluxVente {

    static final String CODE = "ComTdbPrevisionState";
    static final String ECARTS = "Écarts avec OMRAN : chaque pourcentage est rapporté à son propre prévu ; chiffre d'affaires "
            + "sans les dossiers annulés ou désistés ; sociétés actives seulement.";
    private static final String REEL = "Réel";

    private static final List<Definition> SECTIONS = List.of(
            definition("concretisations-quantite", "Nombre de concrétisation", TypeColonne.NOMBRE,
                    new Prevus(BASE_CONCRETISATION_QUANTITE, ANCIEN_PREVU_CONCRETISATION_QUANTITE,
                            PREVU_CONCRETISATION_QUANTITE), "Qté. réelle (dont annulées)", CONCRETISATIONS),
            definition("concretisations-montant", "Montants des concrétisations", TypeColonne.MONTANT,
                    new Prevus(BASE_CONCRETISATION_MONTANT, ANCIEN_PREVU_CONCRETISATION_MONTANT,
                            PREVU_CONCRETISATION_MONTANT), REEL, CONCRETISATIONS_MONTANT),
            definition("ca-quantite", "Quantité Chiffre d'affaires", TypeColonne.NOMBRE,
                    new Prevus(BASE_CA_QUANTITE, ANCIEN_PREVU_CA_QUANTITE, PREVU_CA_QUANTITE), REEL, CA_QUANTITE),
            definition("ca-montant", "Montant chiffre d'affaires", TypeColonne.MONTANT,
                    new Prevus(BASE_CA, ANCIEN_PREVU_CA_TTC, PREVU_CA_TTC), REEL, CA_TTC));

    private static final List<GraphiqueRapport> POURCENTAGES = List.of(TableauPrevisionsVente.graphique(
            TypeGraphique.LIGNES, "Pourcentages", TableauPrevisionsVente.POURCENTAGE_REALISE,
            TableauPrevisionsVente.POURCENTAGE_CUMULE, TableauPrevisionsVente.RENTABILITE));

    public PrevisionsVenteRapport(FluxVenteMensuel flux, Clock clock) {
        super(CODE, flux, clock);
    }

    private static Definition definition(String id, String titre, TypeColonne unite, Prevus prevus, String libelleReel,
                                         MesureFlux reel) {
        return new Definition(id, titre, unite, prevus, libelleReel, List.of(reel), List.of());
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        int annee = annee(contexte);
        FluxMensuel f = calculer(contexte, annee, annee,
                TableauPrevisionsVente.mesures(SECTIONS.toArray(Definition[]::new)));
        List<String> avertissements = new ArrayList<>(LignesMensuelles.avertissements(f));
        avertissements.add(ECARTS);
        return new ResultatRapport(SECTIONS.stream()
                .map(d -> TableauPrevisionsVente.section(d, f, annee, POURCENTAGES)).toList(), avertissements);
    }
}
