package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.faisabilite.CalculFaisabilite;
import com.tsix.immocrm.domain.faisabilite.ValeursFaisabilite;
import com.tsix.immocrm.dto.patrimoine.faisabilite.EtudeFaisabiliteDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.EtudeFaisabiliteListeDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.LigneTrancheEtudeDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.LigneUniteEtudeDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.SaisieEtudeDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudy;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudyStatus;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudyStatusHistory;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.stk.StkFeasibilityStudyRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.perimetre.VisibiliteEntites;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.COURT;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.obligatoire;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.positif;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.texte;

/**
 * Études de faisabilité (FEATURE-patrimoine-faisabilite.md §2) : le bloc A et les phases sont recalculés par
 * {@link CalculFaisabilite} à chaque écriture, Verif passe sur l'état résultant avant que rien ne soit écrit.
 */
@Service
@Transactional(readOnly = true)
public class EtudeFaisabiliteService {

    /** État « Nouveau » : le statut initial est le premier statut de cet état (FeasibilityStudyStatus.cs:431). */
    static final UUID ETAT_NOUVEAU = UUID.fromString("1004DFA9-D4C9-4C61-A3AF-44F174A76D53");
    static final UUID ENTITE_ETUDE = UUID.fromString("E7999F70-7507-40D9-AA78-A2825C42B900");
    static final String BLOQUEE = "Étude bloquée : débloquez-la avant toute modification.";
    private static final String SOCIETE_ID = "societeId";
    private static final Set<String> OBLIGATOIRES =Set.of("code", "description", "dateDocument", SOCIETE_ID);
    private static final String TERRAIN_ID = "terrainId";
    private static final String NATURE_TERRAIN = "TER";
    private static final BigDecimal TVA_DEFAUT = new BigDecimal("0.20");
    private static final BigDecimal TVA_INTERETS = new BigDecimal("0.10");
    private static final String ID = "id";
    private static final String PREVISIONS = "SELECT COUNT(*) FROM FSPrevision WHERE FeasibilityStudyId = :id";

    private final StkFeasibilityStudyRepository etudes;
    private final StkItemRepository articles;
    private final GrillesEtude grilles;
    private final TracePatrimoine trace;
    private final SuppressionProjet suppression;
    private final UtilisateurAffiche utilisateurAffiche;
    private final EntityManager em;

    public EtudeFaisabiliteService(StkFeasibilityStudyRepository etudes, StkItemRepository articles, GrillesEtude grilles,
                                   TracePatrimoine trace, SuppressionProjet suppression,
                                   UtilisateurAffiche utilisateurAffiche) {
        this.etudes = etudes;
        this.articles = articles;
        this.grilles = grilles;
        this.trace = trace;
        this.suppression = suppression;
        this.utilisateurAffiche = utilisateurAffiche;
        this.em = suppression.em();
    }

    // ------------------------------------------------------------------ lecture

    public List<EtudeFaisabiliteListeDto> lister(UUID societeId, UUID statutId, String search) {
        return etudes.lister(societeId, statutId, SaisiePatrimoine.motif(search)).stream().map(FicheEtude::ligne).toList();
    }

    public EtudeFaisabiliteDto fiche(UUID id) {
        return dto(etudes.exigerVisible(id));
    }

    /** Aperçu : même application des entrées et même calcul que l'écriture, sur une étude jamais attachée. */
    public ValeursFaisabilite calculerApercu(SaisieEtudeDto saisie) {
        StkFeasibilityStudy brouillon = new StkFeasibilityStudy();
        FicheEtude.appliquerEntrees(brouillon, saisie, ChampsAVider.de(saisie, List.of(), OBLIGATOIRES));
        ValeursFaisabilite v = CalculFaisabilite.calculer(FicheEtude.valeurs(brouillon), maroc());
        v.erreurs().addAll(grilles.erreursSaisies(v, saisie.unites(), saisie.tranches()));
        return v;
    }

