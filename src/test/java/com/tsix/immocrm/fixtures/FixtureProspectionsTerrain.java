package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.stk.StkKind;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Lignes du lot P7b (FEATURE-patrimoine-prospections-terrain.md §1.2, §6.1) en SQL natif : les tests ne dépendent pas des
 * entités que le lot crée. Configuration de production (entité LndLand, quatre statuts, cinq transitions, types de statut,
 * natures) semée si absente, à ses PKey de production ; jamais d'UUID aléatoire dans une FK. Routes et champs = contrat du fil.
 */
@Component
public class FixtureProspectionsTerrain {

    // ------------------------------------------------------------------ routes
    public static final String RACINE = "/api/prospections-terrain";
    public static final String FICHE = RACINE + "/{id}";
    public static final String ZONES = FICHE + "/zones";
    public static final String APERCU = RACINE + "/apercu-calcul";
    public static final String TRANSITIONS = FICHE + "/transitions";
    public static final String TRANSITION = FICHE + "/transition";
    public static final String HISTORIQUE = FICHE + "/historique";
    public static final String TRANSFORMER = FICHE + "/transformer-en-terrain";
    public static final String PIECES = FICHE + "/pieces-jointes";
    public static final String PORTEUR = "prospection-terrain";

    // ------------------------------------------------------------------ champs du fil (§5.2, §5.3, §5.4, §5.7)
    public static final String ID = "id";
    public static final String CODE = "code";
    public static final String DESCRIPTION = "description";
    public static final String DATE = "date";
    public static final String CATEGORIE_ID = "categorieId";
    public static final String TYPE_ID = "typeId";
    public static final String NIVEAU_ID = "niveauId";
    public static final String VOCATION_ID = "vocationId";
    public static final String PAYS_ID = "paysId";
    public static final String VILLE_ID = "villeId";
    public static final String COURTIER_ID = "courtierId";
    public static final String NOTAIRE_ID = "notaireId";
    public static final String ORIENTATION_ID = "orientationId";
    public static final String LIEN_GOOGLE = "lienGoogle";
    public static final String ADRESSE = "adresse";
    public static final String PROPRIETAIRES = "proprietaires";
    public static final String NOTES = "notes";
    public static final String ANOMALIES = "anomalies";
    public static final String EST_AMENAGE = "estAmenage";
    public static final String EST_ARCHIVEE = "estArchivee";
    public static final String DATE_VALIDITE_STATUT = "dateValiditeStatut";
    public static final String SUPERFICIE = "superficie";
    public static final String SUPERFICIE_EQUIPEMENTS = "superficieEquipements";
    public static final String COEF_EQUIPEMENTS = "coefEquipements";
    public static final String SUPERFICIE_NETTE = "superficieNette";
    public static final String COS = "cos";
    public static final String SUPERFICIE_COS = "superficieCos";
    public static final String CUF = "cuf";
    public static final String SUPERFICIE_CUF = "superficieCuf";
    public static final String COEF_VENDABLE = "coefVendable";
    public static final String SUPERFICIE_VENDABLE = "superficieVendable";
    public static final String COEF_COMMERCE = "coefCommerce";
    public static final String SUPERFICIE_COMMERCE = "superficieCommerce";
    public static final String COEF_BUREAU = "coefBureau";
    public static final String SUPERFICIE_BUREAU = "superficieBureau";
    public static final String SUPERFICIE_HABITAT = "superficieHabitat";
    public static final String COEF_PARKING = "coefParking";
    public static final String SUPERFICIE_PARKING = "superficieParking";
    public static final String ARRETE_ALIGNEMENT = "arreteAlignement";
    public static final String HAUTEUR_MAX = "hauteurMax";
    public static final String PRIX_M2 = "prixM2";
    public static final String PRIX = "prix";
    public static final String PRIX_PROPOSE = "prixPropose";
    public static final String NUMERO_ENREGISTREMENT = "numeroEnregistrement";
    public static final String LIEU_ENREGISTREMENT = "lieuEnregistrement";
    public static final String CPF_IMMATRICULATION = "cpfImmatriculation";
    public static final String DATE_ENREGISTREMENT = "dateEnregistrement";
    public static final String IDENTIFICATION_ZONE = "identificationZone";
    public static final String CONDITIONS_RACCORDEMENT = "conditionsRaccordement";
    public static final String ASSIETTE_FONCIERE = "assietteFonciere";
    public static final String LEVEE_TOPOGRAPHIQUE = "leveeTopographique";
    public static final String CHAMPS_A_VIDER = "champsAVider";
    public static final String CHAMP_MODIFIE = "champModifie";
    public static final String IDENTIFICATION = "identification";
    public static final String LIGNES = "lignes";
    public static final String TRANSITION_ID = "transitionId";
    public static final String COMMENTAIRE = "commentaire";
    public static final String DATE_UTILISATEUR = "dateUtilisateur";
    public static final String ENTITE_ID = "entiteId";

