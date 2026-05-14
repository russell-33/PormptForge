package com.max.aicoder.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
class WebScreenshotUtilsTest {

    @Test
    void saveWebPageScreenshot() {
        String webUrl = "https://www.codefather.cn";
        String screenshotPath = WebScreenshotUtils.saveWebPageScreenshot(webUrl);
        Assertions.assertNotNull(screenshotPath);
    }
}