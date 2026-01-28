package com.woorido;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan("com.woorido.common.mapper")
public class WooridoApplication {

    public static void main(String[] args) {
        // Oracle Wallet 경로 강제 설정 (접속 오류 해결용)
        System.setProperty("oracle.net.tns_admin", "C:/oracle/wallet");
        SpringApplication.run(WooridoApplication.class, args);
    }
}
