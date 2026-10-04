package com.kauan.gamelog.lists;

import com.kauan.gamelog.catalog.dto.GameSummaryDTO;
import com.kauan.gamelog.lists.dto.ListDTO;
import com.kauan.gamelog.lists.dto.ListForm;
import com.kauan.gamelog.lists.dto.ListItemsRequest;
import com.kauan.gamelog.lists.dto.ListSummaryDTO;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** As tabelas {@code user_lists} e {@code user_list_items}. Toda consulta filtra pela dona da lista. */
@Repository
class UserLists {
    private static final String GAME =
            "g.id AS game_id, g.slug, g.title AS game_title, g.cover_image_id, g.release_date";

    private final JdbcClient jdbc;

    UserLists(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    long create(long userId, ListForm form) {
        return jdbc.sql("""
                        INSERT INTO user_lists (user_id, title, description, visibility)
                        VALUES (:userId, :title, :description, :visibility) RETURNING id
                        """)
                .param("userId", userId)
                .param("title", form.title())
                .param("description", form.description())
                .param("visibility", form.visibility().name())
                .query(Long.class)
                .single();
    }

    boolean owns(long listId, long userId) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM user_lists WHERE id = :listId AND user_id = :userId)")
                .param("listId", listId)
                .param("userId", userId)
                .query(Boolean.class)
                .single();
    }

    void updateMeta(long listId, ListForm form) {
        jdbc.sql("""
                        UPDATE user_lists SET title = :title, description = :description, visibility = :visibility,
                                              updated_at = now()
                        WHERE id = :listId
                        """)
                .param("title", form.title())
                .param("description", form.description())
                .param("visibility", form.visibility().name())
                .param("listId", listId)
                .update();
    }

    /** Troca os itens de uma vez; a posição segue a ordem recebida. */
    void replaceItems(long listId, List<ListItemsRequest.Item> items) {
        jdbc.sql("DELETE FROM user_list_items WHERE list_id = :listId")
                .param("listId", listId)
                .update();
        for (int i = 0; i < items.size(); i++) {
            jdbc.sql("""
                            INSERT INTO user_list_items (list_id, game_id, position, note)
                            VALUES (:listId, :gameId, :position, :note)
                            """)
                    .param("listId", listId)
                    .param("gameId", items.get(i).gameId())
                    .param("position", i + 1)
                    .param("note", items.get(i).note())
                    .update();
        }
        jdbc.sql("UPDATE user_lists SET updated_at = now() WHERE id = :listId")
                .param("listId", listId)
                .update();
    }

    boolean delete(long listId, long userId) {
        return jdbc.sql("DELETE FROM user_lists WHERE id = :listId AND user_id = :userId")
                        .param("listId", listId)
                        .param("userId", userId)
                        .update()
                == 1;
    }

    private record Header(
            long id,
            String title,
            String description,
            ListVisibility visibility,
            ListDTO.Owner owner,
            Instant createdAt,
            Instant updatedAt) {}

    /** @param onlyPublic para quem visita: as privadas ficam de fora */
    Optional<ListDTO> find(long listId, long userId, boolean onlyPublic) {
        return jdbc.sql("""
                        SELECT l.id, l.title, l.description, l.visibility, l.created_at, l.updated_at,
                               u.username, u.display_name
                        FROM user_lists l JOIN users u ON u.id = l.user_id
                        WHERE l.id = :listId AND l.user_id = :userId
                        """ + (onlyPublic ? " AND l.visibility = 'PUBLIC'" : ""))
                .param("listId", listId)
                .param("userId", userId)
                .query((rs, row) -> new Header(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        ListVisibility.valueOf(rs.getString("visibility")),
                        new ListDTO.Owner(rs.getString("username"), rs.getString("display_name")),
                        instant(rs, "created_at"),
                        instant(rs, "updated_at")))
                .optional()
                .map(list -> new ListDTO(
                        list.id(),
                        list.title(),
                        list.description(),
                        list.visibility(),
                        list.owner(),
                        items(list.id()),
                        list.createdAt(),
                        list.updatedAt()));
    }

    private record Row(
            long id, String title, String description, ListVisibility visibility, long items, Instant updatedAt) {}

    /** As listas mexidas por último primeiro, com a contagem e os quatro primeiros jogos de cada. */
    Page<ListSummaryDTO> summaries(long userId, boolean onlyPublic, Pageable pageable) {
        String where = " WHERE l.user_id = :userId" + (onlyPublic ? " AND l.visibility = 'PUBLIC'" : "");
        long total = jdbc.sql("SELECT count(*) FROM user_lists l" + where)
                .param("userId", userId)
                .query(Long.class)
                .single();
        List<Row> rows = jdbc.sql("""
                        SELECT l.id, l.title, l.description, l.visibility, l.updated_at,
                               (SELECT count(*) FROM user_list_items i WHERE i.list_id = l.id) AS items
                        FROM user_lists l
                        """ + where + " ORDER BY l.updated_at DESC, l.id DESC LIMIT :limit OFFSET :offset")
                .param("userId", userId)
                .param("limit", pageable.getPageSize())
                .param("offset", pageable.getOffset())
                .query((rs, row) -> new Row(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        ListVisibility.valueOf(rs.getString("visibility")),
                        rs.getLong("items"),
                        instant(rs, "updated_at")))
                .list();
        Map<Long, List<GameSummaryDTO>> previews =
                previews(rows.stream().map(Row::id).toList());
        List<ListSummaryDTO> content = rows.stream()
                .map(row -> new ListSummaryDTO(
                        row.id(),
                        row.title(),
                        row.description(),
                        row.visibility(),
                        row.items(),
                        previews.getOrDefault(row.id(), List.of()),
                        row.updatedAt()))
                .toList();
        return new PageImpl<>(content, pageable, total);
    }

    private List<ListDTO.Item> items(long listId) {
        return jdbc.sql("SELECT i.position, i.note, " + GAME + """
                         FROM user_list_items i JOIN games g ON g.id = i.game_id
                        WHERE i.list_id = :listId ORDER BY i.position
                        """)
                .param("listId", listId)
                .query((rs, row) -> new ListDTO.Item(rs.getInt("position"), game(rs), rs.getString("note")))
                .list();
    }

    private Map<Long, List<GameSummaryDTO>> previews(List<Long> listIds) {
        Map<Long, List<GameSummaryDTO>> previews = new HashMap<>();
        if (!listIds.isEmpty()) {
            jdbc.sql("SELECT i.list_id, " + GAME + """
                             FROM user_list_items i JOIN games g ON g.id = i.game_id
                            WHERE i.list_id IN (:listIds) AND i.position <= 4 ORDER BY i.list_id, i.position
                            """)
                    .param("listIds", listIds)
                    .query((RowCallbackHandler)
                            rs -> previews.computeIfAbsent(rs.getLong("list_id"), id -> new ArrayList<>())
                                    .add(game(rs)));
        }
        return previews;
    }

    private static GameSummaryDTO game(ResultSet rs) throws SQLException {
        return GameSummaryDTO.of(
                rs.getLong("game_id"),
                rs.getString("slug"),
                rs.getString("game_title"),
                rs.getString("cover_image_id"),
                rs.getObject("release_date", LocalDate.class));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, OffsetDateTime.class).toInstant();
    }
}
