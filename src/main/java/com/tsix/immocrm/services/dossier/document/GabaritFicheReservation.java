package com.tsix.immocrm.services.dossier.document;

import java.util.Map;

/**
 * Emplacement des valeurs sur le fond de chaque marque, <b>mesuré dans l'image de fond</b> : le
 * {@code .yes} fourni est une révision antérieure. Origine en haut à gauche, en cm, comme le rapport.
 */
final class GabaritFicheReservation {

    /** 1 cm en points PostScript. */
    static final float CM = 28.3465f;

    /** Largeur et hauteur d'une page A4, en centimètres — les deux fonds font exactement ça. */
    static final float PAGE_L = 21.0f;
    static final float PAGE_H = 29.7f;

    enum Alignement { GAUCHE, CENTRE, DROITE }

    /** {@code hauteurCm} : le texte est centré verticalement dans la boîte, quelle que soit la police. */
    record Zone(float xCm, float yCm, float largeurCm, float hauteurCm, int taille, Alignement alignement) {

        Zone(float xCm, float yCm, float largeurCm, float hauteurCm, int taille) {
            this(xCm, yCm, largeurCm, hauteurCm, taille, Alignement.GAUCHE);
        }
    }

    /** Clés des valeurs, communes aux deux marques. */
    static final String DOSSIER = "dossier";
    static final String NOM = "nom";
    static final String ADRESSE = "adresse";
    static final String CIN = "cin";
    static final String MODE_PAIEMENT = "modePaiement";
    static final String MONTANT = "montant";
    static final String REGLE_PAR = "reglePar";
    static final String PRIX = "prix";
    static final String ARTICLE = "article";
    static final String IMMEUBLE = "immeuble";
    static final String PORTE = "porte";
    static final String ETAGE = "etage";
    static final String SUPERFICIE = "superficie";
    static final String DATE = "date";

    // -- Colonne des cases de valeur, mesurée sur les deux fonds : filets à 6,37 et 19,37 cm.
    private static final float BOITE_X = 6.5f;
    private static final float BOITE_L = 12.7f;

    // -- Colonne APF, filets relevés à 6,22 et 19,23 cm : elle ne coïncide pas avec celle d'ARS.
    private static final float APF_X = 6.35f;
    private static final float APF_L = 12.75f;

    /** ARS (GHRIZA), fond violet : les {@code y} des sept cases sont les filets relevés dans {@code fond-ARS.jpg}. */
    static final Map<String, Zone> ARS = Map.ofEntries(
            // Boîte « N°V » : filets à 3,35 et 4,25 cm, blanc utile de 2,98 à 6,69 cm.
            Map.entry(DOSSIER, new Zone(3.15f, 3.35f, 3.35f, 0.90f, 9)),
            Map.entry(NOM, new Zone(BOITE_X, 7.66f, BOITE_L, 0.58f, 10)),
            Map.entry(ADRESSE, new Zone(BOITE_X, 8.39f, BOITE_L, 1.09f, 10)),
            Map.entry(CIN, new Zone(BOITE_X, 9.64f, BOITE_L, 0.56f, 10)),
            Map.entry(MODE_PAIEMENT, new Zone(BOITE_X, 10.37f, BOITE_L, 0.58f, 10)),
            Map.entry(MONTANT, new Zone(BOITE_X, 11.11f, BOITE_L - 0.15f, 0.57f, 10, Alignement.DROITE)),
            Map.entry(REGLE_PAR, new Zone(BOITE_X, 11.85f, BOITE_L, 0.59f, 10)),
            Map.entry(PRIX, new Zone(BOITE_X, 12.62f, BOITE_L - 0.15f, 0.58f, 10, Alignement.DROITE)),
            // Valeurs du paragraphe « A titre de réservation… » : x = blancs laissés par le fond entre
            // deux mots (absence d'encre), pas les positions du template.
            Map.entry(ARTICLE, new Zone(17.09f, 13.70f, 2.28f, 0.4f, 9, Alignement.CENTRE)),
            Map.entry(IMMEUBLE, new Zone(2.70f, 14.28f, 2.08f, 0.4f, 9, Alignement.CENTRE)),
            Map.entry(PORTE, new Zone(6.01f, 14.28f, 1.45f, 0.4f, 9, Alignement.CENTRE)),
            Map.entry(ETAGE, new Zone(7.95f, 14.28f, 2.96f, 0.4f, 9, Alignement.CENTRE)),
            Map.entry(SUPERFICIE, new Zone(2.57f, 14.69f, 2.11f, 0.6f, 9, Alignement.CENTRE)),
            // Ligne « Fait le … », relevée à 22,00 cm.
            Map.entry(DATE, new Zone(6.8f, 21.70f, 3.8f, 0.6f, 12, Alignement.CENTRE))
    );

