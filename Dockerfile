FROM openjdk:8

RUN mkdir -p /opt/ringjo
COPY build/libs/ringjo-1.0.jar /opt/ringjo/ringjo.jar

WORKDIR /opt/ringjo
ENTRYPOINT ["java", "-jar", "ringjo.jar"]
EXPOSE 8080