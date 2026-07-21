/**
 * Live order stream. Opens SockJS+STOMP to /ws, subscribes /topic/orders, and
 * upserts each event into the order store. On every (re)connect it re-fetches the
 * active orders so a dropped socket never loses a ticket (EMBER-SPEC.md §6).
 */
import { useEffect, useRef, useState } from 'react';
import { Client, type IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { api, apiBase } from './api';
import { useOrderStore } from './useOrderStore';
import type { Order, OrderEvent, OrderEventType } from './types';

export type ConnectionStatus = 'connecting' | 'connected' | 'disconnected';

export interface UseOrderStreamOptions {
  /** Observe each event (in addition to the store upsert). */
  onEvent?: (order: Order, type: OrderEventType) => void;
  /** Keep the shared order store in sync (seed on connect, upsert on event). Default true. */
  syncStore?: boolean;
  /**
   * What to fetch and seed on every (re)connect. Defaults to the active rail
   * (NEW+PREP). The pickup board overrides this to also include READY, so a
   * reload rebuilds its Ready zone too.
   */
  seedOnConnect?: () => Promise<Order[]>;
}

export function useOrderStream(options: UseOrderStreamOptions = {}): ConnectionStatus {
  const { syncStore = true } = options;
  const [status, setStatus] = useState<ConnectionStatus>('connecting');

  // Keep the latest callbacks without re-opening the socket each render.
  const onEventRef = useRef(options.onEvent);
  onEventRef.current = options.onEvent;
  const seedFnRef = useRef(options.seedOnConnect);
  seedFnRef.current = options.seedOnConnect;

  useEffect(() => {
    const { upsert, seed } = useOrderStore.getState();

    const client = new Client({
      webSocketFactory: () => new SockJS(`${apiBase}/ws`),
      reconnectDelay: 2000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        setStatus('connected');
        client.subscribe('/topic/orders', (message: IMessage) => {
          const event = JSON.parse(message.body) as OrderEvent;
          if (syncStore) upsert(event.order);
          onEventRef.current?.(event.order, event.type);
        });
        // Heal any events missed while disconnected.
        if (syncStore) {
          (seedFnRef.current ?? api.getActiveOrders)().then(seed).catch(() => undefined);
        }
      },
      onWebSocketClose: () => setStatus('disconnected'),
    });

    client.activate();
    return () => {
      void client.deactivate();
    };
  }, [syncStore]);

  return status;
}
