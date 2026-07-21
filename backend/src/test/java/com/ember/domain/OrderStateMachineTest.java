package com.ember.domain;

import com.ember.web.error.InvalidTransitionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The order lifecycle lives on the entity: NEW -> PREP -> READY -> DONE, plus a
 * READY -> PREP recall. Every legal move and every illegal move is pinned here;
 * illegal moves must throw {@link InvalidTransitionException} (HTTP 409).
 */
class OrderStateMachineTest {

    private static Order newOrder() {
        return new Order(1, OrderType.DINE_IN);
    }

    private static Order at(OrderStatus target) {
        Order o = newOrder();
        switch (target) {
            case NEW -> { }
            case PREP -> o.advance();
            case READY -> { o.advance(); o.advance(); }
            case DONE -> { o.advance(); o.advance(); o.collect(); }
        }
        assertThat(o.getStatus()).isEqualTo(target);
        return o;
    }

    /* ---------- legal transitions ---------- */

    @Test
    void advanceNewToPrepSetsStartedAt() {
        Order o = newOrder();
        o.advance();
        assertThat(o.getStatus()).isEqualTo(OrderStatus.PREP);
        assertThat(o.getStartedAt()).isNotNull();
    }

    @Test
    void advancePrepToReadySetsReadyAt() {
        Order o = at(OrderStatus.PREP);
        o.advance();
        assertThat(o.getStatus()).isEqualTo(OrderStatus.READY);
        assertThat(o.getReadyAt()).isNotNull();
    }

    @Test
    void recallReadyToPrepClearsReadyAt() {
        Order o = at(OrderStatus.READY);
        o.recall();
        assertThat(o.getStatus()).isEqualTo(OrderStatus.PREP);
        assertThat(o.getReadyAt()).isNull();
    }

    @Test
    void collectReadyToDoneSetsCollectedAt() {
        Order o = at(OrderStatus.READY);
        o.collect();
        assertThat(o.getStatus()).isEqualTo(OrderStatus.DONE);
        assertThat(o.getCollectedAt()).isNotNull();
    }

    /* ---------- illegal transitions ---------- */

    @Test
    void cannotAdvanceFromReady() {
        assertThatThrownBy(() -> at(OrderStatus.READY).advance())
                .isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void cannotAdvanceFromDone() {
        assertThatThrownBy(() -> at(OrderStatus.DONE).advance())
                .isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void cannotRecallFromNew() {
        assertThatThrownBy(() -> at(OrderStatus.NEW).recall())
                .isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void cannotRecallFromPrep() {
        assertThatThrownBy(() -> at(OrderStatus.PREP).recall())
                .isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void cannotCollectFromNew() {
        assertThatThrownBy(() -> at(OrderStatus.NEW).collect())
                .isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void cannotCollectFromPrep() {
        assertThatThrownBy(() -> at(OrderStatus.PREP).collect())
                .isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void cannotCollectTwice() {
        Order done = at(OrderStatus.DONE);
        assertThatThrownBy(done::collect).isInstanceOf(InvalidTransitionException.class);
    }
}
