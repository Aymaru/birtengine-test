FROM openjdk:8-jdk-alpine
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} /app/app.jar
COPY reports/ /app/reports
COPY images/ /app/images
ENTRYPOINT ["java","-jar","/app/app.jar"]