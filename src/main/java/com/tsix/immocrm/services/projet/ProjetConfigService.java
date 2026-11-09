package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.dto.projet.ProjetConfigDto;
import com.tsix.immocrm.dto.projet.ProjetConfigRequest;
import com.tsix.immocrm.dto.projet.ReferentielsProjetConfigDto;
import com.tsix.immocrm.dto.projet.StatutLotDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.prj.PrjProject;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.prj.PrjProjectRepository;
import com.tsix.immocrm.services.patrimoine.ReferentielAdminService;
import com.tsix.immocrm.services.patrimoine.SuppressionProjet;
import com.tsix.immocrm.services.patrimoine.TracePatrimoine;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.obligatoire;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.ou;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.positif;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.sansBlancsAuxBords;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.texte;

/** Configuration Projet (FEATURE-projet-config.md) : écran d'administration, tous les projets hors périmètre. */
@Service
@Transactional(readOnly = true)
public class ProjetConfigService {

    private static final String ID = "id";
    private static final String ENTITE_PROJET = "PrjProject";
    private static final String STATUT_INITIAL = "Nouveau";
    private static final Pattern ANNEE_SOCIETE = Pattern.compile("^\\d{4}-");
    private static final String STATUTS_DU_PROJET = """
            SELECT s FROM StrStatus s WHERE s.strConfigEntityId IN (SELECT e.id FROM StrEntity e WHERE e.code = :entite)
            ORDER BY s.sorting""";
    private static final String COMPTEURS = """
            SELECT CONVERT(varchar(36), p.PKey), (SELECT COUNT(*) FROM PrjMarket m WHERE m.PrjProjectId = p.PKey),
                (SELECT COUNT(*) FROM BuyDocument d WHERE d.PrjProjectId = p.PKey)
            FROM PrjProject p""";
    private static final String COMPTEURS_DU_PROJET = COMPTEURS + " WHERE p.PKey = :id";
    // Les 3 genres retenus (réponse 3) : Administrative et Dealers ont aussi des statuts en prod, sans aucun lot.
    private static final String STATUTS_LOT = """
            SELECT CONVERT(varchar(36), s.PKey), s.Code, s.Description, s.Sorting, s.ColorName,
                CONVERT(varchar(36), e.PKey), e.Code, e.Description, t.Description
            FROM StrStatus s JOIN StrEntity e ON e.PKey = s.StrConfigEntityId
                LEFT JOIN StrStatusType t ON t.PKey = s.StrStatusTypeId
            WHERE e.Code IN ('PrjMarket', 'PrjMarketMultiSupplier', 'PrjMarketStudies')
            ORDER BY e.Code, s.Sorting""";

    /** Une ligne qui référence le projet bloque sa suppression ; habilitations exceptées (§8.4, réponse 4). */
    private record Reference(String sql, String libelle) {
    }

    private static final List<Reference> REFERENCES = List.of(
            new Reference("SELECT COUNT(*) FROM PrjMarket WHERE PrjProjectId = :id", "lot(s)"),
            new Reference("SELECT COUNT(*) FROM BuyDocument WHERE PrjProjectId = :id", "pièce(s) d'achat"),
            new Reference("SELECT COUNT(*) FROM CfgTranche WHERE PrjProjectId = :id", "tranche(s)"),
            new Reference("SELECT COUNT(*) FROM AdmTask WHERE PrjProjectId = :id", "tâche(s)"),
            new Reference("SELECT COUNT(*) FROM BuyLoan WHERE PrjProjectId = :id", "emprunt(s)"),
            new Reference("SELECT COUNT(*) FROM BuySelection WHERE PrjProjectId = :id", "sélection(s) d'achat"),
            new Reference("SELECT COUNT(*) FROM GrhWorkedDay WHERE PrjProjectId = :id", "journée(s) travaillée(s)"),
            new Reference("SELECT COUNT(*) FROM PrjProjectCfgTrancheVentilation WHERE PrjProjectId = :id",
                    "ventilation(s) par tranche"),
            new Reference("SELECT COUNT(*) FROM BuyVentilationExpense WHERE PrjProjectId = :id", "ventilation(s) de dépense"),
            new Reference("SELECT COUNT(*) FROM BuyFolderPrjProjectVentilation WHERE PrjProjectId = :id",
                    "ventilation(s) de dossier d'achat"),
            new Reference("SELECT COUNT(*) FROM BulMeeting WHERE PrjProjectId = :id", "réunion(s)"),
            new Reference("SELECT COUNT(*) FROM PrjAchivementVersion WHERE PrjProjectId = :id", "version(s) d'avancement"),
            new Reference("SELECT COUNT(*) FROM PrjCoefficientCost WHERE PrjProjectId = :id", "coefficient(s) de coût"),
            new Reference("SELECT COUNT(*) FROM PrjGanttTask WHERE PrjProjectId = :id", "tâche(s) de planning"),
            new Reference("SELECT COUNT(*) FROM PrjPrevision WHERE PrjProjectId = :id", "prévision(s)"),
            new Reference("SELECT COUNT(*) FROM PrjPrevisionVersion WHERE PrjProjectId = :id", "version(s) de prévision"),
            new Reference("SELECT COUNT(*) FROM PrjProjectAchievementPrevision WHERE PrjProjectId = :id",
                    "avancement(s) prévu(s)"),
            new Reference("SELECT COUNT(*) FROM PrjProjectAchievementReal WHERE PrjProjectId = :id", "avancement(s) réel(s)"),
            new Reference("SELECT COUNT(*) FROM PrjProjectBuildingPrevision WHERE PrjProjectId = :id",
                    "prévision(s) de construction"),
            new Reference("SELECT COUNT(*) FROM PrjProjectProgressCriteriaLine WHERE PrjProjectId = :id",
                    "critère(s) d'avancement"),
            new Reference("SELECT COUNT(*) FROM PrjProject WHERE ReferenceId = :id", "projet(s) qui le prennent pour référence"),
            // En production, CfgTrancheId de cette table pointe PrjProject.
            new Reference("SELECT COUNT(*) FROM CfgTrancheLevelingDefinition WHERE CfgTrancheId = :id",
                    "définition(s) de nivellement"));

