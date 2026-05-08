# ── Stage 1: build React bundle ───────────────────────────────────────────────
FROM node:20-alpine AS builder
WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm ci

COPY . .

# VITE_API_URL is injected at build time via --build-arg so the compiled JS
# bundle talks to the correct backend.  In docker-compose this is set via
# the `args:` block under `build:`.
ARG VITE_API_URL=http://localhost:4040
ENV VITE_API_URL=$VITE_API_URL

RUN npm run build

# ── Stage 2: serve with Nginx ─────────────────────────────────────────────────
FROM nginx:1.27-alpine

# Remove default config
RUN rm /etc/nginx/conf.d/default.conf

# Our custom config (see nginx.conf in this repo)
COPY nginx.conf /etc/nginx/conf.d/app.conf

# Copy the built bundle
COPY --from=builder /app/dist /usr/share/nginx/html

# Nginx listens on 80; docker-compose maps it to whatever host port you want
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]