package com.macro.mall.portal.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * 摊主今日战绩统计（实收口径：今日创建且状态为已付款/制作中/待取货/已完成）
 * Created by macro on 2026/10/23.
 */
public class StallTodayStats {
    /** 今日实收总额 */
    private BigDecimal totalAmount;
    /** 今日订单数（实收口径） */
    private Long orderCount;
    /** 按商品聚合的销售明细 */
    private List<Item> items;

    public static class Item {
        /** 商品名 */
        private String productName;
        /** 销售数量 */
        private Integer quantity;
        /** 销售金额 */
        private BigDecimal amount;

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Long getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Long orderCount) {
        this.orderCount = orderCount;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }
}
