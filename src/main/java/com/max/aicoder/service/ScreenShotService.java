package com.max.aicoder.service;

public interface ScreenShotService {

    /**
     * 生成和上传截图
     *
     * @param webUrl
     * @return
     */
    String generateAndUploadScreenshot(String webUrl);

    /**
     * 删除 app 后关联删除截屏图片
     *
     * @param coverUrl
     */
    void deleteScreenshot(String coverUrl);
}
