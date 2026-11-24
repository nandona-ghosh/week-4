# StockPilot Product Catalog API

A Spring Boot REST API for the product catalogue proposed in the Week 1 StockPilot design, extended in Week 3 with an Open Food Facts lookup. It supports creating, listing, reading, replacing and deleting products. This Week 2 version intentionally focuses on basic CRUD: authentication, stock movements, archive semantics and PostgreSQL from the broader design are future work. Do not expose it publicly as-is.

## Requirements and setup

- Java 25 or newer (a JDK), and internet access on the first build for Maven and dependencies.
- Unzip the project, open a terminal in the `stockpilot-week3` folder, and run:

```sh
# macOS / Linux
./mvnw spring-boot:run
# Windows PowerShell
.\mvnw.cmd spring-boot:run
```

The API starts at `http://localhost:8080`. If necessary make the Unix wrapper executable with `chmod +x mvnw`. The app uses an H2 file database in `./data/`; data persists between restarts. To reset the local data, stop the application and remove that directory. The initial database is empty. No credentials are needed for the API.

Run the automated endpoint tests with `./mvnw test` (or `.\mvnw.cmd test` on Windows). Tests use a separate in-memory database. Spring Boot 3.5.16, Spring Web, Spring Data JPA, Bean Validation, H2 and JUnit 5 are used.

## Product fields

| Field | Rules |
| --- | --- |
| `id` | Server-generated numeric identifier, returned in responses |
| `sku` | Required, nonblank, unique, at most 64 characters; case-sensitive |
| `name` | Required, nonblank, at most 200 characters |
| `unit` | Required, nonblank, at most 32 characters |
| `reorderPoint` | Required whole number, zero or greater |

String fields are trimmed before storage. PUT replaces every writable field, so include all four. IDs are generated and may differ from the examples below.

## Endpoints

| Method | Path | Result |
| --- | --- | --- |
| POST | `/api/v1/products` | 201 Created, product JSON and `Location` header |
| GET | `/api/v1/products` | 200 OK, array of products ordered by id |
| GET | `/api/v1/products/{id}` | 200 OK, or 404 if absent |
| PUT | `/api/v1/products/{id}` | 200 OK with updated product, or 404 if absent |
| DELETE | `/api/v1/products/{id}` | 204 No Content, or 404 if absent |

## Try it

These commands use `curl`; on Windows PowerShell use `curl.exe`. Run them in order, replacing `1` with the ID returned by the create request if needed.

```sh
curl -i -X POST http://localhost:8080/api/v1/products -H "Content-Type: application/json" -d '{"sku":"PEN-01","name":"Blue pen","unit":"piece","reorderPoint":5}'
curl -i http://localhost:8080/api/v1/products
curl -i http://localhost:8080/api/v1/products/1
curl -i -X PUT http://localhost:8080/api/v1/products/1 -H "Content-Type: application/json" -d '{"sku":"PEN-01","name":"Black pen","unit":"piece","reorderPoint":8}'
curl -i -X DELETE http://localhost:8080/api/v1/products/1
```

Example response to create (the Location host and ID depend on the request):

```http
HTTP/1.1 201 Created
Location: http://localhost:8080/api/v1/products/1
Content-Type: application/json

{"id":1,"sku":"PEN-01","name":"Blue pen","unit":"piece","reorderPoint":5}
```

A list returns an array of these objects; PUT returns the updated object. DELETE returns an empty body. Invalid fields return 400 with a JSON error such as `{"code":"VALIDATION_ERROR","message":"Please check the supplied fields","details":{"sku":"must not be blank"}}`. Malformed JSON returns 400, missing products return 404, and duplicate SKUs return 409. Error messages contain no stack trace. See the tests for more examples.

## Project layout

`src/main/java` contains the controller, service, entity, repository and error handler; `src/main/resources` contains local configuration. `src/test` contains endpoint integration tests exercising the real Spring MVC, service and database layers. The H2 schema is managed by Hibernate for this exercise; use versioned migrations, access control, a production database and deployment configuration before a real deployment.

## Week 3: Open Food Facts lookup

`GET /api/v1/food-products/{barcode}` looks up food-product data from the public [Open Food Facts API](https://openfoodfacts.github.io/documentation/docs/Product-Opener/api/). It does not create or change a local product. This read-only v2 endpoint requires no API key. Open Food Facts currently describes v2 as deprecated; the integration isolates it behind a gateway so the provider client can be upgraded to v3 later.

Before a live lookup, set `FOOD_API_USER_AGENT` to an identifying value with your actual project URL or contact, as requested by the provider. The example default uses a placeholder and should be replaced. Example in PowerShell:

```powershell
$env:FOOD_API_USER_AGENT = 'StockPilot-Internship/1.0 (your-contact@example.com)'
.\mvnw.cmd spring-boot:run
```

Then in another terminal:

```sh
curl http://localhost:8080/api/v1/food-products/3017620422003
```

Example response verified using the local fake provider (live product data may differ):

```json
{"barcode":"3017620422003","name":"Chocolate spread","brands":"Example","quantity":"400 g"}
```

The barcode must contain 8 to 14 digits; this checks format, not its check digit. Invalid format returns 400 `INVALID_BARCODE`; a provider status of zero returns 404 `FOOD_NOT_FOUND`; an upstream HTTP failure, timeout, unreadable response, or missing product name returns 502 `FOOD_PROVIDER_ERROR`. Optional `brands` and `quantity` may be null. The upstream request asks only for selected fields, identifies the client with a User-Agent, and has connection and read timeouts (3 seconds by default). Configure `FOOD_API_BASE_URL`, `FOOD_API_USER_AGENT`, or `FOOD_API_TIMEOUT_MS` using environment variables; leave the base URL at its default for live use. No automatic retries are performed. Successful food lookups are cached in memory for 300 seconds, up to 500 entries (least recently used eviction). Configure FOOD_CACHE_TTL_SECONDS (0 disables caching; maximum 86400) and FOOD_CACHE_MAX_ENTRIES (0 disables caching; maximum 100000). Failures are never cached. Each process has its own cache; simultaneous misses can make duplicate provider calls, and results may be stale until expiry.

Run `.\mvnw.cmd test` (Windows) or `./mvnw test` (macOS/Linux). Unit tests mock the provider gateway, and integration tests run an HTTP fake server on localhost, so the tests do not call Open Food Facts. The existing catalog tests remain included. Check Maven's `Tests run`, `Failures`, and `Errors` summary and the XML/text files in `target/surefire-reports/`; zero failures/errors and `BUILD SUCCESS` indicate a passing run. More detail is in `Week3_Test_Report.pdf`.

## Week 4: performance and verification

The cache avoids repeated provider calls while preserving validation and error responses. It uses a monotonic clock and bounds memory by entry count. Run `./mvnw test` (or `mvnw.cmd test` on Windows). `FoodBenchmarkTest` compares 50 sequential calls with a simulated 20 ms provider delay and prints timings and call counts. This isolates service behavior and is not an HTTP or production load test. Cache correctness tests cover expiry, eviction, failures and disabling. See `Week4_Performance_Report.pdf` for results and limitations.