    /**
     * APF (ADMOD), fond orange : mêmes sept cases qu'ARS, plus basses (paragraphe de société plus long).
     * Le template annonçait six cases ; le fond fait foi.
     */
    static final Map<String, Zone> APF = Map.ofEntries(
            Map.entry(DOSSIER, new Zone(3.05f, 3.75f, 3.35f, 0.90f, 10)),
            Map.entry(NOM, new Zone(APF_X, 8.70f, APF_L, 0.57f, 10)),
            Map.entry(ADRESSE, new Zone(APF_X, 9.43f, APF_L, 1.10f, 10)),
            Map.entry(CIN, new Zone(APF_X, 10.67f, APF_L, 0.57f, 10)),
            Map.entry(MODE_PAIEMENT, new Zone(APF_X, 11.41f, APF_L, 0.58f, 10)),
            Map.entry(MONTANT, new Zone(APF_X, 12.15f, APF_L - 0.15f, 0.57f, 10, Alignement.DROITE)),
            Map.entry(REGLE_PAR, new Zone(APF_X, 12.90f, APF_L, 0.58f, 10)),
            Map.entry(PRIX, new Zone(APF_X, 13.66f, APF_L - 0.15f, 0.58f, 10, Alignement.DROITE)),
            // Lignes du paragraphe relevées à 14,94 / 15,52 / 16,05 cm ; x = blancs du fond.
            Map.entry(ARTICLE, new Zone(16.96f, 14.74f, 2.27f, 0.4f, 10, Alignement.CENTRE)),
            Map.entry(IMMEUBLE, new Zone(2.57f, 15.32f, 2.07f, 0.4f, 10, Alignement.CENTRE)),
            Map.entry(PORTE, new Zone(5.88f, 15.32f, 1.44f, 0.4f, 10, Alignement.CENTRE)),
            Map.entry(ETAGE, new Zone(7.81f, 15.32f, 2.97f, 0.4f, 10, Alignement.CENTRE)),
            Map.entry(SUPERFICIE, new Zone(2.42f, 15.75f, 2.13f, 0.6f, 10, Alignement.CENTRE)),
            // Ligne « Fait le … », relevée à 24,58 cm — APF a deux lignes de plus qu'ARS.
            Map.entry(DATE, new Zone(6.2f, 24.25f, 3.8f, 0.6f, 12, Alignement.CENTRE))
    );

    /**
     * <b>Statique dans le template</b> : le légataire l'imprime sur toute fiche ARS, sans condition —
     * une affirmation commerciale à ne pas étendre à d'autres marques sans le savoir.
     */
    static final String MENTION_ARS = "Cette réservation inclut la place parking sous-sol";

    /** Entre « sans formalités. » (16,68 cm) et « CONDITION DE RESERVATION » (17,97 cm). */
    static final Zone ZONE_MENTION_ARS = new Zone(1.8f, 16.92f, 17.6f, 0.8f, 14);

    private GabaritFicheReservation() {
    }
}
