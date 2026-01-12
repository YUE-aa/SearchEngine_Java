package jmu.net.search.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class HttpUtil {
    // ======================== 跨主机 ========================
    // 本机测试：http://localhost:8080
    // 跨电脑测试改为电脑的IPv4地址：http://172.19.76.197:8080
    public static final String SERVER_URL = "http://localhost:8080";
    // ==================================================================
    public static final String SEARCH_API = SERVER_URL + "/api/search";
    public static final String DOWNLOAD_API = SERVER_URL + "/api/file/download";
    public static final String UPLOAD_API = SERVER_URL + "/api/file/upload";

    public static JSONArray search(String keyword) {
        try {
            String requestUrl = SEARCH_API + "?keyword=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            URL url = new URL(requestUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);

            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                br.close();
                JSONObject resultObj = JSONObject.parseObject(sb.toString());
                if (resultObj.getInteger("code") == 200) {
                    return resultObj.getJSONArray("data");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new JSONArray();
    }

    public static boolean downloadFile(String fileName, String saveDir) {
        try {
            String requestUrl = DOWNLOAD_API + "?fileName=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8);
            URL url = new URL(requestUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(30000);

            if (conn.getResponseCode() == 200) {
                File dir = new File(saveDir);
                if (!dir.exists()) dir.mkdirs();
                File targetFile = new File(saveDir, fileName);
                InputStream is = conn.getInputStream();
                FileUtils.copyInputStreamToFile(is, targetFile);
                is.close();
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean uploadFile(File uploadFile) {
        if (!uploadFile.exists() || !uploadFile.isFile()) {
            return false;
        }
        final String BOUNDARY = "----WebKitFormBoundary" + System.currentTimeMillis();
        final String PREFIX = "--", LINE_END = "\r\n";
        final String CONTENT_TYPE = "multipart/form-data; boundary=" + BOUNDARY;

        try {
            URL url = new URL(UPLOAD_API);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("Content-Type", CONTENT_TYPE);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);

            OutputStream os = new DataOutputStream(conn.getOutputStream());
            StringBuilder sb = new StringBuilder();
            sb.append(PREFIX).append(BOUNDARY).append(LINE_END);
            sb.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                    .append(uploadFile.getName()).append("\"").append(LINE_END);
            sb.append("Content-Type: application/octet-stream").append(LINE_END).append(LINE_END);
            os.write(sb.toString().getBytes(StandardCharsets.UTF_8));

            InputStream is = new FileInputStream(uploadFile);
            byte[] buffer = new byte[1024 * 8];
            int len;
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            is.close();

            os.write(LINE_END.getBytes());
            byte[] end_data = (PREFIX + BOUNDARY + PREFIX + LINE_END).getBytes();
            os.write(end_data);
            os.flush();
            os.close();

            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // ======================== 高亮效果 ========================
    public static String highlightKeyword(String content, String keyword) {
        if (content == null || keyword == null || content.isEmpty() || keyword.isEmpty()) {
            return content;
        }
        String escapedKeyword = Pattern.quote(keyword);
        String regex = "(?i)(" + escapedKeyword + ")";
        // 效果：黄色荧光笔底色 + 原文字颜色
        return content.replaceAll(regex, "<span style='background-color: #FFFF00;'>$1</span>");
    }
}