    // ------------------------------------------------------------------ messages exacts (§3.3, §4)
    public static final String MESSAGE_SUPERFICIE = "La valeur de la supérficie doit être strictement supérieure à 0 !";
    public static final String MESSAGE_ZONES =
            "la superficies des zones ne doit pas dépasser la superficie nette du terrain !";
    public static final String MESSAGE_COMMENTAIRE = "Veuillez saisir un commentaire obligatoirement !";
    public static final String MESSAGE_SECOND_UTILISATEUR =
            "Cette transition exige un second utilisateur : non prise en charge.";
    public static final String MESSAGE_LECTURE_SEULE = "Cette prospection est en lecture seule dans le statut";
    public static final String MESSAGE_SUPPRESSION = "Vous ne pouvez pas supprimer cette entité dans le statut";
    public static final String MESSAGE_GMAP = "Vous devez saisir un lien Gmap valide";
    public static final String MESSAGE_DEJA_TRANSFORMEE = "Cette prospection est déjà transformée en terrain";
    public static final String MESSAGE_PAS_TRANSFORMABLE = "n'autorise pas la transformation en terrain";

    // ------------------------------------------------------------------ PKey de production (§1.2)
    public static final UUID ENTITE_LND_LAND = UUID.fromString("4426BAB2-C40E-4E55-BB21-15FE45976BF5");
    public static final UUID TYPE_NOUVEAU = UUID.fromString("1004DFA9-D4C9-4C61-A3AF-44F174A76D53");
    public static final UUID TYPE_SUCCES = UUID.fromString("D572CE4B-A4DD-4F38-8998-359EAB5A9FCA");
    public static final UUID TYPE_ECHEC = UUID.fromString("22B7D73E-2A8F-4A74-839D-29F1DE329B97");
    public static final UUID TYPE_ANNULATION = UUID.fromString("A9E040DB-533F-4744-AE9A-1425793ADEA0");
    public static final UUID STATUT_EN_COURS = UUID.fromString("F9DD0C63-BFD3-4A36-A54A-91ABC5D138D1");
    public static final UUID STATUT_VALIDE = UUID.fromString("3D3C107C-F3F2-46AC-8ED8-A774D7831F16");
    public static final UUID STATUT_NON_ABOUTIE = UUID.fromString("0CCD8064-6469-4891-B600-90223BFE140F");
    public static final UUID STATUT_REJETE = UUID.fromString("828E63B7-3B98-4DD1-BDDE-558E5B56A0A8");
    public static final UUID T_EN_COURS_VALIDE = UUID.fromString("7A1AC800-BC16-4A96-A4C6-1C18E41FFEDE");
    public static final UUID T_EN_COURS_NON_ABOUTIE = UUID.fromString("1B9E1605-3291-4A4B-84CF-9211A8B8311F");
    public static final UUID T_NON_ABOUTIE_EN_COURS = UUID.fromString("2BB9ABCA-ED58-4267-831E-751D92F6B8D8");
    public static final UUID T_EN_COURS_REJETE = UUID.fromString("B3CFA5A0-30E6-457B-A28D-CF3ADE80983A");
    public static final UUID T_VALIDE_EN_COURS = UUID.fromString("32EE9525-2276-4485-B218-374290387E9E");
    public static final UUID NATURE_PROS_TER = UUID.fromString("4BDDEF6B-6732-449C-97C5-1FB318D6C444");
    public static final String EN_COURS = "En cours";
    public static final String VALIDE = "Validé";
    public static final String NON_ABOUTIE = "Non aboutie";
    public static final String REJETE = "Rejeté";
    public static final String COULEUR = "ffff8000";

