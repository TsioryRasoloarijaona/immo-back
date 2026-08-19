package com.tsix.immocrm.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code ?sort=} naming a DTO field instead of the real JPA entity property (e.g.
 * {@code dateDocument} — the JSON field — instead of {@code ComFolder.docDate}) must return a
 * clean 400, not an unhandled 500 (bug report: "PropertyReferenceException: No property
 * 'dateDocument' found for type 'ComFolder'").
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GlobalExceptionHandlerSortTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getDossiers_invalidSortProperty_returns400NotServerError() throws Exception {
        mockMvc.perform(get("/api/dossiers").param("sort", "dateDocument,desc"))
                .andExpect(status().isBadRequest());
    }
}
