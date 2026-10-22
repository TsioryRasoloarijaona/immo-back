package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.ModuleRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.Rapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.TypeParametre;
import com.tsix.immocrm.repositories.com.ComActionRepository;
import com.tsix.immocrm.services.rapport.vente.commercial.GroupesCommerciaux;
import com.tsix.immocrm.services.rapport.vente.commercial.PeriodesRapport;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * N° 31 ANALYSE DES ACTIONS COMMERCIALES (FEATURE-tdb-vente-rapports-commercial.md §9.2, décision 8), modèle
 * {@code ComSaleDashboardAnalyseActionCommerciale} : prospections par source sur [du ; au], éventuellement limitées
 * aux actions commerciales choisies. Le modèle, inachevé, ignore ses paramètres ; ils sont appliqués ici.
 */
@Component
public class AnalyseActionsCommercialesRapport implements Rapport {

    static final String CODE = "ComSaleDashboardAnalyseActionCommerciale";

    private static final String ACTION_ID = "actionId";

    private final GroupesCommerciaux groupes;
    private final ComActionRepository actions;
    private final Clock clock;

    public AnalyseActionsCommercialesRapport(GroupesCommerciaux groupes, ComActionRepository actions, Clock clock) {
        this.groupes = groupes;
        this.actions = actions;
        this.clock = clock;
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public ModuleRapport module() {
        return ModuleRapport.VENTE;
    }

    @Override
    public List<ParametreRapport> parametres() {
        LocalDate aujourdhui = LocalDate.now(clock);
        List<ParametreRapport.Option> options = actions.findAll(Sort.by("code")).stream()
                .map(a -> new ParametreRapport.Option(a.getId().toString(), a.getCode())).toList();
        return List.of(ParametreRapport.date(PeriodesRapport.DU, "Du", aujourdhui.withDayOfYear(1)),
                ParametreRapport.date(PeriodesRapport.AU, "Au", aujourdhui), ParametreRapport.tranches(),
                new ParametreRapport(ACTION_ID, "Actions commerciales", TypeParametre.CHOIX, false, true, null, options,
                        null, null));
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        List<UUID> choisies = contexte.choix(ACTION_ID).stream().map(UUID::fromString).toList();
        return new ResultatRapport(List.of(SuiviProspectionsRapport.sources(groupes, contexte,
                contexte.date(PeriodesRapport.DU).atStartOfDay(),
                contexte.date(PeriodesRapport.AU).plusDays(1).atStartOfDay(), "sources",
                "Prospections par source de contact", choisies)), List.of());
    }
}
