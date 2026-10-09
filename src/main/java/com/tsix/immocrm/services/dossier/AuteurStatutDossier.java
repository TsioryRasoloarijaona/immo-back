package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.str.StrFolderStatusHistory;
import com.tsix.immocrm.repositories.str.StrHistoryRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Seule règle d'auteur d'une ligne {@code StrFolderStatusHistory}, lue par l'historique du dossier et par
 * celui du tiers : le compte {@code AdmUserId}, sinon la ligne {@code StrHistory} jumelle (même dossier,
 * même {@code DocDate}), sinon {@code null}. Voir FIX-historique-tiers-auteur.md.
 */
@Component
public class AuteurStatutDossier {

    private final UtilisateurAffiche utilisateurAffiche;
    private final StrHistoryRepository strHistoryRepository;

    public AuteurStatutDossier(UtilisateurAffiche utilisateurAffiche, StrHistoryRepository strHistoryRepository) {
        this.utilisateurAffiche = utilisateurAffiche;
        this.strHistoryRepository = strHistoryRepository;
    }

    private record Jumelle(UUID dossierId, LocalDateTime date) {
    }

    /** Résout en deux requêtes au plus l'auteur de chacune des {@code lignes}. */
    public Function<StrFolderStatusHistory, String> resoudre(Collection<StrFolderStatusHistory> lignes) {
        Map<UUID, String> parCompte = utilisateurAffiche.noms(
                lignes.stream().map(StrFolderStatusHistory::getAdmUserId).toList());
        Set<UUID> dossiersARepli = lignes.stream()
                .filter(h -> !parCompte.containsKey(h.getAdmUserId()))
                .map(AuteurStatutDossier::dossierId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Jumelle, String> parJumelle = new HashMap<>();
        if (!dossiersARepli.isEmpty()) {
            strHistoryRepository.findByObjectIdInOrderByDocDateDesc(dossiersARepli).forEach(s -> {
                // Un événement sans statut mais libellé (« Prospect transformé en client ») ne signe jamais un statut.
                boolean evenement = s.getStrStatusId() == null && StringUtils.hasText(s.getDescription());
                if (!evenement && s.getDocDate() != null && StringUtils.hasText(s.getAdmUserDescription())) {
                    parJumelle.putIfAbsent(new Jumelle(s.getObjectId(), s.getDocDate()), s.getAdmUserDescription());
                }
            });
        }
        return h -> {
            String nom = parCompte.get(h.getAdmUserId());
            if (nom != null) {
                return nom;
            }
            UUID dossierId = dossierId(h);
            return dossierId == null || h.getDocDate() == null ? null
                    : parJumelle.get(new Jumelle(dossierId, h.getDocDate()));
        };
    }

    private static UUID dossierId(StrFolderStatusHistory h) {
        return Champs.de(h.getComFolder(), ComFolder::getId);
    }
}
