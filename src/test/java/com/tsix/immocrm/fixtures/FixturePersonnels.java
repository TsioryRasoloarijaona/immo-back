package com.tsix.immocrm.fixtures;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.fixtures.FixtureTiers.Jeu;
import com.tsix.immocrm.fixtures.FixtureTiers.Jeu.Ligne;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.PERSONNELS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Employés de FEATURE-admin-personnels.md : ligne {@code CfgTier} portant paie et pièces, compte et dossier liés, lecture de
 * toutes les colonnes, contrôle « pas de paie sur le fil ». Les méthodes qui posent une FK ou une table exigent un test
 * {@code @Transactional} (DDL annulé au rollback).
 */
@Component
public class FixturePersonnels {

    public static final String TRANSFORMER = "/api/administration/tiers/personnels/{id}/transformer-en-contact";

    public static final BigDecimal SALAIRE = new BigDecimal("98765.43");
    public static final String SECU = "SECU-99887766";
    public static final String RIB = "RIB-11223344";
    public static final String IBAN = "IBAN-55667788";
    public static final String PIECE = "PIECE-44332211";
    public static final String DATE_NAISSANCE = "1985-04-12";

    private static final LocalDateTime NAISSANCE = LocalDateTime.of(1985, 4, 12, 0, 0);
    private static final String DATE_EMBAUCHE = "2019-03-14";
    private static final LocalDateTime EMBAUCHE = LocalDateTime.of(2019, 3, 14, 0, 0);
    private static final String DATE_SORTIE = "2025-06-30";
    private static final LocalDateTime SORTIE = LocalDateTime.of(2025, 6, 30, 0, 0);

    /** Colonnes de paie : valeur non nulle posée par {@link #paieComplete}, jamais écrite ni lue par le lot. */
    public static final List<String> COLONNES_PAIE = List.of("SalaryBase", "SocialSecurityNumber", "BaseMonthNumber",
            "HireDate", "FireDate", "CfgCompanyId", "AdmSalaryId", "GrhDepartmentId", "GrhEmployeeCategoryId", "GrhEchelonId",
            "GrhAgreementTypeId", "GrhPaySlipModelId", "GrhEmployeeClassId", "GrhEmployeeTypeId");

    /** Noms de champs interdits ; {@code raisonSociale} et {@code formeSociale} restent permis. */
    private static final Pattern CLE_INTERDITE =
            Pattern.compile("(?i).*(salair|salary|secu|embauche|hiredate|firedate|datesortie|grh).*");

    private static final List<String> VALEURS_INTERDITES = List.of(SALAIRE.toPlainString(), SECU, DATE_EMBAUCHE, DATE_SORTIE);

    private final JdbcTemplate jdbc;
    private final EntityManager em;
    private final FixtureEtatVentes ventes;

