FROM openjdk:10-jdk-slim

ENV ENV docker

WORKDIR /opt/sentinel

COPY sentinel.example.yaml /opt/sentinel/sentinel.yaml
COPY sentinel.jar /opt/sentinel/sentinel.jar
CMD ["source", "./common.sh"]

ENTRYPOINT ["java", "-Xmx64m", "-jar", "sentinel.jar"]
