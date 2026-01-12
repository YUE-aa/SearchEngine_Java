package jmu.net.search.util;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 日志工具类 - 完整实现：记录IP、操作行为、检索、下载、异常，自动创建日志目录和文件
 * 适配 SpringBoot3.2 + JDK21 无任何报错
 */
public class LogUtils {
    // 日志文件存储根目录
    private static final String LOG_ROOT_DIR = "log";
    // 日志时间格式化（精确到秒）
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    // 日志文件名格式化（按日期拆分文件）
    private static final SimpleDateFormat FILE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * 核心日志写入方法
     * @param ip 访问者IP地址
     * @param operateType 操作类型：检索文件、下载文件、系统操作、异常信息
     * @param content 操作详情：关键词、文件名、异常信息等
     */
    public static void writeLog(String ip, String operateType, String content) {
        // 1. 初始化日志目录，不存在则自动创建
        File logDir = new File(LOG_ROOT_DIR);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }

        // 2. 按日期生成日志文件名 例：2026-01-12-log.txt
        String logFileName = FILE_FORMAT.format(new Date()) + "-log.txt";
        File logFile = new File(logDir, logFileName);

        // 3. 拼接标准日志格式
        String logContent = "[" + TIME_FORMAT.format(new Date()) + "] " +
                "[IP:" + ip + "] " +
                "[操作类型:" + operateType + "] " +
                "详情：" + content + "\r\n";

        // 4. 写入日志（追加模式，UTF-8编码 彻底解决中文乱码 JDK21兼容）
        try (OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(logFile, true), StandardCharsets.UTF_8)) {
            fw.write(logContent);
            fw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取客户端真实IP地址（工具方法，所有Controller可调用）
     * 兼容：直接访问、Nginx反向代理、Apache代理、负载均衡等场景
     * SpringBoot3.x 适配 jakarta.servlet.http.HttpServletRequest
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多IP场景，X-Forwarded-For可能返回多个IP，取第一个有效IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip == null ? "127.0.0.1" : ip;
    }
}