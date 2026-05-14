package com.max.aicoder.ai.guardrail.model;

/**
 * prompt 审查结果
 */
public record PromptSafetyReviewResult(boolean safe, String reason) {

    public static PromptSafetyReviewResult passed() {
        return new PromptSafetyReviewResult(true, null);
    }

    public static PromptSafetyReviewResult blocked(String reason) {
        return new PromptSafetyReviewResult(false, reason);
    }
}
