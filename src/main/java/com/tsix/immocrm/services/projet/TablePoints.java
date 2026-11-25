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
            SELECT PrjMarketId, PointDate, PointAchivement, PointTime FROM PrjMarketPrevision
            WHERE PrjMarketId IN (:lots) AND PointDate IS NOT NULL AND PointAchivement IS NOT NULL ORDER BY PointDate ASC""",
            """
            INSERT INTO PrjMarketPrevision (PKey, PrjMarketId, PointDate, PointAchivement, PointTime, PointPayment, PointAmount)
            SELECT NEWID(), :copie, PointDate, PointAchivement, PointTime, PointPayment, PointAmount FROM PrjMarketPrevision
            WHERE PrjMarketId = :lot"""),
    BASE("""
            SELECT PKey, PointDate, PointAchivement, PointTime, PointPayment, PointAmount FROM PrjMarketUserPrevision
            WHERE PrjMarketId = :lot ORDER BY PointDate ASC""",
            "DELETE FROM PrjMarketUserPrevision WHERE PrjMarketId = :lot",
            """
            INSERT INTO PrjMarketUserPrevision (PKey, PrjMarketId, PointDate, PointAchivement, PointTime, PointPayment, PointAmount)
            VALUES (:id, :lot, :date, :avancement, :temps, :paiement, :montant)""",
            """
            SELECT PrjMarketId, PointDate, PointAchivement, PointTime FROM PrjMarketUserPrevision
            WHERE PrjMarketId IN (:lots) AND PointDate IS NOT NULL AND PointAchivement IS NOT NULL ORDER BY PointDate ASC""",
            """
            INSERT INTO PrjMarketUserPrevision (PKey, PrjMarketId, PointDate, PointAchivement, PointTime, PointPayment, PointAmount)
            SELECT NEWID(), :copie, PointDate, PointAchivement, PointTime, PointPayment, PointAmount FROM PrjMarketUserPrevision
            WHERE PrjMarketId = :lot""");

    final String lire;
    final String effacer;
    final String inserer;
    final String lireSimulation;
    final String copier;

    TablePoints(String lire, String effacer, String inserer, String lireSimulation, String copier) {
        this.lire = lire;
        this.effacer = effacer;
        this.inserer = inserer;
        this.lireSimulation = lireSimulation;
        this.copier = copier;
    }
}
