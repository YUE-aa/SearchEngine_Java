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
    // 配置文件路径，保存在客户端运行目录下
    private final String CONFIG_FILE = "server_config.properties";

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
    // 主界面初始化核心代码
    public DesktopSearchEngine() {
        // 启动时加载服务器地址配置
        loadServerConfig();
        // 初始化定时检测任务
        initStatusCheckTimer();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 1300, 700);
        setLocationRelativeTo(null);// 居中显示
        // 主面板布局
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(new BorderLayout(0, 5));
        setContentPane(contentPane);
        // 北部搜索面板
        JPanel searchPanel = new JPanel();
        FlowLayout flowLayout = (FlowLayout) searchPanel.getLayout();
        flowLayout.setAlignment(FlowLayout.LEFT);
        contentPane.add(searchPanel, BorderLayout.NORTH);
        // 服务器状态红绿灯
        serverStatusLabel = new JLabel();
        serverStatusLabel.setPreferredSize(new Dimension(20, 20));
        updateServerStatusUI();
        searchPanel.add(serverStatusLabel);

        JLabel statusTextLabel = new JLabel("服务器状态：");
        statusTextLabel.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        searchPanel.add(statusTextLabel);

        JLabel lblNewLabel = new JLabel("请输入搜索关键词：");
        lblNewLabel.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        searchPanel.add(lblNewLabel);
        // 搜索框、按钮等组件初始化
        searchTextField = new JTextField();
        searchTextField.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        searchTextField.setColumns(30);
        searchPanel.add(searchTextField);

        JButton searchBtn = new JButton("搜索");
        searchBtn.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        searchPanel.add(searchBtn);

        // ======================== 搜索框回车触发搜索 ========================
        searchTextField.addActionListener(e -> searchBtn.doClick());

        JButton downloadBtn = new JButton("下载选中文件");
        downloadBtn.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        searchPanel.add(downloadBtn);

        JButton pathBtn = new JButton("设置默认保存路径");
        pathBtn.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        searchPanel.add(pathBtn);

        // ======================== 设置服务器地址 ========================
        JButton serverConfigBtn = new JButton("设置服务器地址");
        serverConfigBtn.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        searchPanel.add(serverConfigBtn);
        // 中部左右分栏面板
        JSplitPane splitPane = new JSplitPane();
        splitPane.setResizeWeight(0.3);
        splitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        contentPane.add(splitPane, BorderLayout.CENTER);
        // 左侧文件列表面板
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
        // 右侧内容预览面板
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

        // 搜索事件
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

        // 设置默认保存路径事件
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

        // 下载事件  ★★★★★ 只改这里，只加代码，其余全部原版 ★★★★★
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

            // ======================== 【新增的唯一代码，原版没有的，恢复你的功能】 ========================
            // 检测目标路径是否存在同名文件，和Windows系统一致的提示逻辑
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
                    return; // 点击取消，终止下载，不执行后续操作
                }
            }
            // ==========================================================================================

            boolean isSuccess = HttpUtil.downloadFile(selectedFileName, saveDir);
            if (isSuccess) {
                JOptionPane.showMessageDialog(null, "文件下载成功！\n保存路径：" + saveDir + File.separator + selectedFileName, "成功", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "文件下载失败，请检查服务端是否正常！", "失败", JOptionPane.ERROR_MESSAGE);
            }
        });

        // ======================== 设置服务器地址按钮点击事件 ========================
        serverConfigBtn.addActionListener(e -> {
            showServerConfigDialog();
        });

    }

    // ======================== 加载服务器配置文件 ========================
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

    // ======================== 保存服务器配置到文件 ========================
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

    // ======================== 服务器地址配置弹窗 ========================
    private void showServerConfigDialog() {
        // 创建输入框，默认填充当前的服务器地址
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
            // 简单校验格式：必须以http://开头，包含:端口号
            if (!newServerUrl.startsWith("http://") || !newServerUrl.contains(":")) {
                JOptionPane.showMessageDialog(null, "服务器地址格式错误！\n正确格式示例：http://192.168.1.100:8080", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // 更新地址+保存配置+刷新状态
            HttpUtil.updateServerUrl(newServerUrl);
            saveServerConfig(newServerUrl);
            // 立刻检测服务器状态，刷新红绿灯
            checkServerOnlineStatus();
            updateServerStatusUI();
            JOptionPane.showMessageDialog(null, "服务器地址设置成功！\n当前地址：" + newServerUrl, "成功", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // 初始化状态检测定时器
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

    // 检测服务器状态
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

    // 更新红绿灯UI
    private void updateServerStatusUI() {
        if (isServerOnline) {
            serverStatusLabel.setIcon(new ImageIcon(createColoredCircle(Color.GREEN)));
            serverStatusLabel.setToolTipText("服务器在线：" + HttpUtil.SERVER_URL);
        } else {
            serverStatusLabel.setIcon(new ImageIcon(createColoredCircle(Color.RED)));
            serverStatusLabel.setToolTipText("服务器离线：" + HttpUtil.SERVER_URL);
        }
    }

    // 绘制红绿灯圆形图标
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

    // 关闭窗口停止定时器
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