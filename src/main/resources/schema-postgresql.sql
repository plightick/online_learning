-- One-time compatibility migration for legacy columns:
-- instructors.name -> instructors.first_name + instructors.last_name
-- students.full_name -> students.first_name + students.last_name

DO $$
DECLARE
    single_first_name_unique_constraint RECORD;
BEGIN
    -- Drop legacy UNIQUE(first_name) constraint (remains after name -> first_name rename).
    -- Current model expects UNIQUE(first_name, last_name), so single-column uniqueness must be removed.
    FOR single_first_name_unique_constraint IN
        SELECT con.conname
        FROM pg_constraint con
                 JOIN pg_class rel ON rel.oid = con.conrelid
                 JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
        WHERE nsp.nspname = current_schema()
          AND rel.relname = 'instructors'
          AND con.contype = 'u'
          AND pg_get_constraintdef(con.oid) = 'UNIQUE (first_name)'
        LOOP
            EXECUTE format(
                    'ALTER TABLE instructors DROP CONSTRAINT %I',
                    single_first_name_unique_constraint.conname
                    );
        END LOOP;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'instructors'
          AND column_name = 'name'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'instructors'
          AND column_name = 'first_name'
    ) THEN
        ALTER TABLE instructors RENAME COLUMN name TO first_name;
    END IF;

    -- The unique constraint can survive rename(name -> first_name),
    -- so run the cleanup again after rename as well.
    FOR single_first_name_unique_constraint IN
        SELECT con.conname
        FROM pg_constraint con
                 JOIN pg_class rel ON rel.oid = con.conrelid
                 JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
        WHERE nsp.nspname = current_schema()
          AND rel.relname = 'instructors'
          AND con.contype = 'u'
          AND pg_get_constraintdef(con.oid) = 'UNIQUE (first_name)'
        LOOP
            EXECUTE format(
                    'ALTER TABLE instructors DROP CONSTRAINT %I',
                    single_first_name_unique_constraint.conname
                    );
        END LOOP;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'instructors'
          AND column_name = 'first_name'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'instructors'
          AND column_name = 'last_name'
    ) THEN
        ALTER TABLE instructors ADD COLUMN last_name VARCHAR(255);

        UPDATE instructors
        SET first_name = CASE
                             WHEN POSITION(' ' IN first_name) > 0
                                 THEN SPLIT_PART(first_name, ' ', 1)
                             ELSE first_name
            END,
            last_name = CASE
                            WHEN POSITION(' ' IN first_name) > 0
                                THEN NULLIF(TRIM(SUBSTRING(first_name FROM POSITION(' ' IN first_name) + 1)), '')
                            ELSE NULL
            END;

        UPDATE instructors
        SET last_name = 'Unknown'
        WHERE last_name IS NULL;

        ALTER TABLE instructors ALTER COLUMN last_name SET NOT NULL;
    END IF;

    -- If a previous failed run created last_name but did not backfill it, repair data and enforce NOT NULL.
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'instructors'
          AND column_name = 'last_name'
    ) THEN
        UPDATE instructors
        SET last_name = CASE
                            WHEN POSITION(' ' IN first_name) > 0
                                THEN COALESCE(NULLIF(TRIM(SUBSTRING(first_name FROM POSITION(' ' IN first_name) + 1)), ''), 'Unknown')
                            ELSE 'Unknown'
            END
        WHERE last_name IS NULL;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'students'
          AND column_name = 'full_name'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'students'
          AND column_name = 'first_name'
    ) THEN
        ALTER TABLE students RENAME COLUMN full_name TO first_name;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'students'
          AND column_name = 'first_name'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'students'
          AND column_name = 'last_name'
    ) THEN
        ALTER TABLE students ADD COLUMN last_name VARCHAR(255);

        UPDATE students
        SET first_name = CASE
                             WHEN POSITION(' ' IN first_name) > 0
                                 THEN SPLIT_PART(first_name, ' ', 1)
                             ELSE first_name
            END,
            last_name = CASE
                            WHEN POSITION(' ' IN first_name) > 0
                                THEN NULLIF(TRIM(SUBSTRING(first_name FROM POSITION(' ' IN first_name) + 1)), '')
                            ELSE NULL
            END;

        UPDATE students
        SET last_name = 'Unknown'
        WHERE last_name IS NULL;

        ALTER TABLE students ALTER COLUMN last_name SET NOT NULL;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'students'
          AND column_name = 'last_name'
    ) THEN
        UPDATE students
        SET last_name = CASE
                            WHEN POSITION(' ' IN first_name) > 0
                                THEN COALESCE(NULLIF(TRIM(SUBSTRING(first_name FROM POSITION(' ' IN first_name) + 1)), ''), 'Unknown')
                            ELSE 'Unknown'
            END
        WHERE last_name IS NULL;
    END IF;
END $$;
@@
