package jmu.net.search.dto;

/**
 * 检索结果数据传输对象
 */
public class SearchResultDTO {
    private String fileName;
    private String summary;

    public SearchResultDTO() {}

    public SearchResultDTO(String fileName, String summary) {
        this.fileName = fileName;
        this.summary = summary;
    }

    // Getter & Setter
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}