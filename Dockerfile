FROM openjdk:alpine

VOLUME /tmp

RUN mkdir common

WORKDIR common

COPY build/libs/azure-blob-management-service-*.jar app.jar

CMD [ "sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -jar app.jar" ]