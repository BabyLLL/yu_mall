<template>
  <view class="container">
    <!-- 统计卡 -->
    <view class="stats-row">
      <view class="stats-card">
        <text class="stats-num">¥{{ formatAmount(stats?.totalAmount) }}</text>
        <text class="stats-label">今日实收</text>
      </view>
      <view class="stats-card">
        <text class="stats-num">{{ stats?.orderCount ?? 0 }}</text>
        <text class="stats-label">今日订单</text>
      </view>
    </view>

    <!-- 商品明细 -->
    <view class="detail-card">
      <view class="detail-header">
        <text class="detail-title">卖了什么</text>
      </view>
      <view v-for="item in stats?.items || []" :key="item.productName" class="detail-row">
        <text class="detail-name clamp">{{ item.productName }}</text>
        <text class="detail-qty">x {{ item.quantity }}</text>
        <text class="detail-amount">¥{{ formatAmount(item.amount) }}</text>
      </view>
      <view v-if="!stats || stats.items.length === 0" class="empty-box">
        <text class="empty-text">今天还没有卖出，开摊吧~</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { getVendorTodayStatsAPI } from '@/apis/stall'
import { applyTabBarVisibility } from '@/utils/tab-bar-visibility'
import type { VendorTodayStats } from '@/types/stall'

const stats = ref<VendorTodayStats | null>(null)

// 加载今日战绩；会话失效（401/403）时返回摊主页
const loadStats = async () => {
  try {
    const res = await getVendorTodayStatsAPI()
    stats.value = res.data || null
  } catch (err: unknown) {
    const code = (err as { data?: { code?: number } })?.data?.code
    if (code === 401 || code === 403) {
      uni.navigateBack()
      return
    }
    console.error('加载今日战绩失败', err)
  } finally {
    uni.stopPullDownRefresh()
  }
}

onShow(() => {
  applyTabBarVisibility()
  loadStats()
})

onPullDownRefresh(() => {
  loadStats()
})

// 金额统一两位小数
const formatAmount = (value?: number) => {
  return Number(value ?? 0).toFixed(2)
}
</script>

<style lang="scss" scoped>
$stall-red: #c23a22;
$stall-dark: #3d2b1f;
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

.stats-row {
  display: flex;
  gap: 24rpx;
  margin-bottom: 24rpx;
}

.stats-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40rpx 0 32rpx;
  background: linear-gradient(160deg, $stall-dark 0%, #241811 100%);
  border-radius: 20rpx;

  .stats-num {
    font-size: 44rpx;
    font-weight: 700;
    color: #ffffff;
  }

  .stats-label {
    margin-top: 12rpx;
    font-size: 24rpx;
    color: rgba(255, 255, 255, 0.6);
  }
}

.detail-card {
  background: $stall-card;
  border-radius: 20rpx;
  padding: 12rpx 28rpx;
}

.detail-header {
  padding: 20rpx 0 8rpx;
  border-bottom: 1rpx solid #f7f2ec;

  .detail-title {
    font-size: 30rpx;
    font-weight: 600;
    color: $text-main;
  }
}

.detail-row {
  display: flex;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f7f2ec;

  &:last-child {
    border-bottom: none;
  }

  .detail-name {
    flex: 1;
    font-size: 28rpx;
    color: $text-main;
  }

  .detail-qty {
    margin: 0 24rpx;
    font-size: 26rpx;
    color: $text-sub;
  }

  .detail-amount {
    min-width: 120rpx;
    text-align: right;
    font-size: 28rpx;
    font-weight: 600;
    color: $stall-red;
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

.clamp {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
