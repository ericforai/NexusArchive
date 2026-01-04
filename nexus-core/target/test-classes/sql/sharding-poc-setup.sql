CREATE TABLE IF NOT EXISTS arc_account_item (
  fonds_no VARCHAR(32) NOT NULL,
  fiscal_year INTEGER NOT NULL,
  item_id VARCHAR(64) NOT NULL,
  title VARCHAR(255) NOT NULL,
  created_time TIMESTAMP NOT NULL,
  PRIMARY KEY (fonds_no, fiscal_year, item_id)
);
