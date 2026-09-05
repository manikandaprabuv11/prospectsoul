# ProspectSoul frontend

Vite + React + TypeScript application. See the [root README](../README.md) for
the full development setup; this file covers the frontend only.

## Commands

```bash
npm install
npm run dev       # dev server on http://localhost:5173
npm run build     # tsc -b + vite build
npm run lint      # oxlint
npm run preview   # serve the production build
```

Node 20 LTS or newer is required (see `.nvmrc`).

## Configuration

Copy `.env.example` to `.env.local`. Everything prefixed `VITE_` is embedded in
the browser bundle, so no secret may be placed there.

## Layout

```
src/
├── api/          central HTTP client (client.ts)
├── auth/         authentication layer (not implemented yet)
├── components/
│   └── ui/       shadcn/ui primitives
├── constants/    typed environment access
├── hooks/
├── layouts/
├── lib/          cn() helper, TanStack Query client
├── pages/
├── routes/       React Router configuration
└── types/
```

`@/` resolves to `src/` — configured in both `vite.config.ts` and
`tsconfig.app.json`.

## Stack

React 19, React Router 7, TanStack Query 5, TanStack Table 9, React Hook Form,
Zod, Tailwind CSS 4, shadcn/ui on Radix UI, Lucide icons, Recharts.

Add shadcn/ui components with `npx shadcn@latest add <component>`. The CLI has
been observed to write `import { cn } from "cn"` instead of
`import { cn } from "@/lib/utils"` — check the import after generating a
component.

## Status

Scaffold only. `/` renders a setup verification page that confirms the
toolchain works and checks backend reachability. No ProspectSoul features are
implemented.
