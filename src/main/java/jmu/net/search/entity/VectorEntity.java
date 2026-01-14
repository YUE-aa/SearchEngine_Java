package jmu.net.search.entity;

/**
 * 文档向量存储实体
 */
public class VectorEntity {
    private String fileName; // 文件名
    private String filePath; // 文件路径
    private String content; // 文档内容
    private double[] vector; // 文档向量
    private long createTime; // 创建时间

    // 构造函数、Getter和Setter
    public VectorEntity() {}

    public VectorEntity(String fileName, String filePath, String content, double[] vector) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.content = content;
        this.vector = vector;
        this.createTime = System.currentTimeMillis();
    }

    // Getter & Setter
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public double[] getVector() { return vector; }
    public void setVector(double[] vector) { this.vector = vector; }
    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
}