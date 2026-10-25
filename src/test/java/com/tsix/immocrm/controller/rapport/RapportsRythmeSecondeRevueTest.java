package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureFluxVente;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Jeu;
import com.tsix.immocrm.services.rapport.vente.flux.DemandeFlux;
import com.tsix.immocrm.services.rapport.vente.flux.FluxVenteMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.MesureFlux;
import com.tsix.immocrm.services.rapport.vente.flux.MouvementFlux;
import com.tsix.immocrm.services.rapport.vente.flux.MouvementsFlux;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Seconde revue R2a (FEATURE-tdb-vente-rapports-rythme.md §3.2, réponse 8 du manager) : réservations sur tranche en vente seulement. */
@SpringBootTest
@Transactional
class RapportsRythmeSecondeRevueTest {

    @Autowired private FixtureFluxVente fixture;
    @Autowired private FixtureEtatVentes ev;
    @Autowired private FluxVenteMensuel flux;

    @Test
    void reservations_trancheHorsVente_aucuneLigne_trancheEnVente_compteeASaDocDate() {
        Jeu jeu = fixture.jeu("REV2A");
        CfgProject projet = jeu.projet("P", true);
        CfgTranche enVente = jeu.tranche("V", projet);
        CfgTranche horsVente = ev.tranche("HV-" + jeu.prefixe(), projet, false);
        ComFolder compte = jeu.dossier(enVente, jeu.statuts().reservation());
        compte.setDocDate(LocalDate.of(2077, 3, 4).atStartOfDay());
        ev.enregistrer(compte);
        ComFolder ignore = jeu.dossier(horsVente, jeu.statuts().reservation());
        ignore.setDocDate(LocalDate.of(2077, 3, 5).atStartOfDay());
        ev.enregistrer(ignore);
        fixture.relire();

        MouvementsFlux r = flux.mouvements(new DemandeFlux(List.of(enVente.getId(), horsVente.getId()), 2077, 2077,
                Set.of(MesureFlux.RESERVATIONS)));

        assertThat(r.lignes()).extracting(MouvementFlux::dossierId).containsExactly(compte.getId());
        assertThat(r.lignes()).extracting(MouvementFlux::date).containsExactly(LocalDate.of(2077, 3, 4));
    }
}
