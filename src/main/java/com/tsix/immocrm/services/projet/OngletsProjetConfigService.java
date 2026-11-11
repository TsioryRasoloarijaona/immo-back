package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.dto.projet.ChoixProjetDto;
import com.tsix.immocrm.dto.projet.CritereAvancementDto;
import com.tsix.immocrm.dto.projet.CritereAvancementEcritureDto;
import com.tsix.immocrm.dto.projet.ImportCriteresDto;
import com.tsix.immocrm.dto.projet.VentilationTrancheDto;
import com.tsix.immocrm.dto.projet.VentilationTrancheEcritureDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.entities.prj.PrjProject;
import com.tsix.immocrm.entities.prj.PrjProjectCfgTrancheVentilation;
import com.tsix.immocrm.entities.prj.PrjProjectProgressCriteriaLine;
import com.tsix.immocrm.entities.stl.StlCategory;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.prj.PrjProjectRepository;
import com.tsix.immocrm.services.patrimoine.TracePatrimoine;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.obligatoire;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.positif;

/** Onglets ventilation par tranches et critères d'avancement de la fiche projet (FEATURE-projets-config-parite.md). */
@Service
@Transactional(readOnly = true)
public class OngletsProjetConfigService {

    private static final String PROJET = "projet";
    private static final String VENTILATION_INCORRECTE = "La ventilation de votre document est incorrecte !";
    private static final String CITE_PAR_PV = """
            SELECT COUNT(*) FROM BulMeetingPrjProjectProgressCriteriaLine WHERE PrjProjectProgressCriteriaLineId IN (:ids)""";

    private final PrjProjectRepository projets;
    private final TracePatrimoine trace;
    private final EntityManager em;

    public OngletsProjetConfigService(PrjProjectRepository projets, TracePatrimoine trace, EntityManager em) {
        this.projets = projets;
        this.trace = trace;
        this.em = em;
    }

    public ChoixProjetDto choix(UUID id) {
        PrjProject p = charger(id);
        UUID societe = p.getCfgCompany() == null ? null : p.getCfgCompany().getId();
        return new ChoixProjetDto(
                em.createQuery("SELECT t FROM CfgTranche t WHERE t.cfgProject.cfgCompany.id = :s ORDER BY t.code", CfgTranche.class)
                        .setParameter("s", societe).getResultStream()
                        .map(t -> new ReferentielItemDto(t.getId(), t.getCode(), t.getDescription(), null)).toList(),
                em.createQuery("SELECT m FROM PrjMarket m WHERE m.prjProject.id = :p ORDER BY m.code", PrjMarket.class)
                        .setParameter("p", id).getResultStream()
                        .map(m -> new ReferentielItemDto(m.getId(), m.getCode(), m.getDescription(), null)).toList());
    }

    /** Catégorie de dépense ({@code CategoryType.DEBIT} d'OMRAN) : tout ce qui n'est pas de sens crédit. */
    static boolean estDepense(StlCategory c) {
        return !Boolean.TRUE.equals(c.getIsCredit());
    }

    // ------------------------------------------------------------------ ventilation par tranches

