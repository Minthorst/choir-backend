FROM maven:3.9-amazoncorretto-21 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM amazoncorretto:21-alpine
WORKDIR /app

RUN apk add --no-cache tzdata
ENV TZ=Europe/Berlin

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]