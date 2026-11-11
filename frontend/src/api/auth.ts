import { request } from './http'
import type { User } from '../types/api'
export const login = (username: string, password: string) =>
  request<{ token: string; user: User }>('POST', '/auth/login', { username, password })
export const me = () => request<User>('GET', '/auth/me')
export const logout = () => request<void>('POST', '/auth/logout')
export const uploadAvatar = (file: File) => {
  const form = new FormData()
  form.append('file', file)
  return request<User>('POST', '/profile/avatar', form)
}
export const removeAvatar = () => request<User>('DELETE', '/profile/avatar')
export const updateProfile = (data: { realName: string; phone: string | null }) =>
  request<User>('PATCH', '/profile', data)
export const changePassword = (data: {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}) => request<void>('POST', '/profile/password', data)
