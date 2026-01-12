package jmu.net.search.util;

import jmu.net.search.constant.FileConstant;
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

    // 启动时重建全新索引
    public static void createIndex(File docDir) throws Exception {
        if (docDir == null || !docDir.exists() || !docDir.isDirectory()) {
            System.out.println("⚠️ 文档目录不存在或不是文件夹：" + (docDir == null ? "null" : docDir.getAbsolutePath()));
            return;
        }
        Directory directory = FSDirectory.open(Paths.get(FileConstant.INDEX_DIR));
        IndexWriterConfig config = new IndexWriterConfig(ANALYZER);
        // CREATE模式：启动时创建全新索引，无任何历史残留，绝对不会重复
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter indexWriter = new IndexWriter(directory, config);

        scanAndIndexFiles(docDir, indexWriter);

        indexWriter.close();
        directory.close();
    }

    // 递归扫描docs目录下所有文件，正常索引
    private static void scanAndIndexFiles(File file, IndexWriter indexWriter) throws Exception {
        if (file.isDirectory()) {
            File[] subFiles = file.listFiles();
            if (subFiles != null && subFiles.length > 0) {
                for (File subFile : subFiles) {
                    scanAndIndexFiles(subFile, indexWriter);
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
    }

    // 搜索
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
        parser.setDefaultOperator(QueryParser.Operator.AND);
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