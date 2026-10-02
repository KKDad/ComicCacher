# Promotion

Prod's `PromoteFromDevJob` copies the strips dev already downloaded (see [Batch Jobs](../design/batch-jobs.md#promotefromdevjob)). Dev serves them over two REST endpoints, and prod reads them. This is a machine-to-machine interface, so it uses REST with a shared token rather than GraphQL and JWTs.

## Authentication

Every request carries the shared secret in an `X-Promotion-Token` header. `PromotionTokenFilter` compares it in constant time and grants the `PROMOTION` authority, which `SecurityConfig` requires for `/api/v1/promotion/**`.

| Request | Response |
|---|---|
| The right token, on an instance with `comics.promotion.serve=true` | 200 |
| No token, a wrong one, or `serve=false` (prod, and the default) | 401 |
| A user's JWT without the token | 403 |

The token is never logged. A wrong token logs one WARN line.

## Endpoints

### `GET /api/v1/promotion/manifest`

| Parameter | Required | Meaning |
|---|---|---|
| `from`, `to` | yes | Inclusive date range (`yyyy-MM-dd`), at most `comics.promotion.max-days` (7) days |
| `source` | no | Only this source |
| `sourceIdentifier` | no | Only this comic (together with `source`) |

The response lists every comic with a strip in the range, including comics that are disabled on dev. Comics are identified by source and source identifier, because each instance numbers its comics separately. The JSON is written with Gson:

```json
{
  "from": "2026-10-01",
  "to": "2026-10-01",
  "comics": [
    { "source": "gocomics", "sourceIdentifier": "garfield", "name": "Garfield", "dates": ["2026-10-01"] }
  ]
}
```

A reversed or too-long range gets a 400 with the reason as plain text.

### `GET /api/v1/promotion/strips/{source}/{sourceIdentifier}/{date}`

Returns the strip's image bytes with its content type. When the strip has a transcript, it's sent URL-encoded in an `X-Transcript` header. The response is 404 when there's no such comic or strip. Unlike `/api/v1/comics/{id}/strip/{date}`, this endpoint serves disabled comics too and doesn't count toward access metrics.

## Configuration

| Property | Dev | Prod | Meaning |
|---|---|---|---|
| `comics.promotion.token` | shared | shared | From `COMICS_PROMOTION_TOKEN` |
| `comics.promotion.serve` | `true` | `false` | Answer the endpoints |
| `comics.promotion.source-url` | empty | `http://comics-api-dev:8888` | Where `PromoteFromDevJob` reads from |
| `comics.promotion.max-days` | 7 | 7 | The longest range either side accepts |

On the Docker host, `utils/remote/run.sh` creates the token file `comics-promotion.env` beside both deploy directories (`/root/comics-promotion.env`) the first time either environment deploys. Both compose files load it as an optional `env_file`. A container picks up a new or changed token only when it is next recreated. Until both are, prod's runs fail with a "rejected the promotion token" WARN.
