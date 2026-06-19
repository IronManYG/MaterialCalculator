// Sifr — Tailwind theme.extend (generated)
// Drop into your tailwind.config.ts:
//   import brand from "./tailwind.theme";
//   export default { theme: { extend: brand.theme.extend } } satisfies Config;

const brand = {
  theme: {
    extend: {
      colors: {
      bg: "linear-gradient(160deg, #101A2E 0%, #070A12 100%)",
      fg: "#EDF1F7",
      accent: "#5CE8D4",
      muted: "#5A6478",
      brandDark: "#03201B",
      themes: {
      "layl-dark": {
        bg: "linear-gradient(160deg, #101A2E 0%, #070A12 100%)",
        fg: "#EDF1F7",
        accent: "#5CE8D4",
        muted: "#5A6478",
        brandDark: "#03201B",
      },
      "layl-light": {
        bg: "linear-gradient(160deg, #EDF4F9 0%, #DCE7F1 100%)",
        fg: "#16202E",
        accent: "#0E9C8C",
        muted: "#6A7689",
        brandDark: "#03201B",
      },
      farah: {
        bg: "linear-gradient(160deg, #FFF4E4 0%, #FBE6CC 100%)",
        fg: "#4A3326",
        accent: "#F2683C",
        muted: "#B08A62",
        brandDark: "#FFF6EC",
      },
      },
    },
    },
  },
} as const;

export default brand;
