package com.tsix.immocrm.repositories.stl;

import com.tsix.immocrm.entities.stl.StlCurrency;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

/** {@link Repository} et non {@code JpaRepository} : référentiel du légataire, jamais écrit par nous. */
public interface StlCurrencyRepository extends Repository<StlCurrency, UUID> {

    List<StlCurrency> findByCode(String code);
}
