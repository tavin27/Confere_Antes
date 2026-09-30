"""
Lado da aplicação do código de verificação.

O banco exige hash HMAC-SHA256 em hex e faz a verificação de forma atômica
(verify_transaction_code). Este módulo gera o código, calcula o HMAC e chama o banco.

Variável de ambiente obrigatória (NUNCA guarde no banco nem no repositório):
    VERIFICATION_PEPPER  -> segredo aleatório com 32+ caracteres
    Gerar um: python -c "import secrets; print(secrets.token_urlsafe(48))"

Exemplo:
    user_id = authenticated_user_id  # obtido da sessão autenticada no backend
    tx_id, codigo = emitir_codigo(conn, user_id=user_id, type_transaction="cancelamento_boleto")
    # envie `codigo` ao usuário pelo canal escolhido
    ok = verificar_codigo(conn, user_id=user_id, tx_id=tx_id, codigo=codigo_digitado)
"""
import hashlib
import hmac
import os
import secrets
import uuid


def _pepper() -> bytes:
    pepper = os.environ.get("VERIFICATION_PEPPER", "")
    if len(pepper) < 32:
        raise RuntimeError("Defina VERIFICATION_PEPPER com pelo menos 32 caracteres.")
    return pepper.encode()


def gerar_codigo(digitos: int = 6) -> str:
    """Código numérico com gerador criptograficamente seguro."""
    return f"{secrets.randbelow(10 ** digitos):0{digitos}d}"


def hash_codigo(tx_id: uuid.UUID, codigo: str) -> str:
    """HMAC-SHA256 em hex (64 chars). Amarrado ao id da transação, então o
    mesmo código gera hashes diferentes em transações diferentes."""
    msg = f"{tx_id}:{codigo}".encode()
    return hmac.new(_pepper(), msg, hashlib.sha256).hexdigest()


def emitir_codigo(conn, user_id: int, type_transaction: str, ttl_minutos: int = 10, **campos):
    """Cria a transação já com o código. Retorna (tx_id, codigo_em_claro).
    `campos` aceita: amount, description_transaction, description_client, channel."""
    permitidos = {"amount", "description_transaction", "description_client", "channel"}
    extras = {k: v for k, v in campos.items() if k in permitidos}

    tx_id = uuid.uuid4()
    codigo = gerar_codigo()
    colunas = ["id", "user_id", "type_transaction", "verification_code_hash", *extras]
    valores = [str(tx_id), user_id, type_transaction, hash_codigo(tx_id, codigo), *extras.values()]

    sql = (
        f"INSERT INTO transactions ({', '.join(colunas)}, verification_code_expires_at) "
        f"VALUES ({', '.join(['%s'] * len(colunas))}, now() + %s * INTERVAL '1 minute')"
    )
    with conn, conn.cursor() as cur:
        cur.execute(sql, [*valores, ttl_minutos])
    return tx_id, codigo


def verificar_codigo(conn, user_id: int, tx_id, codigo: str) -> bool:
    """Confere o código. Contagem de tentativas, bloqueio após 5 erros e expiração
    são feitos pelo banco, com lock de linha. `user_id` deve vir da identidade
    autenticada no backend, nunca de um campo enviado pelo cliente."""
    tx_id = uuid.UUID(str(tx_id))
    with conn, conn.cursor() as cur:
        cur.execute(
            "SELECT verify_transaction_code(%s, %s, %s)",
            (str(tx_id), user_id, hash_codigo(tx_id, codigo)),
        )
        return bool(cur.fetchone()[0])