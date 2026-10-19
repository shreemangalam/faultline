-- Reproduce the non-empty shared schema before Survey owns a history table.
CREATE TABLE restore_startup_evidence (id INTEGER PRIMARY KEY);
INSERT INTO restore_startup_evidence (id) VALUES (1);
