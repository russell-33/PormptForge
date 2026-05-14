package com.max.aicoder.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.max.aicoder.ai.guardrail.PromptSafetyInputGuardrail;
import com.max.aicoder.ai.guardrail.PromptSafetyReviewService;
import com.max.aicoder.ai.tools.ContentImageSearchTool;
import com.max.aicoder.ai.tools.IllustrationSearchTool;
import com.max.aicoder.ai.tools.ToolManager;
import com.max.aicoder.exception.BusinessException;
import com.max.aicoder.exception.ErrorCode;
import com.max.aicoder.model.enums.CodeGenTypeEnum;
import com.max.aicoder.service.ChatHistoryService;
import com.max.aicoder.utils.SpringContextUtil;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import com.max.aicoder.ai.memory.CompressingChatMemory;
import com.max.aicoder.ai.memory.CompressingService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.time.Duration;

@Configuration
@Slf4j
public class AiCodeGeneratorServiceFactory {

    @Resource(name = "openAiChatModel")
    private ChatModel chatModel;
    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;
    @Lazy
    @Resource
    private ChatHistoryService chatHistoryService;
    @Resource
    private ToolManager toolManager;
    @Resource
    private ContentImageSearchTool contentImageSearchTool;
    @Resource
    private IllustrationSearchTool illustrationSearchTool;
    @Resource
    private PromptSafetyReviewService promptSafetyReviewService;
    @Resource
    private CompressingService compressingService;

    /**
     * AI 服务实例缓存
     * 缓存策略：
     * - 最大缓存 1000 个实例
     * - 写入后 30 分钟过期
     * - 访问后 10 分钟过期
     */
    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，缓存键: {}, 原因: {}", key, cause);
            })
            .build();

    /**
     * 根据 appId 获取服务（带缓存）
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        String cachedKey = newCachedKey(appId, codeGenType);
        return serviceCache.get(cachedKey,
                key -> createAiCodeGeneratorService(appId, codeGenType));
    }

    /**
     * 根据 appId 获取服务（带缓存）
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return getAiCodeGeneratorService(appId, CodeGenTypeEnum.MULTI_FILE);
    }

    /**
     * 创建新的 AI 服务实例
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        // 根据 appId 构建独立的对话记忆（带压缩）
        CompressingChatMemory chatMemory = CompressingChatMemory.builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxRecentMessages(20)
                .compressThreshold(50)
                .compressingService(compressingService)
                .build();
        // 从数据库加载历史对话到记忆中
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 200);
        // 根据代码生成类型选择不同的模型配置
        return switch (codeGenType) {
            // Vue 项目生成使用推理模型
            case VUE_PROJECT -> {
                //使用多例的 chat model
                StreamingChatModel reasoningStreamingChatModel = SpringContextUtil
                        .<StreamingChatModel>getBean("reasoningStreamingChatModelPrototype",
                                StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .streamingChatModel(reasoningStreamingChatModel)
                        .chatMemoryProvider(memoryId -> chatMemory)
                        .tools(toolManager.getAllTools())
                        .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                                toolExecutionRequest, "Error: there is no tool called " + toolExecutionRequest.name()
                        ))
                        .maxSequentialToolsInvocations(30)  // 最多连续调用 20 次工具
                        .inputGuardrails(new PromptSafetyInputGuardrail(promptSafetyReviewService))// 添加输入护轨
                        //.outputGuardrails(new RetryOutputGuardrail())// 添加输出护轨，但是为了有流式输出，关闭输出护轨
                        .build();
            }
            // HTML 和多文件生成使用默认模型
            case HTML, MULTI_FILE -> {
                //使用多例的 chat model
                StreamingChatModel streamingChatModel = SpringContextUtil
                        .<StreamingChatModel>getBean("streamingChatModelPrototype",
                                StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(chatModel)
                        .streamingChatModel(streamingChatModel)
                        .chatMemory(chatMemory)
                        .tools(contentImageSearchTool, illustrationSearchTool)
                        .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                                toolExecutionRequest, "Error: there is no tool called " + toolExecutionRequest.name()
                        ))
                        .maxSequentialToolsInvocations(10)
                        .inputGuardrails(new PromptSafetyInputGuardrail(promptSafetyReviewService))// 添加输入护轨
                        //.outputGuardrails(new RetryOutputGuardrail())// 添加输出护轨，但是为了有流式输出，关闭输出护轨
                        .build();
            }
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "不支持的代码生成类型: " + codeGenType.getValue());
        };
    }


    /**
     * 给 Spring 预先放一个默认版 AiCodeGeneratorService，默认记忆 key 是 0，
     * 默认模式是 MULTI_FILE；不过当前真正干活的还是工厂按参数动态创建的那些实例
     *
     * @return AiCodeGeneratorService 的实例
     */
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return getAiCodeGeneratorService(0);
    }

    private String newCachedKey(long appId, CodeGenTypeEnum codeGenType) {
        return appId + "_" + codeGenType.getValue();
    }


}
