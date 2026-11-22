FROM maven:3.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /build
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -pl ${MODULE} -am package -DskipTests

FROM eclipse-temurin:21-jre
ARG MODULE
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /build/${MODULE}/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]