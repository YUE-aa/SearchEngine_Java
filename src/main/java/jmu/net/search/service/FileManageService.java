package jmu.net.search.service;

import jmu.net.search.constant.FileConstant;
import jmu.net.search.util.LuceneUtil;
import jmu.net.search.util.LogUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;

/**
 * 文件管理+索引初始化服务
 */
@Service
public class FileManageService {

    /**
     * 项目启动时自动初始化索引（注解：项目启动后执行）
     */
    @PostConstruct
    public void initDocIndex() {
        try {
            File docDir = new File(FileConstant.DOC_ROOT_DIR);
            // 创建文档目录
            if (!docDir.exists()) {
                docDir.mkdirs();
                LogUtils.writeLog("127.0.0.1", "系统操作", "项目启动：docs文档目录不存在，已自动创建，路径=" + docDir.getAbsolutePath());
            }
            // 创建索引目录
            File indexDir = new File(FileConstant.INDEX_DIR);
            if (!indexDir.exists()) {
                indexDir.mkdirs();
                LogUtils.writeLog("127.0.0.1", "系统操作", "项目启动：lucene索引目录不存在，已自动创建，路径=" + indexDir.getAbsolutePath());
            }
            // 初始化文档索引
            LogUtils.writeLog("127.0.0.1", "系统操作", "项目启动：开始扫描docs目录并创建lucene索引");
            LuceneUtil.createIndex(docDir);
            LogUtils.writeLog("127.0.0.1", "系统操作", "项目启动：索引创建完成，扫描完成所有文档");
            System.out.println("✅ 索引初始化完成！");
        } catch (Exception e) {
            LogUtils.writeLog("127.0.0.1", "异常信息", "项目启动失败：索引创建异常，原因=" + e.getMessage());
            e.printStackTrace();
        }
    }
}