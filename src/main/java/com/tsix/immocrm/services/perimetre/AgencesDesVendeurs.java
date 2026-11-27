package com.tsix.immocrm.services.perimetre;

import com.tsix.immocrm.domain.FamilleTiers;
import com.tsix.immocrm.domain.RoleAgence;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierKind;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.cfg.CfgTierContactRepository;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Agence d'un vendeur (lien {@link RoleAgence#RELATION_VENDEUR}), « Siège » sans lien : seule règle de la vue siège
 * (colonne et filtre {@code agence} des dossiers, prospections, offres, état des ventes) — FEATURE-agences-vente.md §6.
 */
@Component
public class AgencesDesVendeurs {

    /** Valeur réservée du filtre {@code agence} : commercial vendeur d'aucune agence (ou absent). */
    public static final String FILTRE_SIEGE = "siege";

    /** Agence d'un vendeur ; {@code id} null pour le siège. */
    public record Agence(UUID id, String libelle) {
        public static final Agence SIEGE = new Agence(null, "Siège");
    }

    private final CfgTierContactRepository liens;

    public AgencesDesVendeurs(CfgTierContactRepository liens) {
        this.liens = liens;
    }

    /** Vendeur → agence, une requête ; seuls les tiers {@link #estAgence} comptent ; premier lien gardé (id croissant). */
    public Map<UUID, Agence> carte() {
        Map<UUID, Agence> carte = new HashMap<>();
        for (Object[] lien : liens.liensDuType(RoleAgence.RELATION_VENDEUR)) {
            if (!FamilleTiers.AGENCES.contient((String) lien[3], (String) lien[4])) {
                continue;
            }
            carte.putIfAbsent((UUID) lien[0], new Agence((UUID) lien[1], (String) lien[2]));
        }
        return carte;
    }

    /** Agence du commercial dans la carte ; sans commercial ou sans lien = siège. */
    public static Agence de(Map<UUID, Agence> carte, CfgTier commercial) {
        return commercial == null ? Agence.SIEGE : carte.getOrDefault(commercial.getId(), Agence.SIEGE);
    }

    /** Tiers de la famille Agences (type CNT, nature AGV) : bénéficiaire d'agence d'une commission. */
    public static boolean estAgence(CfgTier t) {
        return t != null && FamilleTiers.AGENCES.contient(Champs.de(t.getCfgTierType(), CfgTierType::getCode),
                Champs.de(t.getCfgTierKind(), CfgTierKind::getCode));
    }

    /** Nom affiché d'un bénéficiaire : libellé d'agence (Description, comme {@link #carte()}) ou nom du tiers. */
    public static String nomBeneficiaire(CfgTier t) {
        if (t == null) {
            return null;
        }
        return estAgence(t) ? t.getDescription() : com.tsix.immocrm.utils.TierNomFormatter.nom(t);
    }

    public Agence de(CfgTier commercial) {
        return commercial == null ? Agence.SIEGE : de(carte(), commercial);
    }

    /**
     * Prédicat du filtre {@code agence} sur l'id du commercial : ids d'agence et/ou {@code siege}, en OU ;
     * null si rien n'est coché. Une valeur ni UUID ni {@code siege} → 400.
     */
    public Predicate predicat(Expression<UUID> commercialId, CriteriaBuilder cb, List<String> agence) {
        if (agence == null || agence.isEmpty()) {
            return null;
        }
        Set<UUID> agences = new HashSet<>();
        boolean siege = false;
        for (String valeur : agence) {
            if (FILTRE_SIEGE.equalsIgnoreCase(valeur)) {
                siege = true;
            } else {
                agences.add(enUuid(valeur));
            }
        }
        Map<UUID, Agence> carte = carte();
        List<Predicate> ou = new ArrayList<>();
        List<UUID> vendeurs = carte.entrySet().stream()
                .filter(e -> agences.contains(e.getValue().id())).map(Map.Entry::getKey).toList();
        if (!vendeurs.isEmpty()) {
            ou.add(commercialId.in(vendeurs));
        }
        if (siege) {
            ou.add(carte.isEmpty() ? cb.conjunction()
                    : cb.or(cb.isNull(commercialId), cb.not(commercialId.in(carte.keySet()))));
        }
        return ou.isEmpty() ? cb.disjunction() : cb.or(ou.toArray(Predicate[]::new));
    }

    private static UUID enUuid(String valeur) {
        try {
            return UUID.fromString(valeur);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("agence : « %s » n'est ni un id d'agence ni « %s »"
                    .formatted(valeur, FILTRE_SIEGE));
        }
    }
}
