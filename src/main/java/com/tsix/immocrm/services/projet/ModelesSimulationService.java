package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.dto.projet.LigneModeleDto;
import com.tsix.immocrm.dto.projet.ModeleSimulationDetailDto;
import com.tsix.immocrm.dto.projet.ModeleSimulationListItemDto;
import com.tsix.immocrm.dto.projet.ModeleSimulationSaisieDto;
import com.tsix.immocrm.entities.prj.PrjMarketPrevisionModel;
import com.tsix.immocrm.entities.prj.PrjMarketPrevisionModelLine;
import com.tsix.immocrm.entities.stl.StlCategory;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.ForbiddenException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.services.droits.DroitsOmran;
import com.tsix.immocrm.services.patrimoine.ReferentielAdminService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Modèles de simulation lot {@code PrjMarketPrevisionModel(Line)} (FEATURE-lot-previsions-reel.md §4 M1-M4, M7). */
@Service
@Transactional
public class ModelesSimulationService {

    static final String DROIT_MODELES = "Yes.Prj.Front.Config.MarketPrevisionModelList";
    // Messages OMRAN (MarketPrevisionModel.Verif), recopiés tels quels.
    private static final String MESSAGE_LIGNES = "Vous devez saisir au moins deux ligne de prévision avec obligatoirement un "
            + "pourcentage d'avancement du temps égale à 0% et 100%";
    private static final String MESSAGE_PAIEMENT = "le total des % de paiement doit être égale à 100%";
    private static final BigDecimal PAIEMENT_MIN = new BigDecimal("0.99");
    private static final BigDecimal PAIEMENT_MAX = new BigDecimal("1.01");
    private static final int LONGUEUR = 255;
    private static final String ID = "id";

    private final EntityManager em;
    private final DroitsOmran droits;

    public ModelesSimulationService(EntityManager em, DroitsOmran droits) {
        this.em = em;
        this.droits = droits;
    }

    @Transactional(readOnly = true)
    public List<ModeleSimulationListItemDto> liste(UUID categorieId) {
        exigerLecture();
        return em.createQuery("""
                        SELECT new com.tsix.immocrm.dto.projet.ModeleSimulationListItemDto(m.id, m.code, m.description,
                            COALESCE(c.description, c.code))
                        FROM PrjMarketPrevisionModel m LEFT JOIN StlCategory c ON c.id = m.stlCategoryId
                        WHERE :tous = true OR m.stlCategoryId = :categorie ORDER BY m.code ASC""",
                        ModeleSimulationListItemDto.class)
                .setParameter("tous", categorieId == null).setParameter("categorie", categorieId).getResultList();
    }

    @Transactional(readOnly = true)
    public ModeleSimulationDetailDto detail(UUID id) {
        PrjMarketPrevisionModel m = trouver(id);
        exigerLecture();
        return fiche(m);
    }

    public ModeleSimulationDetailDto creer(ModeleSimulationSaisieDto corps) {
        droits.exiger(DROIT_MODELES);
        PrjMarketPrevisionModel m = new PrjMarketPrevisionModel();
        m.setId(UUID.randomUUID());
        ecrire(m, corps);
        em.persist(m);
        return enregistrerLignes(m, corps.lignes());
    }

    public ModeleSimulationDetailDto modifier(UUID id, ModeleSimulationSaisieDto corps) {
        PrjMarketPrevisionModel m = trouver(id);
        droits.exiger(DROIT_MODELES);
        ecrire(m, corps);
        return enregistrerLignes(m, corps.lignes());
    }

    public void supprimer(UUID id) {
        PrjMarketPrevisionModel m = trouver(id);
        droits.exiger(DROIT_MODELES);
        long cites = em.createQuery("SELECT COUNT(p) FROM FSPrevision p WHERE p.prjMarketPrevisionModelId = :id", Long.class)
                .setParameter(ID, id).getSingleResult();
        if (cites > 0) {
            throw new ConflictException("Le modèle %s est cité par %d prévision(s) de faisabilité : suppression impossible."
                    .formatted(m.getCode(), cites));
        }
        effacerLignes(id);
        em.remove(m);
        em.flush();
    }

    private PrjMarketPrevisionModel trouver(UUID id) {
        PrjMarketPrevisionModel m = em.find(PrjMarketPrevisionModel.class, id);
        if (m == null) {
            throw new ResourceNotFoundException("Modèle de simulation introuvable : " + id);
        }
        return m;
    }

    private void exigerLecture() {
        if (droits.parmi(DROIT_MODELES, DroitsPrevisionsLot.DROIT_SAISIR).isEmpty()) {
            throw new ForbiddenException("Vous n'avez pas le droit d'effectuer cette action.");
        }
    }

