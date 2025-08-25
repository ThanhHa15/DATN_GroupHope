
FROM infotechsoft/maven:3-openjdk-21 AS build
WORKDIR /app
COPY datn/pom.xml .
COPY datn/src ./src
RUN mvn clean package -DskipTests

FROM openjdk:21-jdk-slim
WORKDIR /app
COPY --from=build /app/target/datn-0.0.1-SNAPSHOT.jar ./app.jar
CMD ["java", "-jar", "app.jar"]
