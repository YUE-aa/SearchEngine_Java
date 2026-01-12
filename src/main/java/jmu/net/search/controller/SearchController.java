package jmu.net.search.controller;

import jmu.net.search.constant.FileConstant;
import jmu.net.search.service.LuceneSearchService;
import org.apache.lucene.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class SearchController {
    @Autowired
    private LuceneSearchService searchService;

    // 关键修复：必须加 @RequestParam("keyword") 显式指定参数名
    @GetMapping("/api/search")
    public ResultVo search(@RequestParam("keyword") String keyword) {
        try {
            List<Document> documents = searchService.search(keyword);
            List<SearchResultDTO> resultList = documents.stream().map(doc -> {
                String fileName = doc.get(FileConstant.FIELD_NAME);
                String summary = searchService.getSummary(doc.get(FileConstant.FIELD_CONTENT), keyword);
                return new SearchResultDTO(fileName, summary);
            }).collect(Collectors.toList());
            return ResultVo.success("搜索成功", resultList);
        } catch (Exception e) {
            return ResultVo.error("搜索失败：" + e.getMessage());
        }
    }

    static class SearchResultDTO {
        private String fileName;
        private String summary;

        public SearchResultDTO(String fileName, String summary) {
            this.fileName = fileName;
            this.summary = summary;
        }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
    }

    static class ResultVo {
        private int code;
        private String msg;
        private Object data;

        public static ResultVo success(String msg, Object data) {
            ResultVo vo = new ResultVo();
            vo.code = 200;
            vo.msg = msg;
            vo.data = data;
            return vo;
        }

        public static ResultVo error(String msg) {
            ResultVo vo = new ResultVo();
            vo.code = 500;
            vo.msg = msg;
            vo.data = null;
            return vo;
        }

        public int getCode() { return code; }
        public void setCode(int code) { this.code = code; }
        public String getMsg() { return msg; }
        public void setMsg(String msg) { this.msg = msg; }
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
    }
}