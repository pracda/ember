/**
 * Shared Tailwind preset — the Ember design system (EMBER-SPEC.md Section 8).
 * All three apps extend this so colors, fonts and the ember gradient stay
 * identical across the POS, kitchen display and pickup board.
 *
 * CommonJS on purpose: tailwind.config.cjs `require()`s it.
 */
const palette = {
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
};

/** @type {import('tailwindcss').Config} */
module.exports = {
  theme: {
    extend: {
      colors: palette,
      fontFamily: {
        display: ['"Barlow Condensed"', 'system-ui', 'sans-serif'],
        body: ['"Barlow"', 'system-ui', 'sans-serif'],
        mono: ['"Space Mono"', 'ui-monospace', 'monospace'],
      },
      backgroundImage: {
        // Primary action = linear-gradient(150deg, ember, flame)
        'ember-gradient': `linear-gradient(150deg, ${palette.ember}, ${palette.flame})`,
      },
    },
  },
  plugins: [],
};
