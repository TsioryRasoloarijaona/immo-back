package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.faisabilite.BilanFaisabilite;
import com.tsix.immocrm.domain.faisabilite.BilanFaisabilite.Noeud;
import com.tsix.immocrm.domain.faisabilite.EtatFaisabilite;
import com.tsix.immocrm.domain.faisabilite.EtatFaisabilite.Categorie;
import com.tsix.immocrm.domain.faisabilite.EtatFaisabilite.Relation;
import com.tsix.immocrm.domain.faisabilite.EtatFaisabilite.Saisie;
import com.tsix.immocrm.domain.faisabilite.EtatFaisabilite.Unite;
import com.tsix.immocrm.domain.faisabilite.TresorerieFaisabilite;
import com.tsix.immocrm.dto.patrimoine.faisabilite.ApercuTresorerieDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.BilanEtudeDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.BilanFaisabilitePrevisionsDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.CategorieBilanDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.LignePrevisionDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.SaisiePrevisionDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.TotauxPrevisionsDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.TresorerieFaisabiliteDto;
import com.tsix.immocrm.dto.patrimoine.faisabilite.VentilationPrevisionDto;
import com.tsix.immocrm.entities.stk.FSPrevision;
import com.tsix.immocrm.entities.stk.StkFSPrevisionLine;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudy;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudyCfgTranche;
import com.tsix.immocrm.entities.stl.StlCategory;
import com.tsix.immocrm.repositories.stk.StkFeasibilityStudyRepository;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.tsix.immocrm.domain.faisabilite.BilanFaisabilite.r;

/** Onglets Bilan, Analyse et Données d'une étude (FEATURE-patrimoine-faisabilite-workflow.md §2) ; écriture = recalcul §3. */
@Service
@Transactional(readOnly = true)
public class PrevisionFaisabiliteService {

    private final StkFeasibilityStudyRepository etudes;
    private final RecalculFaisabiliteService recalcul;
    private final GrillesEtude grilles;
    private final EntityManager em;

    public PrevisionFaisabiliteService(StkFeasibilityStudyRepository etudes, RecalculFaisabiliteService recalcul,
                                       GrillesEtude grilles, EntityManager em) {
        this.etudes = etudes;
        this.recalcul = recalcul;
        this.grilles = grilles;
        this.em = em;
    }

    /** Arbre recalculé depuis les lignes enregistrées ; bilan = colonnes stockées par le dernier recalcul. */
    public BilanFaisabilitePrevisionsDto lire(UUID etudeId) {
        StkFeasibilityStudy e = etudes.exigerVisible(etudeId);
        EtatFaisabilite etat = recalcul.lire(e, null);
        return dto(etat, BilanFaisabilite.calculer(etat), FicheEtude.bilan(e));
    }

    /** Tout le calcul (trésorerie comprise) a lieu avant la première écriture : un 400 n'écrit rien. */
    @Transactional
    public BilanFaisabilitePrevisionsDto modifierPrevisions(UUID etudeId, List<SaisiePrevisionDto> lignes) {
        StkFeasibilityStudy e = FicheEtude.modifiable(etudes, etudeId);
        EtatFaisabilite etat = recalcul.lire(e, saisies(etudeId, lignes));
        RecalculFaisabiliteService.Calcul calcul = RecalculFaisabiliteService.calculer(etat);
        recalcul.enregistrerCalcul(e, calcul);
        return dto(etat, calcul.bilan(), FicheEtude.bilan(e));
    }

    public BilanFaisabilitePrevisionsDto calculer(UUID etudeId, List<SaisiePrevisionDto> lignes) {
        EtatFaisabilite etat = recalcul.lire(etudes.exigerVisible(etudeId), saisies(etudeId, lignes));
        BilanFaisabilite.Resultat bilan = BilanFaisabilite.calculer(etat);
        return dto(etat, bilan, bilan.bilan());
    }

    public List<CategorieBilanDto> categories() {
        return em.createQuery("SELECT c FROM StlCategory c WHERE c.isWithoutPrevision IS NULL OR c.isWithoutPrevision = false "
                        + "ORDER BY c.sorting, c.description", StlCategory.class).getResultList().stream()
                .map(c -> new CategorieBilanDto(c.getId(), c.getCode(), c.getDescription(), c.getParentId(),
                        Boolean.TRUE.equals(c.getIsCredit()), c.getSorting())).toList();
    }

