# core.sharedlib (minimal shared lib)

Bottom of stack. JDK only — no Spring, no JPA, no domain entities.

## Includes

| Type | Purpose |
|------|---------|
| `ApiResponse<T>` | HTTP envelope (`code`, `message`, `data`) |
| `ErrorCode` | Stable client codes |
| `PageRequest` / `PageResult` | Pagination |
| `MoneyUtil` | Parse/normalize scale 4 |

## Excludes (v1)

- `Wallet`, `CoaTrans` entities
- Command/event DTOs (wire = OpenAPI/AsyncAPI YAML; map in orchestration)
- Kafka/HTTP adapters

## Dependency rule

```
orchestration → accounting | wallet → foundation → JDK
```

Foundation must not import domain modules.
