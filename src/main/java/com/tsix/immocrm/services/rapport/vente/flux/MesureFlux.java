package com.tsix.immocrm.services.rapport.vente.flux;

/** Mesures de {@link FluxVenteMensuel} : définitions dans FEATURE-tdb-vente-rapports-flux.md §3.2. */
public enum MesureFlux {

    CONCRETISATIONS(Famille.DOSSIER),
    CONCRETISATIONS_NETTES(Famille.DOSSIER),
    CONCRETISATIONS_MONTANT(Famille.DOSSIER),
    CA_QUANTITE(Famille.DOSSIER),
    CA_TTC(Famille.DOSSIER),
    CA_HT(Famille.DOSSIER),
    LIVRAISONS(Famille.DOSSIER),
    DESISTEMENTS(Famille.DOSSIER),
    DESISTEMENTS_MONTANT(Famille.DOSSIER),
    ANNULATIONS(Famille.DOSSIER),
    ANNULATIONS_MONTANT(Famille.DOSSIER),
    ENCAISSEMENTS(Famille.REGLEMENT),
    AVANCES_ENCAISSEES(Famille.REGLEMENT),
    ENCAISSEMENTS_APRES_ACTE(Famille.REGLEMENT),
    ENCAISSEMENTS_FRAIS_DOSSIER(Famille.REGLEMENT),
    ENCAISSEMENTS_FRAIS_TRANSFERT(Famille.REGLEMENT),
    FRAIS_DESISTEMENT_DUS(Famille.ECHEANCE),
    PREVU_CONCRETISATION_QUANTITE(Famille.PREVU),
    PREVU_CONCRETISATION_MONTANT(Famille.PREVU),
    PREVU_CA_QUANTITE(Famille.PREVU),
    PREVU_CA_TTC(Famille.PREVU),
    PREVU_ENCAISSEMENT(Famille.PREVU),
    BASE_CA(Famille.PREVU),
    BASE_ENCAISSEMENT(Famille.PREVU);

    /** Source lue : une requête par famille demandée ; {@code PREVU} = {@link SourcePrevu}. */
    public enum Famille { DOSSIER, REGLEMENT, ECHEANCE, PREVU }

    private final Famille famille;

    MesureFlux(Famille famille) {
        this.famille = famille;
    }

    public Famille famille() {
        return famille;
    }
}
