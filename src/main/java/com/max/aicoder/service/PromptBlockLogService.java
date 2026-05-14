package com.max.aicoder.service;

import com.max.aicoder.model.entity.PromptBlockLog;
import com.mybatisflex.core.service.IService;

/**
 * 被拦截 prompt 记录服务
 */
public interface PromptBlockLogService extends IService<PromptBlockLog> {

    /**
     * 记录被拦截的 prompt
     *
     * @param appId         应用 id
     * @param userId        用户 id
     * @param promptContent prompt 内容
     * @param blockReason   拦截原因
     * @return 是否成功
     */
    boolean recordBlockedPrompt(Long appId, Long userId, String promptContent, String blockReason);

    /**
     * 按应用删除被拦截 prompt 记录
     *
     * @param appId 应用 id
     * @return 是否成功
     */
    boolean deleteByAppId(Long appId);
}