    private final PrjProjectRepository projets;
    private final CfgCompanyRepository societes;
    private final CfgTierRepository tiers;
    private final TracePatrimoine trace;
    private final SuppressionProjet suppression;

    public ProjetConfigService(PrjProjectRepository projets, CfgCompanyRepository societes, CfgTierRepository tiers,
                               TracePatrimoine trace, SuppressionProjet suppression) {
        this.projets = projets;
        this.societes = societes;
        this.tiers = tiers;
        this.trace = trace;
        this.suppression = suppression;
    }

    public List<ProjetConfigDto> lister() {
        Map<UUID, long[]> compteurs = compteurs(em().createNativeQuery(COMPTEURS));
        return projets.tousParCode().stream()
                .map(p -> toDto(p, compteurs.getOrDefault(p.getId(), new long[2])))
                .toList();
    }

    public ProjetConfigDto lire(UUID id) {
        return toDto(charger(id), compteursDe(id));
    }

    @Transactional
    public ProjetConfigDto creer(ProjetConfigRequest saisie) {
        PrjProject p = new PrjProject();
        p.setId(UUID.randomUUID());
        p.setIsVentilated(false);
        p.setDefaultVentilationRate(BigDecimal.ZERO);
        p.setIsACommon(false);
        appliquer(p, saisie);
        List<StrStatus> statuts = statutsDuProjet();
        p.setStrEntity(entiteProjet());
        p.setStrStatus(statuts.stream().filter(s -> STATUT_INITIAL.equals(s.getCode())).findFirst()
                .orElseThrow(() -> new ConflictException("Paramétrage : statut « Nouveau » des projets introuvable.")));
        TracePatrimoine.Trace t = trace.maintenant();
        p.setUserCreate(t.auteur());
        p.setUserCreateDateTime(t.le());
        p.setUserUpdate(t.auteur());
        p.setUserUpdateDateTime(t.le());
        em().persist(p);
        trace.habiliterCreateur(p);
        return toDto(p, new long[2]);
    }

    @Transactional
    public ProjetConfigDto modifier(UUID id, ProjetConfigRequest saisie) {
        PrjProject p = charger(id);
        appliquer(p, saisie);
        TracePatrimoine.Trace t = trace.maintenant();
        p.setUserUpdate(t.auteur());
        p.setUserUpdateDateTime(t.le());
        return toDto(p, compteursDe(id));
    }

    /** 409 dès qu'une ligne référence le projet ; sinon ses habilitations partent avec lui, jamais par une cascade. */
    @Transactional
    public void supprimer(UUID id) {
        PrjProject p = charger(id);
        List<String> bloquants = new ArrayList<>();
        REFERENCES.forEach(r -> suppression.ajouterSiPresent(bloquants, r.sql(), true, id, r.libelle()));
        if (!bloquants.isEmpty()) {
            throw new ConflictException("Utilisé par " + String.join(", ", bloquants) + ".");
        }
        suppression.supprimerTout("SELECT h FROM AdmUserHabilitation h WHERE h.prjProjectId = :id", id);
        em().remove(p);
        suppression.flushOuConflit("le projet");
    }

    public ReferentielsProjetConfigDto referentiels() {
        return new ReferentielsProjetConfigDto(
                societes.findAll(Sort.by("code")).stream()
                        .map(c -> new ReferentielItemDto(c.getId(), c.getCode(), c.getDescription(), null)).toList(),
                statutsDuProjet().stream()
                        .map(s -> new ReferentielItemDto(s.getId(), s.getCode(), s.getDescription(), s.getSorting())).toList(),
                tiers.findEmployes().stream()
                        .map(e -> new ReferentielItemDto(e.getId(), e.getCode(), e.getDescription(), null)).toList());
    }

