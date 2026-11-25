"""
API FastAPI para classificação de textos (golpe vs legítimo).

Endpoints:
    POST /classify  - Classifica um texto e retorna label, probabilidade, risco e sinais.
    GET  /health    - Verifica se o serviço e o modelo estão disponíveis.
    GET  /model-info - Retorna metadados do modelo carregado.

Variáveis de ambiente:
    ML_MODEL_PATH      - Caminho do arquivo .joblib (default: models/tfidf-logreg-1.joblib)
    ML_METADATA_PATH   - Caminho do metadata JSON (default: models/tfidf-logreg-1_metadata.json)
    ML_CONFIDENCE_THRESHOLD - Limiar mínimo de confiança para resultado conclusivo (default: 0.6)
"""

import json
import os
from contextlib import asynccontextmanager

import joblib
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

MODEL_PATH = os.environ.get("ML_MODEL_PATH", "models/tfidf-logreg-1.joblib")
METADATA_PATH = os.environ.get("ML_METADATA_PATH", "models/tfidf-logreg-1_metadata.json")
CONFIDENCE_THRESHOLD = float(os.environ.get("ML_CONFIDENCE_THRESHOLD", "0.6"))

# Classes e mapeamento de risco
LABELS = ["GOLPE", "LEGITIMO"]
RISK_MAP = {
    "GOLPE": {"low": "BAIXO", "medium": "MEDIO", "high": "ALTO"},
    "LEGITIMO": {"low": "BAIXO", "medium": "BAIXO", "high": "MEDIO"},
}


def _risk_level(label: str, probability: float) -> str:
    """Mapeia label + probabilidade para nível de risco."""
    if label == "GOLPE":
        if probability >= 0.85:
            return "ALTO"
        elif probability >= 0.6:
            return "MEDIO"
        return "BAIXO"
    else:  # LEGITIMO
        if probability < 0.6:
            return "MEDIO"
        return "BAIXO"


def _extract_signals(pipeline, text: str, top_n: int = 5) -> list[str]:
    """Extrai os termos mais influentes na previsão."""
    try:
        tfidf = pipeline.named_steps["tfidf"]
        clf = pipeline.named_steps["clf"]
        feature_names = tfidf.get_feature_names_out()
        X = tfidf.transform([text])
        coefficients = clf.coef_[0]
        scores = X.toarray()[0] * coefficients
        top_indices = scores.argsort()[-top_n:][::-1]
        return [str(feature_names[i]) for i in top_indices if abs(scores[i]) > 0.001]
    except Exception:
        return []


# Carregamento do modelo no startup
pipeline = None
model_metadata = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global pipeline, model_metadata
    if os.path.exists(MODEL_PATH):
        pipeline = joblib.load(MODEL_PATH)
        print(f"Modelo carregado: {MODEL_PATH}")
    else:
        print(f"AVISO: Modelo não encontrado em {MODEL_PATH}. /classify retornará 503.")

    if os.path.exists(METADATA_PATH):
        with open(METADATA_PATH, "r", encoding="utf-8") as f:
            model_metadata = json.load(f)
        print(f"Metadados carregados: versão={model_metadata.get('model_version', 'desconhecida')}")
    yield


app = FastAPI(
    title="Confere Antes - ML Classifier",
    description="Classificador de textos para detecção de golpes",
    version="1.0.0",
    lifespan=lifespan,
)


class ClassifyRequest(BaseModel):
    text: str = Field(..., min_length=1, max_length=10000, description="Texto para classificação")


class ClassifyResponse(BaseModel):
    label: str
    probability: float
    riskLevel: str
    signals: list[str]
    modelVersion: str
    status: str


@app.get("/health")
async def health():
    return {
        "status": "ok" if pipeline is not None else "no_model",
        "model_loaded": pipeline is not None,
    }


@app.get("/model-info")
async def model_info():
    if model_metadata is None:
        raise HTTPException(status_code=404, detail="Metadados do modelo não disponíveis")
    return model_metadata


@app.post("/classify", response_model=ClassifyResponse)
async def classify(request: ClassifyRequest):
    if pipeline is None:
        raise HTTPException(status_code=503, detail="Serviço de ML indisponível: modelo não carregado")

    text = request.text.strip()
    if not text:
        raise HTTPException(status_code=422, detail="Texto não pode estar vazio após trim")

    prediction = pipeline.predict([text])[0]
    probabilities = pipeline.predict_proba([text])[0]
    proba_dict = dict(zip(pipeline.classes_, probabilities))
    confidence = proba_dict.get(prediction, 0.0)

    # Verificar limiar de confiança
    if confidence < CONFIDENCE_THRESHOLD:
        return ClassifyResponse(
            label="INCONCLUSIVO",
            probability=round(confidence, 4),
            riskLevel="BAIXO",
            signals=[],
            modelVersion=model_metadata.get("model_version", "unknown") if model_metadata else "unknown",
            status="LOW_CONFIDENCE",
        )

    signals = _extract_signals(pipeline, text)
    risk = _risk_level(prediction, confidence)
    version = model_metadata.get("model_version", "unknown") if model_metadata else "unknown"

    return ClassifyResponse(
        label=prediction,
        probability=round(confidence, 4),
        riskLevel=risk,
        signals=signals,
        modelVersion=version,
        status="COMPLETED",
    )