package jmu.net.search.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jmu.net.search.config.ZhipuConfig;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AiUtils {
    // 修复1：关键词优化方法
    public static String optimizeQuery(String keyword) {
        try {
            String prompt = "请将以下搜索关键词优化得更精准、更适合检索：" + keyword;
            return sendZhipuRequest(prompt, ZhipuConfig.COMPLETION_MODEL);
        } catch (Exception e) {
            e.printStackTrace();
            return keyword; // 失败时返回原关键词
        }
    }

    // 修复2：RAG回答生成方法（参数匹配String类型context）
    public static String generateRagAnswer(String query, String context) {
        try {
            String prompt = "根据以下参考内容，简洁准确回答问题，不要编造信息：\n参考内容：" + context + "\n问题：" + query;
            return sendZhipuRequest(prompt, ZhipuConfig.COMPLETION_MODEL);
        } catch (Exception e) {
            e.printStackTrace();
            return "生成回答失败，请检查服务端配置";
        }
    }

    // 通用智谱AI请求方法
    private static String sendZhipuRequest(String prompt, String model) throws Exception {
        URL url = new URL(ZhipuConfig.COMPLETION_API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + ZhipuConfig.API_KEY);
        conn.setDoOutput(true);

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);
        JSONArray messages = new JSONArray();
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.add(userMsg);
        requestBody.put("messages", messages);

        OutputStream os = conn.getOutputStream();
        os.write(requestBody.toJSONString().getBytes(StandardCharsets.UTF_8));
        os.flush();

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        conn.disconnect();

        JSONObject response = JSON.parseObject(sb.toString());
        return response.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");
    }

    // 文本转向量方法（原有逻辑保留）
    public static double[] getEmbedding(String text) throws Exception {
        URL url = new URL(ZhipuConfig.EMBEDDING_API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + ZhipuConfig.API_KEY);
        conn.setDoOutput(true);

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", ZhipuConfig.EMBEDDING_MODEL);
        requestBody.put("input", text);

        OutputStream os = conn.getOutputStream();
        os.write(requestBody.toJSONString().getBytes(StandardCharsets.UTF_8));
        os.flush();

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        conn.disconnect();

        JSONObject response = JSON.parseObject(sb.toString());
        JSONArray data = response.getJSONArray("data");
        return data.getJSONObject(0).getJSONArray("embedding").toJavaList(Double.class).stream().mapToDouble(Double::doubleValue).toArray();
    }
}