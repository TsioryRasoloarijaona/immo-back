package com.tsix.immocrm.repositories.str;

import com.tsix.immocrm.entities.str.StrField;
import jakarta.persistence.Tuple;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Champs obligatoires d'une transition ({@code StrWorkFlowField}) ou d'un statut ({@code StrStatusMandatoryField}).
 * Tuple : [StrField.Code, StrField.Description, StrField.StrConfigEntityId, lien (StrEntityLinkedEntity, peut être nul)].
 */
public interface StrChampObligatoireRepository extends JpaRepository<StrField, UUID> {

    @Query("SELECT DISTINCT f.code, f.description, f.strConfigEntityId, l FROM StrWorkFlowField wf "
            + "JOIN StrField f ON f.id = wf.strFieldId "
            + "LEFT JOIN StrEntityLinkedEntity l ON l.id = wf.strEntityLinkedEntityId "
            + "WHERE wf.strWorkFlowId = :transition")
    List<Object[]> deTransition(UUID transition);

    @Query("SELECT DISTINCT f.code, f.description, f.strConfigEntityId, l FROM StrStatusMandatoryField sf "
            + "JOIN StrField f ON f.id = sf.strFieldId "
            + "LEFT JOIN StrEntityLinkedEntity l ON l.id = sf.strEntityLinkedEntityId "
            + "WHERE sf.strStatusId = :statut")
    List<Object[]> deStatut(UUID statut);

    /** « Date associée » d'un statut ({@code StrStatus.StrFieldDateId}) ; tuple [Code, Description]. */
    @Query("SELECT f.code, f.description FROM StrStatus s JOIN StrField f ON f.id = s.strFieldDateId WHERE s.id = :statut")
    List<Object[]> champDateDuStatut(UUID statut);

    /** 1 si la décision MEILLEURTAUX est renseignée (colonne non mappée sur l'entité dossier). */
    @Query(value = "SELECT CASE WHEN ComLoanCompanyDecisionId IS NULL THEN 0 ELSE 1 END FROM ComFolder "
            + "WHERE PKey = :dossier", nativeQuery = true)
    List<Integer> decisionMeilleurtauxRenseignee(@Param("dossier") UUID dossier);

    /**
     * Champs de workflow d'un tiers ({@code Yes.Cfg/CfgTier.cs}), lus par leur nom de colonne = {@code StrField.Code},
     * avec la forme sociale (personne physique) et le drapeau « avec salaire » de la profession.
     */
    @Query(value = """
            SELECT t.Address, t.AdmCityId, t.AdmCountryId, t.AdmFamilySituationId, t.AdmIdentityDocumentTypeId,
                   t.AdmMatrimonialRegimeId, t.AdmNationalityId, t.AdmProfessionId, t.AdmResidenceTypeId, t.AdmSalaryId,
                   t.Antiquity, t.Asset, t.BankAgency, t.BirthDate, t.BirthPlace, t.CfgBankId, t.CfgCommercialId,
                   t.CfgContactId, t.CfgTierKindId, t.CodeCountable, t.CodeCountableCollective, t.EMail, t.Employer,
                   t.FatherName, t.Fax, t.FaxIndicatif, t.FiscalDouane, t.Iban, t.IDDate, t.IDNumber, t.IDPlace,
                   t.IDValidatePlace, t.MotherName, t.NbrChildren, t.Phone, t.Phone1, t.Phone1Indicatif, t.Phone2,
                   t.PhoneIndicatif, t.Quality, t.Rib, t.WebSite, t.ZipCode,
                   sf.IsPerson AS EstPersonne, p.IsWithSalary AS AvecSalaire, t.AdmProfessionId AS ProfessionId
            FROM CfgTier t
            LEFT JOIN AdmSocialForm sf ON sf.PKey = t.AdmSocialFormId
            LEFT JOIN AdmProfession p ON p.PKey = t.AdmProfessionId
            WHERE t.PKey = :tiers""", nativeQuery = true)
    List<Tuple> tiers(@Param("tiers") UUID tiers);
}
