import type { Config } from "tailwindcss";

export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        border: "hsl(218 22% 88%)",
        input: "hsl(218 22% 88%)",
        ring: "hsl(216 92% 58%)",
        background: "hsl(220 33% 98%)",
        foreground: "hsl(221 39% 14%)",
        primary: {
          DEFAULT: "hsl(216 92% 58%)",
          foreground: "hsl(0 0% 100%)",
        },
        secondary: {
          DEFAULT: "hsl(210 29% 96%)",
          foreground: "hsl(221 39% 14%)",
        },
        muted: {
          DEFAULT: "hsl(210 24% 95%)",
          foreground: "hsl(215 17% 43%)",
        },
        accent: {
          DEFAULT: "hsl(30 94% 58%)",
          foreground: "hsl(221 39% 14%)",
        },
        destructive: {
          DEFAULT: "hsl(0 72% 51%)",
          foreground: "hsl(0 0% 100%)",
        },
        card: {
          DEFAULT: "hsl(0 0% 100%)",
          foreground: "hsl(221 39% 14%)",
        },
      },
      borderRadius: {
        xl: "1rem",
        "2xl": "1.5rem",
      },
      boxShadow: {
        soft: "0 20px 45px rgba(15, 23, 42, 0.08)",
        panel: "0 10px 30px rgba(15, 23, 42, 0.06)",
      },
      fontFamily: {
        sans: ["\"Plus Jakarta Sans\"", "Inter", "system-ui", "sans-serif"],
        heading: ["Sora", "\"Plus Jakarta Sans\"", "system-ui", "sans-serif"],
      },
    },
  },
  plugins: [],
} satisfies Config;