    public List<ReferentielItemDto> terrains() {
        return em.createQuery("SELECT i FROM StkItem i LEFT JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr "
                        + "LEFT JOIN pr.cfgCompany soc WHERE i.stkKind.code = :nature AND " + VisibiliteEntites.CLAUSE_JPQL
                        + " ORDER BY i.code", StkItem.class)
                .setParameter("nature", NATURE_TERRAIN).getResultList().stream()
                .map(i -> new ReferentielItemDto(i.getId(), i.getCode(), i.getDescription(), null)).toList();
    }

    public List<ReferentielItemDto> tranches(UUID societeId) {
        if (societeId == null) {
            return List.of();
        }
        return grilles.tranchesDeLaSociete(societeId).stream()
                .map(t -> new ReferentielItemDto(t.getId(), t.getCode(), t.getDescription(), null)).toList();
    }

    // ------------------------------------------------------------------ écriture

    @Transactional
    public EtudeFaisabiliteDto creerEtude(SaisieEtudeDto saisie) {
        StkFeasibilityStudy e = new StkFeasibilityStudy();
        e.setId(UUID.randomUUID());
        if (saisie.dateDocument() == null) {
            throw new InvalidRequestException("La date du document est obligatoire.");
        }
        if (saisie.societeId() == null) {
            throw new InvalidRequestException("La société est obligatoire.");
        }
        appliquer(e, saisie, ChampsAVider.de(saisie, List.of(), OBLIGATOIRES));
        e.setDefaultVat(Objects.requireNonNullElse(e.getDefaultVat(), TVA_DEFAUT));
        e.setInterestVatRatio(Objects.requireNonNullElse(e.getInterestVatRatio(), TVA_INTERETS));
        StkFeasibilityStudyStatus initial = statutInitial();
        e.setStatus(initial);
        e.setStrEntityId(ENTITE_ETUDE);
        e.setIsLocked(false);
        tracer(e, true);
        em.persist(e);
        em.persist(historique(e, initial));
        return dto(e);
    }

    /** Verif sur les grilles enregistrées ; l'étude est détachée jusqu'au contrôle pour qu'un refus n'écrive rien. */
    @Transactional
    public EtudeFaisabiliteDto modifierEtude(UUID id, SaisieEtudeDto saisie) {
        StkFeasibilityStudy e = modifiable(id);
        // §2.4 : les tranches d'étude pointent des tranches de la société ; en changer les laisserait orphelines.
        if (saisie.societeId() != null && !saisie.societeId().equals(Champs.de(e.getCfgCompany(), CfgCompany::getId))
                && grilles.tranches(id).stream().anyMatch(t -> t.getCfgTranche() != null)) {
            throw new InvalidRequestException("societeId : l'étude a des tranches liées au patrimoine de sa société ; "
                    + "retirez-les avant de changer de société.");
        }
        em.detach(e);
        appliquer(e, saisie, ChampsAVider.de(saisie, saisie.champsAVider(), OBLIGATOIRES));
        GrillesEtude.exigerSansErreur(grilles.erreursEnBase(id, FicheEtude.valeurs(e)));
        tracer(e, false);
        return dto(em.merge(e));
    }

    /** 409 si bloquée ou porteuse de prévisions / financements (§8 bis.3) ; sinon ses lignes partent avec elle. */
    @Transactional
    public void supprimerEtude(UUID id) {
        StkFeasibilityStudy e = modifiable(id);
        long previsions = compter(PREVISIONS, id);
        long financements = compter("SELECT COUNT(*) FROM FSFunding WHERE FeasibilityStudyId = :id", id);
        if (previsions + financements > 0) {
            throw new ConflictException("Suppression impossible : l'étude porte encore %d prévision(s) et %d financement(s)."
                    .formatted(previsions, financements));
        }
        grilles.supprimerTout(id);
        em.remove(e);
        suppression.flushOuConflit("l'étude");
    }

    /** Bloquer = enregistrer (Verif, 409 si déjà bloquée) ; Débloquer passe toujours. */
    @Transactional
    public EtudeFaisabiliteDto modifierBlocage(UUID id, boolean bloquer) {
        StkFeasibilityStudy e = bloquer ? modifiable(id) : etudes.exigerVisible(id);
        if (bloquer) {
            GrillesEtude.exigerSansErreur(grilles.erreursEnBase(id, FicheEtude.valeurs(e)));
        }
        e.setIsLocked(bloquer);
        tracer(e, false);
        return dto(e);
    }

