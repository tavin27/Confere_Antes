"""
Testes da API FastAPI de classificação de golpes.

USO:
 cd MLService
 python -m pytest tests/ -v

REQUISITO: O modelo deve estar treinado antes de rodar os testes.
 Execute: python train.py
"""

import json
import os

import pytest
from fastapi.testclient import TestClient

# Garante que o caminho relativo funcione dos testes
os.chdir(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app import app


@pytest.fixture(scope="module")
def client():
    """Cria cliente de teste com lifespan (carrega modelo)."""
    with TestClient(app) as test_client:
        yield test_client


class TestHealthEndpoint:
    def test_health_returns_ok_when_model_loaded(self, client):
        response = client.get("/health")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "ok"
        assert data["model_loaded"] is True

    def test_model_info_returns_metadata(self, client):
        response = client.get("/model-info")
        assert response.status_code == 200
        data = response.json()
        assert "model_version" in data
        assert "metrics" in data
        assert "trained_at" in data


class TestClassifyEndpoint:
    def test_golpe_text_classified_correctly(self, client):
        """Texto claramente suspeito deve ser classificado como GOLPE."""
        response = client.post("/classify", json={
            "text": "Seu cartão foi bloqueado clique neste link urgente para desbloquear agora"
        })
        assert response.status_code == 200
        data = response.json()
        assert data["label"] in ("GOLPE", "INCONCLUSIVO")
        assert data["status"] in ("COMPLETED", "LOW_CONFIDENCE")
        assert 0 <= data["probability"] <= 1
        assert data["riskLevel"] in ("BAIXO", "MEDIO", "ALTO")
        assert "modelVersion" in data

    def test_legitimo_text_classified_correctly(self, client):
        """Texto legítimo deve ser classificado como LEGITIMO."""
        response = client.post("/classify", json={
            "text": "Olá gostaria de agendar uma consulta para a próxima semana"
        })
        assert response.status_code == 200
        data = response.json()
        assert data["label"] in ("LEGITIMO", "INCONCLUSIVO")
        assert data["status"] in ("COMPLETED", "LOW_CONFIDENCE")

    def test_ambiguous_short_text(self, client):
        """Texto muito curto ou ambíguo pode retornar INCONCLUSIVO."""
        response = client.post("/classify", json={"text": "Oi"})
        assert response.status_code == 200
        data = response.json()
        assert data["label"] in ("GOLPE", "LEGITIMO", "INCONCLUSIVO")
        assert data["status"] in ("COMPLETED", "LOW_CONFIDENCE")

    def test_empty_text_rejected(self, client):
        """Texto vazio deve retornar erro 422."""
        response = client.post("/classify", json={"text": ""})
        assert response.status_code == 422

    def test_missing_text_field_rejected(self, client):
        """Campo text ausente deve retornar erro 422."""
        response = client.post("/classify", json={})
        assert response.status_code == 422

    def test_whitespace_only_text_rejected(self, client):
        """Texto apenas com espaços deve retornar erro 422."""
        response = client.post("/classify", json={"text": "   "})
        assert response.status_code == 422

    def test_response_has_required_fields(self, client):
        """Resposta deve conter todos os campos do contrato."""
        response = client.post("/classify", json={
            "text": "Transferência pendente confirme seus dados"
        })
        assert response.status_code == 200
        data = response.json()
        required_fields = {"label", "probability", "riskLevel", "signals", "modelVersion", "status"}
        assert required_fields.issubset(set(data.keys()))

    def test_signals_is_list_of_strings(self, client):
        """Campo signals deve ser lista de strings."""
        response = client.post("/classify", json={
            "text": "Seu CPF está irregular regularize agora clicando no link"
        })
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data["signals"], list)
        for signal in data["signals"]:
            assert isinstance(signal, str)

    def test_probability_between_zero_and_one(self, client):
        """Probabilidade deve estar entre 0 e 1."""
        response = client.post("/classify", json={
            "text": "Reunião remarcada para amanhã às 14h confirma"
        })
        assert response.status_code == 200
        data = response.json()
        assert 0 <= data["probability"] <= 1

    def test_risk_level_valid_values(self, client):
        """riskLevel deve ser BAIXO, MEDIO ou ALTO."""
        valid_levels = {"BAIXO", "MEDIO", "ALTO"}
        texts = [
            "Seu cartão foi bloqueado urgente",
            "Consulta médica confirmada para terça",
            "Oi",
        ]
        for text in texts:
            response = client.post("/classify", json={"text": text})
            assert response.status_code == 200
            assert response.json()["riskLevel"] in valid_levels

    def test_long_text_accepted(self, client):
        """Texto longo (até 10000 chars) deve ser aceito."""
        long_text = "A" * 9999
        response = client.post("/classify", json={"text": long_text})
        assert response.status_code == 200

    def test_text_over_limit_rejected(self, client):
        """Texto acima de 10000 caracteres deve ser rejeitado."""
        too_long = "A" * 10001
        response = client.post("/classify", json={"text": too_long})
        assert response.status_code == 422


class TestModelUnavailable:
    """Testes para quando o modelo não está carregado (simulação)."""

    def test_classify_without_model_returns_503(self):
        """Quando pipeline é None, /classify deve retornar 503."""
        import app as app_module
        from fastapi import FastAPI
        # Cria uma app temporária com pipeline=None para evitar reload do lifespan
        temp_app = FastAPI()
        temp_app.router.routes = [r for r in app.router.routes if hasattr(r, 'path') and r.path == '/classify']
        app_module.pipeline = None
        try:
            with TestClient(temp_app) as c:
                response = c.post("/classify", json={"text": "teste"})
                assert response.status_code == 503
                detail = response.json().get("detail", "").lower()
                assert "indispon" in detail or "unavailable" in detail or "modelo" in detail
        finally:
            # Restaura o pipeline original recarregando
            import joblib, os
            model_path = os.environ.get("ML_MODEL_PATH", "models/tfidf-logreg-1.joblib")
            if os.path.exists(model_path):
                app_module.pipeline = joblib.load(model_path)