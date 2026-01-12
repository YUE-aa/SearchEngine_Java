package jmu.net.search.util;

import jmu.net.search.constant.FileConstant;
import org.apache.commons.io.FileUtils;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 文档解析工具类：提取各种文档的文本内容
 * 支持：txt/csv/log/ini 纯文本 | pdf/docx/xlsx 办公文档
 * ★修复JDK21 IO流兼容问题 + 中文文件内容不乱码
 */
public class DocumentParseUtil {
    // Apache Tika核心解析器（自动识别文档类型，无需手动指定）
    private static final Tika TIKA = new Tika();

    /**
     * 统一解析入口：根据文件后缀自动选择解析方式
     * @param file 待解析的文件
     * @return 提取后的纯文本内容
     */
    public static String parseFileToText(File file) throws IOException, TikaException, SAXException {
        String fileName = file.getName().toLowerCase();
        // 处理纯文本文件：txt/csv/log/ini  强制UTF8编码 解决中文乱码
        for (String suffix : FileConstant.TXT_SUFFIX) {
            if (fileName.endsWith(suffix)) {
                return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            }
        }
        // 处理办公文档：pdf/docx/xlsx 等，交给Tika解析 适配JDK21
        for (String suffix : FileConstant.OFFICE_SUFFIX) {
            if (fileName.endsWith(suffix)) {
                return TIKA.parseToString(file).trim();
            }
        }
        // 不支持的文件类型 返回空字符串
        return "";
    }
}