    public static final LocalDateTime DATE_PROSPECTION = LocalDateTime.of(2026, 10, 1, 0, 0);

    private static final String P_ID = "id";
    private static final String P_CODE = "code";
    private static final String P_ENTITE = "entite";

    private final EntityManager entityManager;
    private final FixturePatrimoine patrimoine;
    private final FixtureTerrains terrains;
    private final FixtureArticleFiche fiche;

    public FixtureProspectionsTerrain(EntityManager entityManager, FixturePatrimoine patrimoine, FixtureTerrains terrains,
                                      FixtureArticleFiche fiche) {
        this.entityManager = entityManager;
        this.patrimoine = patrimoine;
        this.terrains = terrains;
        this.fiche = fiche;
    }

    /**
     * Références d'une prospection de test ; {@code jeton} suffixe tous les codes (yesRemsTest est partagée).
     * Catégorie de nature PROS TER, type de nature TER, ville du pays, niveau, vocation foncière, orientation, deux tiers.
     */
    public record Contexte(String jeton, AdmCountry pays, UUID ville, UUID categorie, UUID type, UUID niveau,
                           UUID vocation, UUID orientation, UUID courtier, UUID notaire) {
    }

    public Contexte contexte(String prefixe) {
        configuration();
        String jeton = FixtureTerrains.jeton(prefixe);
        StkKind prosTer = natureProsTer();
        StkKind ter = terrains.natureTer();
        AdmCountry pays = patrimoine.pays("PAYS-" + jeton);
        return new Contexte(jeton, pays, terrains.ville("VIL-" + jeton, pays.getId()),
                patrimoine.categorie("CPT-" + jeton, prosTer, false).getId(),
                patrimoine.type("TPT-" + jeton, ter, false).getId(),
                terrains.referenceFonciere("LndLevel", "NIV-" + jeton),
                terrains.referenceFonciere("LndVocation", "VOC-" + jeton),
                fiche.reference(FixtureArticleFiche.ORIENTATION, "ORI-" + jeton),
                terrains.fournisseur("CRT-" + jeton, null),
                terrains.fournisseur("NOT-" + jeton, null));
    }

    // ------------------------------------------------------------------ configuration de production

    /** Entité LndLand, types de statut, quatre statuts et cinq transitions de production, si absents. */
    public void configuration() {
        executer(entityManager.createNativeQuery("""
                        IF NOT EXISTS (SELECT 1 FROM StrEntity WHERE Code = N'LndLand')
                            INSERT INTO StrEntity (PKey, Code, Description) VALUES (:id, N'LndLand', N'Prospection terrain')""")
                .setParameter(P_ID, ENTITE_LND_LAND));
        typeStatut(TYPE_NOUVEAU, "Nouveau", 10);
        typeStatut(TYPE_SUCCES, "Términé avec succès", 20);
        typeStatut(TYPE_ECHEC, "Términé avec échec", 30);
        typeStatut(TYPE_ANNULATION, "Annulation", 40);
        statutDeProduction(STATUT_EN_COURS, EN_COURS, 100, TYPE_NOUVEAU);
        statutDeProduction(STATUT_VALIDE, VALIDE, 200, TYPE_SUCCES);
        statutDeProduction(STATUT_NON_ABOUTIE, NON_ABOUTIE, 300, TYPE_ECHEC);
        statutDeProduction(STATUT_REJETE, REJETE, 400, TYPE_ANNULATION);
        transitionDeProduction(T_EN_COURS_VALIDE, STATUT_EN_COURS, STATUT_VALIDE, 10, false);
        transitionDeProduction(T_EN_COURS_NON_ABOUTIE, STATUT_EN_COURS, STATUT_NON_ABOUTIE, 20, true);
        transitionDeProduction(T_NON_ABOUTIE_EN_COURS, STATUT_NON_ABOUTIE, STATUT_EN_COURS, 30, false);
        transitionDeProduction(T_EN_COURS_REJETE, STATUT_EN_COURS, STATUT_REJETE, 100, true);
        transitionDeProduction(T_VALIDE_EN_COURS, STATUT_VALIDE, STATUT_EN_COURS, 101, true);
    }

