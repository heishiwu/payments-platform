package com.example.demo;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
public class TxnController {

    private final KafkaTemplate<String, String> kafka;

    public TxnController(KafkaTemplate<String, String> kafka) {
        this.kafka = kafka;
    }

    @PostMapping("/credit")
    public void credit(@RequestBody String payload) {
        kafka.send(new ProducerRecord<>("credit", payload));
    }

    @PostMapping("/debit")
    public void debit(@RequestBody String payload) {
        kafka.send(new ProducerRecord<>("debit", payload));
    }

    @PostMapping("/batch")
    public void batch(@RequestBody String payload) {
        kafka.send(new ProducerRecord<>("batch", payload));
    }
}
