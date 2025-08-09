#FROM eclipse-temurin:17-jdk-alpine
#WORKDIR /opt
#ENV PORT=8091
#EXPOSE 8091
#COPY target/student.jar /opt/student.jar
#ENTRYPOINT ["java", "-jar", "/opt/rest-demo.jar"]

FROM eclipse-temurin:17-jdk-alpine
WORKDIR /opt
ENV PORT 8091
EXPOSE 8091
COPY target/student.jar /opt/student.jar
ENTRYPOINT exec java $JAVA_OPTS -jar student.jar