    /** Lignes enregistrées par le dernier recalcul. */
    public TresorerieFaisabiliteDto tresorerie(UUID etudeId, boolean avecFinancement) {
        etudes.exigerVisible(etudeId);
        List<TresorerieFaisabilite.Ligne> lignes = em.createQuery("SELECT l FROM StkFSPrevisionLine l "
                        + "WHERE l.stkFeasibilityStudyId = :etude", StkFSPrevisionLine.class).setParameter("etude", etudeId)
                .getResultList().stream().filter(l -> l.getDocDate() != null && l.getAmount() != null)
                .map(l -> new TresorerieFaisabilite.Ligne(TresorerieFaisabilite.Type.deCode(l.getPrevisionType()),
                        l.getDocDate(), l.getAmount(), l.getStkFeasibilityStudyCfgTrancheId())).toList();
        return TresorerieFaisabilite.serie(lignes, codesTranches(etudeId), avecFinancement);
    }

    /** Mêmes lignes recalculées avec d'autres écarts-types, sans écrire. */
    public TresorerieFaisabiliteDto calculerTresorerie(UUID etudeId, ApercuTresorerieDto apercu) {
        StkFeasibilityStudy e = etudes.exigerVisible(etudeId);
        FicheEtude.exigerEcartsTypes(apercu.ecartTypeTravaux(), apercu.ecartTypeConcretisation(), apercu.ecartTypeFinalisation());
        EtatFaisabilite etat = recalcul.lire(e, null);
        EtatFaisabilite apercuEtat = new EtatFaisabilite(etat.parametres().avecEcartsTypes(apercu.ecartTypeTravaux(),
                apercu.ecartTypeConcretisation(), apercu.ecartTypeFinalisation()), etat.categories(), etat.relations(),
                etat.unites(), etat.tranches(), etat.credits(), etat.saisies());
        return TresorerieFaisabilite.serie(RecalculFaisabiliteService.calculer(apercuEtat).lignes(), codesTranches(etudeId),
                !Boolean.FALSE.equals(apercu.avecFinancement()));
    }

    private Map<UUID, String> codesTranches(UUID etudeId) {
        return grilles.tranches(etudeId).stream().collect(Collectors.toMap(StkFeasibilityStudyCfgTranche::getId,
                t -> Objects.requireNonNullElse(t.getCode(), ""), (a, b) -> a, LinkedHashMap::new));
    }

    /**
     * « Sans prévision » (IsWithoutPrevision) interdit de CHOISIR la catégorie ; une ligne déjà enregistrée (saisie dans
     * OMRAN, ex. YESTEST) reste acceptée, sinon l'étude ne se relirait plus.
     */
    private List<Saisie> saisies(UUID etudeId, List<SaisiePrevisionDto> lignes) {
        Set<UUID> enregistrees = recalcul.previsions(etudeId).stream().map(FSPrevision::getStlCategoryId)
                .collect(Collectors.toSet());
        return Objects.requireNonNullElse(lignes, List.<SaisiePrevisionDto>of()).stream().map(s -> {
            if (s.categorieId() != null && !enregistrees.contains(s.categorieId())
                    && Boolean.TRUE.equals(Champs.de(em.find(StlCategory.class, s.categorieId()),
                    StlCategory::getIsWithoutPrevision))) {
                throw new InvalidRequestException("categorieId : catégorie « sans prévision », non sélectionnable (%s)."
                        .formatted(s.categorieId()));
            }
            return new Saisie(s.categorieId(), s.uniteEtudeId(), s.categorieReferenceId(), s.ratio(), s.montantHt(),
                    s.montant(), s.tauxTva(), s.estPourProjet(), s.dateApplication());
        }).toList();
    }

    private static BilanFaisabilitePrevisionsDto dto(EtatFaisabilite etat, BilanFaisabilite.Resultat b, BilanEtudeDto bilan) {
        List<LignePrevisionDto> lignes = b.noeuds().stream().map(n -> ligne(etat, n)).toList();
        return new BilanFaisabilitePrevisionsDto(lignes, new TotauxPrevisionsDto(r(b.depenseHt()), r(b.depense()),
                r(b.recetteHt()), r(b.recette()), !etat.relations().containsValue(Relation.PRIX_TERRAIN)), bilan);
    }

    private static LignePrevisionDto ligne(EtatFaisabilite etat, Noeud n) {
        Categorie c = n.categorie();
        Saisie s = n.saisie();
        UUID unite = s == null ? null : s.uniteId();
        UUID reference = s == null ? null : s.referenceId();
        return new LignePrevisionDto(c.id(), c.parentId(), c.code(), c.libelle(), c.recette(), n.profondeur(), unite,
                Champs.de(etat.unites().get(unite), Unite::code), reference,
                Champs.de(etat.categories().get(reference), Categorie::libelle), r(n.ratio()), r(n.tva()), r(n.htPropre()),
                r(n.propre()), r(n.ht()), r(n.ttc()), r(n.poids()), n.pourProjet(), n.dateApplication(),
                n.dateApplication() != null, n.verrouillee(), n.ventilation().stream()
                .map(p -> new VentilationPrevisionDto(p.trancheId(), p.code(), r(p.ht()), r(p.ttc()))).toList());
    }
}
