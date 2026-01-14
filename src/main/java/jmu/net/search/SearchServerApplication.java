package jmu.net.search;

import jmu.net.search.service.FileManageService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import jakarta.annotation.Resource;
import org.springframework.boot.CommandLineRunner;

@SpringBootApplication
public class SearchServerApplication implements CommandLineRunner {
    @Resource
    private FileManageService fileManageService;

    public static void main(String[] args) {
        SpringApplication.run(SearchServerApplication.class, args);
    }

    // 项目启动时执行索引初始化
    @Override
    public void run(String... args) throws Exception {
        fileManageService.initDocIndex();
    }
}