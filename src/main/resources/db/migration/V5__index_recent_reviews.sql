-- GET /reviews: avaliações recentes do site todo.
CREATE INDEX library_entries_recent_reviews_idx ON library_entries (reviewed_at DESC) WHERE review_text IS NOT NULL;
