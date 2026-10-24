package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.ModuleRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.Rapport;
import com.tsix.immocrm.services.rapport.vente.flux.DemandeFlux;
import com.tsix.immocrm.services.rapport.vente.flux.FluxMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.FluxVenteMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles;
import com.tsix.immocrm.services.rapport.vente.flux.MesureFlux;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Rapport Vente lu dans {@link FluxVenteMensuel}, sur les seules tranches visibles ; paramètres année + tranches. */
abstract class RapportFluxVente implements Rapport {

    private final String code;
    protected final FluxVenteMensuel flux;
    protected final Clock clock;

    RapportFluxVente(String code, FluxVenteMensuel flux, Clock clock) {
        this.code = code;
        this.flux = flux;
        this.clock = clock;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public ModuleRapport module() {
        return ModuleRapport.VENTE;
    }

    @Override
    public List<ParametreRapport> parametres() {
        return List.of(LignesMensuelles.annee(clock), ParametreRapport.tranches());
    }

    /** Année demandée ; toujours présente quand le paramètre a un défaut (socle). */
    protected static int annee(ContexteRapport contexte) {
        return Objects.requireNonNull(contexte.annee(LignesMensuelles.ANNEE), "Paramètre annee absent");
    }

    protected FluxMensuel calculer(ContexteRapport contexte, Integer debut, Integer fin, Set<MesureFlux> mesures) {
        return flux.calculer(new DemandeFlux(contexte.tranchesVisibles(), debut, fin, mesures));
    }
}
