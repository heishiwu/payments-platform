package com.example.demo;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/transactions")
public class TxnController {

    private final KafkaTemplate<String, String> kafka;

    public TxnController(KafkaTemplate<String, String> kafka) {
        this.kafka = kafka;
    }

    @PostMapping("/credit")
    public void credit(@RequestBody String payload) {
        sendDecoded("credit", payload); 
    }

    @PostMapping("/debit")
    public void debit(@RequestBody String payload) {
        sendDecoded("debit", payload); 
    }

    @PostMapping("/batch")
    public void batch(@RequestBody String payload) {
        kafka.send(new ProducerRecord<>("batch", payload));
    }


    //Base 64 解码
    private void sendDecoded(String topic, String base64) {
        byte[] raw = Base64.getDecoder().decode(base64);          // Base64 → bytes
        String msg = new String(raw, StandardCharsets.UTF_8);     // bytes → UTF-8 字符串
        kafka.send(new ProducerRecord<>(topic, msg));             // 写入指定 topic
    }
}
