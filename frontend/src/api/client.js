import axios from 'axios';

// A sessão fica em sessionStorage (e não localStorage) para ser própria de cada aba: assim duas contas
// diferentes (ex.: produção e financeiro) podem ficar logadas lado a lado, em abas separadas, sem uma
// derrubar ou se misturar com a outra.
const api = axios.create({
  baseURL: 'http://localhost:8080/api',
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
