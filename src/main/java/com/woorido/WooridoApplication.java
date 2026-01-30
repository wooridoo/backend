package com.woorido;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan(basePackages = "com.woorido", annotationClass = Mapper.class)
public class WooridoApplication {

    public static void main(String[] args) {
        System.setProperty("oracle.net.tns_admin", "C:/oracle/wallet");
        SpringApplication.run(WooridoApplication.class, args);
    }
}
