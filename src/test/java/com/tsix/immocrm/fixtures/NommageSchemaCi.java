package com.tsix.immocrm.fixtures;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Règle de nommage de {@code ci/schema/README.md} : {@code NNN_<lot>_tables.sql} puis {@code NNN+1_<lot>_fk.sql}, NNN pair. */
public final class NommageSchemaCi {

    private static final Path SCHEMA_CI = Path.of("ci", "schema");

    private NommageSchemaCi() {
    }

    public static void verifier(int numero, String lot) throws IOException {
        assertThat(numero % 2).as("NNN pair").isZero();
        assertThat(SCHEMA_CI.resolve("%03d_%s_tables.sql".formatted(numero, lot))).isRegularFile();
        assertThat(SCHEMA_CI.resolve("%03d_%s_fk.sql".formatted(numero + 1, lot))).isRegularFile();
        assertThat(Files.readString(SCHEMA_CI.resolve("README.md")))
                .contains("`NNN_<lot>_tables.sql`", "`NNN+1_<lot>_fk.sql`");
    }
}
