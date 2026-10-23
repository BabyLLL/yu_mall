package com.macro.mall.portal.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 摊位下单入参：仅商品+数量+备注，不需要地址、优惠券、积分、支付方式
 * Created by macro on 2026/10/12.
 */
@Data
public class StallOrderParam {
    @Schema(title = "商品与数量列表")
    private List<StallOrderItemParam> items;
    @Schema(title = "订单备注，如：多辣")
    private String note;
}
