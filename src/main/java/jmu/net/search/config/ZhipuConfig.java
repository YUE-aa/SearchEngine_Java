package jmu.net.search.config;
/**
 * 智谱AI配置类
 */
public class ZhipuConfig {
    // ====================== 密钥 ======================
    public static final String API_KEY = "cbf9b151cc544997941fa5779150cc44.X2RK2V0umancq54P";
    // =======================================================

    // 向量生成接口地址
    public static final String EMBEDDING_API_URL = "https://open.bigmodel.cn/api/paas/v4/embeddings";
    // 向量生成模型名称
    public static final String EMBEDDING_MODEL = "embedding-2";
    // 文本对话/总结接口地址
    public static final String COMPLETION_API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
    // 文本总结模型名称
    public static final String COMPLETION_MODEL = "glm-4";
    // 超时时间(毫秒)
    public static final int TIMEOUT = 60000;
    // 向量维度-固定1024，不用改
    public static final int EMBEDDING_DIMENSION = 1024;
}