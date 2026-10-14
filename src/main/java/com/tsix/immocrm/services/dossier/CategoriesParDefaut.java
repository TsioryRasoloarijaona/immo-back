package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.domain.RoleCategorieEcheance;
import com.tsix.immocrm.entities.adm.AdmParamsGuid;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.repositories.adm.AdmParamsGuidRepository;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

/**
 * Rôle d'une catégorie d'échéance (pré-réservation, avance, frais de dossier…) : le paramètre OMRAN
 * {@code AdmParamsGuid.Default*DocumentType} s'il est posé, sinon le code de la catégorie.
 */
@Component
public class CategoriesParDefaut {

    private final AdmParamsGuidRepository parametreRepository;

    public CategoriesParDefaut(AdmParamsGuidRepository parametreRepository) {
        this.parametreRepository = parametreRepository;
    }

    public Optional<RoleCategorieEcheance> role(ComSettlementCategory categorie) {
        if (categorie == null) {
            return Optional.empty();
        }
        return Arrays.stream(RoleCategorieEcheance.values())
                .filter(role -> estDuRole(categorie, role))
                .findFirst();
    }

    public boolean estDuRole(ComSettlementCategory categorie, RoleCategorieEcheance role) {
        if (categorie == null) {
            return false;
        }
        Optional<UUID> parametre = parametreRepository.findById(role.parametre()).map(AdmParamsGuid::getCodeValue);
        if (parametre.isPresent()) {
            return parametre.get().equals(categorie.getId());
        }
        return role.code() != null && categorie.getCode() != null && role.code().equalsIgnoreCase(categorie.getCode().trim());
    }
}
