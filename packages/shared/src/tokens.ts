/**
 * Design + business tokens (EMBER-SPEC.md Section 8). The Tailwind preset mirrors
 * the palette for class names; this module is for anything that needs the values
 * in TS (inline styles, the age-color logic in format.ts, etc.).
 */
export const colors = {
  char: '#14110F',
  graphite: '#211C19',
  graphite2: '#2A2320',
  steel: '#3A322D',
  steel2: '#4A403A',
  bone: '#F6F1E8',
  bone2: '#ECE4D6',
  ink: '#1B1512',
  ember: '#FF5722',
  flame: '#FF8A3D',
  fresh: '#2FCB86',
  working: '#FFB020',
  late: '#FF463B',
  muted: '#A99A8C',
} as const;

export const fonts = {
  display: '"Barlow Condensed", system-ui, sans-serif',
  body: '"Barlow", system-ui, sans-serif',
  mono: '"Space Mono", ui-monospace, monospace',
} as const;

/**
 * Business constants — config, not code (EMBER-SPEC.md Section 8). The server is
 * the source of truth for pricing; these mirror it for display/age logic only.
 */
export const business = {
  taxRate: 0.085,
  mealUpcharge: 3.5,
  /** Order age thresholds in seconds: fresh <300, working <600, late ≥600. */
  age: { freshUnder: 300, workingUnder: 600 },
} as const;
