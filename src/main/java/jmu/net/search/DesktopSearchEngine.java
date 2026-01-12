package jmu.net.search;

import jmu.net.search.util.HttpUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class DesktopSearchEngine extends JFrame {
    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    private JTextField searchTextField;
    private JTextPane resultTextPane;
    private JList<String> fileList;
    private DefaultListModel<String> listModel;
    private String saveFilePath = System.getProperty("user.home") + File.separator + "Desktop";
    private Map<String, String> fileContentMap = new HashMap<>();

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    DesktopSearchEngine frame = new DesktopSearchEngine();
                    frame.setVisible(true);
                    frame.setTitle("文档搜索引擎-客户端");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public DesktopSearchEngine() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 1200, 700);
        setLocationRelativeTo(null);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(new BorderLayout(0, 5));
        setContentPane(contentPane);

        JPanel searchPanel = new JPanel();
        FlowLayout flowLayout = (FlowLayout) searchPanel.getLayout();
        flowLayout.setAlignment(FlowLayout.LEFT);
        contentPane.add(searchPanel, BorderLayout.NORTH);

        JLabel lblNewLabel = new JLabel("请输入搜索关键词：");
        lblNewLabel.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        searchPanel.add(lblNewLabel);

        searchTextField = new JTextField();
        searchTextField.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        searchTextField.setColumns(30);
        searchPanel.add(searchTextField);

        JButton searchBtn = new JButton("搜索");
        searchBtn.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        searchPanel.add(searchBtn);

        JButton uploadBtn = new JButton("上传文件到服务端");
        uploadBtn.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        searchPanel.add(uploadBtn);

        JButton downloadBtn = new JButton("下载选中文件");
        downloadBtn.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        searchPanel.add(downloadBtn);

        JButton pathBtn = new JButton("选择保存路径");
        pathBtn.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        searchPanel.add(pathBtn);

        JSplitPane splitPane = new JSplitPane();
        splitPane.setResizeWeight(0.3);
        splitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        contentPane.add(splitPane, BorderLayout.CENTER);

        JPanel leftPanel = new JPanel();
        splitPane.setLeftComponent(leftPanel);
        leftPanel.setLayout(new BorderLayout(0, 0));

        JLabel lblNewLabel_1 = new JLabel("搜索结果文件列表");
        lblNewLabel_1.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        lblNewLabel_1.setBorder(new EmptyBorder(5, 5, 5, 5));
        leftPanel.add(lblNewLabel_1, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        fileList = new JList<>(listModel);
        fileList.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        leftPanel.add(new JScrollPane(fileList), BorderLayout.CENTER);

        JPanel rightPanel = new JPanel();
        splitPane.setRightComponent(rightPanel);
        rightPanel.setLayout(new BorderLayout(0, 0));

        JLabel lblNewLabel_2 = new JLabel("文档内容预览");
        lblNewLabel_2.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        lblNewLabel_2.setBorder(new EmptyBorder(5, 5, 5, 5));
        rightPanel.add(lblNewLabel_2, BorderLayout.NORTH);

        resultTextPane = new JTextPane();
        resultTextPane.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        resultTextPane.setEditable(false);
        resultTextPane.setEditorKit(new HTMLEditorKit());
        rightPanel.add(new JScrollPane(resultTextPane), BorderLayout.CENTER);

        // 搜索按钮事件
        searchBtn.addActionListener(e -> {
            String keyword = searchTextField.getText().trim();
            if (keyword.isEmpty()) {
                JOptionPane.showMessageDialog(null, "请输入搜索关键词！", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            listModel.clear();
            resultTextPane.setText("");
            fileContentMap.clear();

            JSONArray searchResult = HttpUtil.search(keyword);
            if (searchResult.size() == 0) {
                JOptionPane.showMessageDialog(null, "服务端未检索到相关文件！", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            for (int i = 0; i < searchResult.size(); i++) {
                JSONObject obj = searchResult.getJSONObject(i);
                String fileName = obj.getString("fileName");
                String summary = obj.getString("summary");
                String highlightSummary = HttpUtil.highlightKeyword(summary, keyword);
                listModel.addElement(fileName);
                fileContentMap.put(fileName, highlightSummary);
            }
            JOptionPane.showMessageDialog(null, "检索完成，共找到 " + searchResult.size() + " 个文件！", "成功", JOptionPane.INFORMATION_MESSAGE);
        });

        // 文件列表点击事件
        fileList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedFileName = fileList.getSelectedValue();
                if (selectedFileName != null) {
                    resultTextPane.setText(fileContentMap.getOrDefault(selectedFileName, "无预览内容"));
                }
            }
        });

        // 选择保存路径
        pathBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = fileChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                saveFilePath = fileChooser.getSelectedFile().getAbsolutePath();
                JOptionPane.showMessageDialog(null, "保存路径已设置为：\n" + saveFilePath, "成功", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        // 下载文件
        downloadBtn.addActionListener(e -> {
            String selectedFileName = fileList.getSelectedValue();
            if (selectedFileName == null) {
                JOptionPane.showMessageDialog(null, "请先选中要下载的文件！", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            boolean isSuccess = HttpUtil.downloadFile(selectedFileName, saveFilePath);
            if (isSuccess) {
                JOptionPane.showMessageDialog(null, "文件下载成功！\n保存路径：" + saveFilePath + File.separator + selectedFileName, "成功", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "文件下载失败，请检查服务端是否正常！", "失败", JOptionPane.ERROR_MESSAGE);
            }
        });

        // 上传文件
        uploadBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int result = fileChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File uploadFile = fileChooser.getSelectedFile();
                boolean isSuccess = HttpUtil.uploadFile(uploadFile);
                if (isSuccess) {
                    JOptionPane.showMessageDialog(null, "文件上传成功！已同步到服务端docs目录", "成功", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, "文件上传失败，请检查服务端是否正常！", "失败", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }
}