<template>
  <view class="container">
    <view class="section-box">
      <view v-for="item in productList" :key="item.id" class="product-row">
        <image class="product-pic" :src="item.pic" mode="aspectFill" />
        <view class="product-info">
          <text class="product-name clamp">{{ item.name }}</text>
          <view class="product-meta">
            <text class="product-price">¥{{ item.price }}</text>
            <text v-if="item.stock > 0" class="product-stock">剩余 {{ item.stock }}</text>
            <text v-else class="product-stock sold-out">售罄</text>
          </view>
        </view>
        <switch
          :checked="item.publishStatus === 1"
          color="#C23A22"
          @change="(e: any) => handleTogglePublish(item, e)"
        />
      </view>
      <view v-if="productList.length === 0" class="empty-box">
        <text class="empty-text">暂无商品，请到管理后台添加</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import {
  getVendorProductsAPI,
  hideVendorProductAPI,
  showVendorProductAPI,
} from '@/apis/stall'
import type { StallMenuItem } from '@/types/stall'

// ===== 页面数据 =====
// 商品列表
const productList = ref<StallMenuItem[]>([])

// ===== 加载 =====
// 加载商品列表
const loadProducts = async () => {
  try {
    const res = await getVendorProductsAPI()
    productList.value = (res.data || []) as StallMenuItem[]
  } catch (e) {
    console.error('加载商品失败', e)
  }
}

onShow(() => {
  loadProducts()
})

// ===== 事件处理 =====
// 商品上架/隐藏开关
const handleTogglePublish = async (item: StallMenuItem, e: any) => {
  const show = e.detail.value
  try {
    if (show) {
      await showVendorProductAPI(item.id)
      item.publishStatus = 1
    } else {
      await hideVendorProductAPI(item.id)
      item.publishStatus = 0
    }
    uni.showToast({ icon: 'success', title: show ? '已上架' : '已隐藏' })
  } catch (err: any) {
    // 失败回滚开关
    item.publishStatus = show ? 0 : 1
    uni.showToast({ icon: 'none', title: err?.message || '操作失败' })
  }
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

.product-row {
  display: flex;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f7f2ec;

  &:last-child {
    border-bottom: none;
  }

  .product-pic {
    width: 88rpx;
    height: 88rpx;
    border-radius: 12rpx;
    margin-right: 20rpx;
  }

  .product-info {
    flex: 1;
    display: flex;
    flex-direction: column;

    .product-name {
      font-size: 28rpx;
      color: $text-main;
      margin-bottom: 6rpx;
    }

    .product-meta {
      display: flex;
      align-items: center;
      gap: 16rpx;
    }

    .product-price {
      font-size: 26rpx;
      font-weight: 600;
      color: $stall-red;
    }

    .product-stock {
      font-size: 22rpx;
      color: $text-sub;

      &.sold-out {
        padding: 2rpx 14rpx;
        border: 2rpx solid $text-sub;
        border-radius: 999rpx;
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

.clamp {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
