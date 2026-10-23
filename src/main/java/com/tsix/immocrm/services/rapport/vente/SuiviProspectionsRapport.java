package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.Chemin;
import com.tsix.immocrm.domain.ColonneProspection;
import com.tsix.immocrm.domain.ColonneSuiviAction;
import com.tsix.immocrm.domain.Granularite;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.ModuleRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.Rapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.services.rapport.vente.commercial.GroupesCommerciaux;
import com.tsix.immocrm.services.rapport.vente.commercial.PeriodesRapport;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.services.rapport.vente.commercial.GroupesCommerciaux.parLibelle;
import static com.tsix.immocrm.services.rapport.vente.commercial.SectionsCommerciales.repartition;

/**
 * N° 19 SUIVI DES PROSPECTIONS (FEATURE-tdb-vente-rapports-commercial.md §5), modèle {@code ComTdbProspectionState} :
 * prospections et actions de prospection de l'année, réparties par type, source, nature, résultat, utilisateur, mois.
 */
@Component
public class SuiviProspectionsRapport implements Rapport {

    static final String CODE = "ComTdbProspectionState";
    /** Section reprise telle quelle par le n° 31. */
    static final Chemin SOURCE = ColonneProspection.ORIGINE.chemin();

    private final GroupesCommerciaux groupes;
    private final Clock clock;

    public SuiviProspectionsRapport(GroupesCommerciaux groupes, Clock clock) {
        this.groupes = groupes;
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
        return List.of(PeriodesRapport.parametreAnnee(clock), ParametreRapport.tranches());
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        LocalDateTime debut = LocalDateTime.of(contexte.annee(PeriodesRapport.ANNEE), Month.JANUARY, 1, 0, 0);
        LocalDateTime fin = debut.plusYears(1);
        return new ResultatRapport(List.of(
                prospections(contexte, debut, fin, "prospections-type", "Prospections par type",
                        Chemin.groupable("comActionMessageType.code")),
                sources(groupes, contexte, debut, fin, "prospections-source", "Prospections par source", List.of()),
                prospections(contexte, debut, fin, "prospections-nature", "Prospections par nature",
                        ColonneProspection.ETAPE.chemin()),
                GroupesCommerciaux.mensuel("prospections-mois", "Prospections par mois",
                        groupes.prospections(contexte, debut, fin, (r, q, cb) -> cb.conjunction(),
                                ColonneProspection.DATE_DOCUMENT.chemin(), Granularite.MOIS)),
                actions(contexte, debut, fin, "actions-resultat", "Actions par résultat", ColonneSuiviAction.RESULTAT),
                actions(contexte, debut, fin, "actions-type", "Actions par type", ColonneSuiviAction.TYPE_ACTION),
                actions(contexte, debut, fin, "actions-utilisateur", "Actions par utilisateur",
                        ColonneSuiviAction.UTILISATEUR),
                GroupesCommerciaux.mensuel("actions-mois", "Actions par mois", groupes.actions(contexte, debut, fin,
                        ColonneSuiviAction.DATE_ACTION.chemin(), Granularite.MOIS))), List.of());
    }

    /** Prospections par source, des actions {@code actions} si non vide : la même section aux n° 19 et 31 (§9.2). */
    static SectionRapport sources(GroupesCommerciaux groupes, ContexteRapport contexte, LocalDateTime debut,
                                  LocalDateTime fin, String id, String titre, List<UUID> actions) {
        return repartition(id, titre, parLibelle(groupes.prospections(contexte, debut, fin,
                (r, q, cb) -> actions.isEmpty() ? cb.conjunction() : r.get("comAction").get("id").in(actions),
                SOURCE, null)));
    }

    private SectionRapport prospections(ContexteRapport contexte, LocalDateTime debut, LocalDateTime fin, String id,
                                        String titre, Chemin cle) {
        return repartition(id, titre, parLibelle(groupes.prospections(contexte, debut, fin,
                (r, q, cb) -> cb.conjunction(), cle, null)));
    }

    private SectionRapport actions(ContexteRapport contexte, LocalDateTime debut, LocalDateTime fin, String id,
                                   String titre, ColonneSuiviAction colonne) {
        return repartition(id, titre, parLibelle(groupes.actions(contexte, debut, fin, colonne.chemin(), null)));
    }
}
