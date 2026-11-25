# Confere Antes — Serviço de Classificação ML

Classificador supervisionado de textos (TF-IDF + LogisticRegression) para distinguir **GOLPE** de **LEGITIMO**, integrado ao backend Spring Boot via API REST.

## Arquitetura

```
Texto/Transcrição → Classificador ML (Python/FastAPI) → Resultado JSONB
       ↓                                                        ↓
  Gemini explica                                    Alert.ml_classification
  (contexto + sinais)                              RiskAnalysis.ml_classification
       ↓
  Usuário decide
```

- O classificador ML é **independente** do Gemini. Falhas no ML não bloqueiam o fluxo.
- O Gemini recebe o resultado do ML como contexto adicional para gerar explicações.
- O resultado do ML é armazenado em campo JSONB separado da análise do Gemini.

## Dataset

O dataset padrão (`data/synthetic_dataset.csv`) contém **exemplos sintéticos** criados para demonstrar o pipeline.

> ⚠️ **As métricas obtidas com este dataset NÃO representam desempenho em dados reais.**
> Para uso em produção, substitua por um dataset rotulado com exemplos reais,
> obtido com consentimento dos usuários e política de privacidade adequada.

### Formato do CSV

```csv
label,text
GOLPE,"Seu cartão foi bloqueado, clique neste link urgente"
LEGITIMO,"Reunião remarcada para amanhã às 14h"
```

Labels aceitos: `GOLPE`, `LEGITIMO`.

## Como treinar localmente

```bash
cd MLService
pip install -r requirements.txt
python train.py --dataset data/synthetic_dataset.csv --output models/
```

O script gera:
- `models/tfidf-logreg-1.joblib` — modelo serializado
- `models/tfidf-logreg-1_metadata.json` — métricas, data de treino, versão

## Como executar localmente

```bash
cd MLService
pip install -r requirements.txt
python train.py  # treina se ainda não treinou
uvicorn app:app --host 0.0.0.0 --port 8000 --reload
```

### Endpoints

| Método | Rota          | Descrição                          |
|--------|---------------|------------------------------------|
| POST   | `/classify`   | Classifica texto → `{label, probability, riskLevel, signals, modelVersion, status}` |
| GET    | `/health`     | Verifica disponibilidade           |
| GET    | `/model-info` | Metadados do modelo carregado      |

### Contrato de resposta `/classify`

```json
{
  "label": "GOLPE",
  "probability": 0.87,
  "riskLevel": "ALTO",
  "signals": ["link urgente", "cartão bloqueado"],
  "modelVersion": "tfidf-logreg-1",
  "status": "COMPLETED"
}
```

Status possíveis: `COMPLETED`, `LOW_CONFIDENCE` (quando confiança < limiar).

## Configuração do Backend Java

Adicione no `application.properties` ou variáveis de ambiente:

```properties
ml.service.url=${ML_SERVICE_URL:http://localhost:8000}
ml.service.enabled=${ML_SERVICE_ENABLED:true}
```

Se `ML_SERVICE_URL` estiver vazio ou o serviço indisponível, a classificação ML retorna `null` e o fluxo continua normalmente com o Gemini.

## Deploy no Render

### Limitações do plano gratuito

O plano gratuito do Render permite **apenas um serviço web ativo por vez**. Opções:

1. **Mesmo container**: Adicionar o Python no Dockerfile do backend Java (mais complexo).
2. **Conta separada**: Criar outra conta gratuita no Render para o serviço ML.
3. **Alternativa gratuita**: Hospedar o FastAPI em outro serviço gratuito (Railway, Fly.io, Hugging Face Spaces).

### Variáveis de ambiente necessárias

No dashboard do Render, adicione ao serviço backend:

| Variável         | Valor                                    |
|------------------|------------------------------------------|
| `ML_SERVICE_URL` | URL do serviço ML (ex: `https://ml-service.onrender.com`) |
| `ML_SERVICE_ENABLED` | `true`                               |

### Dockerfile do ML

O `MLService/Dockerfile` já inclui o treinamento no build. Basta fazer deploy apontando para `MLService/`.

## Testes

### Testes Python

```bash
cd MLService
python train.py  # necessário antes dos testes
python -m pytest tests/ -v
```

### Testes Java

Os testes do backend mockam o `MlClassificationService` para não depender do serviço Python.

## Versionamento do Modelo

- A versão do modelo está em `train.py` (`MODEL_VERSION`).
- Ao retreinar, altere a versão e atualize o caminho no `app.py` ou via variável `ML_MODEL_PATH`.
- O campo `modelVersion` na resposta permite rastrear qual versão classificou cada texto.

## Limitações Conhecidas

- **Dataset sintético**: As métricas atuais não refletem desempenho real.
- **Falsos positivos**: Textos legítimos com palavras-chave de golpe podem ser classificados incorretamente.
- **Falsos negativos**: Golpes sofisticados sem padrões textuais óbvios podem passar despercebidos.
- **Idioma**: O modelo foi treinado apenas com textos em português do Brasil.
- **Texto muito curto**: Mensagens com poucas palavras tendem a ter baixa confiança.
- **Sem aprendizado contínuo**: O modelo não aprende com novos dados automaticamente. Retreinamento manual é necessário.