package jmu.net.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SearchServerApplication { // 类名和文件名一致
    public static void main(String[] args) {
        SpringApplication.run(SearchServerApplication.class, args);
        System.out.println("✅ 服务启动成功！端口：8080 | 文档目录： ./docs");
    }
}