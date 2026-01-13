/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        // Team colors
        'team-blue': '#4A90D9',
        'team-blue-light': '#6BA3E0',
        'team-red': '#E74C3C',
        'team-red-light': '#EC7063',
        // Card colors
        'card-neutral': '#D4C4A8',
        'card-assassin': '#1A1A1A',
        'card-face': '#F5F5F5',
        // UI colors
        'game-bg': '#2C2C2C',
        'game-surface': '#3D3D3D',
        'game-border': '#4A4A4A',
      },
      animation: {
        'pulse-slow': 'pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'card-reveal': 'flip 0.6s ease-in-out',
      },
    },
  },
  plugins: [],
};
