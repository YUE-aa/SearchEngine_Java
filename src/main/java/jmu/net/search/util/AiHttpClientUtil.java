package jmu.net.search.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jmu.net.search.config.ZhipuConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 智谱AI调用核心工具类
 * 基于Java原生 HttpURLConnection 实现，✅无任何外部依赖、✅无导包报错、✅无Maven问题
 * 包含：文本转向量(搜索引擎核心)+AI总结生成(RAG核心)
 * 所有变量名和ZhipuConfig完全一致，直接复制即用！
 */
public class AiHttpClientUtil {

    // ========== 功能1：文本转向量【你的搜索引擎核心必用】 ==========
    public static double[] text2Vector(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new double[ZhipuConfig.EMBEDDING_DIMENSION];
        }
        // 超长文本截断，防止接口超限
        String dealText = text.trim().length() > 2000 ? text.trim().substring(0, 2000) : text.trim();

        // 构建智谱向量请求体
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", ZhipuConfig.EMBEDDING_MODEL);
        requestBody.put("input", dealText);

        // 调用原生POST请求
        String responseJson = sendPost(ZhipuConfig.EMBEDDING_API_URL, requestBody.toJSONString());
        if (responseJson == null) {
            System.err.println("❌ 向量生成失败：接口响应为空");
            return new double[ZhipuConfig.EMBEDDING_DIMENSION];
        }

        try {
            JSONObject responseObj = JSON.parseObject(responseJson);
            JSONArray dataArray = responseObj.getJSONArray("data");
            if (dataArray == null || dataArray.isEmpty()) {
                System.err.println("❌ 向量生成失败：接口返回无数据");
                return new double[ZhipuConfig.EMBEDDING_DIMENSION];
            }
            JSONArray vectorArray = dataArray.getJSONObject(0).getJSONArray("embedding");
            double[] vector = new double[vectorArray.size()];
            for (int i = 0; i < vectorArray.size(); i++) {
                vector[i] = vectorArray.getDouble(i);
            }
            System.out.println("✅ 文本转向量成功！向量维度：" + vector.length);
            return vector;
        } catch (Exception e) {
            System.err.println("❌ 向量解析失败：" + e.getMessage());
            return new double[ZhipuConfig.EMBEDDING_DIMENSION];
        }
    }

    // ========== 功能2：AI总结生成【你的RAG问答核心必用】 ==========
    public static String generateSummary(String query, List<String> docContents) {
        if (docContents == null || docContents.isEmpty()) {
            return "未检索到相关文档，无法生成总结";
        }
        // 构建提示词，让AI精准回答
        StringBuilder prompt = new StringBuilder();
        prompt.append("请根据以下文档内容，简洁准确回答用户问题，禁止编造信息，回答控制在300字以内。\n");
        prompt.append("用户问题：").append(query).append("\n");
        prompt.append("参考文档内容：\n");
        for (int i = 0; i < docContents.size() && i < 3; i++) {
            prompt.append((i + 1)).append("、").append(docContents.get(i)).append("\n");
        }

        // 构建智谱对话请求体
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", ZhipuConfig.COMPLETION_MODEL);
        requestBody.put("temperature", 0.6); // 回答精准度，0.6最合适

        JSONArray messages = new JSONArray();
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", prompt.toString());
        messages.add(userMsg);

        requestBody.put("messages", messages);

        // 调用原生POST请求
        String responseJson = sendPost(ZhipuConfig.COMPLETION_API_URL, requestBody.toJSONString());
        if (responseJson == null) {
            System.err.println("❌ AI总结生成失败：接口响应为空");
            return "总结生成失败，请查看文档原文";
        }

        try {
            JSONObject responseObj = JSON.parseObject(responseJson);
            JSONArray choices = responseObj.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                System.err.println("❌ AI总结生成失败：接口返回无结果");
                return "总结生成失败，请查看文档原文";
            }
            String summary = choices.getJSONObject(0).getJSONObject("message").getString("content");
            System.out.println("✅ AI总结生成成功！");
            return summary;
        } catch (Exception e) {
            System.err.println("❌ 总结解析失败：" + e.getMessage());
            return "总结生成失败，请查看文档原文";
        }
    }

    // ========== 核心通用POST请求方法【Java原生，无任何依赖，永不报错】 ==========
    private static String sendPost(String apiUrl, String requestBody) {
        HttpURLConnection conn = null;
        OutputStream os = null;
        BufferedReader br = null;
        try {
            URL url = new URL(apiUrl);
            conn = (HttpURLConnection) url.openConnection();

            // 设置请求基础配置
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setConnectTimeout(ZhipuConfig.TIMEOUT);
            conn.setReadTimeout(ZhipuConfig.TIMEOUT);

            // 设置请求头-智谱AI必传
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setRequestProperty("Authorization", "Bearer " + ZhipuConfig.API_KEY);

            // 写入请求体参数
            byte[] bodyBytes = requestBody.getBytes(StandardCharsets.UTF_8);
            os = conn.getOutputStream();
            os.write(bodyBytes);
            os.flush();

            // 获取响应结果
            int statusCode = conn.getResponseCode();
            if (statusCode == 200) {
                // 读取成功响应
                br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                return sb.toString();
            } else {
                // 读取错误信息，方便你排错
                br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                System.err.println("❌ 接口调用失败 → 状态码：" + statusCode + "，错误信息：" + sb);
                return null;
            }
        } catch (IOException e) {
            System.err.println("❌ 网络请求异常：" + e.getMessage());
            return null;
        } finally {
            // 关闭资源，防止内存泄漏
            try {
                if (br != null) br.close();
                if (os != null) os.close();
                if (conn != null) conn.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // ========== 测试方法【可选，直接运行就能测试是否调用成功】 ==========
    public static void main(String[] args) {
        // 测试文本转向量
        String testText = "Java是一种面向对象的编程语言，具有跨平台、安全性高的特点";
        double[] vector = text2Vector(testText);
        System.out.println("测试向量维度：" + vector.length);

        // 测试AI总结
        String testQuery = "Java的核心特点是什么？";
        List<String> testDocs = List.of("Java是一种面向对象的编程语言，由Sun公司开发，跨平台性强，支持垃圾回收机制，安全性和健壮性优异。");
        String summary = generateSummary(testQuery, testDocs);
        System.out.println("AI总结结果：\n" + summary);
    }
}