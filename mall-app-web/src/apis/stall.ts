import { http } from '@/utils/http'
import type { StallOrder, StallOrderParam, StallMenuItem } from '@/types/stall'
import type { CommonPage } from '@/types/common'

/** 今日菜单（白名单，无需登录） */
export const getStallMenuAPI = () => {
  return http<StallMenuItem[]>({
    method: 'GET',
    url: '/stall/menu/list',
  })
}

/** 摊位下单（仅商品+数量+备注） */
export const createStallOrderAPI = (data: StallOrderParam) => {
  return http<number>({
    method: 'POST',
    url: '/stall/order/create',
    data,
  })
}

/** 分页获取当前会员的摊位订单（含取货码） */
export const getStallOrderListAPI = (params: { status?: number; pageNum?: number; pageSize?: number }) => {
  return http<CommonPage<StallOrder>>({
    method: 'GET',
    url: '/stall/order/list',
    params: {
      status: params.status ?? -1,
      pageNum: params.pageNum ?? 1,
      pageSize: params.pageSize ?? 10,
    },
  })
}

/** 会员取消订单（仅已下单状态可取消） */
export const cancelStallOrderAPI = (orderId: number) => {
  return http<void>({
    method: 'POST',
    url: `/stall/order/cancel/${orderId}`,
  })
}

// ===== 摊主模式接口 =====

/** 商品列表（含已隐藏商品） */
export const getVendorProductsAPI = () => {
  return http<StallMenuItem[]>({
    method: 'GET',
    url: '/stall/vendor/products',
  })
}

/** 隐藏商品 */
export const hideVendorProductAPI = (productId: number) => {
  return http<void>({
    method: 'POST',
    url: `/stall/vendor/product/${productId}/hide`,
  })
}

/** 上架商品 */
export const showVendorProductAPI = (productId: number) => {
  return http<void>({
    method: 'POST',
    url: `/stall/vendor/product/${productId}/show`,
  })
}

/** 摊主订单队列（按状态过滤） */
export const getVendorOrdersAPI = (params: { status?: number; pageNum?: number; pageSize?: number }) => {
  return http<CommonPage<StallOrder>>({
    method: 'GET',
    url: '/stall/vendor/orders',
    params: {
      status: params.status ?? -1,
      pageNum: params.pageNum ?? 1,
      pageSize: params.pageSize ?? 50,
    },
  })
}

/** 确认收款：已下单->已付款 */
export const confirmPayAPI = (orderId: number) => {
  return http<void>({
    method: 'POST',
    url: `/stall/vendor/order/${orderId}/confirmPay`,
  })
}

/** 开始制作：已付款->制作中 */
export const startMakingAPI = (orderId: number) => {
  return http<void>({
    method: 'POST',
    url: `/stall/vendor/order/${orderId}/startMaking`,
  })
}

/** 出餐完成：制作中->待取货 */
export const finishMakingAPI = (orderId: number) => {
  return http<void>({
    method: 'POST',
    url: `/stall/vendor/order/${orderId}/finishMaking`,
  })
}

/** 取货完成：待取货->已完成 */
export const finishOrderAPI = (orderId: number) => {
  return http<void>({
    method: 'POST',
    url: `/stall/vendor/order/${orderId}/finish`,
  })
}

/** 摊主取消订单 */
export const cancelVendorOrderAPI = (orderId: number) => {
  return http<void>({
    method: 'POST',
    url: `/stall/vendor/order/${orderId}/cancel`,
  })
}
