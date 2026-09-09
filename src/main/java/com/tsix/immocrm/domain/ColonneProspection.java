package com.tsix.immocrm.domain;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Catalogue des colonnes sélectionnables de {@code GET /api/prospections}.
 *
 * <p>C'est la <strong>liste blanche</strong> du paramètre {@code ?colonnes=} et, exposée par
 * {@code GET /api/referentiels/colonnes/prospections}, la source du sélecteur de colonnes côté
 * front — qui n'a donc aucune liste à coder en dur.
 *
 * <p>Sans JPA, comme le reste de {@code domain/}.
 *
 * <h2>Ce que la sélection apporte, et ce qu'elle n'apporte pas</h2>
 *
 * <p>Elle réduit la <strong>charge utile</strong>, pas le temps de requête. Mesuré sur la base
 * réelle : charger les colonnes d'activité d'une page de 20 lignes coûte <strong>7 ms</strong>,
 * qu'on en demande 4 ou 20, parce que la vue est interrogée par lot d'identifiants
 * ({@code WHERE PKey IN (…)}) et jamais parcourue. Le filtrage est donc appliqué à la
 * sérialisation, et non traduit en {@code SELECT} dynamique : cela évite d'assembler du SQL à
 * partir d'une entrée client pour un gain nul.
 *
 * <p>En revanche, demander <em>zéro</em> colonne d'activité évite entièrement la requête de vue —
 * c'est ce que fait le comportement par défaut, qui reste identique à l'existant.
 */
public enum ColonneProspection {

    // -- Identité. Colonnes historiques, servies depuis ComProspection.
    ID("id", "Identifiant", Groupe.IDENTITE, TypeColonne.TEXTE, true, Source.ENTITE),
    CODE("code", "Code", Groupe.IDENTITE, TypeColonne.TEXTE, true, Source.ENTITE),
    DATE_DOCUMENT("dateDocument", "Date", Groupe.IDENTITE, TypeColonne.DATE, true, Source.ENTITE),
    TEMP_DESCRIPTION("tempDescription", "Description saisie", Groupe.IDENTITE, TypeColonne.TEXTE, true, Source.ENTITE),
    PROSPECT_ID("prospectId", "Id prospect", Groupe.IDENTITE, TypeColonne.TEXTE, true, Source.ENTITE),
    PROSPECT_NOM("prospectNom", "Prospect", Groupe.IDENTITE, TypeColonne.TEXTE, true, Source.ENTITE),
    COMMERCIAL_ID("commercialId", "Id commercial", Groupe.IDENTITE, TypeColonne.TEXTE, true, Source.ENTITE),
    COMMERCIAL_NOM("commercialNom", "Commercial", Groupe.IDENTITE, TypeColonne.TEXTE, true, Source.ENTITE),
    ORIGINE("origine", "Origine", Groupe.IDENTITE, TypeColonne.TEXTE, true, Source.ENTITE),
    ETAPE("etape", "Étape", Groupe.IDENTITE, TypeColonne.TEXTE, true, Source.ENTITE),
    FERMEE("fermee", "Fermée", Groupe.IDENTITE, TypeColonne.BOOLEEN, true, Source.ENTITE),

    // -- Contact et relance.
    TELEPHONE("telephone", "Téléphone", Groupe.CONTACT, TypeColonne.TEXTE, true, Source.ENTITE),
    EMAIL("email", "Email", Groupe.CONTACT, TypeColonne.TEXTE, true, Source.ENTITE),
    DERNIERE_ALERTE_DATE("derniereAlerteDate", "Relance prévue", Groupe.CONTACT, TypeColonne.DATE, true, Source.ENTITE),
    DERNIERE_ALERTE_MESSAGE("derniereAlerteMessage", "Note de relance", Groupe.CONTACT, TypeColonne.TEXTE, true, Source.ENTITE),

    // -- Contexte et financier.
    TRANCHE_ID("trancheId", "Id tranche", Groupe.CONTEXTE, TypeColonne.TEXTE, true, Source.ENTITE),
    TRANCHE_CODE("trancheCode", "Tranche", Groupe.CONTEXTE, TypeColonne.TEXTE, true, Source.ENTITE),
    TRANCHE_DESCRIPTION("trancheDescription", "Libellé tranche", Groupe.CONTEXTE, TypeColonne.TEXTE, true, Source.ENTITE),
    PRIX("prix", "Budget", Groupe.FINANCIER, TypeColonne.MONTANT, true, Source.ENTITE),
    SURFACE("surface", "Surface", Groupe.FINANCIER, TypeColonne.NOMBRE, true, Source.ENTITE),

    // -- Activité, calculée par la base légataire. Hors sélection par défaut : ajouter un
    //    de ces champs déclenche la requête de vue, absente sinon.
    JOURS_INACTIVITE("joursInactivite", "Jours d'inactivité", Groupe.ACTIVITE, TypeColonne.NOMBRE, false, Source.VUE),
    NB_INTERACTIONS("nbInteractions", "Interactions", Groupe.ACTIVITE, TypeColonne.NOMBRE, false, Source.VUE),
    DERNIERE_ACTION_DATE("derniereActionDate", "Dernière action", Groupe.ACTIVITE, TypeColonne.DATE, false, Source.VUE),
    DERNIERE_COMMUNICATION_DATE("derniereCommunicationDate", "Dernier échange", Groupe.ACTIVITE, TypeColonne.DATE, false, Source.VUE),
    DERNIERE_COMMUNICATION_RESULTAT("derniereCommunicationResultat", "Résultat du dernier échange", Groupe.ACTIVITE, TypeColonne.TEXTE, false, Source.VUE),
    NB_VISITES("nbVisites", "Visites", Groupe.ACTIVITE, TypeColonne.NOMBRE, false, Source.VUE),
    DERNIERE_VISITE_DATE("derniereVisiteDate", "Dernière visite", Groupe.ACTIVITE, TypeColonne.DATE, false, Source.VUE),

