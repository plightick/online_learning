-- One-time compatibility migration for legacy columns:
-- instructors.name -> instructors.first_name + instructors.last_name
-- students.full_name -> students.first_name + students.last_name

DO $$
DECLARE
    single_first_name_unique_constraint RECORD;
    current_schema_name TEXT := current_schema();
    instructors_table_name CONSTANT TEXT := 'instructors';
    students_table_name CONSTANT TEXT := 'students';
    first_name_column CONSTANT TEXT := 'first_name';
    last_name_column CONSTANT TEXT := 'last_name';
    legacy_name_column CONSTANT TEXT := 'name';
    legacy_full_name_column CONSTANT TEXT := 'full_name';
    unknown_last_name CONSTANT TEXT := 'Unknown';
    space_delimiter CONSTANT TEXT := ' ';
BEGIN
    -- Drop legacy UNIQUE(first_name) constraint (remains after name -> first_name rename).
    -- Current model expects UNIQUE(first_name, last_name), so single-column uniqueness must be removed.
    FOR single_first_name_unique_constraint IN
        SELECT con.conname
        FROM pg_constraint con
                 JOIN pg_class rel ON rel.oid = con.conrelid
                 JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
        WHERE nsp.nspname = current_schema_name
          AND rel.relname = instructors_table_name
          AND con.contype = 'u'
          AND pg_get_constraintdef(con.oid) = format('UNIQUE (%s)', first_name_column)
        LOOP
            EXECUTE format(
                    'ALTER TABLE %I DROP CONSTRAINT %I',
                    instructors_table_name,
                    single_first_name_unique_constraint.conname
                    );
        END LOOP;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema_name
          AND table_name = instructors_table_name
          AND column_name = legacy_name_column
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema_name
          AND table_name = instructors_table_name
          AND column_name = first_name_column
    ) THEN
        EXECUTE format(
                'ALTER TABLE %I RENAME COLUMN %I TO %I',
                instructors_table_name,
                legacy_name_column,
                first_name_column
                );
    END IF;

    -- The unique constraint can survive rename(name -> first_name),
    -- so run the cleanup again after rename as well.
    FOR single_first_name_unique_constraint IN
        SELECT con.conname
        FROM pg_constraint con
                 JOIN pg_class rel ON rel.oid = con.conrelid
                 JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
        WHERE nsp.nspname = current_schema_name
          AND rel.relname = instructors_table_name
          AND con.contype = 'u'
          AND pg_get_constraintdef(con.oid) = format('UNIQUE (%s)', first_name_column)
        LOOP
            EXECUTE format(
                    'ALTER TABLE %I DROP CONSTRAINT %I',
                    instructors_table_name,
                    single_first_name_unique_constraint.conname
                    );
        END LOOP;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema_name
          AND table_name = instructors_table_name
          AND column_name = first_name_column
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema_name
          AND table_name = instructors_table_name
          AND column_name = last_name_column
    ) THEN
        EXECUTE format(
                'ALTER TABLE %I ADD COLUMN %I VARCHAR(255)',
                instructors_table_name,
                last_name_column
                );

        UPDATE instructors
        SET first_name = CASE
                             WHEN POSITION(space_delimiter IN first_name) > 0
                                 THEN SPLIT_PART(first_name, space_delimiter, 1)
                             ELSE first_name
            END,
            last_name = CASE
                            WHEN POSITION(space_delimiter IN first_name) > 0
                                THEN NULLIF(TRIM(SUBSTRING(first_name FROM POSITION(space_delimiter IN first_name) + 1)), '')
                            ELSE NULL
            END
        WHERE first_name IS NOT NULL;

        UPDATE instructors
        SET last_name = unknown_last_name
        WHERE last_name IS NULL;

        EXECUTE format(
                'ALTER TABLE %I ALTER COLUMN %I SET NOT NULL',
                instructors_table_name,
                last_name_column
                );
    END IF;

    -- If a previous failed run created last_name but did not backfill it, repair data and enforce NOT NULL.
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema_name
          AND table_name = instructors_table_name
          AND column_name = last_name_column
    ) THEN
        UPDATE instructors
        SET last_name = CASE
                            WHEN POSITION(space_delimiter IN first_name) > 0
                                THEN COALESCE(
                                        NULLIF(TRIM(SUBSTRING(first_name FROM POSITION(space_delimiter IN first_name) + 1)), ''),
                                        unknown_last_name)
                            ELSE unknown_last_name
            END
        WHERE last_name IS NULL;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema_name
          AND table_name = students_table_name
          AND column_name = legacy_full_name_column
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema_name
          AND table_name = students_table_name
          AND column_name = first_name_column
    ) THEN
        EXECUTE format(
                'ALTER TABLE %I RENAME COLUMN %I TO %I',
                students_table_name,
                legacy_full_name_column,
                first_name_column
                );
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema_name
          AND table_name = students_table_name
          AND column_name = first_name_column
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema_name
          AND table_name = students_table_name
          AND column_name = last_name_column
    ) THEN
        EXECUTE format(
                'ALTER TABLE %I ADD COLUMN %I VARCHAR(255)',
                students_table_name,
                last_name_column
                );

        UPDATE students
        SET first_name = CASE
                             WHEN POSITION(space_delimiter IN first_name) > 0
                                 THEN SPLIT_PART(first_name, space_delimiter, 1)
                             ELSE first_name
            END,
            last_name = CASE
                            WHEN POSITION(space_delimiter IN first_name) > 0
                                THEN NULLIF(TRIM(SUBSTRING(first_name FROM POSITION(space_delimiter IN first_name) + 1)), '')
                            ELSE NULL
            END
        WHERE first_name IS NOT NULL;

        UPDATE students
        SET last_name = unknown_last_name
        WHERE last_name IS NULL;

        EXECUTE format(
                'ALTER TABLE %I ALTER COLUMN %I SET NOT NULL',
                students_table_name,
                last_name_column
                );
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema_name
          AND table_name = students_table_name
          AND column_name = last_name_column
    ) THEN
        UPDATE students
        SET last_name = CASE
                            WHEN POSITION(space_delimiter IN first_name) > 0
                                THEN COALESCE(
                                        NULLIF(TRIM(SUBSTRING(first_name FROM POSITION(space_delimiter IN first_name) + 1)), ''),
                                        unknown_last_name)
                            ELSE unknown_last_name
            END
        WHERE last_name IS NULL;
    END IF;
END $$@@
