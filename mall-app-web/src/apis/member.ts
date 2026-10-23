import { http } from '@/utils/http'
import type { LoginResult, MemberInfo, RegisterParam } from '@/types/member'

/** 微信小程序会员登录 */
export const wechatLoginAPI = (data: {
  code: string
  clientId: string
  nickname?: string
  avatarUrl?: string
}) => {
  return http<LoginResult>({
    method: 'POST',
    url: '/sso/wechat/login',
    header: {
      'content-type': 'application/x-www-form-urlencoded;charset=utf-8',
    },
    data,
  })
}

/** 小程序管理员登录（仅隐藏入口调用） */
export const adminLoginAPI = (data: { username: string; password: string }, clientId: string) => {
  return http<LoginResult>({
    method: 'POST',
    url: '/sso/miniapp/admin/login',
    header: {
      'content-type': 'application/json',
      'X-Miniapp-Client-Id': clientId,
    },
    data,
  })
}

/** 注销当前小程序活动会话 */
export const miniappLogoutAPI = () => {
  return http<void>({
    method: 'POST',
    url: '/sso/miniapp/logout',
    suppressAuthRecovery: true,
  })
}

/** 获取用户信息 */
export const getMemberInfoAPI = () => {
  return http<MemberInfo>({
    method: 'GET',
    url: '/sso/info',
  })
}

/** 修改当前会员昵称 */
export const updateMemberInfoAPI = (data: { nickname: string }) => {
  return http<MemberInfo>({
    method: 'POST',
    url: '/sso/update',
    params: { nickname: data.nickname },
  })
}

/** 注册 */
export const registerAPI = (data: RegisterParam) => {
  return http({
    method: 'POST',
    url: '/sso/register',
    header: {
      'content-type': 'application/x-www-form-urlencoded;charset=utf-8',
    },
    data,
  })
}

/** 获取验证码 */
export const getAuthCodeAPI = (telephone: string) => {
  return http({
    method: 'GET',
    url: '/sso/getAuthCode',
    params: { telephone },
  })
}
