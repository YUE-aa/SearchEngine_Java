package jmu.net.search.service;

import jmu.net.search.util.LuceneUtil;
import org.apache.lucene.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LuceneSearchService {

    public List<Document> search(String keyword) throws Exception {
        return LuceneUtil.search(keyword);
    }

    public String getSummary(String content, String keyword) {
        if (content == null || keyword == null || content.isEmpty() || keyword.isEmpty()) {
            return "";
        }
        int keywordIndex = content.toLowerCase().indexOf(keyword.toLowerCase());
        if (keywordIndex == -1) {
            return content.length() > 200 ? content.substring(0, 200) + "..." : content;
        }
        int start = Math.max(0, keywordIndex - 100);
        int end = Math.min(content.length(), keywordIndex + keyword.length() + 100);
        String summary = content.substring(start, end);
        if (start > 0) summary = "..." + summary;
        if (end < content.length()) summary += "...";
        return summary;
    }
}