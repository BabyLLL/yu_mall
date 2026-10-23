import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { StallMenuItem } from '@/types/stall'

/**
 * 已选清单（摊位下单的"购物车"）：
 * 菜单页点"+"加入，底部常驻条汇总，确认下单页提交。
 */
export const useStallCartStore = defineStore(
  'stallCart',
  () => {
    /** 已选商品列表 */
    const items = ref<{ product: StallMenuItem; quantity: number }[]>([])

    /** 已选件数 */
    const totalCount = computed(() => items.value.reduce((sum, item) => sum + item.quantity, 0))

    /** 合计金额 */
    const totalAmount = computed(() =>
      items.value.reduce((sum, item) => sum + item.product.price * item.quantity, 0),
    )

    /** 加单：数量 +1（已存在则累加） */
    const add = (product: StallMenuItem, quantity = 1) => {
      const existing = items.value.find((item) => item.product.id === product.id)
      if (existing) {
        existing.quantity += quantity
      } else {
        items.value.push({ product, quantity })
      }
    }

    /** 减少数量：减到 0 移除 */
    const decrease = (productId: number) => {
      const existing = items.value.find((item) => item.product.id === productId)
      if (!existing) return
      existing.quantity -= 1
      if (existing.quantity <= 0) {
        remove(productId)
      }
    }

    /** 移除商品 */
    const remove = (productId: number) => {
      items.value = items.value.filter((item) => item.product.id !== productId)
    }

    /** 清空 */
    const clear = () => {
      items.value = []
    }

    return { items, totalCount, totalAmount, add, decrease, remove, clear }
  },
  {
    // 持久化配置（兼容多端）
    persist: {
      storage: {
        setItem(key, value) {
          uni.setStorageSync(key, value)
        },
        getItem(key) {
          return uni.getStorageSync(key)
        },
      },
    },
  },
)
