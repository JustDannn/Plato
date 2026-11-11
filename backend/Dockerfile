FROM oven/bun:1.3-alpine

WORKDIR /app

COPY package.json bun.lock ./
RUN bun install --frozen-lockfile

COPY tsconfig.json ./
COPY src ./src
COPY migrations ./migrations
COPY tests ./tests

EXPOSE 8080

CMD ["bun", "--watch", "src/index.ts"]
