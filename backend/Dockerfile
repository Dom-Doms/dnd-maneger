FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace

RUN apk add --no-cache maven

COPY . ./

RUN if [ -f ./mvnw ]; then chmod +x ./mvnw && ./mvnw -q -DskipTests package; else mvn -q -DskipTests package; fi

FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup \
    && mkdir -p /app/uploads \
    && chown -R appuser:appgroup /app

COPY --from=build /workspace/target/*.jar /app/app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
