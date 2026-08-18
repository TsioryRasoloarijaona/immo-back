package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComPaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ComPaymentMethodRepository extends JpaRepository<ComPaymentMethod, UUID> {
}
