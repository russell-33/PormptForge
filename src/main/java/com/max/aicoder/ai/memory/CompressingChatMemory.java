package com.max.aicoder.ai.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class CompressingChatMemory implements ChatMemory {

    private final Object id;
    private final int maxRecentMessages;
    private final int compressThreshold;
    private final ChatMemoryStore store;
    private final CompressingService compressingService;

    private List<ChatMessage> messages;

    private CompressingChatMemory(Builder builder) {
        this.id = builder.id;
        this.maxRecentMessages = builder.maxRecentMessages;
        this.compressThreshold = builder.compressThreshold;
        this.store = builder.store;
        this.compressingService = builder.compressingService;
        this.messages = new ArrayList<>(store.getMessages(id));
    }

    @Override
    public Object id() {
        return id;
    }

    @Override
    public synchronized void add(ChatMessage message) {
        messages.add(message);
        if (messages.size() > compressThreshold) {
            compress();
        }
        store.updateMessages(id, messages);
    }

    /**
     * 批量添加消息，加载完成后统一检查是否需要压缩。
     * 避免逐条 add 导致加载阶段反复触发压缩。
     */
    public synchronized void addAll(List<ChatMessage> newMessages) {
        messages.addAll(newMessages);
        if (messages.size() > compressThreshold) {
            compress();
        }
        store.updateMessages(id, messages);
    }

    @Override
    public synchronized List<ChatMessage> messages() {
        return new ArrayList<>(messages);
    }

    @Override
    public synchronized void clear() {
        messages.clear();
        store.updateMessages(id, messages);
    }

    private synchronized void compress() {
        List<ChatMessage> rawMessages = new ArrayList<>();
        SystemMessage existingSummary = null;

        for (ChatMessage msg : messages) {
            if (msg instanceof SystemMessage sm && sm.text().startsWith("[对话摘要]")) {
                existingSummary = sm;
            } else {
                rawMessages.add(msg);
            }
        }

        int compressEnd = rawMessages.size() - maxRecentMessages;
        List<ChatMessage> toCompress = new ArrayList<>(rawMessages.subList(0, compressEnd));
        List<ChatMessage> toKeep = new ArrayList<>(rawMessages.subList(compressEnd, rawMessages.size()));

        if (existingSummary != null) {
            toCompress.add(0, existingSummary);
        }

        SystemMessage newSummary = compressingService.compress(toCompress);

        if (newSummary == null && existingSummary == null) {
            log.warn("压缩失败且无现有摘要，保留原始消息");
            return;
        }

        List<ChatMessage> newMessages = new ArrayList<>();
        if (newSummary != null) {
            newMessages.add(newSummary);
        } else {
            if (existingSummary != null) {
                newMessages.add(existingSummary);
            }
        }
        newMessages.addAll(toKeep);

        messages = newMessages;
        log.info("上下文已压缩，压缩前: {} 条，压缩后: {} 条（含摘要）",
                toCompress.size() + toKeep.size(), messages.size());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Object id;
        private int maxRecentMessages = 20;
        private int compressThreshold = 50;
        private ChatMemoryStore store;
        private CompressingService compressingService;

        public Builder id(Object id) {
            this.id = id;
            return this;
        }

        public Builder maxRecentMessages(int maxRecentMessages) {
            this.maxRecentMessages = maxRecentMessages;
            return this;
        }

        public Builder compressThreshold(int compressThreshold) {
            this.compressThreshold = compressThreshold;
            return this;
        }

        public Builder chatMemoryStore(ChatMemoryStore store) {
            this.store = store;
            return this;
        }

        public Builder compressingService(CompressingService compressingService) {
            this.compressingService = compressingService;
            return this;
        }

        public CompressingChatMemory build() {
            if (id == null) throw new IllegalArgumentException("id must not be null");
            if (store == null) throw new IllegalArgumentException("store must not be null");
            if (compressingService == null) throw new IllegalArgumentException("compressingService must not be null");
            if (maxRecentMessages <= 0) throw new IllegalArgumentException("maxRecentMessages must be > 0");
            if (maxRecentMessages >= compressThreshold)
                throw new IllegalArgumentException("maxRecentMessages must be less than compressThreshold");
            return new CompressingChatMemory(this);
        }
    }
}