    public UUID entiteLndLand() {
        return premierGuid("SELECT TOP 1 PKey FROM StrEntity WHERE Code = N'LndLand' ORDER BY PKey ASC");
    }

    /** Nature PROS TER (StkKind.ProspectionLand) à sa PKey de production, créée si absente. */
    public StkKind natureProsTer() {
        executer(entityManager.createNativeQuery("""
                        IF NOT EXISTS (SELECT 1 FROM StkKind WHERE PKey = :id)
                            INSERT INTO StkKind (PKey, Code, Description) VALUES (:id, N'PROS TER', N'PROSPECTION TERRAIN')""")
                .setParameter(P_ID, NATURE_PROS_TER));
        return entityManager.find(StkKind.class, NATURE_PROS_TER);
    }

    /** {@code AdmParamsString.CountryFormule} posé ou remplacé (annulé avec la transaction du test). */
    public void formule(String valeur) {
        executer(entityManager.createNativeQuery("""
                        IF EXISTS (SELECT 1 FROM AdmParamsString WHERE Code = N'CountryFormule')
                            UPDATE AdmParamsString SET CodeValue = :valeur WHERE Code = N'CountryFormule'
                        ELSE
                            INSERT INTO AdmParamsString (Code, CodeValue, PKey) VALUES (N'CountryFormule', :valeur, NEWID())""")
                .setParameter("valeur", valeur));
    }

    /** {@code AdmParamsGuid.DefaultCountry} posé ou remplacé (annulé avec la transaction du test). */
    public void paysParDefaut(UUID paysId) {
        executer(entityManager.createNativeQuery("""
                        IF EXISTS (SELECT 1 FROM AdmParamsGuid WHERE Code = N'DefaultCountry')
                            UPDATE AdmParamsGuid SET CodeValue = :valeur WHERE Code = N'DefaultCountry'
                        ELSE
                            INSERT INTO AdmParamsGuid (Code, CodeValue, PKey) VALUES (N'DefaultCountry', :valeur, NEWID())""")
                .setParameter("valeur", paysId));
    }

    private void typeStatut(UUID id, String code, int ordre) {
        executer(entityManager.createNativeQuery("""
                        IF NOT EXISTS (SELECT 1 FROM StrStatusType WHERE PKey = :id)
                            INSERT INTO StrStatusType (PKey, Code, Description, Sorting) VALUES (:id, :code, :code, :ordre)""")
                .setParameter(P_ID, id).setParameter(P_CODE, code).setParameter("ordre", ordre));
    }

    private void statutDeProduction(UUID id, String code, int ordre, UUID type) {
        executer(entityManager.createNativeQuery("""
                        IF NOT EXISTS (SELECT 1 FROM StrStatus WHERE PKey = :id)
                            INSERT INTO StrStatus (PKey, Code, Description, Sorting, StrConfigEntityId, StrStatusTypeId, Delay,
                                IsReadOnly, CanDelete, BoolField001, ColorName)
                            VALUES (:id, :code, :code, :ordre, :entite, :type, 0, 0, 1, 0, N'ffff8000')""")
                .setParameter(P_ID, id).setParameter(P_CODE, code).setParameter("ordre", ordre)
                .setParameter(P_ENTITE, entiteLndLand()).setParameter("type", type));
    }

