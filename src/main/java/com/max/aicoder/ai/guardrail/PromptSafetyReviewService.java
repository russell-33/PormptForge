package com.max.aicoder.ai.guardrail;

import com.max.aicoder.ai.guardrail.model.PromptSafetyReviewResult;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * prompt 安全审查服务
 */
@Service
public class PromptSafetyReviewService {

    private static final int MAX_PROMPT_LENGTH = 1000;

    private static final List<String> SENSITIVE_WORDS = Arrays.asList(
            "忽略之前的指令", "ignore previous instructions", "ignore above",
            "破解", "hack", "绕过", "bypass", "越狱", "jailbreak"
    );

    private static final List<Pattern> INJECTION_PATTERNS = Arrays.asList(
            Pattern.compile("(?i)ignore\\s+(?:previous|above|all)\\s+(?:instructions?|commands?|prompts?)"),
            Pattern.compile("(?i)(?:forget|disregard)\\s+(?:everything|all)\\s+(?:above|before)"),
            Pattern.compile("(?i)(?:pretend|act|behave)\\s+(?:as|like)\\s+(?:if|you\\s+are)"),
            Pattern.compile("(?i)system\\s*:\\s*you\\s+are"),
            Pattern.compile("(?i)new\\s+(?:instructions?|commands?|prompts?)\\s*:")
    );

    public PromptSafetyReviewResult review(String input) {
        if (input == null || input.trim().isEmpty()) {
            return PromptSafetyReviewResult.blocked("输入内容不能为空");
        }
        if (input.length() > MAX_PROMPT_LENGTH) {
            return PromptSafetyReviewResult.blocked("输入内容过长，不要超过 1000 字");
        }
        String lowerInput = input.toLowerCase();
        for (String sensitiveWord : SENSITIVE_WORDS) {
            if (lowerInput.contains(sensitiveWord.toLowerCase())) {
                return PromptSafetyReviewResult.blocked("输入包含不当内容，请修改后重试");
            }
        }
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                return PromptSafetyReviewResult.blocked("检测到恶意输入，请求被拒绝");
            }
        }
        return PromptSafetyReviewResult.passed();
    }
}
