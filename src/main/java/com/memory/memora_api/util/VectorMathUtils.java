package com.memory.memora_api.util;

import java.util.List;

public final class VectorMathUtils {

    private VectorMathUtils() {
    }

    public static double cosineSimilarity(List<Double> vectorA, List<Double> vectorB) {
        if (vectorA == null || vectorB == null || vectorA.isEmpty() || vectorB.isEmpty()) {
            return 0.0;
        }

        if (vectorA.size() != vectorB.size()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.size(); i++) {
            double a = vectorA.get(i);
            double b = vectorB.get(i);
            dotProduct += a * b;
            normA += a * a;
            normB += b * b;
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}