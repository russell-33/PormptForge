package com.max.aicoder.service.impl;

import cn.hutool.core.util.StrUtil;
import com.max.aicoder.exception.ErrorCode;
import com.max.aicoder.mapper.PromptBlockLogMapper;
import com.max.aicoder.model.entity.PromptBlockLog;
import com.max.aicoder.service.PromptBlockLogService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import static com.max.aicoder.exception.ThrowUtils.throwIf;

/**
 * 被拦截 prompt 记录服务实现
 */
@Service
public class PromptBlockLogServiceImpl extends ServiceImpl<PromptBlockLogMapper, PromptBlockLog>
        implements PromptBlockLogService {

    @Override
    public boolean recordBlockedPrompt(Long appId, Long userId, String promptContent, String blockReason) {
        throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用Id不能为空");
        throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户Id不能为空");
        throwIf(StrUtil.isBlank(promptContent), ErrorCode.PARAMS_ERROR, "被拦截的 prompt 不能为空");
        throwIf(StrUtil.isBlank(blockReason), ErrorCode.PARAMS_ERROR, "拦截原因不能为空");
        PromptBlockLog promptBlockLog = PromptBlockLog.builder()
                .appId(appId)
                .userId(userId)
                .promptContent(promptContent)
                .blockReason(blockReason)
                .build();
        return this.save(promptBlockLog);
    }

    @Override
    public boolean deleteByAppId(Long appId) {
        throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用Id不能为空");
        QueryWrapper queryWrapper = QueryWrapper.create().eq("appId", appId);
        return this.remove(queryWrapper);
    }
}
