# Multi-stage Dockerfile for Kendrick Labernetes
# Build frontend
FROM node:22 AS frontend-build
WORKDIR /app/frontend
# Copy only package.json and yarn.lock (if exists)
COPY frontend/package.json frontend/yarn.lock* ./
# Install dependencies
RUN npm install --legacy-peer-deps
# Copy only src, public folders, and tsconfig.json needed for build
COPY frontend/src ./src
COPY frontend/public ./public
COPY frontend/tsconfig.json ./tsconfig.json
# Build the frontend
RUN npm run build

# Build backend
FROM eclipse-temurin:17-jdk AS backend-build
WORKDIR /app/backend
RUN apt-get update && apt-get install -y maven
COPY backend/pom.xml ./
COPY backend/src ./src
RUN mvn package -DskipTests


# Final image with nginx for frontend
FROM eclipse-temurin:17-jre AS base
WORKDIR /app
COPY --from=backend-build /app/backend/target/*.jar app.jar
COPY welcome.txt ./welcome.txt

FROM nginx:1.25 AS frontend-server
WORKDIR /usr/share/nginx/html
COPY --from=frontend-build /app/frontend/build .

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=base /app/app.jar app.jar
COPY --from=base /app/welcome.txt welcome.txt
RUN apt-get update && apt-get install -y nginx
COPY --from=frontend-build /app/frontend/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
# Remove all other nginx configs except our own
RUN find /etc/nginx/conf.d/ -type f ! -name 'default.conf' -delete
RUN [ ! -d /etc/nginx/sites-enabled ] || rm -rf /etc/nginx/sites-enabled/*
EXPOSE 8080 80
ENV SPRING_PROFILES_ACTIVE=prod
ENV REMOTE_DB=false
CMD ["sh", "-c", "nginx -g 'daemon off;' & java -jar app.jar"]
