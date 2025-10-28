# Payments Platform (Prototype)

> **Status:** Proof-of-concept – single-node Docker stack suitable for local development / load-test (~100 QPS)

---

## 1  What this repo contains

| Module | Purpose |
|--------|---------|
| **`producer/`** | Spring Boot service that<br>• exposes `/transactions/credit` & `/transactions/debit` realtime endpoints<br>• decodes Base64 payloads → synchronous *DummyBank* settlement → returns `BankResp` JSON<br>• emits settlement events to **`payment.events`** topic<br>• exposes `/transactions/batch` which streams bulk requests to **`batch`** topic |
| **`processor/`** | Spring Boot service (skeleton) that will:<br>• subscribe only **`batch`** topic<br>• perform async settlement (DummyBank) & write results to **`callback`** topic |
| **`common/`** *(optional later)* | Planned shared code (models, clients) – currently copied into each module |
| **Docker stack** | Kafka + ZooKeeper + Postgres + Kong reverse-proxy defined in `docker-compose.yml` |

---

## 2  Local quick-start

```bash
# 1.  spin up infra (Kafka etc.)
docker compose up -d kafka zookeeper postgres kong

# 2.  start Producer (HTTP API on :8080)
cd producer && ./gradlew bootRun

# 3.  start Processor (optional – only needed for /batch)
cd ../processor && ./gradlew bootRun
