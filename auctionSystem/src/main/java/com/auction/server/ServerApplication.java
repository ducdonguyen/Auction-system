package com.auction.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
// Chỉ định Spring quét các Entity và Repository trong toàn bộ dự án
@EntityScan(basePackages = {"com.auction"})
@EnableJpaRepositories(basePackages = {"com.auction"})
@EnableScheduling
public class ServerApplication {

    public static void main(String[] args) {
        // Lệnh khởi chạy Spring Boot Server
        SpringApplication.run(ServerApplication.class, args);
        System.out.println("=== SERVER ĐẤU GIÁ ĐÃ KHỞI ĐỘNG THÀNH CÔNG ===");
        System.out.println("Cổng mặc định: 8080");
    }
}