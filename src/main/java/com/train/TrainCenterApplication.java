package com.train;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
@MapperScan("com.train.mapper")  // Mapper接口扫描
public class TrainCenterApplication {
    public static void main(String[] args) {
        SpringApplication.run(TrainCenterApplication.class, args);
    }
}