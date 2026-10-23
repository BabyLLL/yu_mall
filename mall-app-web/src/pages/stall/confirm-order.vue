<template>
  <view class="container">
    <!-- 已选清单 -->
    <view class="section">
      <view class="section-title">已选商品</view>
      <view v-for="entry in cartStore.items" :key="entry.product.id" class="order-item">
        <image class="item-pic" :src="entry.product.pic" mode="aspectFill" />
        <view class="item-info">
          <text class="item-name clamp">{{ entry.product.name }}</text>
          <text class="item-price">¥{{ entry.product.price }}</text>
        </view>
        <view class="quantity-ctrl">
          <view class="qty-btn" @click="handleDecrease(entry.product.id)">
            <text>-</text>
          </view>
          <text class="qty-num">{{ entry.quantity }}</text>
          <view class="qty-btn qty-btn-add" @click="handleAdd(entry.product)">
            <text>+</text>
          </view>
        </view>
      </view>
      <view v-if="cartStore.items.length === 0" class="empty-box">
        <text class="empty-text">还没有选商品，回菜单挑一串吧</text>
      </view>
    </view>

    <!-- 备注 -->
    <view class="section">
      <view class="section-title">备注</view>
      <input
        v-model="note"
        class="note-input"
        type="text"
        placeholder="口味要求，如：多辣 / 不要孜然"
        placeholder-class="note-placeholder"
      />
    </view>

    <!-- 合计 -->
    <view class="total-bar">
      <view class="total-info">
        <text class="total-label">合计</text>
        <text class="total-amount">¥{{ cartStore.totalAmount.toFixed(2) }}</text>
      </view>
      <view class="submit-btn" :class="{ disabled: !canSubmit }" @click="handleSubmit">
        <text>{{ submitText }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useStallCartStore } from '@/stores/stallCart'
import { useMemberStore } from '@/stores/member'
import { createStallOrderAPI } from '@/apis/stall'
import type { StallMenuItem } from '@/types/stall'

const cartStore = useStallCartStore()
const memberStore = useMemberStore()

// ===== 页面数据 =====
// 订单备注
const note = ref('')
// 提交中
const submitting = ref(false)

// ===== 计算属性 =====
// 可提交
const canSubmit = computed(() => cartStore.items.length > 0 && !submitting.value)
// 按钮文案
const submitText = computed(() => {
  if (submitting.value) return '提交中...'
  return '提交订单'
})

// ===== 事件处理 =====
// 加数量
const handleAdd = (product: StallMenuItem) => {
  cartStore.add(product)
}

// 减数量
const handleDecrease = (productId: number) => {
  cartStore.decrease(productId)
}

// 提交订单
const handleSubmit = async () => {
  if (cartStore.items.length === 0) {
    uni.showToast({ icon: 'none', title: '请先选择商品' })
    return
  }
  if (submitting.value) return
  //未登录先引导微信登录，登录后继续（已选清单持久化在 store 中不丢失）
  if (!memberStore.hasLogin) {
    uni.navigateTo({
      url: '/pages/public/login?redirect=' + encodeURIComponent('/pages/stall/confirm-order'),
    })
    return
  }
  submitting.value = true
  try {
    const res = await createStallOrderAPI({
      items: cartStore.items.map((entry) => ({
        productId: entry.product.id,
        quantity: entry.quantity,
      })),
      note: note.value || undefined,
    })
    const orderId = res.data
    cartStore.clear()
    uni.redirectTo({
      url: `/pages/stall/order-success?orderId=${orderId}`,
    })
  } catch (err: any) {
    uni.showToast({
      icon: 'none',
      title: err?.message || '下单失败，请重试',
    })
  } finally {
    submitting.value = false
  }
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
  padding: 24rpx 24rpx 180rpx;
}

.section {
  background: $stall-card;
  border-radius: 20rpx;
  padding: 28rpx 32rpx;
  margin-bottom: 24rpx;

  .section-title {
    font-size: 30rpx;
    font-weight: 600;
    color: $text-main;
    margin-bottom: 20rpx;
  }
}

.order-item {
  display: flex;
  align-items: center;
  padding: 16rpx 0;

  .item-pic {
    width: 96rpx;
    height: 96rpx;
    border-radius: 12rpx;
    margin-right: 20rpx;
  }

  .item-info {
    flex: 1;
    display: flex;
    flex-direction: column;

    .item-name {
      font-size: 28rpx;
      color: $text-main;
      margin-bottom: 8rpx;
    }

    .item-price {
      font-size: 28rpx;
      font-weight: 600;
      color: $stall-red;
    }
  }

  .quantity-ctrl {
    display: flex;
    align-items: center;

    .qty-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 48rpx;
      height: 48rpx;
      border: 2rpx solid $stall-red;
      border-radius: 50%;
      font-size: 30rpx;
      color: $stall-red;

      &.qty-btn-add {
        background: $stall-red;
        color: #ffffff;
      }
    }

    .qty-num {
      min-width: 60rpx;
      text-align: center;
      font-size: 28rpx;
      font-weight: 600;
      color: $text-main;
    }
  }
}

.empty-box {
  display: flex;
  justify-content: center;
  padding: 60rpx 0;

  .empty-text {
    font-size: 26rpx;
    color: $text-sub;
  }
}

.note-input {
  height: 88rpx;
  padding: 0 24rpx;
  background: #f7f2ec;
  border-radius: 12rpx;
  font-size: 28rpx;
  color: $text-main;
}

.note-placeholder {
  color: $text-sub;
  font-size: 26rpx;
}

.total-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 32rpx calc(20rpx + env(safe-area-inset-bottom));
  background: $stall-card;
  box-shadow: 0 -4rpx 16rpx rgba(61, 43, 31, 0.08);

  .total-info {
    display: flex;
    align-items: baseline;

    .total-label {
      font-size: 26rpx;
      color: $text-sub;
      margin-right: 12rpx;
    }

    .total-amount {
      font-size: 44rpx;
      font-weight: 700;
      color: $stall-red;
    }
  }

  .submit-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 280rpx;
    height: 88rpx;
    background: $stall-red;
    border-radius: 44rpx;
    font-size: 32rpx;
    font-weight: 600;
    color: #ffffff;

    &.disabled {
      opacity: 0.5;
    }
  }
}

.clamp {
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
}
</style>
