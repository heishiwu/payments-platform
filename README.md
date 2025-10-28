"""# Payments Platform (Prototype)

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

# 1. spin up infra (Kafka etc.)
docker compose up -d kafka zookeeper postgres kong

# 2. start Producer (HTTP API on :8080)
cd producer && ./gradlew bootRun

# 3. start Processor (optional – only needed for /batch)
cd ../processor && ./gradlew bootRun

### Test a realtime credit

echo -n 'hello-credit' | base64   # -> aGVsbG8tY3JlZGl0

curl -X POST http://localhost:8080/transactions/credit \\
     -H "Content-Type: text/plain" \\
     -d "aGVsbG8tY3JlZGl0"

You should receive an immediate JSON BankResp and see an event in payment.events:

docker exec -it kafka bash -c "kafka-console-consumer --bootstrap-server localhost:9092 \\
  --topic payment.events --from-beginning --max-messages 1"

### Load-test (~100 QPS)

brew install hey   # if not installed
hey -n 1000 -c 100 -m POST \\
    -H "Content-Type: text/plain" \\
    -d "aGVsbG8tY3JlZGl0" \\
    http://localhost:8080/transactions/credit

Expect p99 latency ≈ 30 – 40 ms (DummyBank simulates 10–30 ms network).

---

## 3 Project architecture (high-level)

Client ──HTTP (sync)──► Producer ──HTTP──► DummyBank
   ▲      200/4xx         │
   │                     JSON event
   │                      ▼
   │              Kafka: payment.events
   │                     ▲
   │   callback (async)  │ (future work)
   └── Processor (batch) ─┘

credit/debit – synchronous path, user receives immediate result.

batch – asynchronous: Producer streams messages to batch, Processor picks them up, settles, and emits callback.

---

## 4 Topics overview

Topic\tPartitions\tProducer\tConsumer
credit\t4\tProducer (sync endpoints)\t(unused now – kept for raw audits)
debit\t4\tProducer\t(unused)
batch\t4\tProducer /batch\tProcessor
payment.events\t4\tProducer (settlement result)\tBI / audit / downstream services
callback\t4\tProcessor\tCustomer webhook service

---

## 5 Next steps / TODO

- Processor – consume batch, call DummyBank, emit callback
- /batch – support multipart file upload + per-record Base64 decode
- Shared library (common/) instead of duplicated code
- Idempotency-Key + redis/DB dedupe
- Replace DummyBank with real bank adapter & circuit-breaker
- Observability: Prometheus metrics + Grafana dashboard

---

## 6 Useful commands

# List topics
docker exec -it kafka bash -c "kafka-topics --bootstrap-server localhost:9092 --list"

# Describe topic
docker exec -it kafka bash -c "kafka-topics --bootstrap-server localhost:9092 \\
  --describe --topic payment.events"

# Consume last N messages
docker exec -it kafka bash -c "kafka-console-consumer --bootstrap-server localhost:9092 \\
  --topic payment.events --from-beginning --max-messages 10"

© 2025 Xiuyuan Zhao — For demo / educational use only
"""
