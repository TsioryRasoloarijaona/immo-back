package com.tsix.immocrm.repositories.stk;

import com.tsix.immocrm.entities.stk.StkVocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StkVocationRepository extends JpaRepository<StkVocation, UUID> {
}
