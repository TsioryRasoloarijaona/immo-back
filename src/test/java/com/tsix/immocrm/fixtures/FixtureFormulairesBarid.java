package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmCity;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.adm.AdmTierTitle;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderCustomer;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.repositories.app.AppModeleDocumentRepository;
import com.tsix.immocrm.services.modele.DemarrageModeles;
import jakarta.persistence.EntityManager;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Formulaires Barid Al-Maghrib (FEATURE-vente-formulaire-barid.md §3, §4) : ligne {@code AdmReportData} du n° 35, sociétés
 * à adresse complète, clients avec titre, ville du référentiel et pays, co-acquéreurs datés. Tout est annulé avec le test ;
 * aucune ligne de paramétrage partagée n'est relue (titres, villes, pays, statuts propres au test).
 */
@Component
public class FixtureFormulairesBarid {

    public static final String CODE_RAPPORT = "ComSaleFormulaireBaridMaroc";
    public static final String CODE_MODELE = "FORMULAIRE_BARID";
    public static final String VILLE_TEXTE_LIBRE = "VILLETEXTELIBRE";

    private final FixtureRapports rapports;
    private final FixtureEtatVentes ev;
    private final EntityManager em;
    private final JdbcTemplate jdbc;
    private final ApplicationContext contexte;
    private final AppModeleDocumentRepository modeles;

    /** Adresse d'un client ; tout composant peut être nul. */
    public record Adresse(String ligne1, String ligne2, String ligne3, String codePostal, AdmCity ville,
                          AdmCountry pays) {
        public static Adresse aucune() {
            return new Adresse(null, null, null, null, null, null);
        }
    }

    public FixtureFormulairesBarid(FixtureRapports rapports, FixtureEtatVentes ev, EntityManager em, JdbcTemplate jdbc,
                                   ApplicationContext contexte, AppModeleDocumentRepository modeles) {
        this.rapports = rapports;
        this.ev = ev;
        this.em = em;
        this.jdbc = jdbc;
        this.contexte = contexte;
        this.modeles = modeles;
    }

    /**
     * Modèles livrés au démarrage (coupé sous la suite : {@code app.modeles.initialiser=false}), dans la transaction du test ;
     * renvoie l'id du modèle {@code FORMULAIRE_BARID} semé (§3).
     */
    public UUID semerModeles() {
        contexte.getAutowireCapableBeanFactory().createBean(DemarrageModeles.class).run(null);
        return modeles.findByCategorieAndCode("DOSSIER_VENTE", CODE_MODELE)
                .orElseThrow(() -> new AssertionError("Modèle " + CODE_MODELE + " non semé au démarrage"))
                .getId();
    }

    /** Tables des rapports (absentes de yesRemsTest) et ligne du n° 35, comme en prod (§1). */
    public UUID rapport() {
        rapports.tables();
        return rapports.rapportVente(CODE_RAPPORT, "FORMULAIRES DE BARID AL-MAGHRIB", "VENTE", 2050);
    }

    public AdmCountry pays(String libelle) {
        AdmCountry pays = new AdmCountry();
        pays.setId(UUID.randomUUID());
        pays.setCode(libelle.length() > 20 ? libelle.substring(0, 20) : libelle);
        pays.setDescription(libelle);
        em.persist(pays);
        return pays;
    }

    public AdmCity ville(String code, AdmCountry pays) {
        AdmCity ville = new AdmCity();
        ville.setId(UUID.randomUUID());
        ville.setCode(code);
        ville.setDescription("Ville " + code);
        ville.setAdmCountry(pays);
        em.persist(ville);
        return ville;
    }

    public AdmTierTitle titre(String code) {
        AdmTierTitle titre = new AdmTierTitle();
        titre.setId(UUID.randomUUID());
        titre.setCode(code);
        titre.setDescription(code);
        em.persist(titre);
        return titre;
    }

    /**
     * Tranche en vente d'une société à adresse complète : {@code 10 BD SOCIETE}, {@code ETAGE 4}, ville texte
     * {@code casablanca}, code postal {@code 20000}, pays {@code pays}.
     */
    public CfgTranche tranche(String code, boolean societeActive, AdmCountry pays) {
        CfgProject projet = ev.projet("P-" + code, societeActive);
        CfgCompany societe = projet.getCfgCompany();
        societe.setAddress("10 BD SOCIETE");
        societe.setAddress2("ETAGE 4");
        societe.setCity("casablanca");
        societe.setZipCode("20000");
        societe.setAdmCountry(pays);
        em.flush();
        return ev.tranche(code, projet, true);
    }

