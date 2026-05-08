# FROM node:20-alpine

# WORKDIR /app

# # Install dependencies first (layer cache)
# COPY package.json package-lock.json* ./
# RUN npm ci --omit=dev
# # RUN npm install
# # Copy source
# COPY . .

# # Create backups directory
# RUN mkdir -p backups

# EXPOSE 4040

# CMD ["node", "server.js"]

# ── Stage 1: deps ─────────────────────────────────────────────────────────────
FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci --omit=dev

# ── Stage 2: runtime ──────────────────────────────────────────────────────────
FROM node:20-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy installed deps and source
COPY --from=deps /app/node_modules ./node_modules
COPY . .

# Backups directory — will be mounted as a volume in docker-compose
RUN mkdir -p backups && chown -R appuser:appgroup /app

USER appuser

EXPOSE 4040

# Use node directly (not npm start) so signals propagate correctly and the
# process exits cleanly on SIGTERM without a 10 s npm wrapper delay.
CMD ["node", "server.js"]