FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
RUN mkdir -p /home/ruoyi/uploadPath /app/logs
COPY ruoyi-admin.jar /app/ruoyi-admin.jar
EXPOSE 8080
ENTRYPOINT ["sh","-c","java -jar /app/ruoyi-admin.jar --ruoyi.profile=/home/ruoyi/uploadPath ${SPRING_ARGS}"]
