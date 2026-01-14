package jmu.net.search.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jmu.net.search.dto.SearchResultDTO;
import jmu.net.search.service.LuceneSearchService;
import jmu.net.search.util.LogUtils;
import jmu.net.search.vo.ResultVo;
import org.apache.lucene.document.Document;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class SearchController {
    @Resource
    private LuceneSearchService luceneSearchService;

    @GetMapping("/api/search")
    public ResultVo search(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "useAI", defaultValue = "false") boolean useAI,
            @RequestParam(value = "needRag", defaultValue = "false") boolean needRag,
            HttpServletRequest request) {
        String clientIp = LogUtils.getClientIp(request);
        String trimKeyword = keyword.trim();
        try {
            // 纯Lucene搜索
            if (!useAI) {
                List<Document> documents = luceneSearchService.pureLuceneSearch(trimKeyword);
                List<SearchResultDTO> resultList = documents.stream().map(doc -> {
                    String fileName = doc.get("fileName");
                    String content = doc.get("content");
                    String summary = luceneSearchService.getSummary(content, trimKeyword);
                    return new SearchResultDTO(fileName, summary);
                }).collect(Collectors.toList());
                LogUtils.writeLog(clientIp, "检索文件（纯Lucene）", "关键词：" + trimKeyword + " | 匹配数：" + resultList.size());
                return ResultVo.success("纯Lucene检索成功", new JSONArray(resultList));
            }

            // AI混合搜索
            String optimizedKeyword = luceneSearchService.optimizeQuery(trimKeyword);
            LogUtils.writeLog(clientIp, "检索文件（AI混合）", "原始关键词：" + trimKeyword + " | 优化后：" + optimizedKeyword);
            List<Document> documents = luceneSearchService.hybridSearch(optimizedKeyword, true);
            List<SearchResultDTO> resultList = documents.stream().map(doc -> {
                String fileName = doc.get("fileName");
                String content = doc.get("content");
                String summary = luceneSearchService.getSummary(content, optimizedKeyword);
                return new SearchResultDTO(fileName, summary);
            }).collect(Collectors.toList());

            String ragAnswer = "";
            if (needRag && !documents.isEmpty()) {
                ragAnswer = luceneSearchService.generateRagAnswer(trimKeyword, documents);
            }

            JSONObject resultData = new JSONObject();
            resultData.put("searchResults", resultList);
            resultData.put("ragAnswer", ragAnswer);
            resultData.put("optimizedKeyword", optimizedKeyword);
            LogUtils.writeLog(clientIp, "检索成功（AI混合）", "关键词：" + optimizedKeyword + " | 匹配数：" + resultList.size());
            return ResultVo.success("AI混合检索成功", resultData);
        } catch (Exception e) {
            String errorLog = "关键词：" + trimKeyword + " | 异常：" + e.getMessage();
            LogUtils.writeLog(clientIp, "检索异常", errorLog);
            return ResultVo.error("检索失败：" + e.getMessage());
        }
    }
}