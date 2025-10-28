package com.example.demo;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TxnListener {

    private final KafkaTemplate<String, String> kafka;

    public TxnListener(KafkaTemplate<String, String> kafka) {
        this.kafka = kafka;
    }

    /** 监听 credit 和 debit 主题 */
    @KafkaListener(topics = {"credit", "debit"})
    public void handleTxn(String message) {
        System.out.printf(">> Received: %s%n", message);

        // 示例：处理完后写入 callback 主题
        kafka.send(new ProducerRecord<>("callback", "processed-" + message));
    }
}
