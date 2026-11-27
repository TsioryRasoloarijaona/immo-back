package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.domain.DisponibiliteArticle;
import com.tsix.immocrm.dto.dossier.CreerDossierRequest;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.services.perimetre.AgenceDuCompte.Rattachement;
import com.tsix.immocrm.services.perimetre.ArticlesVisibles;
import com.tsix.immocrm.services.perimetre.ObjetsVisibles;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Règles de création d'un dossier par un compte agence (FEATURE-agences-vente.md R-D1 à R-D5) ; le siège passe. */
@Component
public record CreationDossierAgence(PerimetreCommercialService perimetre, ArticlesVisibles articlesVisibles,
                                    ObjetsVisibles objetsVisibles, StkItemRepository items) {

    static final String LOT_RESERVE = "Ce lot vient d'être réservé";

    /** Rattachement d'un compte agence, contrôlé avant toute lecture ; absent pour le siège. */
    public Optional<Rattachement> verifierCompte(CreerDossierRequest request) {
        Optional<Rattachement> agence = perimetre.rattachementAgenceComplet();
        agence.ifPresent(r -> {
            BigDecimal remise = request.remisePercent();
            if (remise != null && remise.signum() != 0) {
                throw new InvalidRequestException("Une remise passe par une demande au siège");
            }
            perimetre.verifierAccesProspection(request.prospectionId());
        });
        return agence;
    }

    /** Agence : un article hors de ses projets ou pris par un autre dossier est introuvable (404). */
    public List<StkItem> articles(Optional<Rattachement> agence, List<UUID> ids, String champ) {
        if (agence.isEmpty()) {
            return objetsVisibles.articles(ids, champ);
        }
        return ids.stream().map(id -> articlesVisibles.trouver(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article introuvable: " + id))).toList();
    }

    /** Lot déjà lié : 409 pour une agence (course entre deux comptes), 400 inchangé pour le siège. */
    public void verifierLibres(Optional<Rattachement> agence, List<StkItem> articles) {
        for (StkItem item : articles) {
            if (!DisponibiliteArticle.estOccupe(item)) {
                continue;
            }
            if (agence.isPresent()) {
                throw new ConflictException(LOT_RESERVE);
            }
            throw new InvalidRequestException("Article déjà occupé (lié à un autre dossier): " + item.getId());
        }
    }

    /** R-D4 : une agence vend au moins au total de référence du lot et de ses annexes. */
    public void verifierPlancher(Optional<Rattachement> agence, BigDecimal prix, BigDecimal plancher) {
        if (agence.isEmpty()) {
            return;
        }
        if (plancher.signum() <= 0) {
            throw new InvalidRequestException("Ce lot n'a pas de prix de référence : la vente passe par le siège");
        }
        if (prix != null && prix.compareTo(plancher) < 0) {
            throw new InvalidRequestException("Le prix ne peut pas être inférieur au prix de référence (%s)"
                    .formatted(plancher.toPlainString()));
        }
    }

    /** Pose conditionnelle (un seul gagnant), puis le setter pour que l'audit Hibernate trace le lien. */
    public void reserver(StkItem principal, UUID dossierId) {
        if (items.reserverSiLibre(principal.getId(), dossierId) == 0) {
            throw new ConflictException(LOT_RESERVE);
        }
        principal.setComFolderId(dossierId);
        items.save(principal);
    }
}
