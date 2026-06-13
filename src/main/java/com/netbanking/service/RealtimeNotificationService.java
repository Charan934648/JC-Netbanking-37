package com.netbanking.service;

import com.netbanking.dto.RealtimeEventResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class RealtimeNotificationService {

    private final ConcurrentHashMap<String, Set<SseEmitter>> emittersByUsername = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String username) {
        SseEmitter emitter = new SseEmitter(0L);
        emittersByUsername.computeIfAbsent(username, ignored -> ConcurrentHashMap.newKeySet()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(username, emitter));
        emitter.onTimeout(() -> removeEmitter(username, emitter));
        emitter.onError(error -> removeEmitter(username, emitter));

        sendNow(username, RealtimeEventResponse.of(
                "CONNECTED",
                null,
                null,
                null,
                null,
                null,
                "JC Bank real-time banking stream connected"
        ));

        return emitter;
    }

    public void publishAccountCreated(String username, String accountNumber, BigDecimal balance) {
        publish(username, RealtimeEventResponse.of(
                "ACCOUNT_CREATED",
                accountNumber,
                null,
                balance,
                null,
                null,
                "JC Bank account created"
        ));
    }

    public void publishDeposit(String username, String accountNumber, BigDecimal balance, BigDecimal amount, String reference) {
        publish(username, RealtimeEventResponse.of(
                "DEPOSIT_POSTED",
                accountNumber,
                reference,
                balance,
                amount,
                "DEPOSIT",
                "Deposit posted in real time"
        ));
    }

    public void publishDebit(String username, String accountNumber, BigDecimal balance, BigDecimal amount, String transferType, String reference) {
        publish(username, RealtimeEventResponse.of(
                "ACCOUNT_DEBITED",
                accountNumber,
                reference,
                balance,
                amount,
                transferType,
                "Debit posted in real time"
        ));
    }

    public void publishCredit(String username, String accountNumber, BigDecimal balance, BigDecimal amount, String transferType, String reference) {
        publish(username, RealtimeEventResponse.of(
                "ACCOUNT_CREDITED",
                accountNumber,
                reference,
                balance,
                amount,
                transferType,
                "Credit posted in real time"
        ));
    }

    private void publish(String username, RealtimeEventResponse event) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            sendNow(username, event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sendNow(username, event);
            }
        });
    }

    private void sendNow(String username, RealtimeEventResponse event) {
        Set<SseEmitter> emitters = emittersByUsername.get(username);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(event.eventType())
                        .data(event));
            } catch (IOException | IllegalStateException ex) {
                log.debug("Removing stale real-time emitter for user {}", username, ex);
                removeEmitter(username, emitter);
            }
        }
    }

    private void removeEmitter(String username, SseEmitter emitter) {
        Set<SseEmitter> emitters = emittersByUsername.get(username);
        if (emitters == null) {
            return;
        }

        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByUsername.remove(username);
        }
    }
}
