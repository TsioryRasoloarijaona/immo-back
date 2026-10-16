package com.tsix.immocrm.repositories.app;

import com.tsix.immocrm.entities.app.AppModeleImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AppModeleImageRepository extends JpaRepository<AppModeleImage, UUID> {
}
