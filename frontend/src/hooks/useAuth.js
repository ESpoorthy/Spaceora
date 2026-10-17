import { useContext } from 'react'
import AuthContext from '../context/contextValue'

export function useAuth() { return useContext(AuthContext) }
