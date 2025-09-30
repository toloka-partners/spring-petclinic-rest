-- Flyway migration: add weight column to pets table
ALTER TABLE pets
ADD COLUMN weight DOUBLE;