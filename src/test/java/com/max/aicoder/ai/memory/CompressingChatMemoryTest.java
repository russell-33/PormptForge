package com.max.aicoder.ai.memory;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CompressingChatMemoryTest {

    private ChatMemoryStore store;
    private TestCompressingService compressingService;

    @BeforeEach
    void setUp() {
        store = new InMemoryChatMemoryStore();
        compressingService = new TestCompressingService();
    }

    @Test
    void shouldStoreAndRetrieveMessages() {
        ChatMemory memory = CompressingChatMemory.builder()
                .id("test-1")
                .chatMemoryStore(store)
                .maxRecentMessages(5)
                .compressThreshold(10)
                .compressingService(compressingService)
                .build();

        memory.add(UserMessage.from("hello"));
        memory.add(AiMessage.from("hi there"));

        List<ChatMessage> messages = memory.messages();
        assertEquals(2, messages.size());
        assertTrue(messages.get(0) instanceof UserMessage);
        assertTrue(messages.get(1) instanceof AiMessage);
    }

    @Test
    void shouldNotCompressBelowThreshold() {
        ChatMemory memory = CompressingChatMemory.builder()
                .id("test-2")
                .chatMemoryStore(store)
                .maxRecentMessages(3)
                .compressThreshold(5)
                .compressingService(compressingService)
                .build();

        for (int i = 0; i < 5; i++) {
            memory.add(UserMessage.from("msg " + i));
        }

        assertFalse(compressingService.wasCalled());
        assertEquals(5, memory.messages().size());
    }

    @Test
    void shouldCompressWhenExceedingThreshold() {
        ChatMemory memory = CompressingChatMemory.builder()
                .id("test-3")
                .chatMemoryStore(store)
                .maxRecentMessages(3)
                .compressThreshold(5)
                .compressingService(compressingService)
                .build();

        for (int i = 0; i < 6; i++) {
            memory.add(UserMessage.from("msg " + i));
        }

        assertTrue(compressingService.wasCalled());
        List<ChatMessage> messages = memory.messages();
        assertEquals(4, messages.size());
        assertTrue(messages.get(0) instanceof SystemMessage);
        assertTrue(((SystemMessage) messages.get(0)).text().startsWith("[对话摘要]"));
    }

    @Test
    void shouldPersistToStore() {
        ChatMemory memory = CompressingChatMemory.builder()
                .id("test-4")
                .chatMemoryStore(store)
                .maxRecentMessages(5)
                .compressThreshold(10)
                .compressingService(compressingService)
                .build();

        memory.add(UserMessage.from("hello"));

        List<ChatMessage> stored = store.getMessages("test-4");
        assertEquals(1, stored.size());
    }

    @Test
    void shouldClearAllMessages() {
        ChatMemory memory = CompressingChatMemory.builder()
                .id("test-5")
                .chatMemoryStore(store)
                .maxRecentMessages(5)
                .compressThreshold(10)
                .compressingService(compressingService)
                .build();

        memory.add(UserMessage.from("hello"));
        memory.add(AiMessage.from("hi"));
        memory.clear();

        assertTrue(memory.messages().isEmpty());
    }

    @Test
    void shouldIncludeExistingSummaryWhenRecompressing() {
        ChatMemory memory = CompressingChatMemory.builder()
                .id("test-6")
                .chatMemoryStore(store)
                .maxRecentMessages(3)
                .compressThreshold(5)
                .compressingService(compressingService)
                .build();

        for (int i = 0; i < 6; i++) {
            memory.add(UserMessage.from("first round " + i));
        }
        // First compression: no existing summary should be passed
        assertFalse(compressingService.lastCompressedMessages().stream()
                .anyMatch(m -> m instanceof SystemMessage));

        for (int i = 0; i < 2; i++) {
            memory.add(UserMessage.from("second round " + i));
        }

        // Second compression: existing summary should be included in messages
        assertTrue(compressingService.lastCompressedMessages().stream()
                .anyMatch(m -> m instanceof SystemMessage));
    }

    @Test
    void shouldReturnId() {
        ChatMemory memory = CompressingChatMemory.builder()
                .id("test-7")
                .chatMemoryStore(store)
                .maxRecentMessages(5)
                .compressThreshold(10)
                .compressingService(compressingService)
                .build();

        assertEquals("test-7", memory.id());
    }

    static class TestCompressingService extends CompressingService {
        private boolean called = false;
        private int lastCompressedCount = 0;
        private List<ChatMessage> lastMessages = new ArrayList<>();

        TestCompressingService() {
            super(null);
        }

        @Override
        public SystemMessage compress(List<ChatMessage> messages) {
            called = true;
            lastCompressedCount = messages.size();
            lastMessages = new ArrayList<>(messages);
            return SystemMessage.from("[对话摘要] 这是测试摘要，包含 " + messages.size() + " 条消息的压缩结果。");
        }

        boolean wasCalled() {
            return called;
        }

        int getLastCompressedCount() {
            return lastCompressedCount;
        }

        List<ChatMessage> lastCompressedMessages() {
            return lastMessages;
        }
    }
}
