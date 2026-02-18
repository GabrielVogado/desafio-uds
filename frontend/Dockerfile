# Multi-stage build for Angular frontend
FROM node:18-alpine AS builder

WORKDIR /app
COPY frontend/package*.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build

# Production stage
FROM nginx:alpine

COPY --from=builder /app/dist/desafio-uds-frontend /usr/share/nginx/html

# Copy nginx configuration
RUN echo 'server { \
  listen 80; \
  location / { \
    root /usr/share/nginx/html; \
    try_files $uri $uri/ /index.html; \
  } \
  location /api { \
    proxy_pass http://host.docker.internal:8080; \
    proxy_set_header Host $host; \
    proxy_set_header X-Real-IP $remote_addr; \
  } \
}' > /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]

