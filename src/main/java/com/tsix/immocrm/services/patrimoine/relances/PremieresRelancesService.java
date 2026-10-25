package com.tsix.immocrm.services.patrimoine.relances;

import com.tsix.immocrm.domain.ActionsDossier;
import com.tsix.immocrm.dto.patrimoine.relances.ApercuPremieresRelancesDto;
import com.tsix.immocrm.dto.patrimoine.relances.LigneApercuRelanceDto;
import com.tsix.immocrm.dto.patrimoine.relances.ResultatPremieresRelancesDto;
import com.tsix.immocrm.entities.adm.AdmParamsGuid;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.adm.AdmParamsGuidRepository;
import com.tsix.immocrm.services.dossier.workflow.AlertesStatutDossier;
import com.tsix.immocrm.services.patrimoine.relances.SelectionPremieresRelances.Relance;
import com.tsix.immocrm.services.patrimoine.relances.SelectionPremieresRelances.Selection;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.TierNomFormatter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** « Imprimer les premières relances » de la tranche (FEATURE-patrimoine-import-export.md §4.1). */
@Service
@Transactional(readOnly = true)
public class PremieresRelancesService {

    static final String PARAMETRE_RESERVATION = "DefaultComFolderStatusReservation";
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SelectionPremieresRelances selection;
    private final AlertesStatutDossier alertes;
    private final AdmParamsGuidRepository parametres;
    private final LettreRelance1 lettre;

    public PremieresRelancesService(SelectionPremieresRelances selection, AlertesStatutDossier alertes,
                                    AdmParamsGuidRepository parametres, LettreRelance1 lettre) {
        this.selection = selection;
        this.alertes = alertes;
        this.parametres = parametres;
        this.lettre = lettre;
    }

    public ApercuPremieresRelancesDto apercu(UUID trancheId) {
        Selection s = selection.selectionner(trancheId);
        return new ApercuPremieresRelancesDto(s.dateRelance().toLocalDate(), s.premierDelai(),
                s.echeance().toLocalDate(), s.relances().size(), (int) s.alertes(),
                s.premierDelai() == null ? "1er délai de la tranche absent : l'échéance est la date de relance." : null,
                s.relances().stream().map(PremieresRelancesService::ligne).toList());
    }

    /** Une transaction : toute erreur annule les dates et les alertes déjà posées. */
    @Transactional
    public ResultatPremieresRelancesDto executer(UUID trancheId) {
        Selection s = selection.selectionner(trancheId);
        UUID reservation = parametres.findById(PARAMETRE_RESERVATION).map(AdmParamsGuid::getCodeValue).orElse(null);
        String echeance = s.echeance().format(JOUR);
        for (Relance r : s.relances()) {
            ComFolder d = r.dossier();
            if (r.alerte()) {
                UUID statut = d.getStrStatus().getId();
                alertes.alerteSysteme(d, statut, Objects.equals(statut, reservation)
                                ? ActionsDossier.CATEGORIE_RESERVATION : ActionsDossier.CATEGORIE_LIVRAISON,
                        s.echeance(), "Titre foncier: Echéance de la notification de la 1ère relance du dossier de vente N° "
                                + d.getCode() + ", date échéance: " + echeance);
            }
            d.setRevival1(s.dateRelance());
            d.setRevival1ValidityDate(s.echeance());
        }
        return new ResultatPremieresRelancesDto(s.relances().size(), (int) s.alertes());
    }

    /** Dossiers de la sélection portant déjà {@code Revival1} ; aucun → 400. */
    public byte[] lettres(UUID trancheId) {
        List<ComFolder> relances = selection.selectionner(trancheId).relances().stream().map(Relance::dossier)
                .filter(d -> d.getRevival1() != null).toList();
        if (relances.isEmpty()) {
            throw new InvalidRequestException("Aucun dossier à imprimer");
        }
        return lettre.pdf(relances);
    }

    private static LigneApercuRelanceDto ligne(Relance r) {
        ComFolder d = r.dossier();
        LocalDateTime actuelle = d.getRevival1ValidityDate();
        return new LigneApercuRelanceDto(d.getId(), d.getCode(), Champs.de(d.getCfgCustomer(), TierNomFormatter::nom),
                Champs.de(d.getStkItem(), StkItem::getCode), Champs.de(d.getStrStatus(), StrStatus::getDescription),
                actuelle == null ? null : actuelle.toLocalDate(), r.alerte());
    }
}
