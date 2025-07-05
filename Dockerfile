FROM eclipse-temurin:21

WORKDIR /app

COPY ./target/auth-service-1.0.0.jar /app/auth-service-1.0.0.jar

EXPOSE 8081

CMD ["java", "-jar", "auth-service-1.0.0.jar"]