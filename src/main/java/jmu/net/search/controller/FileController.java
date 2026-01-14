package jmu.net.search.controller;

import jmu.net.search.constant.FileConstant; // 包路径与FileConstant一致
import jmu.net.search.util.LogUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import java.io.File;

@RestController
public class FileController {
    @GetMapping("/api/file/download")
    public ResponseEntity<Resource> downloadFile(
            @RequestParam("fileName") String fileName,
            HttpServletRequest request) {
        String clientIp = LogUtils.getClientIp(request);
        try {
            // 改回你原来的变量名：DOC_ROOT_DIR
            File file = new File(FileConstant.DOC_ROOT_DIR, fileName);

            if (!file.exists()) {
                LogUtils.writeLog(clientIp, "异常信息", "下载失败：文件不存在，文件名=" + fileName);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            if (!file.isFile()) {
                LogUtils.writeLog(clientIp, "异常信息", "下载失败：不是有效文件，文件名=" + fileName);
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            Resource resource = new FileSystemResource(file);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);

            LogUtils.writeLog(clientIp, "下载文件", "下载成功：文件名=" + fileName);
            return new ResponseEntity<>(resource, headers, HttpStatus.OK);
        } catch (Exception e) {
            LogUtils.writeLog(clientIp, "异常信息", "下载失败：文件名=" + fileName + "，原因=" + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}