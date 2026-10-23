<template>
  <view class="container">
    <!-- 摊位头部 -->
    <view class="stall-header">
      <view class="stall-title-box">
        <text class="stall-name">老王烤肠</text>
        <text class="stall-sub">现烤现卖 · 到摊自取</text>
      </view>
    </view>

    <!-- 今日菜单 -->
    <view class="menu-section">
      <view class="menu-grid">
        <view
          v-for="item in menuList"
          :key="item.id"
          class="menu-card"
          :class="{ 'sold-out': item.stock <= 0 }"
        >
          <view class="menu-pic-wrapper">
            <image class="menu-pic" :src="item.pic" mode="aspectFill" />
            <view v-if="item.stock <= 0" class="sold-out-mask">
              <text class="sold-out-text">已售罄</text>
            </view>
          </view>
          <view class="menu-info">
            <text class="menu-name clamp">{{ item.name }}</text>
            <view class="menu-bottom">
              <view class="menu-price-box">
                <text class="price-symbol">¥</text>
                <text class="menu-price">{{ item.price }}</text>
              </view>
              <view class="add-btn" @click.stop="handleAdd(item)">
                <text class="add-icon">+</text>
              </view>
            </view>
          </view>
        </view>
      </view>
      <uni-load-more v-if="loading" status="loading"></uni-load-more>
      <view v-if="!loading && menuList.length === 0" class="menu-empty">
        <text class="menu-empty-text">今日暂未开摊，稍后再来~</text>
      </view>
    </view>

    <!-- 底部常驻已选条 -->
    <view v-if="cartStore.totalCount > 0" class="cart-bar">
      <view class="cart-bar-left" @click="handleToggleCart">
        <view class="cart-badge">{{ cartStore.totalCount }}</view>
        <view class="cart-bar-info">
          <text class="cart-bar-total">¥{{ cartStore.totalAmount.toFixed(2) }}</text>
          <text class="cart-bar-tip">已选 {{ cartStore.totalCount }} 件</text>
        </view>
      </view>
      <view class="cart-bar-btn" @click="handleGoConfirm">
        <text>去下单</text>
      </view>
    </view>

    <!-- 已选清单弹层 -->
    <view v-if="showCart" class="cart-popup" @touchmove.stop.prevent @click="handleToggleCart">
      <view class="cart-popup-mask"></view>
      <view class="cart-popup-layer" @click.stop>
        <view class="cart-popup-header">
          <text class="cart-popup-title">已选商品</text>
          <text class="cart-popup-clear" @click="handleClearCart">清空</text>
        </view>
        <scroll-view class="cart-popup-list" scroll-y>
          <view v-for="entry in cartStore.items" :key="entry.product.id" class="cart-popup-item">
            <text class="cart-popup-name clamp">{{ entry.product.name }}</text>
            <view class="cart-popup-right">
              <text class="cart-popup-price">¥{{ (entry.product.price * entry.quantity).toFixed(2) }}</text>
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
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { getStallMenuAPI } from '@/apis/stall'
import { useStallCartStore } from '@/stores/stallCart'
import type { StallMenuItem } from '@/types/stall'

const cartStore = useStallCartStore()

// ===== 页面数据 =====
// 菜单列表
const menuList = ref<StallMenuItem[]>([])
// 加载状态
const loading = ref(true)
// 已选弹层显示
const showCart = ref(false)

// ===== 数据加载 =====
// 加载今日菜单
const loadMenu = async () => {
  loading.value = true
  try {
    const res = await getStallMenuAPI()
    menuList.value = res.data || []
  } catch (err) {
    console.error('加载菜单失败', err)
  } finally {
    loading.value = false
    uni.stopPullDownRefresh()
  }
}

// 页面显示时刷新菜单（摊主可能刚改了菜单）
onShow(() => {
  loadMenu()
})

// 下拉刷新
onPullDownRefresh(() => {
  loadMenu()
})

// ===== 事件处理 =====
// 加单
const handleAdd = (item: StallMenuItem) => {
  if (item.stock <= 0) {
    uni.showToast({ icon: 'none', title: '该商品已售罄' })
    return
  }
  cartStore.add(item)
}

// 减少数量
const handleDecrease = (productId: number) => {
  cartStore.decrease(productId)
}

// 清空已选
const handleClearCart = () => {
  cartStore.clear()
}

// 切换已选弹层
const handleToggleCart = () => {
  showCart.value = !showCart.value
}

// 去下单
const handleGoConfirm = () => {
  showCart.value = false
  uni.navigateTo({
    url: '/pages/stall/confirm-order',
  })
}
</script>

<style lang="scss" scoped>
/* 摊位配色：烤肠红棕 + 炭火暖金 */
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
  padding-bottom: 140rpx;
}

