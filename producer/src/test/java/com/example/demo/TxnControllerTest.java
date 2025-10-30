package com.example.demo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TxnController.class)
class TxnControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private BankClient bankClient;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockBean
    private TxnRepository repository;

    @Test
    void creditEndpointNormalizesTypePublishesAndReturnsSettlement() throws Exception {
        TxnMessage request = new TxnMessage(
                "TXN-1",
                null,
                "MERCHANT-1",
                TxnType.DEBIT,
                new BigDecimal("25.75"),
                TxnStatus.PENDING,
                "https://callback.example.com/hook",
                Instant.parse("2025-10-29T14:30:00Z"));

        TxnMessage settled = request.withType(TxnType.CREDIT)
                .withStatusAndTimestamp(TxnStatus.SUCCESS, Instant.parse("2025-10-29T14:31:00Z"));

        when(repository.find("MERCHANT-1", "TXN-1")).thenReturn(Optional.empty());
        when(bankClient.credit(any())).thenReturn(settled);

        mockMvc.perform(post("/transactions/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.txnType").value("CREDIT"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.txnId").value("TXN-1"));

        verify(bankClient).credit(Mockito.argThat(msg -> msg.txnType() == TxnType.CREDIT));
        verify(repository).find("MERCHANT-1", "TXN-1");

        String expectedPayload = mapper.writeValueAsString(settled);
        verify(kafkaTemplate).send(TxnTopics.CREDIT, settled.idempotencyKey(), expectedPayload);
        verify(repository).upsert(settled);
    }

    @Test
    void creditEndpointReturnsCachedResponseWhenDuplicate() throws Exception {
        TxnMessage request = new TxnMessage(
                "TXN-dup",
                null,
                "MERCHANT-dup",
                TxnType.CREDIT,
                new BigDecimal("50.00"),
                TxnStatus.PENDING,
                "https://callback.example.com/dup",
                Instant.parse("2025-10-29T15:00:00Z"));

        TxnMessage existing = request.withStatusAndTimestamp(TxnStatus.SUCCESS, Instant.parse("2025-10-29T15:01:00Z"));

        when(repository.find("MERCHANT-dup", "TXN-dup")).thenReturn(Optional.of(existing));

        mockMvc.perform(post("/transactions/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.timestamp").value("2025-10-29T15:01:00Z"));

        verify(bankClient, never()).credit(any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
        verify(repository, never()).upsert(any());
    }

    @Test
    void batchEndpointForcesPendingStatusAndSkipsBankCall() throws Exception {
        TxnMessage request = new TxnMessage(
                "TXN-2",
                "BATCH-1",
                "MERCHANT-2",
                TxnType.BATCH_CLOSE,
                new BigDecimal("100.00"),
                TxnStatus.SUCCESS,
                "https://client.example.com/cb",
                Instant.parse("2025-10-29T10:00:00Z"));

        mockMvc.perform(post("/transactions/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.txnType").value("BATCH_CLOSE"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(bankClient, never()).credit(any());
        verify(bankClient, never()).debit(any());
        verify(repository, never()).find(anyString(), anyString());
        verify(repository, never()).upsert(any());

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(TxnTopics.BATCH_CLOSE), eq("MERCHANT-2:TXN-2"), payloadCaptor.capture());

        TxnMessage published = mapper.readValue(payloadCaptor.getValue(), TxnMessage.class);
        assertEquals(TxnStatus.PENDING, published.status());
    }

    @Test
    void invalidRequestFailsValidation() throws Exception {
        TxnMessage invalid = new TxnMessage(
                "TXN-3",
                null,
                "", // merchantId missing
                TxnType.CREDIT,
                new BigDecimal("10.00"),
                TxnStatus.PENDING,
                "invalid-url",
                Instant.parse("2025-10-29T12:00:00Z"));

        mockMvc.perform(post("/transactions/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());

        verify(bankClient, never()).credit(any());
        verify(bankClient, never()).debit(any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
        verify(repository, never()).find(anyString(), anyString());
        verify(repository, never()).upsert(any());
    }
}

