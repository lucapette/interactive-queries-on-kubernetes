FROM openjdk:11-jre
VOLUME /tmp
COPY build/libs/playground.jar app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
