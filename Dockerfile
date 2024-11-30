# 使用官方的Java开发镜像作为基础镜像
FROM openjdk:8-jdk-alpine
MAINTAINER haoshenqi/shenqivpn@yahoo.com
# 设置工作目录
WORKDIR /usr/src/app

# 将你的应用的jar文件复制到Docker镜像中
COPY ./target/holiday-2.0.0.jar app.jar

RUN echo "Asia/Shanghai" > /etc/timezone
EXPOSE 8001
ENTRYPOINT ["java","-jar","/app.jar"]

