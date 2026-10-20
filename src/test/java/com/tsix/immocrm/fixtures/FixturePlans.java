package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkFloor;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

/**
 * Plans, étages, tracés et dossiers du lot P3 (FEATURE-patrimoine-plans.md). Plans et tracés en SQL natif : les tests
 * ne dépendent pas des entités que le lot crée. Parents écrits (flush) avant chaque ordre natif : la CI porte les FK.
 */
@Component
public class FixturePlans {

    public static final BigDecimal LARGEUR = new BigDecimal("1312.5979");
    public static final BigDecimal HAUTEUR = new BigDecimal("928.15833");
    public static final BigDecimal X = new BigDecimal("73.34");
    public static final BigDecimal Y = new BigDecimal("22.7");
    public static final String PREFIXE_PNG = "data:image/png;base64,";
    public static final String PREFIXE_JPEG = "data:image/jpeg;base64,";

    private static final String ID = "id";

    private final EntityManager entityManager;
    private final FixturePatrimoine patrimoine;
    private final FixtureDossierVente vente;
    private final FixtureReferentielStk referentielStk;
    private final StkItemRepository articles;
    private final ComFolderRepository dossiers;
    private final AdmUserHabilitationRepository habilitations;

    @SuppressWarnings("java:S107")
    public FixturePlans(EntityManager entityManager, FixturePatrimoine patrimoine, FixtureDossierVente vente,
                        FixtureReferentielStk referentielStk, StkItemRepository articles, ComFolderRepository dossiers,
                        AdmUserHabilitationRepository habilitations) {
        this.entityManager = entityManager;
        this.patrimoine = patrimoine;
        this.vente = vente;
        this.referentielStk = referentielStk;
        this.articles = articles;
        this.dossiers = dossiers;
        this.habilitations = habilitations;
    }

    // ------------------------------------------------------------------ images

    public static byte[] png() {
        return image("png");
    }

    public static byte[] jpeg() {
        return image("jpg");
    }

    public static String dataUriPng() {
        return PREFIXE_PNG + Base64.getEncoder().encodeToString(png());
    }

    private static byte[] image(String format) {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        image.setRGB(1, 1, 0xFF00FF);
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, format, sortie);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return sortie.toByteArray();
    }

    // ------------------------------------------------------------------ patrimoine

    public CfgTranche tranche(String code, boolean entiteActive) {
        return vente.tranche(code, entiteActive);
    }

    public StkFloor etage(String code) {
        return patrimoine.etage(code);
    }

    /** Article de la tranche ; {@code etage} nul = sans étage. */
    public StkItem article(CfgTranche tranche, String code, boolean sousArticle, StkFloor etage) {
        StkKind nature = referentielStk.natureParDefaut();
        StkItemType type = patrimoine.type("TYP-" + code, nature, sousArticle);
        StkItemCategory categorie = patrimoine.categorie("CAT-" + code, nature, sousArticle);
        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode(code);
        article.setStkItemType(type);
        article.setStkItemCategory(categorie);
        article.setStkVocation(patrimoine.vocationVente());
        article.setStkKind(nature);
        article.setStkHierarchy(tranche);
        article.setStkFloor(etage);
        article.setIsUnderItem(sousArticle);
        return articles.save(article);
    }

    /**
     * Dossier de vente sur un article existant ; {@code couleur} = {@code StrStatus.ColorName} (nul = non renseignée).
     * Trigger {@code trgAfterComFolderInsert} : créer le dossier sorti (annulé, désisté) avant le dossier actif.
     */
    public ComFolder dossier(StkItem article, StatutDossier statut, String couleur, String libelleStatut) {
        StrStatus s = vente.statut(statut);
        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode("DOS-" + article.getCode() + "-" + UUID.randomUUID().toString().substring(0, 8));
        folder.setStkVocation(patrimoine.vocationVente());
        folder.setStrStatus(s);
        folder.setCfgCustomer(vente.tier("CLI-" + article.getCode()));
        folder.setStkItem(article);
        ComFolder enregistre = dossiers.save(folder);
        // NULLIF : un paramètre natif nul non typé part en varbinary sous SQL Server.
        executer(entityManager.createNativeQuery(
                        "UPDATE StrStatus SET ColorName = NULLIF(:couleur, N''), Description = :libelle WHERE PKey = :id")
                .setParameter("couleur", couleur == null ? "" : couleur).setParameter("libelle", libelleStatut)
                .setParameter(ID, s.getId()));
        return enregistre;
    }

    /** {@code StkItem.ComFolderId} brut, sans rapport garanti avec un dossier actif (piège 4). */
    public void comFolderId(UUID articleId, UUID dossierId) {
        executer(entityManager.createNativeQuery("UPDATE StkItem SET ComFolderId = :dossier WHERE PKey = :id")
                .setParameter("dossier", dossierId).setParameter(ID, articleId));
    }

    public void habiliter(AdmUser compte, CfgProject projet) {
        AdmUserHabilitation habilitation = new AdmUserHabilitation();
        habilitation.setId(UUID.randomUUID());
        habilitation.setAdmUser(compte);
        habilitation.setCfgProject(projet);
        habilitations.save(habilitation);
    }

    // ------------------------------------------------------------------ plans et tracés (SQL natif)

    /** Plan aux dimensions {@link #LARGEUR} × {@link #HAUTEUR}, origine ({@link #X}, {@link #Y}). */
    public UUID plan(UUID trancheId, UUID etageId, String code, String dataUri) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO StkHierarchyPlan (PKey, Code, Description, PlanImage, StkHierarchyId, StkFloorId,
                            UserCreate, UserCreateDateTime, width, height, X, Y)
                        VALUES (:id, :code, :code, :image, :tranche, :etage, N'Fixture plans', :creation,
                            :largeur, :hauteur, :x, :y)""")
                .setParameter(ID, id).setParameter("code", code).setParameter("image", dataUri)
                .setParameter("tranche", trancheId).setParameter("etage", etageId)
                .setParameter("creation", LocalDateTime.of(2026, 1, 15, 10, 0))
                .setParameter("largeur", LARGEUR).setParameter("hauteur", HAUTEUR)
                .setParameter("x", X).setParameter("y", Y));
        return id;
    }

    public void remplacerImage(UUID planId, String dataUri, LocalDateTime miseAJour) {
        executer(entityManager.createNativeQuery("""
                        UPDATE StkHierarchyPlan SET PlanImage = :image, UserUpdate = N'Fixture plans',
                            UserUpdateDateTime = :maj WHERE PKey = :id""")
                .setParameter("image", dataUri).setParameter("maj", miseAJour).setParameter(ID, planId));
    }

    public UUID trace(UUID articleId, UUID etageId, String d) {
        UUID id = UUID.randomUUID();
        executer(entityManager.createNativeQuery("""
                        INSERT INTO StkHierarchyPath (PKey, SvgPath, StkItemId, StkFloorId)
                        VALUES (:id, CAST(:d AS varchar(max)), :article, :etage)""")
                .setParameter(ID, id).setParameter("d", d).setParameter("article", articleId)
                .setParameter("etage", etageId));
        return id;
    }

    private void executer(Query query) {
        entityManager.flush();
        query.executeUpdate();
        entityManager.clear();
    }
}
