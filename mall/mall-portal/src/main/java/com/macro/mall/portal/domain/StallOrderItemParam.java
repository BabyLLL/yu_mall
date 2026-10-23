package com.macro.mall.portal.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 摊位下单入参中的商品项
 * Created by macro on 2026/10/12.
 */
@Data
public class StallOrderItemParam {
    @Schema(title = "商品ID")
    private Long productId;
    @Schema(title = "数量")
    private Integer quantity;
}
