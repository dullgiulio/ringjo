FROM fabric8/java-jboss-openjdk8-jdk:1.3.1

ENV JAVA_APP_JAR ringjo.jar
ENV JAVA_MAIN_CLASS com.github.dullgiulio.ringjo.Ringjo

EXPOSE 8080

ADD target/ringjo.jar /