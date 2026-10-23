package com.macro.mall.portal.controller;

import com.macro.mall.common.api.CommonResult;
import com.macro.mall.model.PmsProduct;
import com.macro.mall.portal.service.StallMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * 摊位菜单Controller（白名单，无需登录）
 * Created by macro on 2026/10/12.
 */
@Controller
@Tag(name = "StallMenuController", description = "摊位今日菜单")
@RequestMapping("/stall/menu")
public class StallMenuController {

    @Autowired
    private StallMenuService stallMenuService;

    @Operation(summary = "今日菜单：上架且未删除、未售罄的商品")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<List<PmsProduct>> list() {
        return CommonResult.success(stallMenuService.listMenu());
    }
}
