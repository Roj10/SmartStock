-- Em palavras simples: troca o 2FA do aplicativo autenticador pelo código enviado por e-mail.
--
-- 1) Cada conta passa a ter um e-mail (é para lá que vai o código de cada login) e os campos do código
--    em andamento (impressão digital do código, validade, hora do envio e erros seguidos).
-- 2) Os campos do app autenticador (segredo, códigos de recuperação) deixam de existir.
-- Contas que já existiam ficam sem e-mail: no próximo login, cada pessoa informa o e-mail e confirma com
-- o código que chega nele. As senhas, permissões e todos os outros dados continuam iguais.

alter table usuarios add column if not exists email varchar(254);
alter table usuarios add column if not exists email_pendente varchar(254);
alter table usuarios add column if not exists codigo_email_hash varchar(100);
alter table usuarios add column if not exists codigo_email_expira timestamp(6);
alter table usuarios add column if not exists codigo_email_enviado timestamp(6);
alter table usuarios add column if not exists codigo_email_tentativas integer default 0 not null;

alter table usuarios drop column if exists dois_fatores_ativo;
alter table usuarios drop column if exists totp_segredo;
alter table usuarios drop column if exists totp_pendente;
alter table usuarios drop column if exists totp_ultimo_passo;
drop table if exists usuario_codigos_recuperacao;

-- quem já tinha entrado com o app sai de todas as sessões e passa pelo login novo
update usuarios set token_versao = token_versao + 1;
