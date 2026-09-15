import { createContext, useContext, useState, useCallback } from 'react';
import api from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem('smartstock_user');
    return stored ? JSON.parse(stored) : null;
  });

  const login = useCallback(async (username, senha) => {
    const { data } = await api.post('/auth/login', { username, senha });
    localStorage.setItem('smartstock_token', data.token);
    const userData = { username: data.username, nome: data.nome, role: data.role };
    localStorage.setItem('smartstock_user', JSON.stringify(userData));
    setUser(userData);
    return userData;
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('smartstock_token');
    localStorage.removeItem('smartstock_user');
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ user, login, logout, isAuthenticated: !!user }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth deve ser usado dentro de um AuthProvider');
  }
  return ctx;
}
