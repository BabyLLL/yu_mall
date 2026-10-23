package com.macro.mall.portal.controller;

import com.macro.mall.common.api.CommonResult;
import com.macro.mall.mapper.PmsProductMapper;
import com.macro.mall.model.PmsProduct;
import com.macro.mall.model.PmsProductExample;
import com.macro.mall.portal.domain.StallOrderConsts;
import com.macro.mall.portal.service.StallVendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * 摊主模式Controller：商品开关与订单队列操作
 * 仅摊主会话（MINIAPP_ADMIN）可访问。
 * Created by macro on 2026/10/12.
 */
@Controller
@Tag(name = "StallVendorController", description = "摊主模式：商品管理/订单队列")
@RequestMapping("/stall/vendor")
public class StallVendorController {

    @Autowired
    private StallVendorService stallVendorService;
    @Autowired
    private PmsProductMapper productMapper;

    @Operation(summary = "商品列表（含已隐藏商品）")
    @PreAuthorize("hasAuthority('MINIAPP_ADMIN')")
    @RequestMapping(value = "/products", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<List<PmsProduct>> products() {
        return CommonResult.success(stallVendorService.listAllProducts());
    }

    @Operation(summary = "隐藏商品")
    @PreAuthorize("hasAuthority('MINIAPP_ADMIN')")
    @RequestMapping(value = "/product/{productId}/hide", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult hideProduct(@PathVariable Long productId) {
        stallVendorService.updatePublishStatus(productId, 0);
        return CommonResult.success(null, "商品已隐藏");
    }

    @Operation(summary = "上架商品")
    @PreAuthorize("hasAuthority('MINIAPP_ADMIN')")
    @RequestMapping(value = "/product/{productId}/show", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult showProduct(@PathVariable Long productId) {
        stallVendorService.updatePublishStatus(productId, 1);
        return CommonResult.success(null, "商品已上架");
    }

    @Operation(summary = "按状态查询摊位订单队列（-1->全部）")
    @PreAuthorize("hasAuthority('MINIAPP_ADMIN')")
    @RequestMapping(value = "/orders", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult orders(@RequestParam(value = "status", defaultValue = "-1") Integer status,
                               @RequestParam(value = "pageSize", defaultValue = "50") Integer pageSize,
                               @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum) {
        return CommonResult.success(stallVendorService.listOrders(status, pageNum, pageSize));
    }

    @Operation(summary = "确认收款：已下单->已付款")
    @PreAuthorize("hasAuthority('MINIAPP_ADMIN')")
    @RequestMapping(value = "/order/{orderId}/confirmPay", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult confirmPay(@PathVariable Long orderId) {
        stallVendorService.transition(orderId, StallOrderConsts.STATUS_PAID);
        return CommonResult.success(null, "已确认收款");
    }

    @Operation(summary = "开始制作：已付款->制作中")
    @PreAuthorize("hasAuthority('MINIAPP_ADMIN')")
    @RequestMapping(value = "/order/{orderId}/startMaking", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult startMaking(@PathVariable Long orderId) {
        stallVendorService.transition(orderId, StallOrderConsts.STATUS_MAKING);
        return CommonResult.success(null, "已开始制作");
    }

    @Operation(summary = "出餐完成：制作中->待取货")
    @PreAuthorize("hasAuthority('MINIAPP_ADMIN')")
    @RequestMapping(value = "/order/{orderId}/finishMaking", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult finishMaking(@PathVariable Long orderId) {
        stallVendorService.transition(orderId, StallOrderConsts.STATUS_READY);
        return CommonResult.success(null, "已出餐，请取货");
    }

    @Operation(summary = "取货完成：待取货->已完成")
    @PreAuthorize("hasAuthority('MINIAPP_ADMIN')")
    @RequestMapping(value = "/order/{orderId}/finish", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult finish(@PathVariable Long orderId) {
        stallVendorService.transition(orderId, StallOrderConsts.STATUS_FINISHED);
        return CommonResult.success(null, "订单已完成");
    }

    @Operation(summary = "摊主取消订单（未完成均可取消）")
    @PreAuthorize("hasAuthority('MINIAPP_ADMIN')")
    @RequestMapping(value = "/order/{orderId}/cancel", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult cancel(@PathVariable Long orderId) {
        stallVendorService.cancelByVendor(orderId);
        return CommonResult.success(null, "订单已取消");
    }
}
