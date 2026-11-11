import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as api from '../api/auth'
import { setSession } from '../api/http'
import type { User } from '../types/api'

export const useAuth = defineStore('auth', () => {
  const token = ref<string | null>(sessionStorage.getItem('inventory-token'))
  const user = ref<User | null>(null)
  setSession(token.value)
  const loggedIn = computed(() => token.value !== null)
  function can(permission: string) {
    return user.value?.permissions.includes(permission) ?? false
  }
  async function signIn(username: string, password: string) {
    const result = await api.login(username, password)
    token.value = result.token
    user.value = result.user
    sessionStorage.setItem('inventory-token', result.token)
    setSession(result.token)
  }
  async function load() {
    user.value = await api.me()
  }
  function clear() {
    token.value = null
    user.value = null
    sessionStorage.removeItem('inventory-token')
    setSession(null)
  }
  async function signOut() {
    await api.logout()
    clear()
  }
  return { token, user, loggedIn, can, signIn, load, clear, signOut }
})