    @Transactional
    public List<LigneUniteEtudeDto> modifierUnites(UUID id, List<LigneUniteEtudeDto> lignes) {
        StkFeasibilityStudy e = modifiable(id);
        grilles.remplacerUnites(e, Objects.requireNonNullElse(lignes, List.of()), FicheEtude.valeurs(e));
        tracer(e, false);
        em.flush();
        return grilles.unitesDto(id);
    }

    @Transactional
    public List<LigneTrancheEtudeDto> modifierTranches(UUID id, List<LigneTrancheEtudeDto> lignes) {
        StkFeasibilityStudy e = modifiable(id);
        grilles.remplacerTranches(e, Objects.requireNonNullElse(lignes, List.of()), FicheEtude.valeurs(e));
        tracer(e, false);
        em.flush();
        return grilles.tranchesDto(id);
    }

    // ------------------------------------------------------------------ commun

    private StkFeasibilityStudy modifiable(UUID id) {
        StkFeasibilityStudy e = etudes.exigerVisible(id);
        if (Boolean.TRUE.equals(e.getIsLocked())) {
            throw new ConflictException(BLOQUEE);
        }
        return e;
    }

    /** Identité, références et entrées, puis le calcul partagé ; 400 / 409 avant toute écriture. */
    private void appliquer(StkFeasibilityStudy e, SaisieEtudeDto d, ChampsAVider v) {
        e.setCode(ActionnairesService.codeLibre(obligatoire(d.code(), e.getCode(), "Le code"), e.getId(),
                etudes::findFirstByCode, "l'étude"));
        e.setDescription(obligatoire(d.description(), e.getDescription(), "La description"));
        e.setDocDate(SaisiePatrimoine.ou(d.dateDocument(), e.getDocDate()));
        if (d.societeId() != null) {
            e.setCfgCompany(ReferencesPatrimoine.trouver(this::societeActive, d.societeId(), SOCIETE_ID));
        }
        e.setStkItem(v.reference(TERRAIN_ID, d.terrainId(), e.getStkItem(),
                i -> ReferencesPatrimoine.trouver(this::terrain, i, TERRAIN_ID)));
        e.setLandDescription(texte(v.texte("descriptionTerrain", d.descriptionTerrain()), e.getLandDescription(), COURT,
                "La description du terrain"));
        e.setNumberOfItemForSale(v.ou("nombreArticlesAVendre", positif(d.nombreArticlesAVendre(), "nombreArticlesAVendre"),
                e.getNumberOfItemForSale()));
        e.setUseLandProportionForTvaCalculation(v.ou("proportionTerrainPourTva", d.proportionTerrainPourTva(),
                e.getUseLandProportionForTvaCalculation()));
        e.setDefaultVat(v.ou("tauxTvaDefaut", d.tauxTvaDefaut(), e.getDefaultVat()));
        e.setInterestVatRatio(v.ou("tauxTvaInterets", d.tauxTvaInterets(), e.getInterestVatRatio()));
        e.setEcartTypeExpenses(v.ou("ecartTypeTravaux", d.ecartTypeTravaux(), e.getEcartTypeExpenses()));
        e.setEcartTypeConcretisation(v.ou("ecartTypeConcretisation", d.ecartTypeConcretisation(),
                e.getEcartTypeConcretisation()));
        e.setEcartTypeFinalization(v.ou("ecartTypeFinalisation", d.ecartTypeFinalisation(), e.getEcartTypeFinalization()));
        e.setBackTimeOnDays(v.ou("delaiRetourJours", d.delaiRetourJours(), e.getBackTimeOnDays()));
        e.setBackGuaranteeRate(v.ou("tauxRetenueGarantie", d.tauxRetenueGarantie(), e.getBackGuaranteeRate()));
        e.setBackGuaranteeApplicationRate(v.ou("tauxApplicationRetenueGarantie", d.tauxApplicationRetenueGarantie(),
                e.getBackGuaranteeApplicationRate()));
        FicheEtude.appliquerEntrees(e, d, v);
        FicheEtude.ecrire(e, CalculFaisabilite.calculer(FicheEtude.valeurs(e), maroc()));
    }

