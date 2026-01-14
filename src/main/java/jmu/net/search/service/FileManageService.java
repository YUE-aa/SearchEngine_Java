package jmu.net.search.service;

import jmu.net.search.constant.FileConstant; // 包路径与FileConstant一致
import jmu.net.search.service.LuceneSearchService;
import jmu.net.search.service.VectorCacheService;
import jmu.net.search.util.LogUtils;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import java.io.File;

@Service
public class FileManageService {
    @Resource
    private VectorCacheService vectorCacheService;
    @Resource
    private LuceneSearchService luceneSearchService;

    public void initDocIndex() {
        try {
            // 改回你原来的变量名：DOC_ROOT_DIR
            File docDir = new File(FileConstant.DOC_ROOT_DIR);
            if (!docDir.exists()) {
                docDir.mkdirs();
                LogUtils.writeLog("本地", "系统操作", "项目启动：docs文档目录不存在，已自动创建，路径=" + docDir.getAbsolutePath());
            }

            // 改回你原来的变量名：INDEX_DIR
            File indexDir = new File(FileConstant.INDEX_DIR);
            if (!indexDir.exists()) {
                indexDir.mkdirs();
                LogUtils.writeLog("本地", "系统操作", "项目启动：Lucene索引目录不存在，已自动创建，路径=" + indexDir.getAbsolutePath());
            }

            LogUtils.writeLog("本地", "系统操作", "项目启动：开始扫描docs目录并创建Lucene索引（含AI向量）");
            luceneSearchService.createIndex(docDir);
            LogUtils.writeLog("本地", "系统操作", "项目启动：索引创建完成，向量缓存数：" + vectorCacheService.getCacheSize());
        } catch (Exception e) {
            LogUtils.writeLog("本地", "异常信息", "项目启动失败：索引创建异常，原因=" + e.getMessage());
        }
    }
}