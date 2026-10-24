package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureTransformationVente;
import com.tsix.immocrm.fixtures.FixtureTransformationVente.StatutsVente;
import com.tsix.immocrm.services.equipe.RolesEquipe;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Jeu commun des n° 20 et n° 26 (FEATURE-tdb-vente-rapports-commercial.md §6, §10). Période [DU ; AU] = mars 2025.
 * <pre>
 *   projet P (actif) : tranche T — A, C (Commercial Chef), D affectés ; projet P2 (actif) : tranche T2 — B affecté
 *   projet PI (entité INACTIVE) : tranche TI
 *   A sur T : 5 prospections Visite + 1 Appel dans la période, 1 Visite hors période ; dossiers :
 *     d1 CONC (2) avance 10/03 saisi 05/03     d2 MIN (7) avance 12/03 saisi 06/03   d3 CONC avance 10/01 saisi 05/01
 *     d4 ANN avance 15/03 saisi 08/03           d5 SANS statut saisi 09/03             d6 DES avance 01/06/2024
 *     d7 RES (1) saisi 11/03                    d8 CRED (5) avance 20/03 saisi 20/01   d9 CLES (8) avance 21/03 saisi 21/01
 *     d10 ANN sans avance saisi 2024
 *     ⇒ prospections 6, visites 5, réservations 4 (d1 d2 d5 d7), E 2 (d1 d8), G 2 (d2 d9), E total 3 (d1 d3 d8),
 *       annulations total 2 (d4 d6), taux (2 + 2) / 5 = 80
 *   A sur TI : 2 Visite + 1 dossier CONC avance 10/03 (ignorés : entité inactive)
 *   B sur T : 1 Visite, 1 dossier CONC avance 10/03 saisi 01/02  ⇒ visites 1, E 1, taux 100
 *   C sur T : 3 Visite                                             ⇒ taux 0
 *   D sur T : 1 Appel, 1 dossier CONC avance 10/03 saisi 01/02    ⇒ visites 0, E 1, taux 0 (dénominateur nul)
 *   Total sur T : visites 9, E 4, G 2 ⇒ taux imposé 66,67 (somme des taux 180, moyenne 45)
 * </pre>
 * Comptes : rôle libre pour les rapports (WithReportRestriction = 0) + rôle d'équipe ; « direction » = Direction Générale
 * (exemptée). Un commercial se reconnaît à son jeton dans le libellé {@code commercial} (code, nom et employé le portent).
 */
final class JeuTransformationCommerciale {

    static final LocalDate DU = LocalDate.of(2025, 3, 1);
    static final LocalDate AU = LocalDate.of(2025, 3, 31);
    static final String COMMERCIAL = "commercial";

    final String prefixe;
    final AdmRole roleLibre;
    final CfgProject projet;
    final CfgTranche t;
    final CfgTranche t2;
    final CfgTranche ti;
    final StatutsVente statuts;
    final ComActionMessageType visite;
    final ComActionMessageType appel;
    final AdmUser a;
    final AdmUser b;
    final AdmUser c;
    final AdmUser d;
    final AdmUser direction;

    private final FixtureTransformationVente fx;
    private int numero;