    private void transitionDeProduction(UUID id, UUID depuis, UUID vers, int ordre, boolean commentaire) {
        if (compter("SELECT COUNT(*) FROM StrWorkFlow WHERE PKey = :id", id) > 0) {
            return;
        }
        String libelle = libelleStatut(depuis) + "=>" + libelleStatut(vers);
        executer(entityManager.createNativeQuery("""
                        INSERT INTO StrWorkFlow (PKey, StatusFromId, StatusToId, Sorting, Code, Description, WithComment,
                            WithPassword, WithUserDate, WithAutomaticTransition, StrConfigEntityId)
                        VALUES (:id, :depuis, :vers, :ordre, :code, :code, :commentaire, 0, 0, 0, :entite)""")
                .setParameter(P_ID, id).setParameter("depuis", depuis).setParameter("vers", vers)
                .setParameter("ordre", ordre).setParameter(P_CODE, libelle).setParameter("commentaire", commentaire)
                .setParameter(P_ENTITE, entiteLndLand()));
    }

    private String libelleStatut(UUID id) {
        entityManager.flush();
        return (String) entityManager.createNativeQuery("SELECT Code FROM StrStatus WHERE PKey = :id")
                .setParameter(P_ID, id).getSingleResult();
    }

    // ------------------------------------------------------------------ statuts et transitions de test

    public record Statut(boolean lectureSeule, boolean suppressionPermise, boolean transformable, int duree) {
        public static Statut ordinaire() {
            return new Statut(false, true, false, 0);
        }
    }

