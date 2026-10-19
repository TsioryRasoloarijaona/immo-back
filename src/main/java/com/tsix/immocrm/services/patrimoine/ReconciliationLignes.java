package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.patrimoine.LignesTrancheDto;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.cfg.LigneTrancheOmran;
import com.tsix.immocrm.exceptions.InvalidRequestException;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * PUT d'un onglet « liste de lignes » de la tranche : {@code id} connu = mise à jour, absent = création, ligne de la
 * base absente du corps = suppression, {@code id} d'une autre tranche → 400. Appelé dans la transaction d'écriture.
 */
@Component
public class ReconciliationLignes {

    private static final String ID = "id";

    private final EntityManager em;
    private final TracePatrimoine trace;

    public ReconciliationLignes(EntityManager em, TracePatrimoine trace) {
        this.em = em;
        this.trace = trace;
    }

    /** Ligne de la liste reçue : son nom dans un message 400 et son rang parmi les lignes gardées. */
    public record Position(int index, int rang) {
        public String champ(String nom) {
            return "lignes[%d].%s".formatted(index, nom);
        }
    }

    @FunctionalInterface
    public interface Remplir<E, L> {
        void appliquer(E entite, L ligne, Position position);
    }

    /**
     * Onglet réconcilié : requête de ses lignes ({@code :id} = tranche), entité, clé obligatoire de la ligne
     * ({@code cle} nulle = aucune) et sort d'un doublon de clé (ignoré comme OMRAN, ou refusé en 400).
     */
    public record Onglet<E extends LigneTrancheOmran, L extends LignesTrancheDto.Ligne>(
            String jpql, Class<E> classe, Supplier<E> nouvelle, Function<L, UUID> cle, String champCle,
            boolean doublonRefuse) {
    }

    public <E extends LigneTrancheOmran> List<E> lignes(CfgTranche tranche, String jpql, Class<E> classe) {
        return em.createQuery(jpql, classe).setParameter(ID, tranche.getId()).getResultList();
    }

    /** Renvoie les lignes gardées, dans l'ordre du corps. */
    public <E extends LigneTrancheOmran, L extends LignesTrancheDto.Ligne> List<E> reconcilier(
            CfgTranche tranche, LignesTrancheDto<L> corps, Onglet<E, L> onglet, Remplir<E, L> remplir) {
        List<L> recues = lignesRecues(corps);
        Map<UUID, E> existantes = lignes(tranche, onglet.jpql(), onglet.classe()).stream()
                .collect(Collectors.toMap(LigneTrancheOmran::getId, e -> e, (a, b) -> a, LinkedHashMap::new));
        TracePatrimoine.Trace t = trace.maintenant();
        Set<UUID> vues = new HashSet<>();
        List<E> gardees = new ArrayList<>();
        for (int i = 0; i < recues.size(); i++) {
            L ligne = recues.get(i);
            Position position = new Position(i, gardees.size() + 1);
            if (nouvelleCle(onglet, ligne, position, vues)) {
                E entite = ligne.id() == null ? creer(onglet.nouvelle(), tranche, t) : existantes.remove(ligne.id());
                if (entite == null) {
                    throw new InvalidRequestException(position.champ(ID) + " : ligne inconnue de la tranche.");
                }
                remplir.appliquer(entite, ligne, position);
                entite.setUserUpdate(t.auteur());
                entite.setUserUpdateDateTime(t.le());
                gardees.add(entite);
            }
        }
        existantes.values().forEach(em::remove);
        return gardees;
    }

    /** Corps non nul et sans ligne nulle. */
    public static <L extends LignesTrancheDto.Ligne> List<L> lignesRecues(LignesTrancheDto<L> corps) {
        if (corps == null || corps.lignes() == null) {
            throw new InvalidRequestException("lignes est obligatoire.");
        }
        for (int i = 0; i < corps.lignes().size(); i++) {
            if (corps.lignes().get(i) == null) {
                throw new InvalidRequestException("lignes[%d] est obligatoire.".formatted(i));
            }
        }
        return List.copyOf(corps.lignes());
    }

    public <T> T reference(Class<T> classe, UUID id, String champ) {
        T trouvee = em.find(classe, id);
        if (trouvee == null) {
            throw new InvalidRequestException("%s : référence inconnue (%s).".formatted(champ, id));
        }
        return trouvee;
    }

    /** Référence facultative : {@code null} reste {@code null}. */
    public <T> T referenceOuNull(Class<T> classe, UUID id, String champ) {
        return id == null ? null : reference(classe, id, champ);
    }

    private static <L extends LignesTrancheDto.Ligne> boolean nouvelleCle(Onglet<?, L> onglet, L ligne,
                                                                          Position position, Set<UUID> vues) {
        if (onglet.cle() == null) {
            return true;
        }
        UUID cle = onglet.cle().apply(ligne);
        if (cle == null) {
            throw new InvalidRequestException(position.champ(onglet.champCle()) + " est obligatoire.");
        }
        boolean nouvelle = vues.add(cle);
        if (!nouvelle && onglet.doublonRefuse()) {
            throw new InvalidRequestException(position.champ(onglet.champCle()) + " : déjà présent dans la tranche.");
        }
        return nouvelle;
    }

    private <E extends LigneTrancheOmran> E creer(Supplier<E> nouvelle, CfgTranche tranche, TracePatrimoine.Trace t) {
        E entite = nouvelle.get();
        entite.setId(UUID.randomUUID());
        entite.setCfgTranche(tranche);
        entite.setUserCreate(t.auteur());
        entite.setUserCreateDateTime(t.le());
        em.persist(entite);
        return entite;
    }
}
