FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S voltra && adduser -S voltra -G voltra
COPY --from=build /app/target/voltra-platform-3.1.0.jar app.jar
USER voltra
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