    /** Statut de test de l'entité LndLand (Code = Description = {@code code}). */
    public UUID statut(String code, int ordre, UUID type, Statut s) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO StrStatus (PKey, Code, Description, Sorting, StrConfigEntityId, StrStatusTypeId, Delay,
                            IsReadOnly, CanDelete, BoolField001, ColorName)
                        VALUES (:id, :code, :code, :ordre, :entite, :type, :duree, :lecture, :suppression, :transformable,
                            N'ff00ff00')""")
                .setParameter(P_ID, id).setParameter(P_CODE, code).setParameter("ordre", ordre)
                .setParameter(P_ENTITE, entiteLndLand()).setParameter("type", type).setParameter("duree", s.duree())
                .setParameter("lecture", s.lectureSeule()).setParameter("suppression", s.suppressionPermise())
                .setParameter("transformable", s.transformable()));
        return id;
    }

    public record Options(boolean commentaire, boolean motDePasse, boolean dateUtilisateur) {
        public static Options aucune() {
            return new Options(false, false, false);
        }
    }

    /** Transition de test (Code = Description = {@code libelle}). */
    public UUID transition(UUID depuis, UUID vers, String libelle, Options o) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO StrWorkFlow (PKey, StatusFromId, StatusToId, Sorting, Code, Description, WithComment,
                            WithPassword, WithUserDate, WithAutomaticTransition, StrConfigEntityId)
                        VALUES (:id, :depuis, :vers, 500, :code, :code, :commentaire, :motDePasse, :dateUtilisateur, 0,
                            :entite)""")
                .setParameter(P_ID, id).setParameter("depuis", depuis).setParameter("vers", vers)
                .setParameter(P_CODE, libelle).setParameter("commentaire", o.commentaire())
                .setParameter("motDePasse", o.motDePasse()).setParameter("dateUtilisateur", o.dateUtilisateur())
                .setParameter(P_ENTITE, entiteLndLand()));
        return id;
    }

    /** Rôle OMRAN de test et intervenant « rôle » de la transition ({@code StrWorkFlowIntervener}). */
    public UUID intervenantRole(UUID transitionId, String code) {
        UUID role = UUID.randomUUID();
        executer(entityManager.createNativeQuery("INSERT INTO AdmRole (PKey, Code, Description) VALUES (:id, :code, :code)")
                .setParameter(P_ID, role).setParameter(P_CODE, code));
        UUID intervenant = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO StrIntervener (PKey, Code, Description, AdmRoleId, StrConfigEntityId)
                        VALUES (:id, :code, :code, :role, :entite)""")
                .setParameter(P_ID, intervenant).setParameter(P_CODE, code).setParameter("role", role)
                .setParameter(P_ENTITE, entiteLndLand()));
        executer(entityManager.createNativeQuery("""
                        INSERT INTO StrWorkFlowIntervener (PKey, StrIntervenerId, StrWorkFlowId, WithPassword)
                        VALUES (:id, :intervenant, :transition, 0)""")
                .setParameter(P_ID, UUID.randomUUID()).setParameter("intervenant", intervenant)
                .setParameter("transition", transitionId));
        return role;
    }

    // ------------------------------------------------------------------ prospections

    /**
     * Prospection complète, comme la ligne de production (§1.1) : 1 200 m², voies 100, net 1 100, COS 0,30 → 330, CUF 2,5
     * → 2 750, vendable 2 750, prix du m² 4 000 → 4 800 000, proposé 4 000 000 ; Aménagé, non archivée, au statut donné.
     */
    public UUID prospection(Contexte c, String code, UUID statutId) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO LndLand (PKey, Code, Description, DocDate, Notes, Anomalies, Adress, Area, Cos, Cuf,
                            AlinmentStop, MaxHeight, RegistryNumber, RegistryPlace, ImmatriculationCPF, LndCategoryId,
                            LndTypeId, LndVocationId, LndLevelId, IsInArchive, ProspectionUnitCost, ProspectionPrice,
                            ProspectionPriceProposed, Owner, AreaCos, AreaCuf, EquipementsCoef, EquipementsArea, AreaNet,
                            SalableArea, CoefSaleableArea, AdmCountryId, AdmCityId, GmapLink, CfgMiddlemanId, CfgLawyerId,
                            StkOrientationId, IsTransformedToLand, StrStatusId, StrEntityId, AreaEquipmentTransferred,
                            AreaRegularized, AreaCeded, IsFinished, CommerceCoef, CommerceArea, OfficeCoef, OfficeArea,
                            HabitatArea, UndergroundParkingArea, UndergroundParkingCoef, ZoneIdentification,
                            DealerConnectionConditions, LandBase, IsTopographicSurvey)
                        VALUES (:id, :code, :description, :date, N'Notes prospection', N'Anomalie voisinage',
                            N'12 route cotiere', 1200, 0.3, 2.5, 4, 30, N'RG-1', N'Casablanca', N'CPF-1', :categorie, :type,
                            :vocation, :niveau, 0, 4000, 4800000, 4000000, N'Proprietaire A et B', 330, 2750, 0.090909, 100,
                            1100, 2750, 1, :pays, :ville, N'https://www.google.com/maps/@33.5,-7.6', :courtier, :notaire,
                            :orientation, 0, :statut, :entite, 0, 0, 0, 1, 0, 0, 0, 0, 2750, 0, 0, N'', N'', N'', 0)""")
                .setParameter(P_ID, id).setParameter(P_CODE, code).setParameter("description", "Prospection " + code)
                .setParameter("date", DATE_PROSPECTION).setParameter("categorie", c.categorie())
                .setParameter("type", c.type()).setParameter("vocation", c.vocation()).setParameter("niveau", c.niveau())
                .setParameter("pays", c.pays().getId()).setParameter("ville", c.ville())
                .setParameter("courtier", c.courtier()).setParameter("notaire", c.notaire())
                .setParameter("orientation", c.orientation()).setParameter("statut", statutId)
                .setParameter(P_ENTITE, entiteLndLand()));
        return id;
    }

    public UUID prospection(Contexte c, String code) {
        return prospection(c, code, STATUT_EN_COURS);
    }

    /** {@code colonne} : un nom de colonne de LndLand écrit par le test, jamais une saisie. */
    public void definir(UUID prospectionId, String colonne, Object valeur) {
        executer(entityManager.createNativeQuery("UPDATE LndLand SET " + colonne + " = :valeur WHERE PKey = :id")
                .setParameter("valeur", valeur).setParameter(P_ID, prospectionId));
    }

    /** Zone (relation « Zones terrain brut ») de la prospection. */
    public UUID zone(UUID prospectionId, String identification, BigDecimal superficie) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO LndLandZone (PKey, Identification, Area, Cos, Cuf, AreaCos, AreaCuf, SalableArea,
                            CoefSaleableArea, CommerceCoef, CommerceArea, OfficeCoef, OfficeArea, HabitatArea,
                            UndergroundParkingArea, UndergroundParkingCoef, AlinmentStop, MaxHeight, ProspectionId, Notes)
                        VALUES (:id, :identification, :superficie, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, :prospection,
                            N'')""")
                .setParameter(P_ID, id).setParameter("identification", identification)
                .setParameter("superficie", superficie).setParameter("prospection", prospectionId));
        return id;
    }

    /** Ligne d'historique de statut de la prospection (format OMRAN, §1.2). */
    public void historique(UUID prospectionId, String description) {
        executer(entityManager.createNativeQuery("""
                        INSERT INTO StrHistory (PKey, DocDate, Description, Notes, ObjectId, StrStatusId, StrStatusDescription,
                            UserDate, StrConfigEntityId)
                        VALUES (:id, :date, :description, N'', :objet, :statut, N'En cours', :date, :entite)""")
                .setParameter(P_ID, UUID.randomUUID()).setParameter("date", DATE_PROSPECTION)
                .setParameter("description", description).setParameter("objet", prospectionId)
                .setParameter("statut", STATUT_EN_COURS).setParameter(P_ENTITE, entiteLndLand()));
    }

    public void pieceJointe(UUID prospectionId) {
        executer(entityManager.createNativeQuery("""
                        INSERT INTO AdmAttachedFile (PKey, Category, CategoryId, Description)
                        VALUES (:id, N'Yes.Lnd.Prospection', :categorie, N'Piece P7b')""")
                .setParameter(P_ID, UUID.randomUUID()).setParameter("categorie", prospectionId));
    }

    public void note(UUID prospectionId) {
        executer(entityManager.createNativeQuery(
                        "INSERT INTO AdmMessage (PKey, CategoryId, Description) VALUES (:id, :categorie, N'Message P7b')")
                .setParameter(P_ID, UUID.randomUUID()).setParameter("categorie", prospectionId));
    }

    /** Étude de faisabilité minimale liée à la prospection ({@code StkFeasibilityStudy.LndLandId}). */
    public void etudeLiee(UUID prospectionId, String code) {
        executer(entityManager.createNativeQuery(
                        "INSERT INTO StkFeasibilityStudy (PKey, Code, Description, LndLandId) VALUES (:id, :code, :code, :prospection)")
                .setParameter(P_ID, UUID.randomUUID()).setParameter(P_CODE, code).setParameter("prospection", prospectionId));
    }

    // ------------------------------------------------------------------ lecture

    public Tuple ligne(UUID id) {
        return terrains.ligne("SELECT * FROM LndLand WHERE PKey = :id", id);
    }

    public int prospectionsDeCode(String code) {
        entityManager.flush();
        Object n = entityManager.createNativeQuery("SELECT COUNT(*) FROM LndLand WHERE Code = :code")
                .setParameter(P_CODE, code).getSingleResult();
        return ((Number) n).intValue();
    }

    public int compter(String sql, UUID id) {
        return terrains.compter(sql, id);
    }

    public List<Tuple> lignes(String sql, UUID id) {
        return terrains.lignes(sql, id);
    }

    public void executer(String sql) {
        terrains.executer(sql);
    }

    private UUID premierGuid(String sql) {
        entityManager.flush();
        List<?> valeurs = entityManager.createNativeQuery(sql).getResultList();
        return valeurs.isEmpty() || valeurs.get(0) == null ? null : UUID.fromString(valeurs.get(0).toString());
    }

    /** Sans clear : l'écouteur d'audit relit les références des entités en attente à la validation. */
    private void executer(Query query) {
        entityManager.flush();
        query.executeUpdate();
    }
}
