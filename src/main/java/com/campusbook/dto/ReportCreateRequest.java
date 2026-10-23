package com.campusbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 举报书籍请求。
 */
@Data
public class ReportCreateRequest {

    @NotNull(message = "书籍 ID 不能为空")
    private Long bookId;

    @NotBlank(message = "请填写举报原因")
    @Size(max = 255, message = "举报原因不能超过 255 字")
    private String reason;
}