    JeuTransformationCommerciale(String racine, FixtureRapports rapports, FixtureEtatVentes ev,
                                 FixtureTransformationVente fx) {
        this.fx = fx;
        prefixe = FixtureEtatVentes.prefixe(racine);
        roleLibre = rapports.role("Libre " + prefixe, false);
        projet = ev.projet("P-" + prefixe, true);
        CfgProject projet2 = ev.projet("P2-" + prefixe, true);
        t = ev.tranche("T-" + prefixe, projet, true);
        t2 = ev.tranche("T2-" + prefixe, projet2, true);
        ti = ev.tranche("TI-" + prefixe, ev.projet("PI-" + prefixe, false), true);
        statuts = fx.statutsVente(prefixe);
        visite = fx.type("Visite");
        appel = fx.type("APPEL-" + prefixe);

        a = rapports.compte(jeton("alpha"), roleLibre, rapports.role(RolesEquipe.COMMERCIAL, true));
        b = rapports.compte(jeton("beta"), roleLibre, rapports.role(RolesEquipe.COMMERCIAL, true));
        c = rapports.compte(jeton("gamma"), roleLibre, rapports.role(RolesEquipe.CHEF, true));
        d = rapports.compte(jeton("delta"), roleLibre, rapports.role(RolesEquipe.COMMERCIAL, true));
        direction = rapports.compte(jeton("dg"), rapports.role("Direction Générale", false));
        ev.affecter(a, projet);
        ev.affecter(c, projet);
        ev.affecter(d, projet);
        ev.affecter(b, projet2);

        jeuDeA();
        prospections(t, b, visite, 1);
        dossier(t, b, statuts.concretisation(), jour(3, 10), LocalDate.of(2025, 2, 1));
        prospections(t, c, visite, 3);
        prospections(t, d, appel, 1);
        dossier(t, d, statuts.concretisation(), jour(3, 10), LocalDate.of(2025, 2, 1));
        rapports.relire();
    }

    private void jeuDeA() {
        prospections(t, a, visite, 5);
        prospections(t, a, appel, 1);
        fx.prospection(t, a, visite, LocalDate.of(2025, 2, 10), null);
        dossier(t, a, statuts.concretisation(), jour(3, 10), jour(3, 5));
        dossier(t, a, statuts.minute(), jour(3, 12), jour(3, 6));
        dossier(t, a, statuts.concretisation(), jour(1, 10), jour(1, 5));
        dossier(t, a, statuts.annule(), jour(3, 15), jour(3, 8));
        dossier(t, a, null, null, jour(3, 9));
        dossier(t, a, statuts.desiste(), LocalDate.of(2024, 6, 1), LocalDate.of(2024, 5, 1));
        dossier(t, a, statuts.reservation(), null, jour(3, 11));
        dossier(t, a, statuts.credit(), jour(3, 20), jour(1, 20));
        dossier(t, a, statuts.livre(), jour(3, 21), jour(1, 21));
        dossier(t, a, statuts.annule(), null, LocalDate.of(2024, 4, 1));

        prospections(ti, a, visite, 2);
        dossier(ti, a, statuts.concretisation(), jour(3, 10), jour(3, 5));
    }

    static LocalDate jour(int mois, int jour) {
        return LocalDate.of(2025, mois, jour);
    }

    String jeton(String nom) {
        return nom + "." + prefixe;
    }

    void prospections(CfgTranche tranche, AdmUser commercial, ComActionMessageType type, int nombre) {
        for (int i = 0; i < nombre; i++) {
            fx.prospection(tranche, commercial, type, jour(3, 2 + i), null);
        }
    }

    /** Dossier daté : {@code AdvanceDate} = {@code avance}, {@code DocDate} = {@code saisie} (nuls possibles). */
    ComFolder dossier(CfgTranche tranche, AdmUser commercial, StrStatus statut, LocalDate avance, LocalDate saisie) {
        numero++;
        ComFolder dossier = fx.dossier("DTR-" + prefixe + "-" + numero, tranche, commercial, statut, null);
        dossier.setAdvanceDate(avance == null ? null : avance.atTime(10, 0));
        dossier.setDocDate(saisie == null ? null : saisie.atTime(10, 0));
        return fx.enregistrer(dossier);
    }

    /** Lignes dont le libellé {@code commercial} porte le jeton du compte. */
    static List<Map<String, Object>> lignesDe(Map<String, Object> section, AdmUser compte) {
        String jeton = compte.getCode().substring(0, compte.getCode().length() - 37);
        return AppelsRapports.lignes(section).stream()
                .filter(l -> Objects.toString(l.get(COMMERCIAL), "").contains(jeton)).toList();
    }

    /** La ligne unique du compte dans la section. */
    static Map<String, Object> ligneDe(Map<String, Object> section, AdmUser compte) {
        List<Map<String, Object>> trouvees = lignesDe(section, compte);
        assertThat(trouvees).as("ligne de %s", compte.getCode()).hasSize(1);
        return trouvees.get(0);
    }
}
