// O pop-up só fecha pelo "×" ou pelos botões do próprio formulário. Fechar ao clicar fora causava
// fechamentos acidentais (selecionar texto, abrir listas de seleção, arrastar o mouse até a borda).
export default function Modal({ title, onClose, children, footer, largo = false }) {
  return (
    <div className="modal-overlay">
      <div className={largo ? 'modal modal-largo' : 'modal'} role="dialog" aria-modal="true" aria-label={title}>
        <div className="modal-header">
          <h3>{title}</h3>
          <button className="btn-icon" onClick={onClose} aria-label="Fechar">
            &times;
          </button>
        </div>
        <div className="modal-body">{children}</div>
        {footer && <div className="modal-footer">{footer}</div>}
      </div>
    </div>
  );
}
