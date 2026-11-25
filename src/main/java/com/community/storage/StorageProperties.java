package com.community.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地存储配置：数据目录默认是项目根目录下的 data
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    /** 数据目录，相对路径以应用启动目录为基准 */
    private String dir = "./data";

    public Path resolveDir() {
        Path path = Paths.get(dir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new UncheckedIOException("无法创建数据目录：" + path, e);
        }
        return path;
    }
}
