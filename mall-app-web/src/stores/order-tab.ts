import { defineStore } from 'pinia'
import { ref } from 'vue'

// 定义订单Tab Store（switchTab 无法携带 query，用于跨页传递"我的订单"目标 Tab）
export const useOrderTabStore = defineStore('orderTab', () => {
  // 待生效的目标 Tab 索引（null 表示无）
  const pendingTabIndex = ref<number | null>(null)

  // 记录目标 Tab 索引
  const setPendingTab = (index: number) => {
    pendingTabIndex.value = index
  }

  // 取走目标 Tab 索引（取出后即清空）
  const takePendingTab = (): number | null => {
    const index = pendingTabIndex.value
    pendingTabIndex.value = null
    return index
  }

  return {
    pendingTabIndex,
    setPendingTab,
    takePendingTab,
  }
})
