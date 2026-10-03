import { useEffect, useRef, useState } from 'react';

const GRUPOS = [
  {
    nome: 'Carinhas',
    icone: '😀',
    emojis: '😀 😃 😄 😁 😆 😅 😂 🤣 😊 🙂 🙃 😉 😍 🥰 😘 😎 🤩 🥳 🤔 🤨 😐 😑 🙄 😏 😬 😴 😮 😯 😲 😳 🥺 😢 😭 😤 😠 😡 🤯 😱 😰 😓 🤗 🤝 😇 🤠 🥱'.split(' '),
  },
  {
    nome: 'Gestos',
    icone: '👍',
    emojis: '👍 👎 👌 ✌️ 🤞 🤟 🤘 👏 🙌 👐 🙏 💪 👋 🤚 ✋ 👆 👇 👉 👈 ☝️ ✍️ 👀 🧠 ❤️ 🧡 💛 💚 💙 💜 🖤 💔 ✨ 🔥 💯 🎉 🎊'.split(' '),
  },
  {
    nome: 'Trabalho',
    icone: '🔧',
    emojis: '🔧 🛠️ ⚙️ 🔩 ⛏️ 🏭 📦 🚚 🚛 🏗️ 🧰 📏 📐 🔍 💡 🔋 ⚡ 📅 📆 ⏰ ⌛ 💰 💵 💳 🧾 📈 📉 📊 📝 📋 📌 📎 📞 📧 💬 🔔 🔒 🔑 🗂️ 📁'.split(' '),
  },
  {
    nome: 'Símbolos',
    icone: '✅',
    emojis: '✅ ❌ ⚠️ ❗ ❓ ➕ ➖ ➡️ ⬅️ ⬆️ ⬇️ 🔄 🆗 🆕 🔴 🟠 🟡 🟢 🔵 ⭐ 🌟 🏁 🚫 ⛔ ✔️ ✖️ ♻️ 💤 🎯 🚀 🏆 🥇'.split(' '),
  },
];

// Seletor simples de emojis (sem biblioteca externa). Fecha ao clicar fora ou com Esc.
export default function EmojiPicker({ onEscolher, onFechar }) {
  const [grupo, setGrupo] = useState(0);
  const caixaRef = useRef(null);

  useEffect(() => {
    function aoClicarFora(e) {
      // o botão que abre o seletor trata o próprio clique
      if (caixaRef.current && !caixaRef.current.contains(e.target) && !e.target.closest('[data-emoji-toggle]')) {
        onFechar();
      }
    }
    function aoTeclar(e) {
      if (e.key === 'Escape') onFechar();
    }
    document.addEventListener('mousedown', aoClicarFora);
    document.addEventListener('keydown', aoTeclar);
    return () => {
      document.removeEventListener('mousedown', aoClicarFora);
      document.removeEventListener('keydown', aoTeclar);
    };
  }, [onFechar]);

  return (
    <div className="emoji-picker" ref={caixaRef} role="dialog" aria-label="Escolher emoji">
      <div className="emoji-abas">
        {GRUPOS.map((g, i) => (
          <button
            key={g.nome}
            type="button"
            className={'emoji-aba' + (i === grupo ? ' ativa' : '')}
            title={g.nome}
            onClick={() => setGrupo(i)}
          >
            {g.icone}
          </button>
        ))}
      </div>
      <div className="emoji-grade">
        {GRUPOS[grupo].emojis.map((emoji) => (
          <button key={emoji} type="button" className="emoji-item" onClick={() => onEscolher(emoji)}>
            {emoji}
          </button>
        ))}
      </div>
    </div>
  );
}
