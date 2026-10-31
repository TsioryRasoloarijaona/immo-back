package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmWebCultures;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdmWebCulturesRepository extends JpaRepository<AdmWebCultures, UUID> {
}