    /** Client de nom {@code nom} ({@code CfgTier.Description}) ; la ville texte libre {@link #VILLE_TEXTE_LIBRE} ne doit jamais sortir. */
    public CfgTier client(String nom, AdmTierTitle titre, Adresse adresse) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode("CLI-" + UUID.randomUUID().toString().substring(0, 12));
        tier.setDescription(nom);
        tier.setLastName(nom);
        tier.setAdmTierTitle(titre);
        tier.setAddress(adresse.ligne1());
        tier.setAddress2(adresse.ligne2());
        tier.setAddress3(adresse.ligne3());
        tier.setZipCode(adresse.codePostal());
        tier.setCity(VILLE_TEXTE_LIBRE);
        tier.setAdmCountry(adresse.pays());
        em.persist(tier);
        em.flush();
        if (adresse.ville() != null) {
            // AdmCityId est en lecture seule dans l'entité.
            jdbc.update("UPDATE CfgTier SET AdmCityId = ? WHERE PKey = ?", adresse.ville().getId().toString(),
                    tier.getId().toString());
        }
        return tier;
    }

    /** Dossier de vente sur un article propre (un seul dossier vivant par article : trigger CI) ; {@code statut} peut être nul. */
    public ComFolder dossier(CfgTranche tranche, String code, StrStatus statut, CfgTier client) {
        return ev.vendre(ev.article("ART-" + code, tranche, ev.categorie("CAT-" + code), ev.type("TYP-" + code)),
                code, statut, client, null);
    }

    /** Co-acquéreur ({@code ComFolderCustomer}) saisi à {@code saisiLe}. */
    public void coAcquereur(ComFolder dossier, CfgTier tier, LocalDateTime saisiLe) {
        ComFolderCustomer co = new ComFolderCustomer();
        co.setId(UUID.randomUUID());
        co.setComFolder(dossier);
        co.setCfgTier(tier);
        co.setUserCreateDateTime(saisiLe);
        em.persist(co);
    }

    /**
     * Jeu du lot (§4, §8.1 « Lot / rapport ») ; noms = racine de {@link #ORDRE} + espace + {@code p}.
     * <pre>
     *   T1 (société active) : D1 statut S1 — BENNANI (sans titre, adresse, sans code postal) + co-acquéreur ZIANI (Mr, « B12 »)
     *                         D5 sans statut — EL FASSI (Mlle, adresse, sans code postal)
     *                         D3 annulé — DAOUDI ; D4 désisté — DRISSI
     *   T2 (société active) : D2 statut S2 — AMRANI (Mr, adresse complète, 20250) + co-acquéreur CHERKAOUI (Mme, sans adresse)
     *   TI (société INACTIVE) : DI statut S1 — INACTIF
     *   ordre d'impression T1+T2 : AMRANI (D2), BENNANI (D1), CHERKAOUI (D2), EL FASSI (D5), ZIANI (D1)
     * </pre>
     */
    public Jeu jeu(String p) {
        AdmCountry pays = pays("PAYS-" + p);
        AdmCity ville = ville("VILLE" + p, pays);
        CfgTranche t1 = tranche("T1-" + p, true, pays);
        CfgTranche t2 = tranche("T2-" + p, true, pays);
        CfgTranche ti = tranche("TI-" + p, false, pays);
        StrStatus s1 = ev.statutEnCours("S1-" + p);
        StrStatus s2 = ev.statutEnCours("S2-" + p);
        StrStatus annulation = ev.statutSortie("ANN-" + p, true);
        StrStatus desistement = ev.statutSortie("DES-" + p, false);

        ComFolder d1 = dossier(t1, "D1-" + p, s1,
                client("BENNANI " + p, null, new Adresse("2 RUE BENNANI", null, null, null, ville, pays)));
        coAcquereur(d1, client("ZIANI " + p, titre("Mr" + p), new Adresse("4 RUE ZIANI", null, null, "B12", null, pays)),
                LocalDateTime.of(2025, 1, 1, 0, 0));
        ComFolder d2 = dossier(t2, "D2-" + p, s2,
                client("AMRANI " + p, titre("Mr" + p), new Adresse("1 RUE AMRANI", null, null, "20250", ville, pays)));
        coAcquereur(d2, client("CHERKAOUI " + p, titre("Mme" + p), Adresse.aucune()), LocalDateTime.of(2025, 1, 1, 0, 0));
        ComFolder d5 = dossier(t1, "D5-" + p, null,
                client("EL FASSI " + p, titre("Mlle" + p), new Adresse("5 RUE FASSI", null, null, null, null, pays)));
        ComFolder d3 = dossier(t1, "D3-" + p, annulation, client("DAOUDI " + p, null, Adresse.aucune()));
        ComFolder d4 = dossier(t1, "D4-" + p, desistement, client("DRISSI " + p, null, Adresse.aucune()));
        ComFolder di = dossier(ti, "DI-" + p, s1, client("INACTIF " + p, null, Adresse.aucune()));
        relire();
        return new Jeu(p, t1, t2, ti, s1, s2, annulation, desistement, d1, d2, d5, d3, d4, di);
    }

    /** Racines des noms du jeu, dans l'ordre d'impression sur T1 + T2. */
    public static final List<String> ORDRE = List.of("AMRANI", "BENNANI", "CHERKAOUI", "EL FASSI", "ZIANI");

    /** Jeu du lot ; voir {@link #jeu(String)}. */
    public record Jeu(String p, CfgTranche t1, CfgTranche t2, CfgTranche ti, StrStatus s1, StrStatus s2,
                      StrStatus annulation, StrStatus desistement, ComFolder d1, ComFolder d2, ComFolder d5,
                      ComFolder annule, ComFolder desiste, ComFolder inactif) {
        public String nom(String racine) {
            return racine + " " + p;
        }
    }

    /** Dossier vivant à {@code nombreCoAcquereurs} co-acquéreurs, dans une tranche propre (plafond de pages). */
    public CfgTranche trancheAvecCoAcquereurs(String p, int nombreCoAcquereurs) {
        CfgTranche t = tranche("TP-" + p, true, pays("PAYSP-" + p));
        ComFolder d = dossier(t, "DP-" + p, ev.statutEnCours("SP-" + p), client("PLAFOND " + p, null, Adresse.aucune()));
        for (int i = 0; i < nombreCoAcquereurs; i++) {
            CfgTier tier = new CfgTier();
            tier.setId(UUID.randomUUID());
            tier.setCode("CO-" + UUID.randomUUID().toString().substring(0, 12));
            tier.setDescription("CO " + i + " " + p);
            em.persist(tier);
            coAcquereur(d, tier, LocalDateTime.of(2025, 1, 1, 0, 0).plusMinutes(i));
        }
        relire();
        return t;
    }

    /** Vide le cache JPA après les colonnes posées en SQL. */
    public void relire() {
        em.flush();
        em.clear();
    }
}
