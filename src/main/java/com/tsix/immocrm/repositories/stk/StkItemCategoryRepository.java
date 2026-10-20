package com.tsix.immocrm.repositories.stk;

import com.tsix.immocrm.entities.stk.StkItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StkItemCategoryRepository extends JpaRepository<StkItemCategory, UUID> {

    List<StkItemCategory> findByStkKind_Code(String natureCode);

    List<StkItemCategory> findByStkKind_CodeAndForUnderItem(String natureCode, Boolean pourSousArticle);
}
