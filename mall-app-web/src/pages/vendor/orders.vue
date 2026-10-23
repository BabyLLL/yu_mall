<template>
  <view class="container">
    <view class="section-box">
      <!-- 状态过滤 -->
      <view class="order-filter">
        <view
          v-for="f in orderFilters"
          :key="f.state"
          class="filter-item"
          :class="{ current: orderFilter === f.state }"
          @click="handleSwitchFilter(f.state)"
        >
          <text>{{ f.text }}</text>
        </view>
      </view>

      <view v-for="order in orderList" :key="order.id" class="order-card">
        <view class="order-card-top">
          <text class="order-code">{{ order.pickupCode || order.orderSn }}</text>
          <text class="order-state">{{ statusText(order.status) }}</text>
        </view>
        <view class="order-goods">
          <text v-for="(g, gi) in order.orderItemList" :key="gi" class="order-goods-line">
            {{ g.productName }} x {{ g.productQuantity }}
          </text>
        </view>
        <view v-if="order.note" class="order-note">
          <text class="order-note-label">备注</text>
          <text class="order-note-text">{{ order.note }}</text>
        </view>
        <view class="order-card-bottom">
          <text class="order-amount">¥{{ order.payAmount }}</text>
          <view class="order-actions">
            <view
              v-if="order.status === 0"
              class="order-action-btn primary"
              @click="handleTransition(order.id, 'confirmPay')"
            >
              <text>确认收款</text>
            </view>
            <view
              v-if="order.status === 1"
              class="order-action-btn primary"
              @click="handleTransition(order.id, 'startMaking')"
            >
              <text>开始制作</text>
            </view>
            <view
              v-if="order.status === 2"
              class="order-action-btn primary"
              @click="handleTransition(order.id, 'finishMaking')"
            >
              <text>出餐完成</text>
            </view>
            <view
              v-if="order.status === 3"
              class="order-action-btn primary"
              @click="handleTransition(order.id, 'finish')"
            >
              <text>取货完成</text>
            </view>
            <view
              v-if="order.status !== 4 && order.status !== 5"
              class="order-action-btn plain"
              @click="handleCancelOrder(order.id)"
            >
              <text>取消</text>
            </view>
          </view>
        </view>
      </view>
      <view v-if="orderList.length === 0" class="empty-box">
        <text class="empty-text">暂无订单</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import {
  cancelVendorOrderAPI,
  confirmPayAPI,
  finishMakingAPI,
  finishOrderAPI,
  getVendorOrdersAPI,
  startMakingAPI,
} from '@/apis/stall'
import { STALL_ORDER_STATUS, STALL_ORDER_STATUS_TEXT } from '@/types/stall'
import type { StallOrder } from '@/types/stall'

// ===== 页面数据 =====
// 订单列表
const orderList = ref<StallOrder[]>([])
// 订单过滤状态
const orderFilter = ref(-1)
// 订单过滤选项
const orderFilters = [
  { state: -1, text: '全部' },
  { state: STALL_ORDER_STATUS.CREATED, text: '待收款' },
  { state: STALL_ORDER_STATUS.PAID, text: '已付款' },
  { state: STALL_ORDER_STATUS.MAKING, text: '制作中' },
  { state: STALL_ORDER_STATUS.READY, text: '待取货' },
]

// ===== 加载 =====
// 加载订单队列
const loadOrders = async () => {
  try {
    const res = await getVendorOrdersAPI({ status: orderFilter.value, pageNum: 1, pageSize: 50 })
    orderList.value = (res.data?.list || []) as StallOrder[]
  } catch (e) {
    console.error('加载订单失败', e)
  }
}

onShow(() => {
  loadOrders()
})

// ===== 事件处理 =====
// 切换订单过滤
const handleSwitchFilter = (state: number) => {
  orderFilter.value = state
  loadOrders()
}

// 状态推进
const handleTransition = async (
  orderId: number,
  action: 'confirmPay' | 'startMaking' | 'finishMaking' | 'finish',
) => {
  try {
    if (action === 'confirmPay') {
      await confirmPayAPI(orderId)
    } else if (action === 'startMaking') {
      await startMakingAPI(orderId)
    } else if (action === 'finishMaking') {
      await finishMakingAPI(orderId)
    } else {
      await finishOrderAPI(orderId)
    }
    uni.showToast({ icon: 'success', title: '操作成功' })
    loadOrders()
  } catch (err: any) {
    uni.showToast({ icon: 'none', title: err?.message || '操作失败' })
  }
}

// 取消订单
const handleCancelOrder = (orderId: number) => {
  uni.showModal({
    title: '提示',
    content: '确认取消该订单？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await cancelVendorOrderAPI(orderId)
          uni.showToast({ icon: 'success', title: '已取消' })
          loadOrders()
        } catch (err: any) {
          uni.showToast({ icon: 'none', title: err?.message || '取消失败' })
        }
      }
    },
  })
}

// 状态文案
const statusText = (status: number): string => {
  return STALL_ORDER_STATUS_TEXT[status] || ''
}
</script>

<style lang="scss" scoped>
$stall-red: #c23a22;
$stall-bg: #fdf8f2;
$stall-card: #ffffff;
$text-main: #3d2b1f;
$text-sub: #8a7a6d;

page {
  background: $stall-bg;
}

.container {
  padding: 24rpx;
}

.section-box {
  background: $stall-card;
  border-radius: 20rpx;
  padding: 12rpx 24rpx;
}

.order-filter {
  display: flex;
  padding: 12rpx 0;
  border-bottom: 1rpx solid #f7f2ec;

  .filter-item {
    padding: 8rpx 24rpx;
    margin-right: 12rpx;
    border-radius: 28rpx;
    font-size: 24rpx;
    color: $text-sub;
    background: #f7f2ec;

    &.current {
      background: $stall-red;
      color: #ffffff;
    }
  }
}

.order-card {
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f7f2ec;

  &:last-child {
    border-bottom: none;
  }

  .order-card-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12rpx;

    .order-code {
      font-size: 40rpx;
      font-weight: 800;
      color: $stall-red;
      letter-spacing: 2rpx;
    }

    .order-state {
      font-size: 26rpx;
      font-weight: 600;
      color: $text-main;
    }
  }

  .order-goods {
    display: flex;
    flex-direction: column;

    .order-goods-line {
      font-size: 26rpx;
      color: $text-main;
      line-height: 40rpx;
    }
  }

  .order-note {
    display: flex;
    margin-top: 8rpx;

    .order-note-label {
      font-size: 24rpx;
      color: $text-sub;
      margin-right: 12rpx;
      flex-shrink: 0;
    }

    .order-note-text {
      font-size: 24rpx;
      color: $stall-red;
    }
  }

  .order-card-bottom {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 16rpx;

    .order-amount {
      font-size: 32rpx;
      font-weight: 700;
      color: $text-main;
    }

    .order-actions {
      display: flex;

      .order-action-btn {
        padding: 12rpx 28rpx;
        margin-left: 16rpx;
        border-radius: 32rpx;
        font-size: 26rpx;

        &.primary {
          background: $stall-red;
          color: #ffffff;
        }

        &.plain {
          border: 2rpx solid #e8dfd4;
          color: $text-sub;
        }
      }
    }
  }
}

.empty-box {
  display: flex;
  justify-content: center;
  padding: 80rpx 0;

  .empty-text {
    font-size: 26rpx;
    color: $text-sub;
  }
}
</style>
