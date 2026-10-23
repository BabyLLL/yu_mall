<template>
  <view class="container">
    <!-- 下单成功 -->
    <view class="success-box">
      <view class="success-icon">
        <text class="success-check">✓</text>
      </view>
      <text class="success-title">下单成功</text>
      <text class="success-tip">请记好取货码，到摊叫号取货</text>
    </view>

    <!-- 取货码 -->
    <view class="pickup-card" :class="{ 'pickup-card-failed': loadFailed }" @click="loadFailed && loadOrder()">
      <template v-if="loadFailed">
        <text class="pickup-label">取货码加载失败</text>
        <view class="pickup-retry-btn">
          <text class="pickup-retry-icon">↻</text>
          <text class="pickup-retry-text">{{ loading ? '加载中...' : '点击重试' }}</text>
        </view>
        <text class="pickup-failed-tip">可稍后在"我的订单"中查看取货码</text>
      </template>
      <template v-else>
        <text class="pickup-label">取货码</text>
        <text class="pickup-code">{{ pickupCode || '...' }}</text>
        <view class="pickup-amount-box">
          <text class="pickup-amount-label">应付金额</text>
          <text class="pickup-amount">¥{{ payAmount }}</text>
        </view>
        <view class="pickup-status-box">
          <text class="pickup-status-tip">取餐时当面付款</text>
        </view>
      </template>
    </view>

    <!-- 操作 -->
    <view class="action-box">
      <view class="action-btn action-btn-primary" @click="handleGoOrders">
        <text>查看订单</text>
      </view>
      <view class="action-btn action-btn-plain" @click="handleBackMenu">
        <text>再买一点</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { getStallOrderListAPI } from '@/apis/stall'
import type { StallOrder } from '@/types/stall'

// ===== 页面数据 =====
// 订单ID
const orderId = ref<number>(0)
// 取货码
const pickupCode = ref('')
// 应付金额
const payAmount = ref('0.00')
// 订单状态
const status = ref(0)
// 加载中（重试按钮显示加载态）
const loading = ref(false)
// 加载失败（查不到订单时展示降级态，点击卡片重试）
const loadFailed = ref(false)

// ===== 数据加载 =====
// 加载订单信息（取货码在订单上，下单接口只返回ID，需查一次）
const loadOrder = async () => {
  if (!orderId.value || loading.value) return
  loading.value = true
  loadFailed.value = false
  try {
    const res = await getStallOrderListAPI({ pageNum: 1, pageSize: 10 })
    const orders = (res.data?.list || []) as StallOrder[]
    const order = orders.find((item) => item.id === orderId.value)
    if (order) {
      pickupCode.value = order.pickupCode || ''
      payAmount.value = Number(order.payAmount || 0).toFixed(2)
      status.value = order.status
    } else {
      loadFailed.value = true
    }
  } catch (err) {
    console.error('加载订单失败', err)
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

onLoad((options: any) => {
  orderId.value = Number(options?.orderId || 0)
})

onShow(() => {
  loadOrder()
})

// ===== 事件处理 =====
// 查看订单
const handleGoOrders = () => {
  uni.switchTab({ url: '/pages/order/order' })
}

// 回菜单
const handleBackMenu = () => {
  uni.switchTab({ url: '/pages/index/index' })
}
</script>

<style lang="scss" scoped>
$stall-red: #c23a22;
$stall-dark: #3d2b1f;
$stall-gold: #f5a623;
$stall-bg: #fdf8f2;
$stall-card: #ffffff;
$text-main: #3d2b1f;
$text-sub: #8a7a6d;

page {
  background: $stall-bg;
}

.container {
  padding: 48rpx 32rpx;
}

.success-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 40rpx;

  .success-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 120rpx;
    height: 120rpx;
    background: $stall-red;
    border-radius: 50%;
    margin-bottom: 24rpx;

    .success-check {
      font-size: 64rpx;
      font-weight: 700;
      color: #ffffff;
    }
  }

  .success-title {
    font-size: 40rpx;
    font-weight: 700;
    color: $text-main;
    margin-bottom: 12rpx;
  }

  .success-tip {
    font-size: 26rpx;
    color: $text-sub;
  }
}

.pickup-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  background: $stall-card;
  border-radius: 24rpx;
  padding: 48rpx 32rpx;
  box-shadow: 0 8rpx 24rpx rgba(61, 43, 31, 0.08);

  .pickup-label {
    font-size: 28rpx;
    color: $text-sub;
    margin-bottom: 16rpx;
  }

  .pickup-code {
    font-size: 128rpx;
    font-weight: 800;
    color: $stall-red;
    letter-spacing: 8rpx;
    line-height: 1.2;
  }

  .pickup-retry-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 32rpx 0;
    padding: 20rpx 56rpx;
    background: rgba(194, 58, 34, 0.08);
    border: 2rpx solid rgba(194, 58, 34, 0.35);
    border-radius: 44rpx;

    .pickup-retry-icon {
      font-size: 32rpx;
      color: $stall-red;
      margin-right: 8rpx;
    }

    .pickup-retry-text {
      font-size: 30rpx;
      font-weight: 600;
      color: $stall-red;
    }
  }

  .pickup-failed-tip {
    font-size: 24rpx;
    color: $text-sub;
    text-align: center;
    line-height: 1.6;
  }

  &.pickup-card-failed {
    padding: 64rpx 32rpx;
  }

  .pickup-amount-box {
    display: flex;
    align-items: baseline;
    margin-top: 24rpx;

    .pickup-amount-label {
      font-size: 26rpx;
      color: $text-sub;
      margin-right: 12rpx;
    }

    .pickup-amount {
      font-size: 40rpx;
      font-weight: 700;
      color: $text-main;
    }
  }

  .pickup-status-box {
    margin-top: 20rpx;
    padding: 8rpx 24rpx;
    background: rgba(245, 166, 35, 0.15);
    border-radius: 24rpx;

    .pickup-status-tip {
      font-size: 24rpx;
      color: $stall-dark;
    }
  }
}

.action-box {
  display: flex;
  margin-top: 48rpx;

  .action-btn {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    height: 92rpx;
    border-radius: 46rpx;
    font-size: 30rpx;
    font-weight: 600;

    &.action-btn-primary {
      background: $stall-red;
      color: #ffffff;
      margin-right: 24rpx;
    }

    &.action-btn-plain {
      background: $stall-card;
      color: $text-main;
      border: 2rpx solid #e8dfd4;
    }
  }
}
</style>
