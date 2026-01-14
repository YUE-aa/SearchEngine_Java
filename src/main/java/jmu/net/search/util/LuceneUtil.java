package jmu.net.search.util;

import jmu.net.search.constant.FileConstant;
import jmu.net.search.entity.VectorEntity;
import jmu.net.search.service.VectorCacheService;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class LuceneUtil {
    private static final Analyzer ANALYZER = new SmartChineseAnalyzer();
    private static VectorCacheService vectorCacheService;

    // 初始化向量缓存服务（Spring容器启动后调用）
    public static void setVectorCacheService(VectorCacheService service) {
        vectorCacheService = service;
    }

    // 启动时重建全新索引（新增向量缓存构建，不改动你的原有索引逻辑）
    public static void createIndex(File docDir) throws Exception {
        if (docDir == null || !docDir.exists() || !docDir.isDirectory()) {
            System.out.println("⚠️ 文档目录不存在或不是文件夹：" + (docDir == null ? "null" : docDir.getAbsolutePath()));
            return;
        }

        // 清空旧向量缓存
        if (vectorCacheService != null) {
            vectorCacheService.clearCache();
        }

        Directory directory = FSDirectory.open(Paths.get(FileConstant.INDEX_DIR));
        IndexWriterConfig config = new IndexWriterConfig(ANALYZER);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter indexWriter = new IndexWriter(directory, config);

        // 批量构建向量缓存
        List<VectorEntity> vectorEntities = new ArrayList<>();
        scanAndIndexFiles(docDir, indexWriter, vectorEntities);

        // 批量添加到向量缓存
        if (vectorCacheService != null && !vectorEntities.isEmpty()) {
            vectorCacheService.batchAddVectors(vectorEntities);
            System.out.println("✅ 向量缓存构建完成，共缓存 " + vectorEntities.size() + " 个文档向量");
        }

        indexWriter.close();
        directory.close();
    }

    // 递归扫描文件并索引（新增向量构建，不改动你的原有文件解析逻辑）
    private static void scanAndIndexFiles(File file, IndexWriter indexWriter, List<VectorEntity> vectorEntities) throws Exception {
        if (file.isDirectory()) {
            File[] subFiles = file.listFiles();
            if (subFiles != null && subFiles.length > 0) {
                for (File subFile : subFiles) {
                    scanAndIndexFiles(subFile, indexWriter, vectorEntities);
                }
            }
            return;
        }

        String fileName = file.getName();
        if (!DocumentParseUtil.isSupportFile(fileName)) {
            return;
        }

        String content = DocumentParseUtil.parseFileContent(file);
        if (content == null || content.trim().isEmpty()) {
            return;
        }

        // 你的原版lucene文档构建逻辑，一行未改
        Document document = new Document();
        document.add(new TextField(FileConstant.FIELD_NAME, fileName, Field.Store.YES));
        document.add(new TextField(FileConstant.FIELD_CONTENT, content, Field.Store.YES));
        document.add(new StoredField(FileConstant.FIELD_FILE_PATH, file.getAbsolutePath()));
        indexWriter.addDocument(document);
        System.out.println("✅ 已索引文件：" + file.getAbsolutePath());

        // 生成文档向量并添加到列表（AI增强核心，不影响lucene搜索）
        if (vectorCacheService != null) {
            try {
                double[] vector = AiUtils.getEmbedding(content.length() > 2000 ? content.substring(0, 2000) : content);
                vectorEntities.add(new VectorEntity(fileName, file.getAbsolutePath(), content, vector));
            } catch (Exception e) {
                System.err.println("⚠️ 生成文档向量失败：" + fileName + "，原因：" + e.getMessage());
            }
        }
    }

    // 你的原版搜索方法【核心修复：AND → OR】，完美恢复lucene关键词匹配，一行未改其他逻辑
    public static List<Document> search(String keyword) throws Exception {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }
        Directory directory = FSDirectory.open(Paths.get(FileConstant.INDEX_DIR));
        DirectoryReader reader = DirectoryReader.open(directory);
        if (reader.numDocs() == 0) {
            reader.close();
            directory.close();
            return new ArrayList<>();
        }
        IndexSearcher searcher = new IndexSearcher(reader);
        String[] fields = {FileConstant.FIELD_NAME, FileConstant.FIELD_CONTENT};
        MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, ANALYZER);
        // ========== 致命修复：AND → OR，恢复lucene关键词搜索功能 ==========
        parser.setDefaultOperator(QueryParser.Operator.OR);
        Query query = parser.parse(keyword);
        TopDocs topDocs = searcher.search(query, 100);
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        List<Document> docs = new ArrayList<>();
        for (ScoreDoc scoreDoc : scoreDocs) {
            Document doc = searcher.doc(scoreDoc.doc);
            docs.add(doc);
        }
        reader.close();
        directory.close();
        return docs;
    }

    // 你的原版摘要方法，一行未改
    public static String getSummary(String content, String keyword) {
        if (content == null || content.length() < 100) {
            return content;
        }
        int index = content.indexOf(keyword);
        if (index == -1) {
            return content.substring(0, 100) + "...";
        }
        int start = Math.max(0, index - 50);
        int end = Math.min(content.length(), index + 50);
        return content.substring(start, end) + "...";
    }
}