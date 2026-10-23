package com.macro.mall.portal.service;

import com.macro.mall.common.api.CommonPage;
import com.macro.mall.portal.domain.OmsOrderDetail;
import com.macro.mall.portal.domain.StallOrderParam;

/**
 * 摊位订单管理Service
 * Created by macro on 2026/10/12.
 */
public interface StallOrderService {

    /**
     * 摊位下单：仅商品+数量+备注，生成取货码
     *
     * @return 订单ID
     */
    Long create(StallOrderParam stallOrderParam);

    /**
     * 分页获取当前会员的摊位订单（含取货码与订单商品）
     *
     * @param status -1->全部；否则按摊位状态过滤
     */
    CommonPage<OmsOrderDetail> list(Integer status, Integer pageNum, Integer pageSize);

    /**
     * 会员取消订单：仅"已下单"状态可取消
     */
    void cancelByMember(Long orderId);
}
