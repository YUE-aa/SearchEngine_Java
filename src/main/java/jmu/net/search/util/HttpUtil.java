package jmu.net.search.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Pattern;

public class HttpUtil {
    // ======================== 核心改动：去掉final，改为可动态修改的静态变量 ========================
    // 本机测试默认值：http://localhost:8080
    public static String SERVER_URL = "http://172.19.76.60:8080";
    // ==================================================================
    public static String SEARCH_API = SERVER_URL + "/api/search";
    public static String DOWNLOAD_API = SERVER_URL + "/api/file/download";

    // 新增：更新API地址（修改IP后调用，自动同步接口地址）
    public static void updateServerUrl(String newServerUrl) {
        SERVER_URL = newServerUrl;
        SEARCH_API = SERVER_URL + "/api/search";
        DOWNLOAD_API = SERVER_URL + "/api/file/download";
    }

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

    // downloadFile方法
    public static boolean downloadFile(String fileName, String saveDir) {
        try {
            String encodeFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
            URL url = new URL(SERVER_URL + "/api/file/download?fileName=" + encodeFileName);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() != 200) {
                return false;
            }

            // 写入文件时用BufferedOutputStream，避免覆盖时文件占用
            File saveFile = new File(saveDir, fileName);
            try (InputStream in = conn.getInputStream();
                 BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(saveFile))) {
                byte[] buffer = new byte[1024 * 10];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    bos.write(buffer, 0, len);
                }
            }
            conn.disconnect();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String highlightKeyword(String content, String keyword) {
        if (content == null || keyword == null || content.isEmpty() || keyword.isEmpty()) {
            return content;
        }
        String escapedKeyword = Pattern.quote(keyword);
        String regex = "(?i)(" + escapedKeyword + ")";
        return content.replaceAll(regex, "<span style='background-color: #FFFF00;'>$1</span>");
    }
}