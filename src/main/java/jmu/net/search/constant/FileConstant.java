package jmu.net.search.constant;

/**
 * 常量类：索引字段、文件类型、索引存储路径
 */
public class FileConstant {
    // Lucene索引存储目录（项目根目录下的index文件夹，自动创建）
    public static final String INDEX_DIR = "./lucene_index";
    // 索引字段-文档内容
    public static final String FIELD_CONTENT = "content";
    // 索引字段-文档名称
    public static final String FIELD_NAME = "fileName";
    // 纯文本文件后缀
    public static final String[] TXT_SUFFIX = {".txt", ".csv", ".log", ".ini"};
    // 办公文档后缀（Tika解析）
    public static final String[] OFFICE_SUFFIX = {".pdf", ".docx", ".xlsx", ".doc", ".xls", ".pptx", ".ppt"};
}