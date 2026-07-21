// A ticket number on the board. Ready tiles are big ember buttons the customer (or
// staff) taps to collect; preparing tiles are quieter, non-interactive.
interface Props {
  ticketNumber: number;
  variant: 'preparing' | 'ready';
  isNew?: boolean;
  onCollect?: () => void;
}

export function NumberTile({ ticketNumber, variant, isNew, onCollect }: Props) {
  if (variant === 'ready') {
    return (
      <button
        onClick={onCollect}
        aria-label={`Collect order ${ticketNumber}`}
        className={[
          'grid place-items-center rounded-3xl bg-ember-gradient text-[#1a0f08] shadow-xl',
          'aspect-[4/3] min-w-32 font-display leading-none text-7xl sm:text-8xl',
          'focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-bone',
          // New calls pulse (motion) AND ring (perceivable without motion).
          isNew ? 'ring-4 ring-bone motion-safe:animate-[pop_600ms_ease-out]' : '',
        ].join(' ')}
      >
        {ticketNumber}
      </button>
    );
  }

  return (
    <div
      className="grid aspect-[4/3] min-w-24 place-items-center rounded-2xl bg-graphite text-muted font-display text-5xl leading-none"
      aria-label={`Preparing order ${ticketNumber}`}
    >
      {ticketNumber}
    </div>
  );
}
