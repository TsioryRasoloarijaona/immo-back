package com.tsix.immocrm.entities.com;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Cadeau d'un dossier de vente ({@code Yes.Com.Sales.FolderGift}). */
@Getter
@Setter
@Entity
@Table(name = "ComFolderGift")
public class ComFolderGift {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ComFolderId", nullable = false)
    private ComFolder comFolder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComGiftId")
    private ComGift comGift;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComGiftReasonId")
    private ComGiftReason comGiftReason;

    // Cadeau au parrain d'une prospection (Sale.cs:AddGiftToProposer) : lu, jamais écrit ici.
    @Column(name = "ComProspectionId")
    private UUID comProspectionId;

    @Column(name = "Delivered")
    private Boolean delivered;

    @Column(name = "Price", precision = 26, scale = 6)
    private BigDecimal price;

    @Column(name = "IsAvailable")
    private Boolean isAvailable;

    @Column(name = "DeliveryDate")
    private LocalDateTime deliveryDate;

    @Column(name = "VoucherDate")
    private LocalDateTime voucherDate;

    @Size(max = 500)
    @Nationalized
    @Column(name = "Notes", length = 500)
    private String notes;

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
}
