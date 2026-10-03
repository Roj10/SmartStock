export default function Modal({ title, onClose, children, footer, largo = false }) {
  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className={largo ? 'modal modal-largo' : 'modal'} onClick={(e) => e.stopPropagation()}>
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
