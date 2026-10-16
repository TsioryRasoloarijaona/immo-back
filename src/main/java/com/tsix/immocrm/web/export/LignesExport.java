package com.tsix.immocrm.web.export;

import com.tsix.immocrm.domain.Colonne;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/** Lignes et colonnes d'un export de liste, sans connaissance d'un écran particulier. */
public final class LignesExport {

    /** Plafond de l'export : au-delà, un classeur n'est plus lisible et la requête bloque la base. */
    public static final int LIGNES_EXPORT_MAX = 20_000;

    /** Une page lie au plus ce nombre d'ids : SQL Server refuse plus de 2 100 paramètres par requête. */
    static final int TAILLE_PAGE = 2_000;

    private static final String ID = "id";

    private LignesExport() {
    }

    /**
     * Toutes les lignes de la liste, page après page, avec la lecture paginée de la liste elle-même (mêmes
     * filtres, même périmètre). {@code id} départage les ex æquo, sans quoi une ligne changerait de page.
     */
    public static <T> List<T> toutes(Sort tri, Function<Pageable, Page<T>> lecture) {
        Sort stable = tri.getOrderFor(ID) != null ? tri : tri.and(Sort.by(ID));
        List<T> lignes = new ArrayList<>();
        Page<T> page;
        int numero = 0;
        do {
            page = lecture.apply(PageRequest.of(numero++, TAILLE_PAGE, stable));
            lignes.addAll(page.getContent());
        } while (page.hasNext() && lignes.size() < LIGNES_EXPORT_MAX);
        return lignes.size() > LIGNES_EXPORT_MAX ? List.copyOf(lignes.subList(0, LIGNES_EXPORT_MAX)) : lignes;
    }

    /**
     * Colonnes exportées dans l'ordre reçu (le serveur ne réordonne pas) ; sans {@code demandees}, la
     * sélection par défaut. {@code resolues} : la résolution du catalogue, qui a déjà rejeté un nom inconnu.
     */
    public static <C extends Colonne> List<C> ordre(List<String> demandees, Set<C> resolues) {
        if (demandees == null || demandees.isEmpty()) {
            return List.copyOf(resolues);
        }
        List<C> ordre = new ArrayList<>();
        for (String nom : demandees) {
            for (String morceau : nom.split(",")) {
                resolues.stream()
                        .filter(c -> c.nom().equalsIgnoreCase(morceau.trim()) && !ordre.contains(c))
                        .findFirst()
                        .ifPresent(ordre::add);
            }
        }
        return ordre;
    }
}
