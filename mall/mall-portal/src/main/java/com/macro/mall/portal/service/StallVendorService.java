package com.macro.mall.portal.service;

import com.macro.mall.common.api.CommonPage;
import com.macro.mall.model.PmsProduct;
import com.macro.mall.portal.domain.OmsOrderDetail;
import com.macro.mall.portal.domain.StallTodayStats;

import java.util.List;

/**
 * 摊主模式Service：商品开关与订单队列状态推进
 * Created by macro on 2026/10/12.
 */
public interface StallVendorService {

    /**
     * 全部商品（含已隐藏），供摊主开关操作
     */
    List<PmsProduct> listAllProducts();

    /**
     * 今日战绩统计（实收口径：今日创建且状态为已付款/制作中/待取货/已完成）
     */
    StallTodayStats todayStats();

    /**
     * 更新商品上架状态：0->隐藏；1->上架
     */
    void updatePublishStatus(Long productId, Integer publishStatus);

    /**
     * 按摊位状态分页查询订单队列（-1->全部）
     */
    CommonPage<OmsOrderDetail> listOrders(Integer status, Integer pageNum, Integer pageSize);

    /**
     * 摊主推进订单状态（校验合法流转），记录时间戳
     */
    void transition(Long orderId, Integer targetStatus);

    /**
     * 摊主取消未完成订单并释放库存
     */
    void cancelByVendor(Long orderId);
}
