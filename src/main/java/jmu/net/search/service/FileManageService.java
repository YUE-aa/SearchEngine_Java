package jmu.net.search.service;

import jmu.net.search.constant.FileConstant;
import jmu.net.search.util.LuceneUtil;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class FileManageService {
    private ScheduledExecutorService scheduler;

    public void initDocIndex() throws Exception {
        File docDir = new File(FileConstant.DOC_ROOT_DIR);
        if (!docDir.exists()) {
            docDir.mkdirs();
        }
        LuceneUtil.createIndex(docDir);
    }

    public void startIndexRefreshTask(long intervalSeconds) {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("index-refresh-thread");
            thread.setDaemon(true);
            return thread;
        });

        scheduler.scheduleAtFixedRate(() -> {
            try {
                File docDir = new File(FileConstant.DOC_ROOT_DIR);
                LuceneUtil.createIndex(docDir);
                System.out.println("索引自动刷新完成");
            } catch (Exception e) {
                System.err.println("索引刷新失败：" + e.getMessage());
            }
        }, 0, intervalSeconds, TimeUnit.SECONDS);
    }

    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
}