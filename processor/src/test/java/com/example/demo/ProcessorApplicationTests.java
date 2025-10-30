package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;

@SpringBootTest
class ProcessorApplicationTests {

    @MockBean
    KafkaTemplate<String, String> kafkaTemplate;

    @MockBean
    SettlementRepository settlementRepository;

	@Test
	void contextLoads() {
	}

}
