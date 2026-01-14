package jmu.net.search.service;

import jmu.net.search.entity.VectorEntity;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class VectorCacheService {
    // 向量缓存存储
    private List<VectorEntity> vectorCache = new ArrayList<>();

    // 清空缓存
    public void clearCache() {
        vectorCache.clear();
    }

    // 批量添加向量
    public void batchAddVectors(List<VectorEntity> vectors) {
        vectorCache.addAll(vectors);
    }

    // 相似度搜索
    public List<VectorEntity> searchSimilarVectors(double[] queryVector, int topK) {
        List<VectorEntity> similarVectors = new ArrayList<>();
        for (VectorEntity vec : vectorCache) {
            double similarity = calculateCosineSimilarity(queryVector, vec.getVector());
            if (similarity > 0.5) {
                similarVectors.add(vec);
                if (similarVectors.size() >= topK) break;
            }
        }
        return similarVectors;
    }

    // 余弦相似度计算
    private double calculateCosineSimilarity(double[] vec1, double[] vec2) {
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        int minLen = Math.min(vec1.length, vec2.length);
        for (int i = 0; i < minLen; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += Math.pow(vec1[i], 2);
            norm2 += Math.pow(vec2[i], 2);
        }
        if (norm1 == 0 || norm2 == 0) return 0.0;
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    // 新增：获取缓存大小（修复报错核心）
    public int getCacheSize() {
        return vectorCache.size();
    }
}