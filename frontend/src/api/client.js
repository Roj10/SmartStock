import axios from 'axios';

// A sessão fica em sessionStorage (e não localStorage) para ser própria de cada aba: assim duas contas
// diferentes (ex.: produção e financeiro) podem ficar logadas lado a lado, em abas separadas, sem uma
// derrubar ou se misturar com a outra.
//
// Endereço da API: por padrão "/api", no MESMO endereço do site. Assim o mesmo build funciona em qualquer lugar:
//  - no computador (npm run dev), o Vite repassa /api para o backend em localhost:8080 (veja vite.config.js);
//  - no servidor (AWS EC2), o Nginx repassa /api para o backend (veja deploy/nginx-smartstock.conf).
// Se um dia o backend ficar em outro endereço, basta definir VITE_API_URL no arquivo frontend/.env antes do build.
export const API_URL = import.meta.env.VITE_API_URL || '/api';

const api = axios.create({
  baseURL: API_URL,
});

api.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('smartstock_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      sessionStorage.removeItem('smartstock_token');
      sessionStorage.removeItem('smartstock_user');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default api;