    public List<VentilationTrancheDto> ventilations(UUID id) {
        return lignesVentilation(charger(id).getId()).stream()
                .map(v -> ventilationDto(v, lu(CfgTranche.class, v.getCfgTrancheId())))
                .sorted(Comparator.comparing(VentilationTrancheDto::trancheCode, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    @Transactional
    public VentilationTrancheDto ajouterVentilation(UUID id, VentilationTrancheEcritureDto saisie) {
        PrjProject p = charger(id);
        CfgTranche tranche = verifierVentilation(p, null, saisie);
        PrjProjectCfgTrancheVentilation v = new PrjProjectCfgTrancheVentilation();
        v.setId(UUID.randomUUID());
        v.setPrjProjectId(p.getId());
        v.setCfgTrancheId(tranche.getId());
        v.setVentilationRate(saisie.taux());
        em.persist(v);
        return ventilationDto(v, tranche);
    }

    @Transactional
    public VentilationTrancheDto modifierVentilation(UUID id, UUID ligneId, VentilationTrancheEcritureDto saisie) {
        PrjProject p = charger(id);
        PrjProjectCfgTrancheVentilation v = ligne(PrjProjectCfgTrancheVentilation.class, ligneId, p,
                PrjProjectCfgTrancheVentilation::getPrjProjectId);
        CfgTranche tranche = verifierVentilation(p, ligneId, saisie);
        v.setCfgTrancheId(tranche.getId());
        v.setVentilationRate(saisie.taux());
        return ventilationDto(v, tranche);
    }

    @Transactional
    public void supprimerVentilation(UUID id, UUID ligneId) {
        PrjProject p = charger(id);
        em.remove(ligne(PrjProjectCfgTrancheVentilation.class, ligneId, p, PrjProjectCfgTrancheVentilation::getPrjProjectId));
    }

    /** Toutes les lectures avant le moindre setter ; {@code ligneId} = ligne modifiée, exclue des « autres ». */
    private CfgTranche verifierVentilation(PrjProject p, UUID ligneId, VentilationTrancheEcritureDto s) {
        if (s.trancheId() == null) {
            throw new InvalidRequestException("trancheId : la tranche est obligatoire.");
        }
        if (s.taux() == null) {
            throw new InvalidRequestException("taux : le taux est obligatoire.");
        }
        positif(s.taux(), "Le taux");
        CfgTranche tranche = em.find(CfgTranche.class, s.trancheId());
        if (tranche == null || !Objects.equals(societeDe(tranche), p.getCfgCompany() == null ? null : p.getCfgCompany().getId())) {
            throw new InvalidRequestException("trancheId : tranche inconnue ou hors de la société du projet.");
        }
        List<PrjProjectCfgTrancheVentilation> autres = lignesVentilation(p.getId()).stream()
                .filter(v -> !v.getId().equals(ligneId)).toList();
        if (autres.stream().anyMatch(v -> s.trancheId().equals(v.getCfgTrancheId()))) {
            throw new InvalidRequestException("trancheId : tranche déjà ventilée sur ce projet.");
        }
        BigDecimal somme = autres.stream().map(PrjProjectCfgTrancheVentilation::getVentilationRate).filter(Objects::nonNull)
                .reduce(s.taux(), BigDecimal::add);
        // Math.Round .NET (Project.cs:Verif) : arrondi bancaire.
        if (somme.setScale(2, RoundingMode.HALF_EVEN).compareTo(BigDecimal.ONE) > 0) {
            throw new InvalidRequestException(VENTILATION_INCORRECTE);
        }
        return tranche;
    }

    private static UUID societeDe(CfgTranche t) {
        if (t.getCfgProject() == null || t.getCfgProject().getCfgCompany() == null) {
            return null;
        }
        return t.getCfgProject().getCfgCompany().getId();
    }

    private List<PrjProjectCfgTrancheVentilation> lignesVentilation(UUID projet) {
        return em.createQuery("SELECT v FROM PrjProjectCfgTrancheVentilation v WHERE v.prjProjectId = :projet",
                PrjProjectCfgTrancheVentilation.class).setParameter(PROJET, projet).getResultList();
    }

    private static VentilationTrancheDto ventilationDto(PrjProjectCfgTrancheVentilation v, CfgTranche t) {
        return new VentilationTrancheDto(v.getId(), v.getCfgTrancheId(), t == null ? null : t.getCode(),
                t == null ? null : t.getDescription(), v.getVentilationRate());
    }

    // ------------------------------------------------------------------ critères d'avancement

    public List<CritereAvancementDto> criteres(UUID id) {
        return lignesCriteres(charger(id).getId()).stream()
                .map(c -> critereDto(c, lu(StlCategory.class, c.getStlCategoryId()))).toList();
    }

    @Transactional
    public CritereAvancementDto ajouterCritere(UUID id, CritereAvancementEcritureDto saisie) {
        PrjProject p = charger(id);
        String description = description(saisie);
        StlCategory categorie = categorie(saisie.categorieId());
        int ordre = saisie.ordre() != null ? saisie.ordre() : lignesCriteres(p.getId()).size() + 1;
        PrjProjectProgressCriteriaLine c = nouveauCritere(p.getId(), ordre, description, categorie.getId());
        return critereDto(c, categorie);
    }

    @Transactional
    public CritereAvancementDto modifierCritere(UUID id, UUID ligneId, CritereAvancementEcritureDto saisie) {
        PrjProject p = charger(id);
        PrjProjectProgressCriteriaLine c = ligne(PrjProjectProgressCriteriaLine.class, ligneId, p,
                PrjProjectProgressCriteriaLine::getPrjProjectId);
        String description = description(saisie);
        StlCategory categorie = categorie(saisie.categorieId());
        if (saisie.ordre() != null) {
            c.setSorting(saisie.ordre());
        }
        c.setDescription(description);
        c.setStlCategoryId(categorie.getId());
        TracePatrimoine.Trace t = trace.maintenant();
        c.setUserUpdate(t.auteur());
        c.setUserUpdateDateTime(t.le());
        return critereDto(c, categorie);
    }

    @Transactional
    public void supprimerCritere(UUID id, UUID ligneId) {
        PrjProject p = charger(id);
        PrjProjectProgressCriteriaLine c = ligne(PrjProjectProgressCriteriaLine.class, ligneId, p,
                PrjProjectProgressCriteriaLine::getPrjProjectId);
        refuserSiCite(List.of(c), "Critère cité par un PV de réunion : suppression impossible.");
        em.remove(c);
    }

    @Transactional
    public List<CritereAvancementDto> importerCriteres(UUID id, ImportCriteresDto saisie) {
        PrjProject p = charger(id);
        UUID source = saisie.projetSourceId();
        if (source == null || source.equals(p.getId()) || !projets.existsById(source)) {
            throw new InvalidRequestException("projetSourceId : projet source absent, inconnu ou égal au projet.");
        }
        List<PrjProjectProgressCriteriaLine> actuelles = lignesCriteres(p.getId());
        refuserSiCite(actuelles, "Un critère actuel du projet est cité par un PV de réunion : import impossible.");
        List<PrjProjectProgressCriteriaLine> modeles = lignesCriteres(source);
        actuelles.forEach(em::remove);
        modeles.forEach(m -> nouveauCritere(p.getId(), m.getSorting(), m.getDescription(), m.getStlCategoryId()));
        em.flush();
        return criteres(id);
    }

    private void refuserSiCite(Collection<PrjProjectProgressCriteriaLine> lignes, String message) {
        if (lignes.isEmpty()) {
            return;
        }
        Number n = (Number) em.createNativeQuery(CITE_PAR_PV)
                .setParameter("ids", lignes.stream().map(PrjProjectProgressCriteriaLine::getId).toList()).getSingleResult();
        if (n.intValue() > 0) {
            throw new ConflictException(message);
        }
    }

    private PrjProjectProgressCriteriaLine nouveauCritere(UUID projet, Integer ordre, String description, UUID categorie) {
        PrjProjectProgressCriteriaLine c = new PrjProjectProgressCriteriaLine();
        c.setId(UUID.randomUUID());
        c.setPrjProjectId(projet);
        c.setSorting(ordre);
        c.setDescription(description);
        c.setStlCategoryId(categorie);
        TracePatrimoine.Trace t = trace.maintenant();
        c.setUserCreate(t.auteur());
        c.setUserCreateDateTime(t.le());
        c.setUserUpdate(t.auteur());
        c.setUserUpdateDateTime(t.le());
        em.persist(c);
        return c;
    }

    private static String description(CritereAvancementEcritureDto s) {
        return obligatoire(s.description(), null, "La description");
    }

    private StlCategory categorie(UUID id) {
        if (id == null) {
            throw new InvalidRequestException("categorieId : la catégorie est obligatoire.");
        }
        StlCategory c = em.find(StlCategory.class, id);
        if (c == null || !estDepense(c)) {
            throw new InvalidRequestException("categorieId : catégorie de dépense inconnue.");
        }
        return c;
    }

    private List<PrjProjectProgressCriteriaLine> lignesCriteres(UUID projet) {
        return em.createQuery("""
                        SELECT c FROM PrjProjectProgressCriteriaLine c WHERE c.prjProjectId = :projet
                        ORDER BY c.sorting ASC, c.description ASC""", PrjProjectProgressCriteriaLine.class)
                .setParameter(PROJET, projet).getResultList();
    }

    private static CritereAvancementDto critereDto(PrjProjectProgressCriteriaLine c, StlCategory k) {
        return new CritereAvancementDto(c.getId(), c.getSorting(), c.getDescription(), c.getStlCategoryId(),
                k == null ? null : k.getCode(), k == null ? null : k.getDescription());
    }

    // ------------------------------------------------------------------ communs

    private PrjProject charger(UUID id) {
        return projets.findById(id).orElseThrow(() -> new ResourceNotFoundException("Projet introuvable: " + id));
    }

    /** Ligne de l'onglet appartenant au projet, sinon 404. */
    private <T> T ligne(Class<T> classe, UUID ligneId, PrjProject p, Function<T, UUID> projetDe) {
        T ligne = em.find(classe, ligneId);
        if (ligne == null || !p.getId().equals(projetDe.apply(ligne))) {
            throw new ResourceNotFoundException("Ligne introuvable dans ce projet: " + ligneId);
        }
        return ligne;
    }

    /** Référence lue par le cache de premier niveau ; null si absente ou supprimée. */
    private <T> T lu(Class<T> classe, UUID id) {
        return id == null ? null : em.find(classe, id);
    }
}
