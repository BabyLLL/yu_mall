package com.macro.mall.portal.domain;

/**
 * 摊位订单相关常量
 * 订单状态在 oms_order.status 上复用电商数值段，语义重映射为摊位状态；
 * 摊位订单通过 order_type=2 与电商订单区分。
 * Created by macro on 2026/10/12.
 */
public final class StallOrderConsts {

    private StallOrderConsts() {
    }

    /**
     * 摊位订单状态（oms_order.status 语义重映射）
     * 0->已下单(待付款)；1->已付款；2->制作中；3->待取货；4->已完成；5->已取消(含超时取消)
     */
    public static final int STATUS_CREATED = 0;
    public static final int STATUS_PAID = 1;
    public static final int STATUS_MAKING = 2;
    public static final int STATUS_READY = 3;
    public static final int STATUS_FINISHED = 4;
    public static final int STATUS_CANCELED = 5;

    /**
     * 订单类型：2->摊位订单
     */
    public static final int ORDER_TYPE_STALL = 2;

    /**
     * 摊主可执行状态推进的合法流转：当前状态 -> 允许的目标状态集合
     */
    public static final java.util.Map<Integer, java.util.Set<Integer>> VENDOR_TRANSITIONS = buildVendorTransitions();

    private static java.util.Map<Integer, java.util.Set<Integer>> buildVendorTransitions() {
        java.util.Map<Integer, java.util.Set<Integer>> map = new java.util.HashMap<>();
        map.put(STATUS_CREATED, new java.util.HashSet<>(java.util.Arrays.asList(STATUS_PAID, STATUS_CANCELED)));
        map.put(STATUS_PAID, new java.util.HashSet<>(java.util.Arrays.asList(STATUS_MAKING, STATUS_CANCELED)));
        map.put(STATUS_MAKING, new java.util.HashSet<>(java.util.Arrays.asList(STATUS_READY, STATUS_CANCELED)));
        map.put(STATUS_READY, new java.util.HashSet<>(java.util.Arrays.asList(STATUS_FINISHED, STATUS_CANCELED)));
        return map;
    }

    /**
     * 顾客仅可取消"已下单"状态的订单
     */
    public static boolean canMemberCancel(int status) {
        return status == STATUS_CREATED;
    }
}
