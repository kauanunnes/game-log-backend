package com.kauan.gamelog.catalog;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "games")
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String slug;
    private String title;
    private String summary;
    private LocalDate releaseDate;
    private String coverImageId;

    @Enumerated(EnumType.STRING)
    private GameKind kind;

    @JdbcTypeCode(SqlTypes.JSON)
    private GameMetadata metadata;

    private BigDecimal igdbRating;
    private Integer igdbRatingCount;

    @ManyToMany
    @JoinTable(
            name = "game_genres",
            joinColumns = @JoinColumn(name = "game_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id"))
    private Set<Genre> genres = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "game_platforms",
            joinColumns = @JoinColumn(name = "game_id"),
            inverseJoinColumns = @JoinColumn(name = "platform_id"))
    private Set<Platform> platforms = new HashSet<>();

    protected Game() {}

    public Long getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public String getCoverImageId() {
        return coverImageId;
    }

    public GameKind getKind() {
        return kind;
    }

    public GameMetadata getMetadata() {
        return metadata;
    }

    public BigDecimal getIgdbRating() {
        return igdbRating;
    }

    public Integer getIgdbRatingCount() {
        return igdbRatingCount;
    }

    public Set<Genre> getGenres() {
        return genres;
    }

    public Set<Platform> getPlatforms() {
        return platforms;
    }
}
