# Build stage
FROM node:18-alpine AS builder

WORKDIR /app

# Copy package files
COPY package*.json ./
RUN npm ci --only=production

# Copy source code
COPY . .

# Build the application (no env vars needed)
RUN npm run build

# Production stage
FROM node:18-alpine AS runner

WORKDIR /app

# Create non-root user
RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs

# Copy built application
COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs

EXPOSE 2010

ENV PORT=2010
ENV HOSTNAME="0.0.0.0"

# Environment variables will be provided at runtime
# ENV DEVICE_SERVICE_URL=
# ENV ACL_SERVICE_URL=
# ENV NEXTAUTH_SECRET=
# ENV ADMIN_USER=
# ENV ADMIN_PASSWORD=

CMD ["node", "server.js"]