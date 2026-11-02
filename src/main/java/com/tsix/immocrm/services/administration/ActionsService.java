package com.tsix.immocrm.services.administration;

import com.tsix.immocrm.dto.administration.LigneActionDto;
import com.tsix.immocrm.dto.common.PageDto;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;

/** Journal des actions en lecture seule, FEATURE-admin-actions.md. SQL natif : une entité fondrait les PKey en double. */
@Service
public class ActionsService {

    private static final String FILTRES = """
            FROM AdmActionsView
            WHERE (:motif IS NULL
                   OR REPLACE(Code, ' ', '') LIKE :motif
                   OR REPLACE(Description, ' ', '') LIKE :motif
                   OR REPLACE(ActionLabel, ' ', '') LIKE :motif
                   OR REPLACE(UserCreate, ' ', '') LIKE :motif
                   OR REPLACE(UserUpdate, ' ', '') LIKE :motif)
              AND (:creeDepuis IS NULL OR UserCreateDateTime >= :creeDepuis)
              AND (:creeAvant IS NULL OR UserCreateDateTime < :creeAvant)
              AND (:modifieDepuis IS NULL OR UserUpdateDateTime >= :modifieDepuis)
              AND (:modifieAvant IS NULL OR UserUpdateDateTime < :modifieAvant)
            """;
    private static final String LIGNES = """
            SELECT PKey, Code, Description, ActionLabel, UserCreateDateTime, UserCreate, UserUpdateDateTime, UserUpdate
            """ + FILTRES + """
            ORDER BY CASE WHEN UserCreateDateTime IS NULL THEN 1 ELSE 0 END, UserCreateDateTime DESC, PKey ASC, ActionLabel ASC
            OFFSET :decalage ROWS FETCH NEXT :taille ROWS ONLY
            """;
    private static final String TOTAL = "SELECT COUNT_BIG(*) " + FILTRES;

    private final NamedParameterJdbcTemplate jdbc;

    public ActionsService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public PageDto<LigneActionDto> lister(FiltresActions filtres, int page, int size) {
        filtres.verifierPeriodes();
        PageRequest pageable = PageRequest.of(page, size);
        MapSqlParameterSource parametres = parametres(filtres)
                .addValue("decalage", pageable.getOffset())
                .addValue("taille", size);
        List<LigneActionDto> lignes = jdbc.query(LIGNES, parametres, ActionsService::ligne);
        Long total = jdbc.queryForObject(TOTAL, parametres, Long.class);
        return PageDto.of(new PageImpl<>(lignes, pageable, total == null ? 0 : total));
    }

    // Types explicites : une borne absente reste un NULL typé, sans deviner le type côté pilote.
    private static MapSqlParameterSource parametres(FiltresActions filtres) {
        return new MapSqlParameterSource()
                .addValue("motif", filtres.motif(), Types.NVARCHAR)
                .addValue("creeDepuis", FiltresActions.debut(filtres.creationDebut()), Types.TIMESTAMP)
                .addValue("creeAvant", FiltresActions.finExclue(filtres.creationFin()), Types.TIMESTAMP)
                .addValue("modifieDepuis", FiltresActions.debut(filtres.modificationDebut()), Types.TIMESTAMP)
                .addValue("modifieAvant", FiltresActions.finExclue(filtres.modificationFin()), Types.TIMESTAMP);
    }

    private static LigneActionDto ligne(ResultSet rs, int rang) throws SQLException {
        String action = rs.getString("ActionLabel");
        return new LigneActionDto(rs.getString("PKey") + ":" + action, rs.getString("Code"), action,
                rs.getString("Description"), rs.getString("UserCreate"),
                rs.getObject("UserCreateDateTime", LocalDateTime.class), rs.getString("UserUpdate"),
                rs.getObject("UserUpdateDateTime", LocalDateTime.class));
    }
}
