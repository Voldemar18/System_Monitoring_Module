package ru.student.testing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

@SpringBootApplication
@EnableScheduling
@EnableWebSocket
public class MonitoringApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonitoringApplication.class, args);

        System.out.println("""
                \n
                1. Система мониторинга запущена!
                2. Доступна по адресу: http://localhost:8080
                3. Swagger UI: http://localhost:8080/swagger-ui.html
                4. Health Check: http://localhost:8080/actuator/health
                
                """);
    }
}