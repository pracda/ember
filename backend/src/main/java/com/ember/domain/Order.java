package com.ember.domain;

import com.ember.web.error.InvalidTransitionException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * The order aggregate. Owns its lines and its money snapshot, and is the only
 * place order-status transitions happen — controllers and services call these
 * methods rather than setting the status field directly.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Customer-facing number shown on tickets and the pickup board. */
    @Column(name = "ticket_number", nullable = false)
    private int ticketNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.NEW;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id", nullable = false)
    @OrderColumn(name = "line_index")
    private List<OrderLine> lines = new ArrayList<>();

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal tax = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ready_at")
    private Instant readyAt;

    @Column(name = "collected_at")
    private Instant collectedAt;

    /** Username of the staff member who created the order (for the staff sales report). */
    @Column(name = "served_by")
    private String servedBy;

    /** Reason a VOIDED/REFUNDED order was reversed (audit + voids report). */
    @Column(length = 255)
    private String reason;

    /** When the order was voided/refunded, and by whom. */
    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by")
    private String resolvedBy;

    protected Order() { }

    public Order(int ticketNumber, OrderType type) {
        this.ticketNumber = ticketNumber;
        this.type = type;
    }

    /* ----- transitions ----- */

    /** NEW → PREP, or PREP → READY. */
    public void advance() {
        OrderStatus next = status.advanced();
        if (next == null) {
            throw new InvalidTransitionException(id, status, "advance");
        }
        if (next == OrderStatus.PREP) {
            startedAt = Instant.now();
        } else if (next == OrderStatus.READY) {
            readyAt = Instant.now();
        }
        status = next;
    }

    /** READY → PREP, e.g. the kitchen bumped it too early. */
    public void recall() {
        if (status != OrderStatus.READY) {
            throw new InvalidTransitionException(id, status, "recall");
        }
        status = OrderStatus.PREP;
        readyAt = null;
    }

    /** READY → DONE, the customer has collected the order. */
    public void collect() {
        if (status != OrderStatus.READY) {
            throw new InvalidTransitionException(id, status, "collect");
        }
        status = OrderStatus.DONE;
        collectedAt = Instant.now();
    }

    /** Cancel an order before completion (NEW/PREP/READY → VOIDED). */
    public void voidOrder() {
        if (status != OrderStatus.NEW && status != OrderStatus.PREP && status != OrderStatus.READY) {
            throw new InvalidTransitionException(id, status, "void");
        }
        status = OrderStatus.VOIDED;
        resolvedAt = Instant.now();
    }

    /** Return the money on a completed order (DONE → REFUNDED). */
    public void refund() {
        if (status != OrderStatus.DONE) {
            throw new InvalidTransitionException(id, status, "refund");
        }
        status = OrderStatus.REFUNDED;
        resolvedAt = Instant.now();
    }

    public void addLine(OrderLine line) {
        lines.add(line);
    }

    /* ----- getters / setters ----- */

    public Long getId() { return id; }

    public int getTicketNumber() { return ticketNumber; }
    public void setTicketNumber(int ticketNumber) { this.ticketNumber = ticketNumber; }

    public OrderType getType() { return type; }
    public void setType(OrderType type) { this.type = type; }

    public OrderStatus getStatus() { return status; }

    public List<OrderLine> getLines() { return lines; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getTax() { return tax; }
    public void setTax(BigDecimal tax) { this.tax = tax; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getReadyAt() { return readyAt; }
    public Instant getCollectedAt() { return collectedAt; }

    public String getServedBy() { return servedBy; }
    public void setServedBy(String servedBy) { this.servedBy = servedBy; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Instant getResolvedAt() { return resolvedAt; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }
}
