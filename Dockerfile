FROM openjdk:8-jre
VOLUME /tmp
COPY build/libs/playground.jar app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
