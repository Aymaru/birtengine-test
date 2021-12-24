FROM openjdk:11-jdk

RUN addgroup --system spring 
RUN adduser --system spring
RUN adduser spring spring

USER spring:spring

ARG JAR_FILE=target/*.jar

COPY ${JAR_FILE} /app/app.jar
COPY reports/ /app/reports/
COPY images/ /app/images/

WORKDIR /app

ENTRYPOINT ["java","-jar","/app/app.jar"]
