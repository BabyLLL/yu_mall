import { readAuthSession } from './auth-session'

/**
 * 按会话类型控制 tabBar 可见性：摊主（管理员）会话隐藏底栏，会员/游客显示三栏。
 * 各 tab 页与摊主页 onShow 时幂等调用；调用失败静默（不影响页面功能）。
 */
export const applyTabBarVisibility = () => {
  const isAdmin = readAuthSession()?.type === 'admin'
  try {
    if (isAdmin) {
      uni.hideTabBar({ fail: () => {} })
    } else {
      uni.showTabBar({ fail: () => {} })
    }
  } catch {
    // 无 tabBar 的平台或调用失败时静默
  }
}
