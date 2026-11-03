package com.tsix.immocrm.entities.act;

import com.tsix.immocrm.entities.CodeLibelleOmran;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Journal comptable (« Journal » des banques et organismes). */
@Entity
@Table(name = "ActJournal")
public class ActJournal extends CodeLibelleOmran {
}
