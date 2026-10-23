package com.campusbook;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 校园二手书交易平台启动类。
 *
 * <p>@MapperScan 扫描 Mapper 接口，省去每个接口上写 @Mapper。</p>
 * <p>@EnableScheduling 开启定时任务（订单超时自动取消）。</p>
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.campusbook.mapper")
public class CampusBookApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusBookApplication.class, args);
    }
}
