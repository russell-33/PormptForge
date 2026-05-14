package com.max.aicoder.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 被拦截 prompt 记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("prompt_block_log")
public class PromptBlockLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 被拦截的 prompt 内容
     */
    @Column("promptContent")
    private String promptContent;

    /**
     * 拦截原因
     */
    @Column("blockReason")
    private String blockReason;

    /**
     * 应用 id
     */
    @Column("appId")
    private Long appId;

    /**
     * 用户 id
     */
    @Column("userId")
    private Long userId;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
