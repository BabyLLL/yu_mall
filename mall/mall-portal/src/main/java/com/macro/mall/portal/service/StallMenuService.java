package com.macro.mall.portal.service;

import com.macro.mall.model.PmsProduct;

import java.util.List;

/**
 * 摊位菜单Service
 * Created by macro on 2026/10/12.
 */
public interface StallMenuService {

    /**
     * 今日菜单：上架（publish_status=1）、未删除且未售罄的商品
     */
    List<PmsProduct> listMenu();
}