    /** Tous les contrôles avant le premier setter : un refus laisse l'entité intacte. */
    private void ecrire(PrjMarketPrevisionModel m, ModeleSimulationSaisieDto corps) {
        String code = texte(corps.code(), "Le code");
        String description = texte(corps.description(), "La description");
        verifierLignes(corps.lignes());
        if (corps.categorieId() != null && em.find(StlCategory.class, corps.categorieId()) == null) {
            throw new InvalidRequestException("Catégorie de dépense inconnue : " + corps.categorieId());
        }
        ReferentielAdminService.verrouiller(em, PrjMarketPrevisionModel.class, code, "Modèle de simulation");
        long pris = em.createQuery("""
                        SELECT COUNT(m) FROM PrjMarketPrevisionModel m
                        WHERE LOWER(TRIM(m.code)) = LOWER(:code) AND m.id <> :id""", Long.class)
                .setParameter("code", code).setParameter(ID, m.getId()).getSingleResult();
        if (pris > 0) {
            throw new ConflictException("Le code %s est déjà pris par un autre modèle de simulation.".formatted(code));
        }
        m.setCode(code);
        m.setDescription(description);
        m.setStlCategoryId(corps.categorieId());
    }

    private static String texte(String valeur, String libelle) {
        String t = valeur == null ? "" : valeur.trim();
        if (t.isEmpty() || t.length() > LONGUEUR) {
            throw new InvalidRequestException("%s est obligatoire (%d caractères au plus).".formatted(libelle, LONGUEUR));
        }
        return t;
    }

    private static void verifierLignes(List<LigneModeleDto> lignes) {
        if (lignes == null || lignes.size() < 2) {
            throw new InvalidRequestException(MESSAGE_LIGNES);
        }
        if (lignes.stream().anyMatch(l -> l == null || l.temps() == null || l.avancement() == null || l.paiement() == null)) {
            throw new InvalidRequestException("Chaque ligne porte un temps, un avancement et un paiement.");
        }
        BigDecimal tempsMax = lignes.stream().map(LigneModeleDto::temps).reduce(BigDecimal::max).orElse(BigDecimal.ZERO);
        if (tempsMax.compareTo(BigDecimal.ONE) != 0) {
            throw new InvalidRequestException(MESSAGE_LIGNES);
        }
        BigDecimal paiements = lignes.stream().map(LigneModeleDto::paiement).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (paiements.compareTo(PAIEMENT_MIN) < 0 || paiements.compareTo(PAIEMENT_MAX) > 0) {
            throw new InvalidRequestException(MESSAGE_PAIEMENT);
        }
    }

    private ModeleSimulationDetailDto enregistrerLignes(PrjMarketPrevisionModel m, List<LigneModeleDto> lignes) {
        effacerLignes(m.getId());
        lignes.forEach(l -> {
            PrjMarketPrevisionModelLine ligne = new PrjMarketPrevisionModelLine();
            ligne.setId(UUID.randomUUID());
            ligne.setPointTime(l.temps());
            ligne.setPointAchivement(l.avancement());
            ligne.setPointPayment(l.paiement());
            ligne.setPrjMarketPrevisionModelId(m.getId());
            em.persist(ligne);
        });
        em.flush();
        return fiche(m);
    }

    private static String libelleCode(StlCategory c) {
        return c == null ? null : c.getCode();
    }

    private void effacerLignes(UUID modele) {
        em.createQuery("DELETE FROM PrjMarketPrevisionModelLine l WHERE l.prjMarketPrevisionModelId = :id")
                .setParameter(ID, modele).executeUpdate();
    }

    private ModeleSimulationDetailDto fiche(PrjMarketPrevisionModel m) {
        List<LigneModeleDto> lignes = em.createQuery("""
                        SELECT new com.tsix.immocrm.dto.projet.LigneModeleDto(l.pointTime, l.pointAchivement, l.pointPayment)
                        FROM PrjMarketPrevisionModelLine l WHERE l.prjMarketPrevisionModelId = :id
                        ORDER BY l.pointTime ASC, l.pointAchivement ASC""", LigneModeleDto.class)
                .setParameter(ID, m.getId()).getResultList();
        StlCategory c = m.getStlCategoryId() == null ? null : em.find(StlCategory.class, m.getStlCategoryId());
        String categorie = c == null || c.getDescription() == null ? libelleCode(c) : c.getDescription();
        return new ModeleSimulationDetailDto(m.getId(), m.getCode(), m.getDescription(), m.getStlCategoryId(), categorie,
                lignes);
    }
}
