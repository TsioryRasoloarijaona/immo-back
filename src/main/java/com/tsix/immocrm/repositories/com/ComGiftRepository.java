package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComGift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ComGiftRepository extends JpaRepository<ComGift, UUID> {
}
