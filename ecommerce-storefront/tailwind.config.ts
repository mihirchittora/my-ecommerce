import type { Config } from "tailwindcss";
import tailwindcssAnimate from "tailwindcss-animate";

const config: Config = {
  content: ["./app/**/*.{js,ts,jsx,tsx,mdx}", "./components/**/*.{js,ts,jsx,tsx,mdx}", "./lib/**/*.{js,ts,jsx,tsx,mdx}"],
  theme: {
    extend: {
      colors: {
        ink: "#19312d",
        moss: "#2d6558",
        sage: "#dbe9df",
        coral: "#e27c59",
        sand: "#f7f3ec",
        mist: "#eef4f0",
      },
      fontFamily: {
        sans: ["var(--font-geist-sans)", "ui-sans-serif", "system-ui", "sans-serif"],
        display: ["var(--font-geist-sans)", "ui-sans-serif", "system-ui", "sans-serif"],
      },
      boxShadow: {
        soft: "0 18px 50px rgba(25, 49, 45, 0.10)",
      },
      borderRadius: {
        "4xl": "2rem",
      },
    },
  },
  plugins: [tailwindcssAnimate],
};

export default config;
