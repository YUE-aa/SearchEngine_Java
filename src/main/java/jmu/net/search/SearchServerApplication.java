package jmu.net.search;

import jmu.net.search.service.FileManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SearchServerApplication implements CommandLineRunner {
    @Autowired
    private FileManageService fileManageService;

    @Value("${lucene.index.refresh-interval:3600}")
    private long indexRefreshInterval;

    public static void main(String[] args) {
        SpringApplication.run(SearchServerApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        fileManageService.initDocIndex();
        fileManageService.startIndexRefreshTask(indexRefreshInterval);
        System.out.println("✅ 服务端启动成功！端口：8080 | 文档目录：./docs");
    }
}