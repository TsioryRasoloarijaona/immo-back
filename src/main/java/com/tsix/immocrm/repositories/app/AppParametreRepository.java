package com.tsix.immocrm.repositories.app;

import com.tsix.immocrm.entities.app.AppParametre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppParametreRepository extends JpaRepository<AppParametre, UUID> {

    Optional<AppParametre> findByCle(String cle);
}
