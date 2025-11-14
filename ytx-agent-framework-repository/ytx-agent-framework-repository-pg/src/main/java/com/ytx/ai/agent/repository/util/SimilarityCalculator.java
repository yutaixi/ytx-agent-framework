package com.ytx.ai.agent.repository.util;

/**
 * 相似度计算器接口
 */
public interface SimilarityCalculator {

    /**
     * 将距离转换为相似度分数
     * @param distance 距离值（越小越相似）
     * @return 相似度分数（0-1之间，越大越相似）
     */
    float calculate(float distance);
}

