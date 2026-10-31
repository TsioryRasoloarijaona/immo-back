package com.tsix.immocrm.repositories.stl;

import com.tsix.immocrm.entities.stl.StlDepositUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StlDepositUserRepository extends JpaRepository<StlDepositUser, UUID> {

    List<StlDepositUser> findByAdmUserId(UUID admUserId);
}
