package com.tsix.immocrm.domain.rapport;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Ce que le socle remet à un rapport (FEATURE-tdb-vente-rapports-socle.md §3.3, B2, B4) : valeurs déjà validées,
 * tranches visibles (entités actives ∩ périmètre ∩ demandées) et commerciaux visibles (portée ∩ demandés).
 * Un rapport ne filtre que par {@link #tranchesVisibles()} / {@link #commerciauxVisibles()}, jamais par les ids demandés.
 */
public final class ContexteRapport {

    private final UUID admUserId;
    private final Clock clock;
    private final Map<String, List<String>> valeurs;
    private final List<UUID> tranchesVisibles;
    private final Supplier<List<UUID>> commerciaux;
    private List<UUID> commerciauxCalcules;

    public ContexteRapport(UUID admUserId, Clock clock, Map<String, List<String>> valeurs,
                           List<UUID> tranchesVisibles, Supplier<List<UUID>> commerciaux) {
        this.admUserId = admUserId;
        this.clock = clock;
        this.valeurs = valeurs;
        this.tranchesVisibles = List.copyOf(tranchesVisibles);
        this.commerciaux = commerciaux;
    }

    public UUID admUserId() {
        return admUserId;
    }

    public Clock clock() {
        return clock;
    }

    /** Vide = aucune tranche à lire (jamais « toutes ») ; à borner par lots de 2 000 dans une requête. */
    public List<UUID> tranchesVisibles() {
        return tranchesVisibles;
    }

    /** Clés des commerciaux visibles ({@code CommerciauxVisibles.cle}), calculées au premier appel. */
    public List<UUID> commerciauxVisibles() {
        if (commerciauxCalcules == null) {
            commerciauxCalcules = List.copyOf(commerciaux.get());
        }
        return commerciauxCalcules;
    }

    /** Toutes les valeurs appliquées du paramètre (défaut compris), vide si aucune. */
    public List<String> choix(String nom) {
        return valeurs.getOrDefault(nom, List.of());
    }

    public String texte(String nom) {
        List<String> v = choix(nom);
        return v.isEmpty() ? null : v.get(0);
    }

    public LocalDate date(String nom) {
        String v = texte(nom);
        return v == null ? null : LocalDate.parse(v);
    }

    public Integer entier(String nom) {
        String v = texte(nom);
        return v == null ? null : Integer.valueOf(v);
    }

    public Integer annee(String nom) {
        return entier(nom);
    }

    public Boolean booleen(String nom) {
        String v = texte(nom);
        return v == null ? null : Boolean.valueOf(v);
    }
}
