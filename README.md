# MySociety Operations Service

Spring Boot 3 / Java 21 API for complaints and work orders. It uses the canonical
`mysociety` schema from `identity-service`; Hibernate is validation-only and no
tables are created or migrated by this service.

## Run

Copy `.env.example` to `.env`, load its environment variables, then run:

```powershell
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
```

The service listens on `8084` under `/api/v1`. OpenAPI is at
`/api/v1/swagger-ui.html`; health probes and Prometheus metrics are exposed at
`/api/v1/actuator/health/liveness`, `/api/v1/actuator/health/readiness`, and
`/api/v1/actuator/prometheus`.

## Authentication and tenancy

All business endpoints require a HS256 JWT signed with `JWT_HS256_SECRET` and
issuer `mysociety-identity`. Required claims are `sub` (user UUID) and
`society_id` (society UUID); `roles` and `permissions` are optional arrays.
Tenant identity is derived only from claims, never from request bodies, and every
repository method includes `societyId`. Send an optional `X-Correlation-Id`; the
service generates one if absent and returns it on every response.

## Outbox

Domain mutations persist a versioned record in the existing `mysociety.outbox_events`
table in the same transaction. This service deliberately does not claim durable
Kafka publishing: the canonical schema supplies durable storage but no relay,
broker configuration, or consumer contract. An external relay can publish rows
marked `PENDING`. The schema's `id`, `event_type`, `event_version`, `society_id`,
`aggregate_type`, `aggregate_id`, and payload columns hold the event contract;
`occurredAt` and `correlationId` are recorded in the JSON headers because the
canonical table has no dedicated columns for them.