    PROJET_ID("projetId", "Id projet", Groupe.CONTEXTE, TypeColonne.TEXTE, false, Source.VUE),
    PROJET_CODE("projetCode", "Projet", Groupe.CONTEXTE, TypeColonne.TEXTE, false, Source.VUE),
    PROJET_LIBELLE("projetLibelle", "Libellé projet", Groupe.CONTEXTE, TypeColonne.TEXTE, false, Source.VUE),
    CIN("cin", "CIN", Groupe.IDENTITE, TypeColonne.TEXTE, false, Source.VUE),

    // -- Conversion. Renseignées sur 2 % des lignes — c'est le taux de conversion réel.
    DOSSIER_ID("dossierId", "Id dossier", Groupe.CONVERSION, TypeColonne.TEXTE, false, Source.VUE),
    DOSSIER_CODE("dossierCode", "Dossier", Groupe.CONVERSION, TypeColonne.TEXTE, false, Source.VUE),
    STATUT_DOSSIER("statutDossier", "Statut dossier", Groupe.CONVERSION, TypeColonne.TEXTE, false, Source.VUE);

    /** Regroupement d'affichage du sélecteur. */
    public enum Groupe { IDENTITE, CONTACT, CONTEXTE, FINANCIER, ACTIVITE, CONVERSION }

    /** Type d'affichage, pour que le front aligne et formate sans table de correspondance. */
    public enum TypeColonne { TEXTE, NOMBRE, MONTANT, DATE, BOOLEEN }

    /** D'où vient la valeur — décide si la requête de vue est nécessaire. */
    public enum Source { ENTITE, VUE }

    private static final Map<String, ColonneProspection> PAR_NOM = Arrays.stream(values())
            .collect(Collectors.toMap(c -> c.nom.toLowerCase(Locale.ROOT), Function.identity()));

    private final String nom;
    private final String libelle;
    private final Groupe groupe;
    private final TypeColonne type;
    private final boolean parDefaut;
    private final Source source;

    ColonneProspection(String nom, String libelle, Groupe groupe, TypeColonne type,
                       boolean parDefaut, Source source) {
        this.nom = nom;
        this.libelle = libelle;
        this.groupe = groupe;
        this.type = type;
        this.parDefaut = parDefaut;
        this.source = source;
    }

    public String nom() {
        return nom;
    }

    public String libelle() {
        return libelle;
    }

    public Groupe groupe() {
        return groupe;
    }

    public TypeColonne type() {
        return type;
    }

    public boolean parDefaut() {
        return parDefaut;
    }

    public Source source() {
        return source;
    }

    /**
     * Sélection appliquée quand {@code ?colonnes=} est absent : exactement les colonnes servies
     * avant l'introduction du paramètre. C'est ce qui rend le lot additif — une intégration qui
     * ignore {@code colonnes} ne voit aucun changement.
     */
    public static Set<ColonneProspection> parDefautSet() {
        return Arrays.stream(values())
                .filter(ColonneProspection::parDefaut)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Tous les noms valides, dans l'ordre de déclaration — pour le message d'erreur du 400. */
    public static List<String> nomsValides() {
        return Arrays.stream(values()).map(ColonneProspection::nom).toList();
    }

    /**
     * Résout une sélection reçue du client.
     *
     * <p>{@code id} est toujours ajouté, même s'il n'est pas demandé : sans lui la ligne n'est
     * pas cliquable et le front ne peut plus appeler la fiche.
     *
     * @throws ColonneInconnueException si un nom ne figure pas au catalogue — plutôt que de
     *         l'ignorer en silence, ce qui laisserait le front croire à une colonne vide.
     */
    public static Set<ColonneProspection> resoudre(List<String> demandees) {
        if (demandees == null || demandees.isEmpty()) {
            return parDefautSet();
        }
        Set<ColonneProspection> resolues = new LinkedHashSet<>();
        resolues.add(ID);
        for (String demandee : demandees) {
            if (demandee == null || demandee.isBlank()) {
                continue;
            }
            ColonneProspection c = PAR_NOM.get(demandee.trim().toLowerCase(Locale.ROOT));
            if (c == null) {
                throw new ColonneInconnueException(demandee.trim());
            }
            resolues.add(c);
        }
        return resolues;
    }

    /** Vrai si la sélection oblige à interroger {@code ComSaleProspectionView}. */
    public static boolean exigeLaVue(Set<ColonneProspection> selection) {
        return selection.stream().anyMatch(c -> c.source == Source.VUE);
    }

    /** Nom inconnu passé à {@code ?colonnes=} — traduit en 400 par le GlobalExceptionHandler. */
    public static class ColonneInconnueException extends RuntimeException {
        private final transient String colonne;

        public ColonneInconnueException(String colonne) {
            super("Colonne inconnue : " + colonne);
            this.colonne = colonne;
        }

        public String colonne() {
            return colonne;
        }
    }
}
