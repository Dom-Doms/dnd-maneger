package it.univ.ddmanager.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TestDatabaseCleanup {

    private final JdbcTemplate jdbcTemplate;

    public TestDatabaseCleanup(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void truncateAll() {
        jdbcTemplate.execute(
            "TRUNCATE TABLE session_chat_messages, campaign_players, player_character_languages, player_characters, "
                + "session_events, sessions, campaigns, items, locations, npcs, worlds, users, users_roles "
                + "RESTART IDENTITY CASCADE");
    }
}
