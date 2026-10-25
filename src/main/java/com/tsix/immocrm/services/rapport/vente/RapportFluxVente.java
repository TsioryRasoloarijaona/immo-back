package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.FonctionTotal;
import com.tsix.immocrm.domain.rapport.ModuleRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.Rapport;
import com.tsix.immocrm.domain.rapport.TypeColonne;
import com.tsix.immocrm.services.rapport.vente.flux.DemandeFlux;
import com.tsix.immocrm.services.rapport.vente.flux.FluxMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.FluxVenteMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles;
import com.tsix.immocrm.services.rapport.vente.flux.MesureFlux;
import com.tsix.immocrm.services.rapport.vente.flux.ValorisationTranche;

import com.tsix.immocrm.services.rapport.LotsTranches;
import jakarta.persistence.EntityManager;

import java.time.Clock;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Rapport Vente lu dans {@link FluxVenteMensuel}, sur les seules tranches visibles ; paramètres année + tranches. */
abstract class RapportFluxVente implements Rapport {

    static final String PROGRAMME = "programme";
    static final String TRANCHE = "tranche";

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

    /** Colonnes {@code programme}, {@code tranche} d'une ligne par tranche (n° 7, 11). */
    static List<ColonneRapport> colonnesTranche() {
        return List.of(ColonneRapport.texte(PROGRAMME, "Programme"), ColonneRapport.texte(TRANCHE, "Tranche"));
    }

    /** Libellés d'une tranche et de son projet ({@code projet} nul si la tranche n'en a pas). */
    record TrancheProjet(UUID tranche, String libelle, UUID projet, String libelleProjet) {
    }

    /** Tranches {@code ids} (visibles) avec leur projet, triées par libellé de tranche. */
    static List<TrancheProjet> tranchesProjets(EntityManager em, List<UUID> ids) {
        return LotsTranches.lire(ids, lot -> em.createQuery("SELECT t.id, t.description, p.id, p.description "
                        + "FROM CfgTranche t LEFT JOIN t.cfgProject p WHERE t.id IN :ids", Object[].class)
                .setParameter("ids", lot).getResultList()).stream()
                .map(r -> new TrancheProjet((UUID) r[0], (String) r[1], (UUID) r[2], (String) r[3]))
                .sorted(Comparator.comparing(t -> Objects.toString(t.libelle(), ""))).toList();
    }

    /** Codes et libellé d'une tranche en vente ; {@code projet} = code du programme ; {@code lancee} = date de lancement posée. */
    record TrancheEnVente(UUID id, String code, String libelle, String projet, boolean lancee) {
    }

    /** Tranches en vente parmi {@code ids} (visibles), triées par code de programme puis code de tranche. */
    static List<TrancheEnVente> tranchesEnVente(EntityManager em, List<UUID> ids) {
        return LotsTranches.lire(ids, lot -> em.createQuery("SELECT t.id, t.code, t.description, p.code, "
                        + "t.marketingStartDate FROM CfgTranche t LEFT JOIN t.cfgProject p "
                        + "WHERE t.id IN :ids AND t.isForSale = true", Object[].class)
                .setParameter("ids", lot).getResultList()).stream()
                .map(r -> new TrancheEnVente((UUID) r[0], (String) r[1], (String) r[2], (String) r[3], r[4] != null))
                .sorted(Comparator.comparing((TrancheEnVente t) -> Objects.toString(t.projet(), ""))
                        .thenComparing(t -> Objects.toString(t.code(), ""))).toList();
    }

    /** Nombre entier sans total (axe, stock, cumul). */
    static ColonneRapport compteur(String nom, String libelle) {
        return new ColonneRapport(nom, libelle, TypeColonne.NOMBRE, null, FonctionTotal.AUCUNE, true, 0);
    }

    /** Ligne de section à partir de paires clé, valeur (valeurs nulles permises). */
    static Map<String, Object> ligne(Object... clesValeurs) {
        Map<String, Object> ligne = new LinkedHashMap<>();
        for (int i = 0; i + 1 < clesValeurs.length; i += 2) {
            ligne.put((String) clesValeurs[i], clesValeurs[i + 1]);
        }
        return ligne;
    }

    static Map<String, Object> ligneTranche(ValorisationTranche.Valorisation v) {
        Map<String, Object> ligne = new LinkedHashMap<>();
        ligne.put(PROGRAMME, v.programme());
        ligne.put(TRANCHE, v.libelle());
        return ligne;
    }
}
