package it.univ.ddmanager.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSchemaAdjuster {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensurePlayerCharacterWorldIsNullable() {
        try {
            Boolean isNotNullable = jdbcTemplate.queryForObject(
                "SELECT is_nullable = 'NO' FROM information_schema.columns "
                    + "WHERE table_name = 'player_characters' AND column_name = 'world_id'",
                Boolean.class);
            if (Boolean.TRUE.equals(isNotNullable)) {
                jdbcTemplate.execute("ALTER TABLE player_characters ALTER COLUMN world_id DROP NOT NULL");
                log.info("Adjusted player_characters.world_id column to allow null values");
            }
        } catch (DataAccessException ex) {
            log.debug("Skipping player_characters.world_id adjustment: {}", ex.getMessage());
        }
    }
}
