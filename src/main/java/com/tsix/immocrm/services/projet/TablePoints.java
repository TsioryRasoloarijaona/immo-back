package com.tsix.immocrm.services.projet;

/** Requêtes natives des deux tables de points de prévision, écrites en entier (pas de SQL assemblé). */
enum TablePoints {
    PREVISION("""
            SELECT PKey, PointDate, PointAchivement, PointTime, PointPayment, PointAmount FROM PrjMarketPrevision
            WHERE PrjMarketId = :lot ORDER BY PointDate ASC""",
            "DELETE FROM PrjMarketPrevision WHERE PrjMarketId = :lot",
            """
            INSERT INTO PrjMarketPrevision (PKey, PrjMarketId, PointDate, PointAchivement, PointTime, PointPayment, PointAmount)
            VALUES (:id, :lot, :date, :avancement, :temps, :paiement, :montant)""",
            """
            SELECT PointDate, PointAchivement, PointTime FROM PrjMarketPrevision
            WHERE PrjMarketId = :lot AND PointDate IS NOT NULL AND PointAchivement IS NOT NULL ORDER BY PointDate ASC"""),
    BASE("""
            SELECT PKey, PointDate, PointAchivement, PointTime, PointPayment, PointAmount FROM PrjMarketUserPrevision
            WHERE PrjMarketId = :lot ORDER BY PointDate ASC""",
            "DELETE FROM PrjMarketUserPrevision WHERE PrjMarketId = :lot",
            """
            INSERT INTO PrjMarketUserPrevision (PKey, PrjMarketId, PointDate, PointAchivement, PointTime, PointPayment, PointAmount)
            VALUES (:id, :lot, :date, :avancement, :temps, :paiement, :montant)""",
            """
            SELECT PointDate, PointAchivement, PointTime FROM PrjMarketUserPrevision
            WHERE PrjMarketId = :lot AND PointDate IS NOT NULL AND PointAchivement IS NOT NULL ORDER BY PointDate ASC""");

    final String lire;
    final String effacer;
    final String inserer;
    final String lireSimulation;

    TablePoints(String lire, String effacer, String inserer, String lireSimulation) {
        this.lire = lire;
        this.effacer = effacer;
        this.inserer = inserer;
        this.lireSimulation = lireSimulation;
    }
}
