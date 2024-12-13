FROM sbtscala/scala-sbt:eclipse-temurin-23.0.1_11_1.10.5_3.5.2

ARG executable=executable.jar

EXPOSE 8080

VOLUME [ "/data/db" ]

WORKDIR /

COPY ${executable} /executable.jar

ENTRYPOINT [ "java", "-jar", "executable.jar" ]
