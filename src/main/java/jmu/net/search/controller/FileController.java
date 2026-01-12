package jmu.net.search.controller;

import jmu.net.search.constant.FileConstant;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@RestController
@RequestMapping("/api/file")
public class FileController {

    @PostMapping("/upload")
    public ResultVo uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            File docDir = new File(FileConstant.DOC_ROOT_DIR);
            if (!docDir.exists()) docDir.mkdirs();
            File targetFile = new File(docDir, file.getOriginalFilename());
            file.transferTo(targetFile);
            return ResultVo.success("文件上传成功", targetFile.getName());
        } catch (Exception e) {
            return ResultVo.error("文件上传失败：" + e.getMessage());
        }
    }

    // 关键修复：下载接口也必须加 @RequestParam("fileName")
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam("fileName") String fileName) {
        try {
            File file = new File(FileConstant.DOC_ROOT_DIR, fileName);
            if (!file.exists()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            Resource resource = new FileSystemResource(file);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
            return new ResponseEntity<>(resource, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
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