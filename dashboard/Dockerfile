FROM node:24-alpine AS build
WORKDIR /app
ARG SURVEY_API_URL=http://survey:8081
ENV SURVEY_API_URL=$SURVEY_API_URL
COPY dashboard/package.json dashboard/package-lock.json ./
RUN npm ci
COPY dashboard ./
RUN npm run build

FROM node:24-alpine
WORKDIR /app
ENV NODE_ENV=production
COPY --from=build /app/package.json /app/package-lock.json ./
RUN npm ci --omit=dev
COPY --from=build /app/.next ./.next
COPY --from=build /app/public ./public
COPY --from=build /app/next.config.mjs ./next.config.mjs
EXPOSE 3000
CMD ["npm", "run", "start"]