package com.macro.mall.portal.controller;

import com.macro.mall.common.api.CommonPage;
import com.macro.mall.common.api.CommonResult;
import com.macro.mall.portal.domain.OmsOrderDetail;
import com.macro.mall.portal.domain.StallOrderParam;
import com.macro.mall.portal.service.StallOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 摊位订单管理Controller（顾客端）
 * Created by macro on 2026/10/12.
 */
@Controller
@Tag(name = "StallOrderController", description = "摊位订单管理（顾客端）")
@RequestMapping("/stall/order")
public class StallOrderController {

    @Autowired
    private StallOrderService stallOrderService;

    @Operation(summary = "摊位下单（仅商品+数量+备注）")
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult<Long> create(@RequestBody StallOrderParam stallOrderParam) {
        return CommonResult.success(stallOrderService.create(stallOrderParam), "下单成功");
    }

    @Operation(summary = "分页获取当前会员的摊位订单（含取货码）")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<CommonPage<OmsOrderDetail>> list(@RequestParam(value = "status", defaultValue = "-1") Integer status,
                                                         @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                                                         @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum) {
        CommonPage<OmsOrderDetail> orderPage = stallOrderService.list(status, pageNum, pageSize);
        return CommonResult.success(orderPage);
    }

    @Operation(summary = "会员取消订单（仅已下单状态可取消）")
    @RequestMapping(value = "/cancel/{orderId}", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult cancel(@PathVariable Long orderId) {
        stallOrderService.cancelByMember(orderId);
        return CommonResult.success(null, "订单已取消");
    }
}
