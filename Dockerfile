FROM openjdk:8

RUN mkdir -p /opt/ringjo
COPY build/libs/ringjo*.jar /opt/ringjo/ringjo.jar

WORKDIR /opt/ringjo
ENTRYPOINT ["java", "-jar", "ringjo.jar"]
EXPOSE 8080