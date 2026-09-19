# Meridian Catalog Web

Next.js + TypeScript catalog management UI for the sibling `ecommerce-catalog` Spring Boot service.

## Run locally

```bash
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000). The frontend expects the backend at `http://localhost:8080` and uses a Next.js `/backend-api` rewrite so browser requests remain same-origin while developing.

## Verification

```bash
npm run typecheck
npm run lint
npm run build
```

The catalog service must be running separately for category/product data and mutations to load.