/* 摊位头部 */
.stall-header {
  padding: 48rpx 32rpx 24rpx;
  background: linear-gradient(160deg, $stall-red 0%, #a52a15 100%);
  border-radius: 0 0 40rpx 40rpx;

  .stall-title-box {
    display: flex;
    flex-direction: column;
  }

  .stall-name {
    font-size: 52rpx;
    font-weight: 700;
    color: #ffffff;
    letter-spacing: 4rpx;
  }

  .stall-sub {
    margin-top: 10rpx;
    font-size: 26rpx;
    color: rgba(255, 255, 255, 0.85);
  }
}

/* 菜单区 */
.menu-section {
  padding: 24rpx 24rpx 0;
}

.menu-grid {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
}

.menu-card {
  width: 338rpx;
  margin-bottom: 24rpx;
  background: $stall-card;
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 16rpx rgba(61, 43, 31, 0.06);

  &.sold-out {
    opacity: 0.72;
  }
}

.menu-pic-wrapper {
  position: relative;
  width: 100%;
  height: 254rpx;

  .menu-pic {
    width: 100%;
    height: 100%;
  }

  .sold-out-mask {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(61, 43, 31, 0.45);

    .sold-out-text {
      padding: 8rpx 24rpx;
      font-size: 26rpx;
      color: #ffffff;
      border: 2rpx solid #ffffff;
      border-radius: 8rpx;
    }
  }
}

.menu-info {
  padding: 16rpx 20rpx 20rpx;
}

.menu-name {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: $text-main;
  line-height: 42rpx;
  min-height: 84rpx;
}

.menu-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8rpx;
}

.menu-price-box {
  display: flex;
  align-items: baseline;
  color: $stall-red;

  .price-symbol {
    font-size: 24rpx;
    font-weight: 600;
  }

  .menu-price {
    font-size: 38rpx;
    font-weight: 700;
  }
}

.add-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56rpx;
  height: 56rpx;
  background: $stall-red;
  border-radius: 50%;

  .add-icon {
    font-size: 40rpx;
    font-weight: 700;
    color: #ffffff;
    line-height: 40rpx;
  }
}

.menu-empty {
  display: flex;
  justify-content: center;
  padding: 120rpx 0;

  .menu-empty-text {
    font-size: 28rpx;
    color: $text-sub;
  }
}

/* 底部常驻已选条 */
.cart-bar {
  position: fixed;
  left: 24rpx;
  right: 24rpx;
  bottom: 24rpx;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 104rpx;
  padding: 0 12rpx 0 32rpx;
  background: $stall-dark;
  border-radius: 52rpx;
  box-shadow: 0 8rpx 24rpx rgba(61, 43, 31, 0.35);

  .cart-bar-left {
    position: relative;
    display: flex;
    align-items: center;
    flex: 1;
  }

  .cart-badge {
    position: absolute;
    top: -34rpx;
    left: -6rpx;
    min-width: 36rpx;
    height: 36rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0 10rpx;
    background: $stall-gold;
    border-radius: 18rpx;
    font-size: 22rpx;
    font-weight: 700;
    color: $stall-dark;
  }

  .cart-bar-info {
    display: flex;
    flex-direction: column;
  }

  .cart-bar-total {
    font-size: 34rpx;
    font-weight: 700;
    color: #ffffff;
  }

  .cart-bar-tip {
    font-size: 22rpx;
    color: rgba(255, 255, 255, 0.6);
  }

  .cart-bar-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 200rpx;
    height: 80rpx;
    background: $stall-red;
    border-radius: 40rpx;
    font-size: 30rpx;
    font-weight: 600;
    color: #ffffff;
  }
}

/* 已选清单弹层 */
.cart-popup {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 200;

  .cart-popup-mask {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.4);
  }

  .cart-popup-layer {
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    background: $stall-card;
    border-radius: 32rpx 32rpx 0 0;
    padding-bottom: 180rpx;
  }

  .cart-popup-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 32rpx 32rpx 20rpx;
    border-bottom: 1rpx solid #f0e9e1;

    .cart-popup-title {
      font-size: 32rpx;
      font-weight: 600;
      color: $text-main;
    }

    .cart-popup-clear {
      font-size: 26rpx;
      color: $text-sub;
    }
  }

  .cart-popup-list {
    max-height: 560rpx;
    padding: 0 32rpx;
  }

  .cart-popup-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 24rpx 0;
    border-bottom: 1rpx solid #f7f2ec;

    .cart-popup-name {
      flex: 1;
      font-size: 28rpx;
      color: $text-main;
    }

    .cart-popup-right {
      display: flex;
      align-items: center;
    }

    .cart-popup-price {
      margin-right: 24rpx;
      font-size: 28rpx;
      font-weight: 600;
      color: $stall-red;
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
}

.clamp {
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
</style>
