const preset = require('@ember/shared/tailwind-preset');

/** @type {import('tailwindcss').Config} */
module.exports = {
  presets: [preset],
  content: [
    './index.html',
    './src/**/*.{ts,tsx}',
    // shared components (e.g. PinLogin) so their utility classes are generated
    '../../packages/shared/src/**/*.{ts,tsx}',
  ],
};
