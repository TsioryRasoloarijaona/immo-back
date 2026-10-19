package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.patrimoine.LigneContenanceDto;
import com.tsix.immocrm.dto.patrimoine.LignesTrancheDto;
import com.tsix.immocrm.entities.stk.StkContenance;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemContenance;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.ou;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.positif;

/**
 * Onglet Contenances de l'article (FEATURE-patrimoine-article-fiche.md §2), patron de la grille réconciliée P1b : toute
 * la liste est contrôlée avant la première écriture, un 400 n'écrit rien.
 */
@Component
class ContenancesArticle {

    private static final String LIGNES = "SELECT c FROM StkItemContenance c LEFT JOIN c.stkContenance r "
            + "WHERE c.stkItem.id = :id ORDER BY r.sorting, r.code";

    private final EntityManager em;
    private final ReferencesFicheArticle references;
    private final FicheArticle fiche;
    private final TracePatrimoine trace;

    ContenancesArticle(EntityManager em, ReferencesFicheArticle references, FicheArticle fiche, TracePatrimoine trace) {
        this.em = em;
        this.references = references;
        this.fiche = fiche;
        this.trace = trace;
    }

    private record LigneValide(LigneContenanceDto saisie, StkContenance contenance) {
    }

    LignesTrancheDto<LigneContenanceDto> lire(StkItem article) {
        return new LignesTrancheDto<>(lignes(article).stream().map(ContenancesArticle::dto).toList());
    }

    LignesTrancheDto<LigneContenanceDto> enregistrer(StkItem article, LignesTrancheDto<LigneContenanceDto> corps) {
        if (corps == null || corps.lignes() == null) {
            throw new InvalidRequestException("lignes est obligatoire.");
        }
        Map<UUID, StkItemContenance> existantes = lignes(article).stream()
                .collect(Collectors.toMap(StkItemContenance::getId, Function.identity(), (x, y) -> x, LinkedHashMap::new));
        List<LigneValide> valides = valider(corps.lignes(), existantes.keySet());
        TracePatrimoine.Trace t = trace.maintenant();
        valides.forEach(v -> {
            StkItemContenance ligne = v.saisie().id() == null ? nouvelle(article) : existantes.remove(v.saisie().id());
            ecrire(ligne, v.contenance(), v.saisie());
            ligne.setUserUpdate(t.auteur());
            ligne.setUserUpdateDateTime(t.le());
        });
        existantes.values().forEach(em::remove);
        return lire(article);
    }

    private List<LigneValide> valider(List<LigneContenanceDto> saisies, Set<UUID> connues) {
        Set<UUID> vues = new HashSet<>();
        List<LigneValide> valides = new ArrayList<>();
        for (int i = 0; i < saisies.size(); i++) {
            LigneContenanceDto l = saisies.get(i);
            String champ = "lignes[%d].".formatted(i);
            if (l == null || l.contenanceId() == null) {
                throw new InvalidRequestException(champ + "contenanceId est obligatoire.");
            }
            if (l.id() != null && (!connues.contains(l.id()) || !vues.add(l.id()))) {
                throw new InvalidRequestException(champ + "id : ligne inconnue de l'article, ou en double.");
            }
            StkContenance c = references.reference(StkContenance.class, l.contenanceId(), champ + "contenanceId");
            positif(l.nombre(), champ + "nombre");
            option(champ, 1, c.getOption1(), c.getIsOption1WithQuantity(), l.avecOption1(), l.quantiteOption1());
            option(champ, 2, c.getOption2(), c.getIsOption2WithQuantity(), l.avecOption2(), l.quantiteOption2());
            option(champ, 3, c.getOption3(), c.getIsOption3WithQuantity(), l.avecOption3(), l.quantiteOption3());
            valides.add(new LigneValide(l, c));
        }
        return valides;
    }

    /** Option vide = case grisée ; quantité seulement si la contenance la prévoit ({@code IsOptionNWithQuantity}). */
    private static void option(String champ, int n, String libelle, Boolean avecQuantite, Boolean cochee,
                               Integer quantite) {
        boolean grisee = libelle == null || libelle.isBlank();
        if (Boolean.TRUE.equals(cochee) && grisee) {
            throw new InvalidRequestException("%savecOption%d : l'option %d de cette contenance n'existe pas."
                    .formatted(champ, n, n));
        }
        positif(quantite, champ + "quantiteOption" + n);
        if (quantite != null && quantite != 0 && (grisee || !Boolean.TRUE.equals(avecQuantite))) {
            throw new InvalidRequestException("%squantiteOption%d : cette option ne prend pas de quantité."
                    .formatted(champ, n));
        }
    }

    private StkItemContenance nouvelle(StkItem article) {
        StkItemContenance ligne = fiche.nouvelle(new StkItemContenance());
        ligne.setStkItem(article);
        em.persist(ligne);
        return ligne;
    }

    private static void ecrire(StkItemContenance e, StkContenance c, LigneContenanceDto l) {
        e.setStkContenance(c);
        e.setItemsNumber(ou(l.nombre(), 0));
        e.setOption1(c.getOption1());
        e.setIsWithOption1(Boolean.TRUE.equals(l.avecOption1()));
        e.setQuantityOption1(ou(l.quantiteOption1(), 0));
        e.setOption2(c.getOption2());
        e.setIsWithOption2(Boolean.TRUE.equals(l.avecOption2()));
        e.setQuantityOption2(ou(l.quantiteOption2(), 0));
        e.setOption3(c.getOption3());
        e.setIsWithOption3(Boolean.TRUE.equals(l.avecOption3()));
        e.setQuantityOption3(ou(l.quantiteOption3(), 0));
    }

    private List<StkItemContenance> lignes(StkItem article) {
        return em.createQuery(LIGNES, StkItemContenance.class).setParameter("id", article.getId()).getResultList();
    }

    private static LigneContenanceDto dto(StkItemContenance e) {
        StkContenance c = e.getStkContenance();
        return new LigneContenanceDto(e.getId(), Champs.de(c, StkContenance::getId), e.getItemsNumber(),
                e.getIsWithOption1(), e.getQuantityOption1(), e.getIsWithOption2(), e.getQuantityOption2(),
                e.getIsWithOption3(), e.getQuantityOption3(),
                Champs.de(c, StkContenance::getCode), Champs.de(c, StkContenance::getDescription));
    }
}
