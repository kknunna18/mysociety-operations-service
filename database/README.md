# Operations database scripts

`migrations/V1__operations_schema.sql` creates the Operations Service tables in
the `mysociety` schema: `complaints`, `complaint_comments`,
`complaint_status_history`, and `work_orders`. The script safely creates
`pgcrypto` and the schema when they do not already exist, and is rerunnable for
an empty or already-initialized Operations database.

Apply it with a PostgreSQL 16+ client, for example:

```powershell
psql $env:DATABASE_URL -f database/migrations/V1__operations_schema.sql
```

The script deliberately preserves foreign keys between Operations-owned records:
comments and status history cascade with their complaint, and a work order's
complaint reference is set to null if the complaint is deleted. It intentionally
does not create foreign keys for UUIDs owned by Society or Identity services:
`society_id`, `unit_id`, `reported_by`, `assigned_user_id`, `author_user_id`,
`changed_by`, and `created_by`.

Validate those external UUIDs through the Operations APIs and the corresponding
Society/Identity events before persisting or acting on them. Cross-service
referential integrity must not rely on a shared-database foreign key.
