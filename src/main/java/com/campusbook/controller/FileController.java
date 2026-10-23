package com.campusbook.controller;

import com.campusbook.common.BusinessException;
import com.campusbook.common.Result;
import com.campusbook.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 图片上传。
 *
 * <p>只允许图片类型、限制 5MB，文件名用 UUID 重新生成，
 * 避免用户上传的可执行文件和路径穿越问题。</p>
 */
@Slf4j
@RestController
public class FileController {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM");

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @PostMapping("/api/files/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的图片");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("图片不能超过 5MB");
        }

        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.lastIndexOf('.') >= 0) {
            ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        }
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException("只支持 jpg / jpeg / png / webp / gif 格式");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("上传的文件不是图片");
        }

        String datePath = LocalDate.now().format(DATE_PATH);
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path target = Paths.get(uploadDir, datePath, filename).toAbsolutePath().normalize();

        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target.toFile());
        } catch (IOException e) {
            log.error("保存图片失败: {}", target, e);
            throw new BusinessException(ResultCode.ERROR, "图片保存失败，请稍后重试");
        }

        String url = "/uploads/" + datePath + "/" + filename;
        log.info("图片上传成功: {}", url);
        return Result.success(url);
    }
}
