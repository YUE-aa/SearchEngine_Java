package jmu.net.search.util;

import jmu.net.search.constant.FileConstant;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
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

/**
 * Lucene核心工具类：创建索引、执行查询
 * 最终版：使用Lucene官方中文分词器 无依赖报错 + JDK21完美兼容 + 无文件不报错
 */
public class LuceneUtil {

    /**
     * 创建Lucene索引：遍历指定目录下的所有文件，解析内容后创建索引
     * @param docDir 待索引的文档目录
     */
    public static void createIndex(File docDir) throws Exception {
        // 1. 指定索引存储目录 适配JDK21 自动创建目录
        File indexFile = new File(FileConstant.INDEX_DIR);
        if(!indexFile.exists()){
            indexFile.mkdirs();
        }
        Directory directory = FSDirectory.open(Paths.get(FileConstant.INDEX_DIR));

        // 核心：Lucene官方中文分词器，100%无依赖问题
        Analyzer analyzer = new SmartChineseAnalyzer();

        // 3. 配置索引写入器
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter indexWriter = new IndexWriter(directory, config);

        // 4. 遍历文档目录下的所有文件
        File[] files = docDir.listFiles();
        int count = 0;
        if (files != null && files.length > 0) {
            for (File file : files) {
                if (file.isFile()) {
                    // 解析文件内容
                    String content = DocumentParseUtil.parseFileToText(file);
                    if (content.isEmpty()) {
                        System.out.println("ℹ️ 跳过不支持的文件：" + file.getName());
                        continue;
                    }
                    // 创建Lucene的Document对象，封装字段
                    Document document = new Document();
                    document.add(new TextField(FileConstant.FIELD_NAME, file.getName(), Field.Store.YES));
                    document.add(new TextField(FileConstant.FIELD_CONTENT, content, Field.Store.YES));
                    // 写入索引
                    indexWriter.addDocument(document);
                    count++;
                }
            }
        }

        // 5. 提交索引并关闭资源
        indexWriter.commit();
        indexWriter.close();
        directory.close();
        System.out.println("✅ 索引创建完成！共为 " + count + " 个文档创建索引");
    }

    /**
     * 执行关键词查询
     * @param keyword 搜索关键词（如：编程）
     * @return 匹配的文档列表
     */
    public static List<Document> search(String keyword) throws Exception {
        // 1. 打开索引目录
        Directory directory = FSDirectory.open(Paths.get(FileConstant.INDEX_DIR));
        DirectoryReader reader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(reader);

        // 核心：Lucene官方中文分词器
        Analyzer analyzer = new SmartChineseAnalyzer();
        QueryParser parser = new QueryParser(FileConstant.FIELD_CONTENT, analyzer);
        Query query = parser.parse(keyword);

        // 3. 执行查询，返回前10条匹配结果
        TopDocs topDocs = searcher.search(query, 10);
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        List<Document> resultList = new ArrayList<>();

        System.out.println("\n🔍 查询关键词：【" + keyword + "】，共匹配到 " + topDocs.totalHits.value + " 个文档");

        // 4. 封装查询结果
        for (ScoreDoc scoreDoc : scoreDocs) {
            Document doc = searcher.doc(scoreDoc.doc);
            resultList.add(doc);
        }

        // 5. 关闭资源
        reader.close();
        directory.close();
        return resultList;
    }
}