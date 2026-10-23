/**
 * 摊位相关类型
 */

import type { PmsProduct } from './product'
import type { OmsOrderItem } from './order'

/** 今日菜单商品（复用商品模型，取需要的字段） */
export type StallMenuItem = Pick<
  PmsProduct,
  'id' | 'name' | 'pic' | 'price' | 'sale' | 'subTitle' | 'stock' | 'publishStatus'
>

/** 摊位下单入参商品项 */
export type StallOrderItemParam = {
  productId: number
  quantity: number
}

/** 摊位下单入参 */
export type StallOrderParam = {
  items: StallOrderItemParam[]
  note?: string
}

/** 摊位订单状态 */
export const STALL_ORDER_STATUS = {
  /** 已下单（待付款） */
  CREATED: 0,
  /** 已付款 */
  PAID: 1,
  /** 制作中 */
  MAKING: 2,
  /** 待取货 */
  READY: 3,
  /** 已完成 */
  FINISHED: 4,
  /** 已取消 */
  CANCELED: 5,
} as const

/** 摊位订单状态文案 */
export const STALL_ORDER_STATUS_TEXT: Record<number, string> = {
  0: '已下单',
  1: '已付款',
  2: '制作中',
  3: '待取货',
  4: '已完成',
  5: '已取消',
}

/** 摊位订单（含取货码） */
export type StallOrder = {
  id: number
  orderSn: string
  memberId: number
  memberUsername: string
  totalAmount: number
  payAmount: number
  status: number
  orderType: number
  note: string
  createTime: string
  paymentTime: string
  /** 取货码：N+按天三位流水 */
  pickupCode: string
  orderItemList: OmsOrderItem[]
}

/** 今日战绩统计（实收口径） */
export type VendorTodayStats = {
  /** 今日实收总额 */
  totalAmount: number
  /** 今日订单数 */
  orderCount: number
  /** 按商品聚合的销售明细（金额降序） */
  items: VendorTodayStatsItem[]
}

export type VendorTodayStatsItem = {
  productName: string
  quantity: number
  amount: number
}
