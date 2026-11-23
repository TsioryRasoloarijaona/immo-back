package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.FonctionTotal;
import com.tsix.immocrm.domain.rapport.ModuleRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.Rapport;
import com.tsix.immocrm.domain.rapport.TypeColonne;
import com.tsix.immocrm.domain.rapport.TypeParametre;
import com.tsix.immocrm.dto.projet.RetenueGarantieDto;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.services.projet.LotsProjetService.LotCalcule;
import com.tsix.immocrm.utils.TierNomFormatter;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Rapport du module Projet (FEATURE-n5b-rapports-projet.md) : lots visibles des projets demandés, projet hors périmètre
 * ignoré (D3). Le descripteur affiche {@code projets} en choix ; la requête l'accepte en texte libre pour ignorer
 * un projet hors options au lieu du 400 du socle.
 */
abstract class RapportProjet implements Rapport {

    static final String PROJETS = "projets";
    static final String MESSAGE_SANS_PROJET = "Veuillez sélectionner au moins un projet !";
    static final String PROJET = "projet";
    private static final String LIBELLE_PROJETS = "Projets";

    final SourcesRapportsProjet sources;

    RapportProjet(SourcesRapportsProjet sources) {
        this.sources = sources;
    }

    /** Montants de retenue de garantie d'un lot (R1) ; {@code retenue} = décomptée si mono-fournisseur, facturée sinon. */
    record Garantie(BigDecimal retenue, BigDecimal restituee, BigDecimal reste, BigDecimal penalite) {

        static Garantie de(LotCalcule l) {
            RetenueGarantieDto rg = l.calcul().suivi().retenueGarantie();
            BigDecimal retenue = l.calcul().drapeaux().multi() ? rg.retenue() : l.calcul().retenueDecomptee();
            return new Garantie(retenue, rg.restituee(), rg.resteARestituer(), l.calcul().penaliteRg());
        }

        /** Filtre OMRAN de R1, repris par R2 / R3. */
        boolean nonNulle() {
            return Stream.of(retenue, restituee, reste, penalite).anyMatch(m -> m != null && m.signum() != 0);
        }
    }

    @Override
    public ModuleRapport module() {
        return ModuleRapport.PROJET;
    }

    @Override
    public List<ParametreRapport> parametres() {
        return List.of(sources.requetes().projets(PROJETS, LIBELLE_PROJETS, true));
    }

    @Override
    public List<ParametreRapport> parametresAcceptes(List<ParametreRapport> affiches) {
        return List.of(new ParametreRapport(PROJETS, LIBELLE_PROJETS, TypeParametre.TEXTE, false, true, null, List.of(),
                null, null));
    }

    /** R1–R3 : sans projet, ou sans aucun projet habilité parmi ceux demandés → 400. */
    List<LotCalcule> lotsExiges(ContexteRapport contexte) {
        List<UUID> demandes = projets(contexte);
        if (demandes.isEmpty() || sources.perimetre().restreindre(demandes).isEmpty()) {
            throw new InvalidRequestException(MESSAGE_SANS_PROJET);
        }
        return lots(demandes);
    }

    /** Lots visibles (∩ périmètre ; vide = tous), triés par code projet puis code lot, fournisseurs préchargés. */
    List<LotCalcule> lots(List<UUID> projets) {
        List<LotCalcule> lots = sources.lots().calcules(projets);
        List<UUID> fournisseurs = lots.stream().map(l -> l.lot().getCfgSupplier()).filter(Objects::nonNull)
                .map(CfgTier::getId).distinct().toList();
        if (!fournisseurs.isEmpty()) {
            sources.em().createQuery("SELECT t FROM CfgTier t WHERE t.id IN :ids", CfgTier.class)
                    .setParameter("ids", fournisseurs).getResultList();
        }
        return lots.stream().sorted(Comparator.comparing(l -> Objects.toString(l.lot().getPrjProject().getCode(), "")))
                .toList();
    }

    static List<UUID> projets(ContexteRapport contexte) {
        try {
            return contexte.choix(PROJETS).stream().map(UUID::fromString).toList();
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Valeur invalide pour « projets » : identifiant de projet attendu.");
        }
    }

    /** {@code code}, {@code description}, {@code fournisseur}, {@code projet} (code ou libellé du projet). */
    static Map<String, Object> ligne(PrjMarket lot, String projet) {
        Map<String, Object> ligne = new LinkedHashMap<>();
        ligne.put("code", lot.getCode());
        ligne.put("description", lot.getDescription());
        ligne.put("fournisseur", lot.getCfgSupplier() == null ? null : TierNomFormatter.nom(lot.getCfgSupplier()));
        ligne.put(PROJET, projet);
        return ligne;
    }

    static List<ColonneRapport> colonnesLot() {
        return List.of(ColonneRapport.texte("code", "Lot"), ColonneRapport.texte("description", "Description"),
                ColonneRapport.texte("fournisseur", "Fournisseur"), ColonneRapport.texte(PROJET, "Projet"));
    }

    /** Colonne sans total (montant ou pourcentage). */
    static ColonneRapport sansTotal(String nom, String libelle, TypeColonne type) {
        return new ColonneRapport(nom, libelle, type, null, FonctionTotal.AUCUNE, true, 0);
    }

    static List<ColonneRapport> avec(List<ColonneRapport> debut, ColonneRapport... suite) {
        return Stream.concat(debut.stream(), Stream.of(suite)).toList();
    }
}
