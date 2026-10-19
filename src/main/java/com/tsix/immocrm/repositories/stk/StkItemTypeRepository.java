package com.tsix.immocrm.repositories.stk;

import com.tsix.immocrm.entities.stk.StkItemType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StkItemTypeRepository extends JpaRepository<StkItemType, UUID> {

    List<StkItemType> findByStkKind_CodeAndForUnderItem(String natureCode, Boolean pourSousArticle);
}
