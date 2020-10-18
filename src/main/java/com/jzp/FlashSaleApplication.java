package com.jzp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.jzp"})
@MapperScan("com.jzp.mapper")
public class FlashSaleApplication {

    public static void main(String[] args) {
        System.out.println("Hello World!");
        SpringApplication.run(FlashSaleApplication.class, args);
    }
}
