-- DESTRUCTIVE: permanently deletes the whole lms_db database (all users, courses, submissions...).
-- Use only when you really want a clean slate, then run database/lms.sql to recreate everything:
--   mysql -u root -p < database/reset_database.sql
--   mysql -u root -p < database/lms.sql
DROP DATABASE IF EXISTS lms_db;
