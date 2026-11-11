package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmUser;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Décomptes de lot pour FEATURE-decomptes-parite : droits OMRAN {@code AdmRight}, lignes de bordereau et de décompte,
 * colonnes posées et relues en SQL (les entités {@code PrjWorkingOut*} ne mappent pas toutes les colonnes).
 */
@Component
public class FixtureDecomptes {

    // ------------------------------------------------------------------ droits AdmRight (spec §3-§8, Q11)
    /** A_0152_13 : ajout / modification (Enregistrer, création, Définir dernier, Recharger). */
    public static final String DROIT_MODIFIER = "Yes.Prj.Front.Market.WorkingOutList.toolNew";
    /** A_0152_14 : suppression. */
    public static final String DROIT_SUPPRIMER = "Yes.Prj.Front.Market.WorkingOutList.toolDelete";
    /** A_0152_15 : recalcul (et A_0152_13 sur l'écran sans décomposition). */
    public static final String DROIT_RECALCULER = "Yes.Prj.Front.Market.MarketMonitoringList.RecalculateWorkingOut";

    private final EntityManager em;
    private final JdbcTemplate jdbc;
    private final FixtureDossierVente vente;
    private final FixtureProjetLots lots;

    public FixtureDecomptes(EntityManager em, JdbcTemplate jdbc, FixtureDossierVente vente, FixtureProjetLots lots) {
        this.em = em;
        this.jdbc = jdbc;
        this.vente = vente;
        this.lots = lots;
    }

    /** Compte habilité sur le projet, rôle propre (jamais Administrateur) portant exactement ces droits. */
    public RequestPostProcessor compte(UUID projet, String jeton, String... droits) {
        String role = "Decomptes " + jeton + "-" + UUID.randomUUID().toString().substring(0, 6);
        AdmUser compte = vente.compte("prj.dr." + jeton, role);
        lots.habiliter(compte, projet);
        String roleId = jdbc.queryForObject("SELECT CAST(AdmRoleId AS varchar(36)) FROM AdmUserRole WHERE AdmUserId = ?",
                String.class, compte.getId().toString());
        for (String droit : droits) {
            jdbc.update("INSERT INTO AdmRight (PKey, Code, Description, AdmRoleId) VALUES (?, ?, ?, ?)",
                    UUID.randomUUID().toString(), droit, droit, roleId);
        }
        return FixtureDossierVente.connecte(compte, role);
    }

    /** Ligne de bordereau ({@code PrjMarketLine}), FODEC 0, prix TTC = HT × (1 + TVA). */
    public UUID ligneMarche(UUID lot, int ordre, String quantite, String puHT, String tva) {
        UUID id = UUID.randomUUID();
        BigDecimal ht = new BigDecimal(puHT);
        BigDecimal taux = new BigDecimal(tva);
        flush();
        jdbc.update("""
                        INSERT INTO PrjMarketLine (PKey, Code, Description, PrjMarketId, Sorting, Unity, Quantity, UnitPriceHT,
                            VatRatio, UnitPriceTTC, FODECRatio, IsStock) VALUES (?, ?, ?, ?, ?, 'U', ?, ?, ?, ?, 0, 0)""",
                id.toString(), String.valueOf(ordre), "Ligne " + ordre, lot.toString(), ordre, new BigDecimal(quantite), ht,
                taux, ht.add(ht.multiply(taux)));
        return id;
    }

    /** Ligne de décompte sur une ligne de bordereau : quantité du mois, cumul antérieur, coefficient 1, FODEC 0. */
    public UUID ligne(UUID decompte, UUID ligneMarche, int ordre, String quantite, String quantiteAnterieure, String puHT,
                      String tva) {
        UUID id = UUID.randomUUID();
        BigDecimal ht = new BigDecimal(puHT);
        BigDecimal taux = new BigDecimal(tva);
        flush();
        jdbc.update("""
                        INSERT INTO PrjWorkingOutLine (Pkey, Sorting, Code, Description, Unity, QuantityOrigin, Coefficient,
                            Quantity, QuantityOld, UnitPriceHT, VatRatio, UnitPriceTTC, FODECRatio, PrjWorkingOutId, PrjMarketLineId)
                        VALUES (?, ?, ?, ?, 'U', ?, 1, ?, ?, ?, ?, ?, 0, ?, ?)""",
                id.toString(), ordre, String.valueOf(ordre), "Ligne " + ordre, new BigDecimal(quantite),
                new BigDecimal(quantite), new BigDecimal(quantiteAnterieure), ht, taux, ht.add(ht.multiply(taux)),
                decompte.toString(), ligneMarche.toString());
        return id;
    }

    /** Travail supplémentaire ({@code PrjWorkingOutLineExtra}), FODEC 0. */
    public void extra(UUID decompte, String quantite, String puHT, String tva) {
        BigDecimal ht = new BigDecimal(puHT);
        BigDecimal taux = new BigDecimal(tva);
        flush();
        jdbc.update("""
                        INSERT INTO PrjWorkingOutLineExtra (Pkey, Sorting, Description, Quantity, UnitPriceHT, VatRatio,
                            UnitPriceTTC, FODECRatio, PrjWorkingOutId) VALUES (?, 1, 'Extra', ?, ?, ?, ?, 0, ?)""",
                UUID.randomUUID().toString(), new BigDecimal(quantite), ht, taux, ht.add(ht.multiply(taux)),
                decompte.toString());
    }

    /** {@code UPDATE table SET c1 = v1, … WHERE PKey = id} ; valeurs typées par JDBC (BigDecimal, Integer, Boolean, String). */
    public void poser(String table, UUID id, Object... colonneValeur) {
        List<String> affectations = new ArrayList<>();
        List<Object> valeurs = new ArrayList<>();
        for (int i = 0; i < colonneValeur.length; i += 2) {
            affectations.add(colonneValeur[i] + " = ?");
            valeurs.add(colonneValeur[i + 1]);
        }
        valeurs.add(id.toString());
        flush();
        jdbc.update("UPDATE " + table + " SET " + String.join(", ", affectations) + " WHERE PKey = ?", valeurs.toArray());
    }

    /** Colonnes d'un décompte relues en base, après écriture des changements en attente du contexte JPA. */
    public Map<String, Object> lire(UUID decompte, String... colonnes) {
        flush();
        return jdbc.queryForMap("SELECT " + String.join(", ", colonnes) + " FROM PrjWorkingOut WHERE PKey = ?",
                decompte.toString());
    }

    public List<Map<String, Object>> lignes(UUID decompte) {
        flush();
        return jdbc.queryForList("""
                SELECT CAST(PrjMarketLineId AS varchar(36)) AS marche, Quantity, QuantityOld FROM PrjWorkingOutLine
                WHERE PrjWorkingOutId = ? ORDER BY Sorting""", decompte.toString());
    }

    /** Écrit les changements en attente et vide le cache : l'appel HTTP suivant relit la base. */
    public void flush() {
        em.flush();
        em.clear();
    }
}