    public List<StatutLotDto> statutsLot() {
        List<?> lignes = em().createNativeQuery(STATUTS_LOT).getResultList();
        return lignes.stream().map(Object[].class::cast).map(r -> new StatutLotDto(
                UUID.fromString((String) r[0]), (String) r[1], (String) r[2], r[3] == null ? null : ((Number) r[3]).intValue(),
                (String) r[4], UUID.fromString((String) r[5]), (String) r[6], (String) r[7], (String) r[8])).toList();
    }

    /** PUT : remplacement complet, code, libellé et société obligatoires ; booléens et taux absents = inchangés. */
    private void appliquer(PrjProject p, ProjetConfigRequest s) {
        // Avant tout setter : les requêtes du contrôle flushent l'entité.
        p.setCode(codeLibre(obligatoire(sansBlancsAuxBords(s.code()), null, "Le code"), p.getId()));
        p.setDescription(obligatoire(sansBlancsAuxBords(s.description()), null, "La description"));
        p.setDescriptionLong(texte(s.descriptionLongue(), null, 500, "La description longue"));
        p.setAddress(texte(s.adresse(), null, 500, "L'adresse"));
        p.setNotes(texte(s.notes(), null, 1000, "Les notes"));
        p.setCfgCompany(societe(s.societeId()));
        p.setCfgTierManager(s.gestionnaireId() == null ? null : tiers.findEmploye(s.gestionnaireId())
                .orElseThrow(() -> new InvalidRequestException("gestionnaireId : aucun membre du personnel avec cet id.")));
        p.setIsVentilated(ou(s.ventile(), p.getIsVentilated()));
        p.setDefaultVentilationRate(ou(positif(s.tauxVentilationDefaut(), "Le taux de ventilation"),
                p.getDefaultVentilationRate()));
        p.setIsACommon(ou(s.partiesCommunes(), p.getIsACommon()));
    }

    private CfgCompany societe(UUID id) {
        if (id == null) {
            throw new InvalidRequestException("La société est obligatoire.");
        }
        return societes.findById(id).orElseThrow(() -> new InvalidRequestException("societeId : société inconnue."));
    }

    /** Verrou puis recherche : deux créations simultanées du même code ne passent pas toutes les deux. */
    private String codeLibre(String code, UUID id) {
        ReferentielAdminService.verrouiller(em(), PrjProject.class, code, "Projet");
        projets.findFirstByCode(code).filter(autre -> !autre.getId().equals(id)).ifPresent(autre -> {
            throw new ConflictException("Code déjà utilisé par le projet « %s ».".formatted(autre.getDescription()));
        });
        return code;
    }

    private List<StrStatus> statutsDuProjet() {
        return em().createQuery(STATUTS_DU_PROJET, StrStatus.class).setParameter("entite", ENTITE_PROJET).getResultList();
    }

    private StrEntity entiteProjet() {
        return em().createQuery("SELECT e FROM StrEntity e WHERE e.code = :code", StrEntity.class)
                .setParameter("code", ENTITE_PROJET).getResultStream().findFirst()
                .orElseThrow(() -> new ConflictException("Paramétrage : entité « PrjProject » introuvable."));
    }

    private PrjProject charger(UUID id) {
        return projets.findById(id).orElseThrow(() -> new ResourceNotFoundException("Projet introuvable: " + id));
    }

    private long[] compteursDe(UUID id) {
        return compteurs(em().createNativeQuery(COMPTEURS_DU_PROJET).setParameter(ID, id))
                .getOrDefault(id, new long[2]);
    }

    private static Map<UUID, long[]> compteurs(Query requete) {
        List<?> lignes = requete.getResultList();
        return lignes.stream().map(Object[].class::cast).collect(Collectors.toMap(r -> UUID.fromString((String) r[0]),
                r -> new long[]{((Number) r[1]).longValue(), ((Number) r[2]).longValue()}));
    }

    private EntityManager em() {
        return suppression.em();
    }

    private static ProjetConfigDto toDto(PrjProject p, long[] compteurs) {
        CfgCompany societe = p.getCfgCompany();
        StrEntity entite = p.getStrEntity();
        StrStatus statut = p.getStrStatus();
        CfgTier gestionnaire = p.getCfgTierManager();
        return new ProjetConfigDto(p.getId(), p.getCode(), p.getDescription(), p.getDescriptionLong(), p.getAddress(),
                p.getNotes(), Champs.de(societe, CfgCompany::getId), Champs.de(societe, CfgCompany::getCode),
                Champs.de(entite, StrEntity::getId), Champs.de(entite, StrEntity::getCode),
                Champs.de(statut, StrStatus::getId), Champs.de(statut, StrStatus::getCode),
                Champs.de(gestionnaire, CfgTier::getId), Champs.de(gestionnaire, CfgTier::getDescription),
                Boolean.TRUE.equals(p.getIsVentilated()), p.getDefaultVentilationRate(), Boolean.TRUE.equals(p.getIsACommon()),
                p.getCode() != null && ANNEE_SOCIETE.matcher(p.getCode()).find(), compteurs[0], compteurs[1],
                p.getUserCreateDateTime(), p.getUserCreate(), p.getUserUpdateDateTime(), p.getUserUpdate());
    }
}
