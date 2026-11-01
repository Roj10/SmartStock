/*
 * Em palavras simples: Ponto de partida do frontend (as telas). O navegador abre o index.html, que carrega este arquivo.
 * Ele "monta" o aplicativo dentro da <div id="root"> e liga três coisas em volta dele:
 * - BrowserRouter: permite ter endereços como /progresso e /usuarios sem recarregar a página;
 * - AuthProvider: guarda quem está logado e as abas que pode ver (context/AuthContext.jsx);
 * - App: a lista de telas (App.jsx).
 */
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import './index.css'
import App from './App.jsx'
import { AuthProvider } from './context/AuthContext.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <App />
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
)
