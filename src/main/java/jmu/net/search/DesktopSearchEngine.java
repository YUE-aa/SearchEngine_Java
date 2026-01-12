package jmu.net.search;

import jmu.net.search.constant.FileConstant;
import jmu.net.search.util.LuceneUtil;
import org.apache.lucene.document.Document;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

/**
 * 特点：关键词黄色背景高亮+无乱码+格式美观+检索正常+摘要，自动检索电脑桌面所有文档
 */
public class DesktopSearchEngine extends JFrame {
    // 摘要优化：关键词前后各显示100字，摘要更完整，体验更好
    private static final int SUMMARY_LENGTH = 100;
    private JTextField searchTextField;
    private JTextArea resultTextArea;
    private Highlighter highlighter;
    private Highlighter.HighlightPainter yellowPainter; // 黄色高亮器
    // 新增：索引创建标记，只创建一次索引，提速核心，避免重复扫描桌面
    public static boolean isIndexCreated = false;

    public static void main(String[] args) {
        initLuceneIndex();
        SwingUtilities.invokeLater(() -> new DesktopSearchEngine().setVisible(true));
    }

    // ========== 扫描电脑桌面 ==========
    private static void initLuceneIndex() {
        // 索引已创建则直接返回，不重复扫描，大幅提速
        if(isIndexCreated){
            return;
        }
        try {
            // 获取电脑桌面的绝对路径，所有Windows电脑通用
            String desktopPath = System.getProperty("user.home") + File.separator + "Desktop";
            File docDir = new File(desktopPath);

            if (!docDir.exists()) {
                JOptionPane.showMessageDialog(null, "电脑桌面目录不存在！", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // 关键优化：文件过滤器，只扫描支持的文档格式，过滤图片/视频/快捷方式/压缩包等无用文件
            File[] files = docDir.listFiles(file -> {
                if(file.isDirectory()){ // 支持扫描桌面【子文件夹】里的文档，完美兼容
                    return true;
                }
                String fileName = file.getName().toLowerCase();
                // 只检索这些常用可解析文档格式
                return fileName.endsWith(".txt")
                        || fileName.endsWith(".pdf")
                        || fileName.endsWith(".xlsx") || fileName.endsWith(".xls")
                        || fileName.endsWith(".docx") || fileName.endsWith(".doc")
                        || fileName.endsWith(".csv") || fileName.endsWith(".log")
                        || fileName.endsWith(".ini") || fileName.endsWith(".md");
            });

            if (files != null && files.length > 0) {
                LuceneUtil.createIndex(docDir);
                isIndexCreated = true; // 标记索引创建完成
                JOptionPane.showMessageDialog(null, "桌面文档扫描完成！共找到 "+files.length+" 个可检索文档", "初始化成功", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "桌面暂无支持的文档（PDF/Excel/Word/TXT等）", "提示", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "索引创建失败：" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    public DesktopSearchEngine() {
        // 窗口基础配置
        setTitle("Lucene 本地桌面全文搜索引擎");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 650);
        setLocationRelativeTo(null);
        setResizable(true);

        highlighter = new DefaultHighlighter();
        yellowPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW); // 保留你喜欢的亮黄色高亮
        resultTextArea = new JTextArea();
        resultTextArea.setHighlighter(highlighter);

        // 顶部搜索面板
        JPanel searchPanel = new JPanel(new BorderLayout(10, 0));
        searchPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
        searchTextField = new JTextField();
        searchTextField.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        searchTextField.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        JButton searchBtn = new JButton("搜索");
        searchBtn.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        searchBtn.setBackground(new Color(45, 129, 255));
        searchBtn.setForeground(Color.WHITE);
        searchBtn.setFocusPainted(false);
        searchBtn.addActionListener(new SearchListener());
        searchPanel.add(searchTextField, BorderLayout.CENTER);
        searchPanel.add(searchBtn, BorderLayout.EAST);

        // 结果展示区
        resultTextArea.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        resultTextArea.setEditable(false);
        resultTextArea.setBorder(new EmptyBorder(12, 12, 12, 12));
        resultTextArea.setLineWrap(true);
        resultTextArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(resultTextArea);
        scrollPane.setBorder(null);

        // 组装窗口
        add(searchPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        resultTextArea.setText("搜索引擎已就绪\n\n" +
                "输入关键词后点击【搜索】按钮即可查询\n" +
                "支持文件类型：txt/csv/log/ini/pdf/docx/xlsx/xls/doc/md\n" +
                "检索位置：电脑桌面（含桌面子文件夹）");
    }

    private class SearchListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // ========== 每次搜索自动检查索引，新增文档无需重启 ==========
            initLuceneIndex();

            String keyword = searchTextField.getText().trim();
            if (keyword.isEmpty()) {
                JOptionPane.showMessageDialog(null, "请输入搜索关键词！", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            highlighter.removeAllHighlights(); // 清除上一次高亮

            try {
                List<Document> results = LuceneUtil.search(keyword);
                StringBuilder sb = new StringBuilder();
                sb.append("搜索关键词：").append(keyword).append("\n");
                sb.append("共找到 ").append(results.size()).append(" 个匹配结果\n");
                sb.append("---------------------------------------------------------\n\n");

                if (results.isEmpty()) {
                    sb.append("未查询到相关内容");
                } else {
                    int count = 1;
                    for (Document doc : results) {
                        String fileName = doc.get(FileConstant.FIELD_NAME);
                        String content = doc.get(FileConstant.FIELD_CONTENT);
                        String summary = getSummary(content, keyword);

                        sb.append(count).append("、文件名称：").append(fileName).append("\n");
                        sb.append("   内容摘要：").append(summary).append("\n\n");
                        sb.append("---------------------------------------------------------\n\n");
                        count++;
                    }
                }
                resultTextArea.setText(sb.toString());
                // ========== 延迟100毫秒高亮 解决Swing文本加载渲染延迟 ==========
                new Timer(100, ae -> {
                    highlightKeyword(keyword);
                    ((Timer)ae.getSource()).stop();
                }).start();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "搜索失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    // 关键词高亮核心方法
    private void highlightKeyword(String keyword) {
        if (keyword == null || keyword.isEmpty() || resultTextArea.getText().isEmpty()) {
            return;
        }
        String text = resultTextArea.getText();
        int index = 0;
        try {
            while ((index = text.indexOf(keyword, index)) != -1) {
                highlighter.addHighlight(index, index + keyword.length(), yellowPainter);
                index += keyword.length();
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private String getSummary(String content, String keyword) {
        if (content == null || content.isEmpty()) return "无文档内容";


        // 清除换行符、多余空格，解决乱码和排版混乱问题
        String cleanContent = content.replaceAll("\\r\\n|\\r|\\n", " ").replaceAll("\\s+", " ");
        // 清除Excel/Word的特殊分隔符，摘要更整洁
        cleanContent = cleanContent.replaceAll("\\|", " ").replaceAll("\\t", " ");
        int keywordIndex = cleanContent.indexOf(keyword);

        if (keywordIndex == -1) {
            return cleanContent.length() > 200 ? cleanContent.substring(0, 200) + "..." : cleanContent;
        }

        int start = Math.max(0, keywordIndex - SUMMARY_LENGTH);
        int end = Math.min(cleanContent.length(), keywordIndex + keyword.length() + SUMMARY_LENGTH);
        String summary = cleanContent.substring(start, end);

        if (start > 0) summary = "..." + summary;
        if (end < cleanContent.length()) summary = summary + "...";

        return summary;
    }
}