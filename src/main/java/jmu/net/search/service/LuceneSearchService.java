package jmu.net.search.service;

import jmu.net.search.constant.FileConstant;
import jmu.net.search.entity.VectorEntity;
import jmu.net.search.util.AiUtils;
import jmu.net.search.util.DocumentParseUtil;
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
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class LuceneSearchService {
    private static final Analyzer ANALYZER = new SmartChineseAnalyzer();
    private final VectorCacheService vectorCacheService;

    public LuceneSearchService(VectorCacheService vectorCacheService) {
        this.vectorCacheService = vectorCacheService;
    }

    // 启动时重建索引（含向量缓存）
    public void createIndex(File docDir) throws Exception {
        if (docDir == null || !docDir.exists() || !docDir.isDirectory()) {
            System.out.println("⚠️ 文档目录不存在或不是文件夹：" + (docDir == null ? "null" : docDir.getAbsolutePath()));
            return;
        }

        vectorCacheService.clearCache();
        Directory directory = FSDirectory.open(Paths.get(FileConstant.INDEX_DIR));
        IndexWriterConfig config = new IndexWriterConfig(ANALYZER);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter indexWriter = new IndexWriter(directory, config);

        List<VectorEntity> vectorEntities = new ArrayList<>();
        scanAndIndexFiles(docDir, indexWriter, vectorEntities);

        if (!vectorEntities.isEmpty()) {
            vectorCacheService.batchAddVectors(vectorEntities);
            System.out.println("✅ 向量缓存构建完成，共缓存 " + vectorEntities.size() + " 个文档向量");
        }

        indexWriter.close();
        directory.close();
    }

    private void scanAndIndexFiles(File file, IndexWriter indexWriter, List<VectorEntity> vectorEntities) throws Exception {
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

        Document document = new Document();
        document.add(new TextField(FileConstant.FIELD_NAME, fileName, Field.Store.YES));
        document.add(new TextField(FileConstant.FIELD_CONTENT, content, Field.Store.YES));
        document.add(new StoredField(FileConstant.FIELD_FILE_PATH, file.getAbsolutePath()));
        indexWriter.addDocument(document);
        System.out.println("✅ 已索引文件：" + file.getAbsolutePath());

        try {
            double[] vector = AiUtils.getEmbedding(content.length() > 2000 ? content.substring(0, 2000) : content);
            vectorEntities.add(new VectorEntity(fileName, file.getAbsolutePath(), content, vector));
        } catch (Exception e) {
            System.err.println("⚠️ 生成文档向量失败：" + fileName + "，原因：" + e.getMessage());
        }
    }

    // 纯Lucene搜索方法
    public List<Document> pureLuceneSearch(String keyword) throws Exception {
        Directory directory = FSDirectory.open(Paths.get(FileConstant.INDEX_DIR));
        DirectoryReader reader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(reader);

        String[] fields = {FileConstant.FIELD_NAME, FileConstant.FIELD_CONTENT};
        MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, ANALYZER);
        parser.setDefaultOperator(QueryParser.Operator.OR);
        Query query = parser.parse(keyword);

        TopDocs topDocs = searcher.search(query, 100);
        List<Document> docs = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            docs.add(searcher.doc(scoreDoc.doc));
        }
        reader.close();
        directory.close();
        return docs;
    }

    // AI混合搜索方法
    public List<Document> hybridSearch(String keyword, boolean useAI) throws Exception {
        double[] queryVector = AiUtils.getEmbedding(keyword);
        List<VectorEntity> similarVectors = vectorCacheService.searchSimilarVectors(queryVector, 10);

        List<Document> docs = new ArrayList<>();
        Directory directory = FSDirectory.open(Paths.get(FileConstant.INDEX_DIR));
        DirectoryReader reader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(reader);

        for (VectorEntity vec : similarVectors) {
            Query query = new QueryParser(FileConstant.FIELD_NAME, ANALYZER).parse(QueryParser.escape(vec.getFileName()));
            TopDocs topDocs = searcher.search(query, 1);
            if (topDocs.scoreDocs.length > 0) {
                docs.add(searcher.doc(topDocs.scoreDocs[0].doc));
            }
        }
        reader.close();
        directory.close();
        return docs;
    }

    // 关键词优化（调用AiUtils）
    public String optimizeQuery(String keyword) {
        return AiUtils.optimizeQuery(keyword);
    }

    // 生成RAG回答（调用AiUtils）
    public String generateRagAnswer(String query, List<Document> docs) {
        StringBuilder context = new StringBuilder();
        for (Document doc : docs) {
            context.append("文件：").append(doc.get(FileConstant.FIELD_NAME))
                    .append("\n内容：").append(doc.get(FileConstant.FIELD_CONTENT)).append("\n\n");
        }
        return AiUtils.generateRagAnswer(query, context.toString());
    }

    // 获取摘要
    public String getSummary(String content, String keyword) {
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