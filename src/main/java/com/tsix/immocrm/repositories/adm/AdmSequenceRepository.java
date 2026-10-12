package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmSequence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdmSequenceRepository extends JpaRepository<AdmSequence, UUID> {

    /** Compteurs globaux seulement : les codes par société (« Facture », « Avoir »…) se répètent d'une société à l'autre. */
    Optional<AdmSequence> findByCode(String code);

    /** Les 4 compteurs créés avec une entité juridique ({@code CfgCompany.AfterInsert}). */
    List<AdmSequence> findByCfgCompanyId(UUID entiteId);
}
