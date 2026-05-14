package com.max.aicoder.ai.tools;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.max.aicoder.langgraph4j.model.ImageResource;
import com.max.aicoder.langgraph4j.tools.ImageSearchTool;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 内容图片搜索工具
 */
@Component
public class ContentImageSearchTool extends BaseTool {

    private static final int MAX_RETURNED_IMAGES = 6;

    @Resource
    private ImageSearchTool imageSearchTool;

    @Tool("搜索网站内容相关的真实图片，适用于产品展示、人物场景、行业配图等，关键词尽量使用英文")
    public String searchContentImages(
            @P("图片搜索关键词，尽量使用英文，简洁准确")
            String query
    ) {
        List<ImageResource> images = imageSearchTool.searchContentImages(query);
        if (CollUtil.isEmpty(images)) {
            return "未找到可用的内容图片，请改用占位图或尝试更通用的英文关键词。";
        }
        StringBuilder result = new StringBuilder();
        result.append("以下是真实内容图片候选，请优先直接使用这些 URL，不要虚构图片地址：\n");
        int limit = Math.min(MAX_RETURNED_IMAGES, images.size());
        for (int i = 0; i < limit; i++) {
            ImageResource image = images.get(i);
            result.append(i + 1)
                    .append(". 描述：")
                    .append(StrUtil.blankToDefault(image.getDescription(), query))
                    .append("\n")
                    .append("   URL: ")
                    .append(image.getUrl())
                    .append("\n");
        }
        return result.toString().trim();
    }

    @Override
    public String getToolName() {
        return "searchContentImages";
    }

    @Override
    public String getDisplayName() {
        return "搜索内容图片";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        return String.format("[工具调用] %s %s", getDisplayName(), arguments.getStr("query"));
    }
}
