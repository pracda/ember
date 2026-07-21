// Category tabs across the top of the menu grid. Order follows the spec's menu
// sections; only categories actually present in the menu are shown.
const ORDER = ['Burgers', 'Chicken', 'Sides', 'Drinks', 'Sweets'];

export function orderedCategories(all: string[]): string[] {
  const present = new Set(all);
  const known = ORDER.filter((c) => present.has(c));
  const extra = [...present].filter((c) => !ORDER.includes(c)).sort();
  return [...known, ...extra];
}

interface Props {
  categories: string[];
  active: string;
  onSelect: (category: string) => void;
}

export function CategoryTabs({ categories, active, onSelect }: Props) {
  return (
    <div role="tablist" aria-label="Menu categories" className="flex flex-wrap gap-2">
      {categories.map((category) => {
        const selected = category === active;
        return (
          <button
            key={category}
            role="tab"
            aria-selected={selected}
            onClick={() => onSelect(category)}
            className={[
              'min-h-11 px-5 rounded-full font-display text-lg tracking-wide transition-colors',
              'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ember',
              selected
                ? 'bg-ink text-bone'
                : 'bg-bone2 text-ink/70 hover:bg-ink/10',
            ].join(' ')}
          >
            {category}
          </button>
        );
      })}
    </div>
  );
}
