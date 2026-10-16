package com.tsix.immocrm.entities.com;

import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "ComFolderItem")
public class ComFolderItem {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComFolderId")
    private ComFolder comFolder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkItemId")
    private StkItem stkItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkItemTypeId")
    private StkItemType stkItemType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkItemCategoryId")
    private StkItemCategory stkItemCategory;

    @Column(name = "ParentId")
    private UUID parentId;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;

    @Size(max = 500)
    @Nationalized
    @Column(name = "Address", length = 500)
    private String address;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes", length = 1000)
    private String notes;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Contenance", length = 1000)
    private String contenance;

    @Column(name = "AreaNet", precision = 26, scale = 6)
    private BigDecimal areaNet;

    @Column(name = "AreaFloor", precision = 26, scale = 6)
    private BigDecimal areaFloor;

    @Column(name = "Area", precision = 26, scale = 6)
    private BigDecimal area;

    @Column(name = "PriceBeforeDiscount", precision = 26, scale = 6)
    private BigDecimal priceBeforeDiscount;

    @Column(name = "Discount", precision = 26, scale = 6)
    private BigDecimal discount;

    @Column(name = "Price", precision = 26, scale = 6)
    private BigDecimal price;

    @Column(name = "AlwaysAffected")
    private Boolean alwaysAffected;

    @Column(name = "IsUnderItem")
    private Boolean isUnderItem;

    @Size(max = 255)
    @Nationalized
    @Column(name = "UserCreate")
    private String userCreate;

    @Column(name = "UserCreateDateTime")
    private LocalDateTime userCreateDateTime;

    @Size(max = 255)
    @Nationalized
    @Column(name = "UserUpdate")
    private String userUpdate;

    @Column(name = "UserUpdateDateTime")
    private LocalDateTime userUpdateDateTime;

    @Column(name = "IsUnderItemOrigin")
    private Boolean isUnderItemOrigin;

    @Column(name = "PriceBeforeDiscountHT", precision = 26, scale = 6)
    private BigDecimal priceBeforeDiscountHT;

    @Column(name = "PriceHT", precision = 26, scale = 6)
    private BigDecimal priceHT;

    @Column(name = "VATRatio", precision = 26, scale = 6)
    private BigDecimal vATRatio;

    @Column(name = "IsTitled")
    private Boolean isTitled;

    @Column(name = "StkBuildingId")
    private UUID stkBuildingId;

    @Column(name = "StkFloorId")
    private UUID stkFloorId;

}