    public FixturePersonnels(DataSource dataSource, EntityManager em, FixtureEtatVentes ventes) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.em = em;
        this.ventes = ventes;
    }

    public static MockHttpServletRequestBuilder transformer(UUID id, RequestPostProcessor qui) {
        return post(TRANSFORMER, id).with(qui);
    }

    /** Employé personne physique : salaire, sécurité sociale, embauche, sortie, pièce, RIB, IBAN, naissance. */
    public static Ligne employe(Jeu j, String suffixe) {
        return j.tiers(PERSONNELS, suffixe).avec("FirstName", "Amal").avec("LastName", "Employe " + j.jeton())
                .avec("Mobile", "0612345678").avec("SalaryBase", SALAIRE).avec("SocialSecurityNumber", SECU)
                .avec("BaseMonthNumber", new BigDecimal("13")).avec("HireDate", EMBAUCHE).avec("FireDate", SORTIE)
                .avec("Rib", RIB).avec("Iban", IBAN).avec("IDNumber", PIECE).avec("BirthDate", NAISSANCE);
    }

    /** {@link #employe} avec toutes les {@link #COLONNES_PAIE} ; les tables {@code Grh*} manquent à yesRemsTest. */
    public Ligne paieComplete(Jeu j, String suffixe) {
        Ligne ligne = employe(j, suffixe).avec("CfgCompanyId", j.parent("CfgCompany")).avec("AdmSalaryId", j.parent("AdmSalary"));
        Map.of("GrhDepartmentId", "GrhDepartment", "GrhEmployeeCategoryId", "GrhEmployeeCategory", "GrhEchelonId", "GrhEchelon",
                "GrhAgreementTypeId", "GrhAgreementType", "GrhPaySlipModelId", "GrhPaySlipModel",
                "GrhEmployeeClassId", "GrhEmployeeClass", "GrhEmployeeTypeId", "GrhEmployeeType")
                .forEach((colonne, table) -> {
                    jdbc.execute("""
                            IF OBJECT_ID('dbo.%1$s', 'U') IS NULL
                            CREATE TABLE dbo.%1$s (PKey uniqueidentifier NOT NULL PRIMARY KEY, Code nvarchar(255) NULL,
                                                   Description nvarchar(255) NULL)""".formatted(table));
                    ligne.avec(colonne, j.parent(table));
                });
        return ligne;
    }

    /** Toutes les colonnes de la ligne, écritures JPA du test d'abord. */
    public Map<String, Object> colonnes(UUID id) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            em.flush();
        }
        return jdbc.queryForMap("SELECT * FROM CfgTier WHERE PKey = ?", id.toString());
    }

    /** Compte {@code AdmUser} dont {@code CfgEmployeeId} pointe l'employé. */
    public UUID compteLie(Jeu j, UUID employe) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO AdmUser (PKey, Code, CfgEmployeeId) VALUES (?, ?, ?)", id.toString(),
                j.code("CPT" + id.toString().substring(0, 4)), employe.toString());
        return id;
    }

    public int comptesLies(UUID employe) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM AdmUser WHERE CfgEmployeeId = ?", Integer.class, employe.toString());
        return n == null ? 0 : n;
    }

    /** FK {@code AdmUser.CfgEmployeeId} de la CI, absente de yesRemsTest. */
    public void poserCleCompte() {
        jdbc.execute("""
                IF NOT EXISTS (SELECT 1 FROM sys.foreign_key_columns
                               WHERE parent_object_id = OBJECT_ID('dbo.AdmUser')
                                 AND referenced_object_id = OBJECT_ID('dbo.CfgTier')
                                 AND parent_column_id = COLUMNPROPERTY(OBJECT_ID('dbo.AdmUser'), 'CfgEmployeeId', 'ColumnId'))
                ALTER TABLE dbo.AdmUser WITH NOCHECK ADD CONSTRAINT FK_TEST_AdmUser_CfgEmployeeId
                    FOREIGN KEY (CfgEmployeeId) REFERENCES dbo.CfgTier (PKey)""");
    }

    /** État du dossier de la règle 7 : {@code SANS_STATUT} compte comme actif. */
    public enum EtatDossier { ACTIF, SORTI, SANS_STATUT }

    /** Dossier sans article (aucun trigger de dossier vivant) : seul porteur posé, {@code ComFolder.CfgCommercialId}. */
    public void dossierDuCommercial(Jeu j, UUID employe, EtatDossier etat) {
        String code = j.code(etat.name());
        String statut = switch (etat) {
            case ACTIF -> ventes.statutEnCours(code).getId().toString();
            case SORTI -> ventes.statutSortie(code, true).getId().toString();
            case SANS_STATUT -> null;
        };
        StkVocation vocation = ventes.vente();
        em.flush();
        jdbc.update("""
                INSERT INTO ComFolder (PKey, Code, StkVocationId, StrStatusId, CfgCommercialId)
                VALUES (?, ?, ?, ?, ?)""", UUID.randomUUID().toString(), code, vocation.getId().toString(), statut,
                employe.toString());
    }

    /** Règle 2 : ni champ de paie à quelque profondeur, ni valeur de paie dans le corps, réponses d'erreur comprises. */
    public static void assertSansPaie(String corps) {
        assertThat(corps).isNotBlank().doesNotContain(VALEURS_INTERDITES);
        assertThat(cles(JsonPath.parse(corps).json())).isNotEmpty().noneMatch(c -> CLE_INTERDITE.matcher(c).matches());
    }

    private static List<String> cles(Object noeud) {
        List<String> cles = new ArrayList<>();
        if (noeud instanceof Map<?, ?> objet) {
            objet.forEach((cle, valeur) -> {
                cles.add(String.valueOf(cle));
                cles.addAll(cles(valeur));
            });
        } else if (noeud instanceof Collection<?> liste) {
            liste.forEach(element -> cles.addAll(cles(element)));
        }
        return cles;
    }
}
