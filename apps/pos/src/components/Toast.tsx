// Brief confirmation shown after an order is sent (the assigned ticket number).
import { useEffect } from 'react';

interface Props {
  ticketNumber: number;
  onDismiss: () => void;
}

export function Toast({ ticketNumber, onDismiss }: Props) {
  useEffect(() => {
    const t = setTimeout(onDismiss, 3500);
    return () => clearTimeout(t);
  }, [ticketNumber, onDismiss]);

  return (
    <div
      role="status"
      className="fixed bottom-6 left-1/2 z-50 -translate-x-1/2 rounded-2xl bg-ink px-6 py-4 text-bone shadow-2xl motion-safe:animate-[fade_150ms_ease-out]"
    >
      <span className="font-display text-2xl">Sent · Ticket #{ticketNumber}</span>
    </div>
  );
}
