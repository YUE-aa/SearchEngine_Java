package jmu.net.search;

import jmu.net.search.util.HttpUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

public class DesktopSearchEngine extends JFrame {
    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    private JTextField searchTextField;
    private JTextPane resultTextPane;
    private JList<String> fileList;
    private DefaultListModel<String> listModel;
    private String defaultSaveFilePath = System.getProperty("user.home") + File.separator + "Desktop";
    private Map<String, String> fileContentMap = new HashMap<>();
    private JLabel serverStatusLabel;
    private boolean isServerOnline = false;
    private Timer statusCheckTimer;
    private final String CONFIG_FILE = "server_config.properties";

    private JCheckBox aiSearchCheckBox;
    private JCheckBox ragAnswerCheckBox;
    private JTextPane ragTextPane;

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    DesktopSearchEngine frame = new DesktopSearchEngine();
                    frame.setVisible(true);
                    frame.setTitle("文档搜索引擎-客户端(AI语义增强版)");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public DesktopSearchEngine() {
        loadServerConfig();
        initStatusCheckTimer();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 1300, 900);
        setLocationRelativeTo(null);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(new BorderLayout(0, 5));
        setContentPane(contentPane);

        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.Y_AXIS));
        contentPane.add(searchPanel, BorderLayout.NORTH);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        serverStatusLabel = new JLabel();
        serverStatusLabel.setPreferredSize(new Dimension(20, 20));
        updateServerStatusUI();
        row1.add(serverStatusLabel);

        JLabel statusTextLabel = new JLabel("服务器状态：");
        statusTextLabel.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        row1.add(statusTextLabel);

        JLabel lblNewLabel = new JLabel("请输入搜索关键词：");
        lblNewLabel.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        row1.add(lblNewLabel);

        searchTextField = new JTextField();
        searchTextField.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        searchTextField.setColumns(30);
        row1.add(searchTextField);

        JButton searchBtn = new JButton("搜索");
        searchBtn.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        row1.add(searchBtn);

        aiSearchCheckBox = new JCheckBox("AI语义搜索", false);
        aiSearchCheckBox.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        row1.add(aiSearchCheckBox);

        ragAnswerCheckBox = new JCheckBox("生成AI总结", false);
        ragAnswerCheckBox.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        ragAnswerCheckBox.setEnabled(false);
        aiSearchCheckBox.addActionListener(e -> {
            ragAnswerCheckBox.setEnabled(aiSearchCheckBox.isSelected());
            if (!aiSearchCheckBox.isSelected()) {
                ragAnswerCheckBox.setSelected(false);
            }
        });
        row1.add(ragAnswerCheckBox);
        searchPanel.add(row1);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton downloadBtn = new JButton("下载选中文件");
        downloadBtn.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        row2.add(downloadBtn);

        JButton pathBtn = new JButton("设置默认保存路径");
        pathBtn.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        row2.add(pathBtn);

        JButton serverConfigBtn = new JButton("设置服务器地址");
        serverConfigBtn.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        row2.add(serverConfigBtn);
        searchPanel.add(row2);

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
        JScrollPane fileListScrollPane = new JScrollPane(fileList);
        fileListScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        leftPanel.add(fileListScrollPane, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel();
        splitPane.setRightComponent(rightPanel);
        rightPanel.setLayout(new BorderLayout(0, 0));

        JLabel previewLabel = new JLabel("文档内容预览");
        previewLabel.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        previewLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
        rightPanel.add(previewLabel, BorderLayout.NORTH);

        resultTextPane = new JTextPane();
        resultTextPane.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        resultTextPane.setEditable(false);
        resultTextPane.setEditorKit(new HTMLEditorKit());
        JScrollPane previewScrollPane = new JScrollPane(resultTextPane);
        previewScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        rightPanel.add(previewScrollPane, BorderLayout.CENTER);

        JPanel ragPanel = new JPanel();
        ragPanel.setLayout(new BorderLayout(0, 0));
        ragPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        ragPanel.setPreferredSize(new Dimension(1280, 260));

        JLabel ragLabel = new JLabel("AI总结回答");
        ragLabel.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        ragPanel.add(ragLabel, BorderLayout.NORTH);

        ragTextPane = new JTextPane();
        ragTextPane.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        ragTextPane.setEditable(false);
        JScrollPane ragScrollPane = new JScrollPane(ragTextPane);
        ragScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        ragScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        ragPanel.add(ragScrollPane, BorderLayout.CENTER);
        contentPane.add(ragPanel, BorderLayout.SOUTH);

        searchTextField.addActionListener(e -> searchBtn.doClick());

        searchBtn.addActionListener(e -> {
            if (!isServerOnline) {
                JOptionPane.showMessageDialog(null, "服务器已下线，无法进行检索！", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String keyword = searchTextField.getText().trim();
            if (keyword.isEmpty()) {
                JOptionPane.showMessageDialog(null, "请输入搜索关键词！", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            listModel.clear();
            resultTextPane.setText("");
            ragTextPane.setText("");
            fileContentMap.clear();

            boolean useAiSearch = aiSearchCheckBox.isSelected();
            boolean needRag = ragAnswerCheckBox.isSelected();

            new Thread(() -> {
                try {
                    JSONArray searchResult;
                    if (useAiSearch) {
                        searchResult = HttpUtil.search(keyword, true, needRag);
                    } else {
                        searchResult = HttpUtil.search(keyword);
                    }

                    SwingUtilities.invokeLater(() -> {
                        if (searchResult.size() == 0) {
                            JOptionPane.showMessageDialog(null, "服务端未检索到相关文件！", "提示", JOptionPane.INFORMATION_MESSAGE);
                            return;
                        }

                        if (useAiSearch) {
                            JSONObject resultObj = searchResult.getJSONObject(0);
                            JSONArray docResults = resultObj.getJSONArray("searchResults");
                            String ragAnswer = resultObj.getString("ragAnswer");

                            for (int i = 0; i < docResults.size(); i++) {
                                JSONObject obj = docResults.getJSONObject(i);
                                String fileName = obj.getString("fileName");
                                String summary = obj.getString("summary");
                                String highlightSummary = HttpUtil.highlightKeyword(summary, keyword);
                                listModel.addElement(fileName);
                                fileContentMap.put(fileName, highlightSummary);
                            }

                            if (needRag) {
                                ragTextPane.setText(ragAnswer);
                            } else {
                                ragTextPane.setText("✅ 已启用AI语义模糊搜索，如需生成总结请勾选【生成AI总结】");
                            }
                            JOptionPane.showMessageDialog(null, "AI模糊检索完成，共找到 " + docResults.size() + " 个相关文件", "检索成功", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            for (int i = 0; i < searchResult.size(); i++) {
                                JSONObject obj = searchResult.getJSONObject(i);
                                String fileName = obj.getString("fileName");
                                String summary = obj.getString("summary");
                                // 调用修复后的高亮方法，自动标红核心关键词
                                String highlightSummary = HttpUtil.highlightKeyword(summary, keyword);
                                listModel.addElement(fileName);
                                fileContentMap.put(fileName, highlightSummary);
                            }
                            JOptionPane.showMessageDialog(null, "关键词精准检索完成，共找到 " + searchResult.size() + " 个相关文件", "检索成功", JOptionPane.INFORMATION_MESSAGE);
                        }
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(null, "检索失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                    });
                }
            }).start();
        });

        fileList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedFileName = fileList.getSelectedValue();
                if (selectedFileName != null) {
                    resultTextPane.setText(fileContentMap.getOrDefault(selectedFileName, "无预览内容"));
                }
            }
        });

        pathBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setSelectedFile(new File(defaultSaveFilePath));
            int result = fileChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                defaultSaveFilePath = fileChooser.getSelectedFile().getAbsolutePath();
                JOptionPane.showMessageDialog(null, "默认保存路径已设置为：\n" + defaultSaveFilePath, "成功", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        downloadBtn.addActionListener(e -> {
            if (!isServerOnline) {
                JOptionPane.showMessageDialog(null, "服务器已下线，无法下载文件！", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String selectedFileName = fileList.getSelectedValue();
            if (selectedFileName == null) {
                JOptionPane.showMessageDialog(null, "请先选中要下载的文件！", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setSelectedFile(new File(defaultSaveFilePath));
            fileChooser.setDialogTitle("选择文件保存路径");
            int result = fileChooser.showOpenDialog(null);
            if (result != JFileChooser.APPROVE_OPTION) {
                return;
            }
            String saveDir = fileChooser.getSelectedFile().getAbsolutePath();

            File targetFile = new File(saveDir, selectedFileName);
            if (targetFile.exists()) {
                int confirm = JOptionPane.showConfirmDialog(
                        null,
                        "当前路径已存在同名文件【" + selectedFileName + "】，是否覆盖该文件？",
                        "文件重名提示",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            boolean isSuccess = HttpUtil.downloadFile(selectedFileName, saveDir);
            if (isSuccess) {
                JOptionPane.showMessageDialog(null, "文件下载成功！\n保存路径：" + saveDir + File.separator + selectedFileName, "成功", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "文件下载失败，请检查服务端是否正常！", "失败", JOptionPane.ERROR_MESSAGE);
            }
        });

        serverConfigBtn.addActionListener(e -> {
            showServerConfigDialog();
        });

    }

    private void loadServerConfig() {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (InputStream in = new FileInputStream(configFile)) {
                Properties props = new Properties();
                props.load(in);
                String serverUrl = props.getProperty("server.url");
                if (serverUrl != null && !serverUrl.trim().isEmpty()) {
                    HttpUtil.updateServerUrl(serverUrl.trim());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void saveServerConfig(String serverUrl) {
        try (OutputStream out = new FileOutputStream(CONFIG_FILE)) {
            Properties props = new Properties();
            props.setProperty("server.url", serverUrl);
            props.store(out, "Server Configuration - 文档搜索引擎客户端");
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "配置保存失败！", "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showServerConfigDialog() {
        JTextField serverUrlField = new JTextField(HttpUtil.SERVER_URL, 40);
        serverUrlField.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(5,5));
        panel.add(new JLabel("请输入服务器完整地址：", JLabel.RIGHT), BorderLayout.WEST);
        panel.add(serverUrlField, BorderLayout.CENTER);
        panel.setBorder(new EmptyBorder(10,10,10,10));

        int result = JOptionPane.showConfirmDialog(null, panel, "设置服务器地址", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String newServerUrl = serverUrlField.getText().trim();
            if (!newServerUrl.startsWith("http://") || !newServerUrl.contains(":")) {
                JOptionPane.showMessageDialog(null, "服务器地址格式错误！\n正确格式示例：http://192.168.1.100:8080", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            HttpUtil.updateServerUrl(newServerUrl);
            saveServerConfig(newServerUrl);
            checkServerOnlineStatus();
            updateServerStatusUI();
            JOptionPane.showMessageDialog(null, "服务器地址设置成功！\n当前地址：" + newServerUrl, "成功", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void initStatusCheckTimer() {
        statusCheckTimer = new Timer(true);
        statusCheckTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkServerOnlineStatus();
                SwingUtilities.invokeLater(DesktopSearchEngine.this::updateServerStatusUI);
            }
        }, 0, 5000);
    }

    private void checkServerOnlineStatus() {
        try {
            URL url = new URL(HttpUtil.SERVER_URL + "/api/health/check");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            isServerOnline = conn.getResponseCode() != -1;
            conn.disconnect();
        } catch (Exception ex) {
            isServerOnline = false;
        }
    }

    private void updateServerStatusUI() {
        if (isServerOnline) {
            serverStatusLabel.setIcon(new ImageIcon(createColoredCircle(Color.GREEN)));
            serverStatusLabel.setToolTipText("服务器在线：" + HttpUtil.SERVER_URL);
        } else {
            serverStatusLabel.setIcon(new ImageIcon(createColoredCircle(Color.RED)));
            serverStatusLabel.setToolTipText("服务器离线：" + HttpUtil.SERVER_URL);
        }
    }

    private Image createColoredCircle(Color color) {
        int size = 16;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(color);
        g2d.fillOval(0, 0, size, size);
        g2d.dispose();
        return image;
    }

    @Override
    protected void processWindowEvent(java.awt.event.WindowEvent e) {
        super.processWindowEvent(e);
        if (e.getID() == java.awt.event.WindowEvent.WINDOW_CLOSING) {
            if (statusCheckTimer != null) {
                statusCheckTimer.cancel();
            }
        }
    }
}