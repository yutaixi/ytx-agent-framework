package com.ytx.ai.agent.repository.util;

import org.springframework.stereotype.Component;

/**
 * 默认相似度计算器实现
 * 使用公式：similarity = max(0, min(1, 1 - distance))
 */
@Component
public class DefaultSimilarityCalculator implements SimilarityCalculator {

    @Override
    public float calculate(float distance) {
        // 距离为0时相似度为1，距离为1时相似度为0
        // 如果距离可能大于1，需要确保结果在0-1范围内
        return Math.max(0.0f, Math.min(1.0f, 1.0f - distance));
    }
}

