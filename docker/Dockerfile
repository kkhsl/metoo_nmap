FROM java:8

MAINTAINER whhc

WORKDIR /

ADD nmap.jar /opt/nmap/service/nmap/nmap.jar

EXPOSE 7070


ENTRYPOINT ["java", "-server", "-Xms.512m", "-Xmx512m", "-jar", "/opt/java/project/nmap/release/nmap.jar"]
#docker build -t 新镜像名字:TAG .



