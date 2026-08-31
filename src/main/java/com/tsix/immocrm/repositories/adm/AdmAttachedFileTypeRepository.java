package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmAttachedFileType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdmAttachedFileTypeRepository extends JpaRepository<AdmAttachedFileType, UUID> {
}
