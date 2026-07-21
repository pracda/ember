package com.ember.web;

import com.ember.web.dto.OrderEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Fans out order changes to the stations over STOMP — but only once the database
 * transaction has committed. {@link com.ember.service.OrderService} publishes an
 * {@link OrderEvent} inside its transaction; this listener sends it after commit,
 * so a client never sees an event for a change that later rolls back.
 */
@Component
public class OrderEventBroadcaster {

    /** All stations subscribe here and filter by status, mirroring the prototype's shared stream. */
    public static final String TOPIC = "/topic/orders";

    private final SimpMessagingTemplate broker;

    public OrderEventBroadcaster(SimpMessagingTemplate broker) {
        this.broker = broker;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderEvent(OrderEvent event) {
        broker.convertAndSend(TOPIC, event);
    }
}
