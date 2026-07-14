FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /build
COPY pom.xml ./
RUN mvn dependency:go-offline -B
COPY src/ src/
RUN mvn package -DskipTests -B && \
    mv target/sgo-*.jar target/app.jar

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S sgo && adduser -S sgo -G sgo
WORKDIR /app
COPY --from=build /build/target/app.jar app.jar
RUN mkdir -p /app/logs /data/almacen && chown -R sgo:sgo /app /data/almacen
USER sgo
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
