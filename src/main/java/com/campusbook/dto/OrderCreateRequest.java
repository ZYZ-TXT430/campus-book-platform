package com.campusbook.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 下单请求：二手书一次只买一本，所以只需要 bookId。
 */
@Data
public class OrderCreateRequest {

    @NotNull(message = "书籍 ID 不能为空")
    private Long bookId;

    /** 1-校内自提 2-快递 */
    @Min(value = 1, message = "交接方式不合法")
    @Max(value = 2, message = "交接方式不合法")
    private Integer deliveryType = 1;

    @Size(max = 255, message = "地址过长")
    private String deliveryAddr;

    @Size(max = 255, message = "留言过长")
    private String remark;
}
