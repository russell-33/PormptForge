package com.max.aicoder.ai.tools;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.max.aicoder.langgraph4j.model.ImageResource;
import com.max.aicoder.langgraph4j.tools.UndrawIllustrationTool;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 插画搜索工具。
 */
@Component
public class IllustrationSearchTool extends BaseTool {

    private static final int MAX_RETURNED_IMAGES = 6;

    @Resource
    private UndrawIllustrationTool undrawIllustrationTool;

    @Tool("搜索装饰性插画，适用于首页视觉、空状态、功能说明等，关键词必须优先使用英文")
    public String searchIllustrations(
            @P("插画搜索关键词，优先使用英文，尽量简洁")
            String query
    ) {
        List<ImageResource> images = undrawIllustrationTool.searchIllustrations(query);
        if (CollUtil.isEmpty(images)) {
            return "未找到可用的插画，请改用内容图片或占位图。";
        }
        StringBuilder result = new StringBuilder();
        result.append("以下是插画候选，请优先直接使用这些 URL，不要虚构图片地址：\n");
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
        return "searchIllustrations";
    }

    @Override
    public String getDisplayName() {
        return "搜索插画";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        return String.format("[工具调用] %s %s", getDisplayName(), arguments.getStr("query"));
    }
}
