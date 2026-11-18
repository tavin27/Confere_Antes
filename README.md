# Confere_Antes

## Processamento de ligações por IA

Ao registrar uma ligação em `POST /alerts/{alertId}/calls`, a API salva o registro e inicia o processamento em segundo plano. O áudio deve estar em uma URL HTTPS pública que permita download sem autenticação interativa, usando WAV, MP3, FLAC, M4A, OGG, WEBM ou AAC. O limite padrão é 25 MB.

O worker transcreve em português pela API Speech-to-Text do OpenRouter (modelo padrão `openai/whisper-1`) e classifica a transcrição com `inclusionai/ling-3.2-flash:free`. Ambos podem ser substituídos por configuração. O resultado fica disponível em `GET /calls/{callId}`; durante o processamento, o campo `status` avança até `WAITING_USER_DECISION` ou `FAILED`.

Configure `OPENROUTER_API_KEY` como variável secreta no Render. Opcionalmente, configure `OPENROUTER_TRANSCRIPTION_MODEL`, `OPENROUTER_ANALYSIS_MODEL` e `CALL_AUDIO_MAX_BYTES`. Nunca inclua a chave no repositório ou no aplicativo cliente.
Sistema anti-golpe com verificação em duas etapas e classificação de risco por IA — projeto da UC Sistemas Distribuídos (Anhembi Morumbi).
