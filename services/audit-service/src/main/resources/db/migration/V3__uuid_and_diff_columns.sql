CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Keep historical rows if possible; rebuild only id type for empty friendliness on local.
ALTER TABLE audit_events ADD COLUMN IF NOT EXISTS before_data TEXT;
ALTER TABLE audit_events ADD COLUMN IF NOT EXISTS after_data TEXT;

-- Convert PK to UUID when still bigint (local docker after prior migrations)
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'audit_events' AND column_name = 'id' AND data_type = 'bigint'
  ) THEN
    ALTER TABLE audit_events ADD COLUMN id_uuid UUID DEFAULT gen_random_uuid();
    UPDATE audit_events SET id_uuid = gen_random_uuid() WHERE id_uuid IS NULL;
    ALTER TABLE audit_events DROP CONSTRAINT IF EXISTS audit_events_pkey;
    ALTER TABLE audit_events DROP COLUMN id;
    ALTER TABLE audit_events RENAME COLUMN id_uuid TO id;
    ALTER TABLE audit_events ALTER COLUMN id SET NOT NULL;
    ALTER TABLE audit_events ALTER COLUMN id SET DEFAULT gen_random_uuid();
    ALTER TABLE audit_events ADD PRIMARY KEY (id);
  END IF;
END $$;
