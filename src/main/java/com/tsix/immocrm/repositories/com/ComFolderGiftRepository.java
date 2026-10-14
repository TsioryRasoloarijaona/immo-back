package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComFolderGift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ComFolderGiftRepository extends JpaRepository<ComFolderGift, UUID> {

    List<ComFolderGift> findByComFolderIdOrderByUserCreateDateTimeAsc(UUID comFolderId);

    /**
     * Cadeaux encore en stock ({@code Gift.cs:QuantityInStock}) : quantités reçues ({@code ComGiftQuantity}) moins
     * cadeaux livrés, tous dossiers confondus. Tuple : [ComGiftId, stock].
     */
    @Query(value = """
            SELECT CAST(g.PKey AS nvarchar(36)),
                   ISNULL((SELECT SUM(q.Quantity) FROM ComGiftQuantity q WHERE q.ComGiftId = g.PKey), 0)
                 - (SELECT COUNT(*) FROM ComFolderGift f WHERE f.Delivered = 1 AND f.ComGiftId = g.PKey)
            FROM ComGift g""", nativeQuery = true)
    List<Object[]> stockParCadeau();
}
