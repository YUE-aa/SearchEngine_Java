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
    public static void createIndex(File docDir) throws Exception {
        if (!docDir.exists() || !docDir.isDirectory()) {
            return;
        }
        Analyzer analyzer = new SmartChineseAnalyzer();
        Directory directory = FSDirectory.open(Paths.get(FileConstant.INDEX_DIR));
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter indexWriter = new IndexWriter(directory, config);

        File[] files = docDir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                createIndex(file);
            } else {
                String fileName = file.getName();
                boolean isSupport = DocumentParseUtil.isSupportFile(fileName);
                if (isSupport) {
                    String content = DocumentParseUtil.parseFileContent(file);
                    Document document = new Document();
                    document.add(new TextField(FileConstant.FIELD_NAME, fileName, Field.Store.YES));
                    document.add(new TextField(FileConstant.FIELD_CONTENT, content, Field.Store.YES));
                    document.add(new StoredField(FileConstant.FIELD_FILE_PATH, file.getAbsolutePath()));
                    indexWriter.addDocument(document);
                }
            }
        }
        indexWriter.close();
        directory.close();
    }

    public static List<Document> search(String keyword) throws Exception {
        Analyzer analyzer = new SmartChineseAnalyzer();
        Directory directory = FSDirectory.open(Paths.get(FileConstant.INDEX_DIR));
        DirectoryReader reader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(reader);

        String[] fields = {FileConstant.FIELD_NAME, FileConstant.FIELD_CONTENT};
        MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer);
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
}