package com.max.aicoder.ai.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;

import java.util.Objects;

/**
 * prompt 安全审查护轨
 */
public class PromptSafetyInputGuardrail implements InputGuardrail {

    private final PromptSafetyReviewService promptSafetyReviewService;

    public PromptSafetyInputGuardrail(PromptSafetyReviewService promptSafetyReviewService) {
        this.promptSafetyReviewService = Objects.requireNonNull(promptSafetyReviewService);
    }

    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        var reviewResult = promptSafetyReviewService.review(userMessage.singleText());
        if (!reviewResult.safe()) {
            return fatal(reviewResult.reason());
        }
        return success();
    }
}
