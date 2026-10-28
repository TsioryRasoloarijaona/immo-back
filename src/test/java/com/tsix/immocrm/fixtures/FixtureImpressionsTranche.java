package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderCustomer;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixturePremieresRelances.Jeu;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Relance 2 par statut (FEATURE-patrimoine-impressions-tranche.md, lot P9f) : statuts aux codes exacts du modèle OMRAN, créés
 * par le test (jamais relus dans la base partagée), dossiers sur article de la tranche, co-acquéreurs, prospection d'une autre
 * tranche, ville de signature. S'appuie sur {@link FixturePremieresRelances} (tranche, articles, dossiers, montants).
 */
@Component
public class FixtureImpressionsTranche {

    public static final String CONCRETISATION = "CONCRETISATION";
    public static final String MEILLEURTAUX = "MEILLEURTAUX";
    public static final String ENVOI_NOTAIRE = "ENVOI AU NOTAIRE";
    public static final String CREDIT_EN_COURS = "CREDIT EN COURS";
    public static final String CREDIT_ACCORDE = "CREDIT ACCORDE";
    public static final List<String> CINQ_STATUTS =
            List.of(CONCRETISATION, MEILLEURTAUX, ENVOI_NOTAIRE, CREDIT_EN_COURS, CREDIT_ACCORDE);

    private final FixturePremieresRelances relances;
    private final FixtureEtatVentes ev;
    private final EntityManager em;

    public FixtureImpressionsTranche(FixturePremieresRelances relances, FixtureEtatVentes ev, EntityManager em) {
        this.relances = relances;
        this.ev = ev;
        this.em = em;
    }

    public Jeu jeu() {
        return relances.jeu(true);
    }

    public Jeu jeu(boolean entiteActive) {
        return relances.jeu(entiteActive);
    }

    /** Statut propre au test, au code exact donné (ni annulation ni désistement). */
    public StrStatus statut(String code) {
        return ev.statutEnCours(code);
    }

    public StrStatus annulation(Jeu j) {
        return relances.annulation(j);
    }

    public StrStatus desistement(Jeu j) {
        return relances.desistement(j);
    }

    /** Dossier sur un article racine de la tranche ; client {@code NOM<code>} ; prix 1 200 000, rien de réglé. */
    public ComFolder dossier(Jeu j, String code, StrStatus statut) {
        return relances.dossier(j, code, statut, false);
    }

    public ComFolder dossierSousArticle(Jeu j, String code, StrStatus statut) {
        return relances.dossierSousArticle(j, code, statut);
    }

    public CfgTier coAcquereur(ComFolder dossier, String nom) {
        CfgTier tier = ev.client(nom);
        ComFolderCustomer lien = new ComFolderCustomer();
        lien.setId(UUID.randomUUID());
        lien.setComFolder(dossier);
        lien.setCfgTier(tier);
        em.persist(lien);
        em.flush();
        return tier;
    }

    /** Rattache le dossier à une prospection portée par une autre tranche (la tranche de l'article fait foi). */
    public ComFolder prospectionDans(ComFolder dossier, CfgTranche tranche) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("PSN-" + dossier.getCode());
        p.setStkHierarchy(tranche);
        em.persist(p);
        dossier.setComProspection(p);
        ComFolder sauve = ev.enregistrer(dossier);
        em.flush();
        return sauve;
    }

    public ComFolder relance2(ComFolder dossier, LocalDateTime date) {
        dossier.setRevival2(date);
        ComFolder sauve = ev.enregistrer(dossier);
        em.flush();
        return sauve;
    }

    public void villeSignature(Jeu j, String ville) {
        CfgCompany societe = j.projet().getCfgCompany();
        societe.setSignatureCity(ville);
        em.merge(societe);
        em.flush();
    }

    public ComFolder prix(ComFolder dossier, String prix) {
        return relances.prix(dossier, prix);
    }

    /** Échéance au crédit réglée de {@code regle} (encaissement), puis d'un règlement annulé {@code annule}. */
    public void echeance(ComFolder dossier, FixtureEtatVentes.Nature nature, String montant, String regle, String annule) {
        relances.echeance(dossier, nature, montant, regle, annule);
    }

    public void relire() {
        em.flush();
        em.clear();
    }

    // ------------------------------------------------------------------ lecture seule (M5)

    /** Empreinte des écritures possibles : {@code ComFolder} (toutes colonnes), {@code ComActionMessage}, {@code AdmWebHistorics}. */
    public List<Object> empreinte(List<UUID> dossiers, List<UUID> autresObjets) {
        em.flush();
        List<String> ids = dossiers.stream().map(UUID::toString).toList();
        List<String> tous = new ArrayList<>(ids);
        autresObjets.forEach(id -> tous.add(id.toString()));
        Object[] folder = (Object[]) em.createNativeQuery(
                        "SELECT COUNT(*), CHECKSUM_AGG(BINARY_CHECKSUM(*)) FROM ComFolder WHERE PKey IN (:ids)")
                .setParameter("ids", ids).getSingleResult();
        Object[] actions = (Object[]) em.createNativeQuery(
                        "SELECT COUNT(*), CHECKSUM_AGG(BINARY_CHECKSUM(*)) FROM ComActionMessage WHERE ComFolderId IN (:ids)")
                .setParameter("ids", ids).getSingleResult();
        Object historiques = em.createNativeQuery(
                        "SELECT COUNT(*) FROM AdmWebHistorics WHERE ObjectId IN (:ids) OR ParentObjectId IN (:ids)")
                .setParameter("ids", tous).getSingleResult();
        List<Object> empreinte = new ArrayList<>(Arrays.asList(folder));
        empreinte.addAll(Arrays.asList(actions));
        empreinte.add(historiques);
        return empreinte;
    }

    public void ecrireDateImpressionContrat(ComFolder dossier, LocalDateTime date) {
        em.flush();
        em.createNativeQuery("UPDATE ComFolder SET ContractPrintDate = :date WHERE PKey = :id")
                .setParameter("date", date).setParameter("id", dossier.getId().toString()).executeUpdate();
    }

    public LocalDateTime dateImpressionContrat(ComFolder dossier) {
        relire();
        return em.find(ComFolder.class, dossier.getId()).getContractPrintDate();
    }
}