    private Optional<CfgCompany> societeActive(UUID id) {
        return Optional.ofNullable(em.find(CfgCompany.class, id)).filter(c -> Boolean.TRUE.equals(c.getIsActive()));
    }

    private Optional<StkItem> terrain(UUID id) {
        return articles.findVisible(id)
                .filter(i -> NATURE_TERRAIN.equalsIgnoreCase(Champs.de(i.getStkKind(), StkKind::getCode)));
    }

    /** {@code AdmParamsString.CountryFormule}, lu à chaque calcul ; absent = Maroc. */
    private boolean maroc() {
        List<?> valeurs = em.createNativeQuery("SELECT CodeValue FROM AdmParamsString WHERE Code = 'CountryFormule'")
                .getResultList();
        return valeurs.isEmpty() || valeurs.get(0) == null || "MA".equalsIgnoreCase(valeurs.get(0).toString().trim());
    }

    private StkFeasibilityStudyStatus statutInitial() {
        return em.createQuery("SELECT s FROM StkFeasibilityStudyStatus s WHERE s.admWorkFlowStateTypeId = :etat "
                        + "ORDER BY s.sorting", StkFeasibilityStudyStatus.class)
                .setParameter("etat", ETAT_NOUVEAU).setMaxResults(1).getResultStream().findFirst()
                .orElseThrow(() -> new ConflictException("Aucun statut d'étude à l'état « Nouveau » n'est paramétré."));
    }

    private StkFeasibilityStudyStatusHistory historique(StkFeasibilityStudy e, StkFeasibilityStudyStatus statut) {
        StkFeasibilityStudyStatusHistory h = new StkFeasibilityStudyStatusHistory();
        h.setId(UUID.randomUUID());
        h.setFeasibilityStudyId(e.getId());
        h.setStatusId(statut.getId());
        h.setDocDate(e.getUserCreateDateTime());
        h.setAdmUserId(Champs.de(utilisateurAffiche.courant().compte(), AdmUser::getId));
        return h;
    }

    private long compter(String sql, UUID id) {
        return ((Number) em.createNativeQuery(sql).setParameter(ID, id).getSingleResult()).longValue();
    }

    private void tracer(StkFeasibilityStudy e, boolean creation) {
        TracePatrimoine.Trace t = trace.maintenant();
        if (creation) {
            e.setUserCreate(t.auteur());
            e.setUserCreateDateTime(t.le());
        }
        e.setUserUpdate(t.auteur());
        e.setUserUpdateDateTime(t.le());
    }

    private EtudeFaisabiliteDto dto(StkFeasibilityStudy e) {
        StkFeasibilityStudyStatus s = e.getStatus();
        return new EtudeFaisabiliteDto(e.getId(), e.getCode(), e.getDescription(), e.getDocDate(),
                Champs.de(e.getCfgCompany(), CfgCompany::getId), Champs.de(e.getCfgCompany(), CfgCompany::getDescription),
                Champs.de(e.getStkItem(), StkItem::getId), Champs.de(e.getStkItem(), StkItem::getDescription),
                e.getLndLandId(), e.getLandDescription(), Champs.de(s, StkFeasibilityStudyStatus::getId),
                Champs.de(s, StkFeasibilityStudyStatus::getDescription), Champs.de(s, StkFeasibilityStudyStatus::getColorName),
                Boolean.TRUE.equals(e.getIsLocked()), e.getNumberOfItemForSale(), e.getUseLandProportionForTvaCalculation(),
                e.getDefaultVat(), e.getInterestVatRatio(), e.getEcartTypeExpenses(), e.getEcartTypeConcretisation(),
                e.getEcartTypeFinalization(), e.getBackTimeOnDays(), e.getBackGuaranteeRate(),
                e.getBackGuaranteeApplicationRate(), FicheEtude.valeurs(e), FicheEtude.bilan(e),
                grilles.unitesDto(e.getId()), grilles.tranchesDto(e.getId()),
                compter(PREVISIONS, e.getId()) > 0,
                e.getUserCreate(), e.getUserCreateDateTime(), e.getUserUpdate(), e.getUserUpdateDateTime());
    }
}
