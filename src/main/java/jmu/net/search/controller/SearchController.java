package jmu.net.search.controller;

import jmu.net.search.dto.SearchResultDTO;
import jmu.net.search.util.LogUtils;
import jmu.net.search.util.LuceneUtil;
import jmu.net.search.vo.ResultVo;
import org.apache.lucene.document.Document;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class SearchController {

    @GetMapping("/api/search")
    public ResultVo search(@RequestParam("keyword") String keyword, HttpServletRequest request) {
        String clientIp = LogUtils.getClientIp(request);
        String trimKeyword = keyword.trim();

        try {
            // 执行检索逻辑
            List<Document> documents = LuceneUtil.search(trimKeyword);
            List<SearchResultDTO> resultList = documents.stream().map(doc -> {
                String fileName = doc.get("fileName");
                String content = doc.get("content");
                return new SearchResultDTO(fileName, content);
            }).collect(Collectors.toList());

            // ✅ 核心优化：合并成1条日志，记录完整信息
            String logContent = "检索关键词：" + trimKeyword + " | 匹配文档数：" + resultList.size();
            LogUtils.writeLog(clientIp, "检索文件", logContent);

            return ResultVo.success("检索成功", resultList);
        } catch (Exception e) {
            // 异常日志也保留，方便排查问题
            String errorLog = "检索关键词：" + trimKeyword + " | 异常原因：" + e.getMessage();
            LogUtils.writeLog(clientIp, "异常信息", errorLog);
            return ResultVo.error("检索失败：" + e.getMessage());
        }
    }
}