FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B
COPY src/ src/
RUN ./mvnw package -DskipTests -B && \
    mv target/sgo-*.jar target/app.jar

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S sgo && adduser -S sgo -G sgo
WORKDIR /app
COPY --from=build /build/target/app.jar app.jar
RUN mkdir -p /app/logs && chown -R sgo:sgo /app
USER sgo
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
