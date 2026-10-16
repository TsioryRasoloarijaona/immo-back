package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.stl.StlAccount;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.stl.StlAccountPositionRepository;
import com.tsix.immocrm.repositories.stl.StlAccountRepository;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

/**
 * Compte d'un pointage ({@code Settlement.cs:Verif} l.2202 et l.2218-2224) : obligatoire, de la société du règlement,
 * et date de pointage postérieure au dernier rapprochement du compte. Voir FEATURE-dossier-durcissement.md §4.
 */
@Component
public class ComptePointage {

    static final String COMPTE_OBLIGATOIRE =
            "Si votre pièce possède une date de pointage, il est obligatoire de lui affecter un compte !";
    private static final DateTimeFormatter FORMAT_OMRAN = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final StlAccountRepository accountRepository;
    private final StlAccountPositionRepository positionRepository;

    public ComptePointage(StlAccountRepository accountRepository, StlAccountPositionRepository positionRepository) {
        this.accountRepository = accountRepository;
        this.positionRepository = positionRepository;
    }

    /** Le compte à écrire sur le règlement ; 400 s'il manque ou n'est pas de sa société, 409 avant le rapprochement. */
    public StlAccount verifier(StlSettlement reglement, UUID compteId, LocalDateTime datePointage) {
        if (compteId == null) {
            throw new InvalidRequestException(COMPTE_OBLIGATOIRE);
        }
        StlAccount compte = accountRepository.findById(compteId)
                .orElseThrow(() -> new InvalidRequestException("Compte introuvable: " + compteId));
        UUID societeReglement = Champs.de(reglement.getCfgCompany(), CfgCompany::getId);
        if (societeReglement != null
                && !Objects.equals(societeReglement, Champs.de(compte.getCfgCompany(), CfgCompany::getId))) {
            throw new InvalidRequestException("Le compte " + compte.getCode()
                    + " n'appartient pas à la société du règlement.");
        }
        positionRepository.dernierRapprochement(compteId)
                .filter(dernier -> !dernier.isBefore(datePointage))
                .ifPresent(dernier -> {
                    throw new ConflictException("Vous ne pouvez pas pointer une pièce à une date inférieure à la date "
                            + "du dernier rapprochement du compte affecté " + FORMAT_OMRAN.format(dernier) + " !");
                });
        return compte;
    }
}
