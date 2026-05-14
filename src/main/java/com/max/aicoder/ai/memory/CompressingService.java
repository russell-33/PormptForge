package com.max.aicoder.ai.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class CompressingService {

    private static final String COMPRESS_PROMPT = """
            请将以下对话历史压缩为一段简洁的摘要，保留关键信息：
            - 用户的核心需求和偏好
            - 已完成的代码修改和决策
            - 未完成的任务和待解决的问题
            - 重要的技术约束和约定

            对话历史：
            ${conversation}

            输出一段连贯的摘要，不超过 500 字。
            """;

    private final ChatModel compressModel;

    public CompressingService(ChatModel compressModel) {
        this.compressModel = compressModel;
    }

    /**
     * 将消息列表压缩为一条摘要消息
     *
     * @param messages 待压缩的消息列表
     * @return 摘要消息，失败返回 null
     */
    public SystemMessage compress(List<ChatMessage> messages) {
        try {
            String conversationText = messages.stream()
                    .map(this::formatMessage)
                    .collect(Collectors.joining("\n"));

            String prompt = COMPRESS_PROMPT.replace("${conversation}", conversationText);

            ChatResponse response = compressModel.chat(
                    dev.langchain4j.data.message.UserMessage.from(prompt)
            );

            String summary = response.aiMessage().text();
            log.info("上下文压缩完成，原始消息数: {}，摘要长度: {}", messages.size(), summary.length());

            return SystemMessage.from("[对话摘要] 以下是之前对话的关键信息总结：\n" + summary);
        } catch (Exception e) {
            log.warn("上下文压缩失败，跳过压缩", e);
            return null;
        }
    }

    private String formatMessage(ChatMessage message) {
        String type = message.type().name();
        String text = switch (message) {
            case dev.langchain4j.data.message.UserMessage m -> m.singleText();
            case dev.langchain4j.data.message.AiMessage m -> m.text() != null ? m.text() : "[工具调用]";
            case SystemMessage m -> "[系统] " + m.text();
            default -> message.toString();
        };
        return type + ": " + text;
    }
}
