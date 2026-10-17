import { useCallback, useEffect, useState } from 'react'
import axios from '../api/axiosInstance'
import AuthContext from './contextValue'

const storedToken = () => localStorage.getItem('token') || sessionStorage.getItem('token')

export function AuthProvider({ children }) {
  const [token, setToken] = useState(storedToken)
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(Boolean(storedToken()))

  const refreshProfile = useCallback(async () => {
    if (!storedToken()) return null
    try {
      const { data } = await axios.get('/api/users/me')
      setUser(data)
      return data
    } finally { setLoading(false) }
  }, [])

  useEffect(() => {
    if (storedToken()) refreshProfile().catch(() => setUser(null))
    const expire = () => { localStorage.removeItem('token'); sessionStorage.removeItem('token'); setToken(null); setUser(null); setLoading(false) }
    window.addEventListener('spaceora:session-expired', expire)
    return () => window.removeEventListener('spaceora:session-expired', expire)
  }, [refreshProfile])

  const login = async (credentials, remember = true) => {
    const { data } = await axios.post('/api/auth/login', credentials)
    if (remember) localStorage.setItem('token', data.token)
    else sessionStorage.setItem('token', data.token)
    setToken(data.token)
    setLoading(true)
    await refreshProfile()
    return data
  }

  const register = async (profile) => (await axios.post('/api/auth/register', profile)).data
  const logout = () => { localStorage.removeItem('token'); sessionStorage.removeItem('token'); setToken(null); setUser(null) }

  const updateProfile = async (profile) => {
    const { data } = await axios.put('/api/users/me', profile)
    setUser(data)
    return data
  }

  return <AuthContext.Provider value={{ token, user, loading, login, logout, register, refreshProfile, updateProfile }}>{children}</AuthContext.Provider>
}
