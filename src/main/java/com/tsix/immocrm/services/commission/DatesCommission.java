package com.tsix.immocrm.services.commission;

import com.tsix.immocrm.domain.commission.TypeCommission;
import com.tsix.immocrm.entities.cfg.CfgTrancheFolderEntity;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.str.StrChampObligatoireRepository;
import com.tsix.immocrm.services.dossier.workflow.DateAssociee;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * Date du fait d'une commission (R-F) : concrétisation = {@code AdvanceDate}, désistement = {@code CancelDate},
 * finalisation = colonne de {@code ComFolder} nommée par la date associée du statut configuré dans la tranche.
 */
@Component
public class DatesCommission {

    private static final String CONFIGURATION = """
            SELECT e FROM CfgTrancheFolderEntity e LEFT JOIN FETCH e.strStatusDelivery1 LEFT JOIN FETCH e.strStatusDelivery2
            WHERE e.cfgTranche.id = :tranche ORDER BY e.userCreateDateTime ASC, e.id ASC""";

    /** Colonnes de date des deux finalisations ; {@code null} et un message OMRAN si non configurée. */
    public record Finalisations(String colonne1, String erreur1, String colonne2, String erreur2) {

        /** Colonne de la finalisation du type, sinon 400 avec le message OMRAN (rien n'est écrit). */
        public String exiger(TypeCommission type) {
            String erreur = type == TypeCommission.FINALISATION_1 ? erreur1 : erreur2;
            if (erreur != null) {
                throw new InvalidRequestException(erreur);
            }
            return type == TypeCommission.FINALISATION_1 ? colonne1 : colonne2;
        }
    }

    private final EntityManager em;
    private final StrChampObligatoireRepository champs;
    private final DateAssociee dateAssociee;

    public DatesCommission(EntityManager em, StrChampObligatoireRepository champs, DateAssociee dateAssociee) {
        this.em = em;
        this.champs = champs;
        this.dateAssociee = dateAssociee;
    }

    public Finalisations finalisations(UUID trancheId) {
        List<CfgTrancheFolderEntity> lignes = em.createQuery(CONFIGURATION, CfgTrancheFolderEntity.class)
                .setParameter("tranche", trancheId).getResultList();
        String[] f1 = finalisation(lignes, CfgTrancheFolderEntity::getStrStatusDelivery1, 1);
        String[] f2 = finalisation(lignes, CfgTrancheFolderEntity::getStrStatusDelivery2, 2);
        return new Finalisations(f1[0], f1[1], f2[0], f2[1]);
    }

    /** [colonne, erreur] : l'une des deux est nulle. */
    private String[] finalisation(List<CfgTrancheFolderEntity> lignes, Function<CfgTrancheFolderEntity, StrStatus> statut,
                                  int numero) {
        StrStatus s = lignes.stream().map(statut).filter(Objects::nonNull).findFirst().orElse(null);
        if (s == null) {
            return new String[] {null, ("Il faut faire le paramétrage du statut de la Finalisation %d dans la tranche et "
                    + "s'assurer que ce statut est bien associé à une date !").formatted(numero)};
        }
        String colonne = champs.champDateDuStatut(s.getId()).stream().map(r -> (String) r[0]).filter(Objects::nonNull)
                .findFirst().orElse(null);
        return colonne != null ? new String[] {colonne, null} : new String[] {null,
                "Il faut s'assurer que le statut de la Finalisation %d est bien associé à une date !".formatted(numero)};
    }

    /** Date du fait du type pour ce dossier ; {@code colonne} = colonne de la finalisation du type (sinon ignorée). */
    public LocalDateTime date(ComFolder f, TypeCommission type, String colonne) {
        return switch (type) {
            case CONCRETISATION -> f.getAdvanceDate();
            case DESISTEMENT -> f.getCancelDate();
            case FINALISATION_1, FINALISATION_2 -> colonne == null ? null : dateAssociee.valeurActuelle(f, colonne);
        };
    }
}
