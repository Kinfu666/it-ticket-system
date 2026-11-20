CREATE TABLE IF NOT EXISTS departments (
 id INTEGER PRIMARY KEY, name TEXT NOT NULL UNIQUE, enabled INTEGER NOT NULL DEFAULT 1 CHECK(enabled IN (0,1)));
CREATE TABLE IF NOT EXISTS users (
 id INTEGER PRIMARY KEY, username TEXT NOT NULL UNIQUE, password TEXT NOT NULL, full_name TEXT NOT NULL,
 role TEXT NOT NULL CHECK(role IN ('ADMIN','ENGINEER','EMPLOYEE')), department_id INTEGER NOT NULL REFERENCES departments(id) ON DELETE RESTRICT,
 enabled INTEGER NOT NULL DEFAULT 1 CHECK(enabled IN (0,1)), created_at TEXT NOT NULL);
CREATE TABLE IF NOT EXISTS categories (
 id INTEGER PRIMARY KEY, name TEXT NOT NULL UNIQUE, description TEXT, enabled INTEGER NOT NULL DEFAULT 1 CHECK(enabled IN (0,1)));
CREATE TABLE IF NOT EXISTS tickets (
 id INTEGER PRIMARY KEY, ticket_no TEXT NOT NULL UNIQUE, title TEXT NOT NULL, description TEXT NOT NULL, location TEXT NOT NULL,
 category_id INTEGER NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
 priority TEXT NOT NULL CHECK(priority IN ('LOW','MEDIUM','HIGH')),
 status TEXT NOT NULL CHECK(status IN ('PENDING','ASSIGNED','IN_PROGRESS','RESOLVED','CLOSED','CANCELLED')),
 requester_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
 engineer_id INTEGER REFERENCES users(id) ON DELETE RESTRICT,
 created_at TEXT NOT NULL, updated_at TEXT NOT NULL, closed_at TEXT, return_reason TEXT, version INTEGER NOT NULL DEFAULT 0);
CREATE TABLE IF NOT EXISTS repair_records (
 id INTEGER PRIMARY KEY, ticket_id INTEGER NOT NULL REFERENCES tickets(id) ON DELETE RESTRICT,
 engineer_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
 cause TEXT NOT NULL, solution TEXT NOT NULL, result TEXT NOT NULL, created_at TEXT NOT NULL);
CREATE INDEX IF NOT EXISTS idx_tickets_requester ON tickets(requester_id);
CREATE INDEX IF NOT EXISTS idx_tickets_engineer ON tickets(engineer_id);
CREATE INDEX IF NOT EXISTS idx_tickets_category ON tickets(category_id);
CREATE INDEX IF NOT EXISTS idx_tickets_status ON tickets(status);
CREATE INDEX IF NOT EXISTS idx_tickets_created ON tickets(created_at);
CREATE INDEX IF NOT EXISTS idx_repairs_ticket ON repair_records(ticket_id);
