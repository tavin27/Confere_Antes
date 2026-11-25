"""
Script de treinamento do classificador de golpes (TF-IDF + LogisticRegression).

USO:
    python train.py [--dataset data/synthetic_dataset.csv] [--output models/]

NOTA IMPORTANTE:
    O dataset padrão (synthetic_dataset.csv) contém exemplos SINTÉTICOS criados
    para demonstrar o pipeline de ML. As métricas obtidas NÃO representam
    desempenho em dados reais. Para uso em produção, substitua por um dataset
    rotulado com exemplos reais (com consentimento e política de privacidade).
"""

import argparse
import json
import os
import sys
from datetime import datetime, timezone

import joblib
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import classification_report, confusion_matrix
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline

MODEL_VERSION = "tfidf-logreg-1"
RANDOM_SEED = 42


def load_dataset(path: str) -> pd.DataFrame:
    """Carrega o CSV e valida colunas obrigatórias."""
    if not os.path.exists(path):
        print(f"ERRO: Dataset não encontrado em {path}", file=sys.stderr)
        sys.exit(1)

    df = pd.read_csv(path)
    required = {"label", "text"}
    if not required.issubset(df.columns):
        print(f"ERRO: Dataset deve ter colunas {required}. Encontrado: {set(df.columns)}", file=sys.stderr)
        sys.exit(1)

    valid_labels = {"GOLPE", "LEGITIMO"}
    invalid = set(df["label"].unique()) - valid_labels
    if invalid:
        print(f"ERRO: Labels inválidos encontrados: {invalid}. Use apenas GOLPE ou LEGITIMO.", file=sys.stderr)
        sys.exit(1)

    df = df.dropna(subset=["text", "label"])
    df["text"] = df["text"].astype(str).str.strip()
    df = df[df["text"].str.len() > 0]

    print(f"Dataset carregado: {len(df)} amostras")
    print(f"Distribuição: {df['label'].value_counts().to_dict()}")
    return df


def build_pipeline():
    """Constrói o pipeline TF-IDF + LogisticRegression."""
    return Pipeline([
        ("tfidf", TfidfVectorizer(
            max_features=5000,
            ngram_range=(1, 2),
            sublinear_tf=True,
            strip_accents="unicode",
            lowercase=True,
        )),
        ("clf", LogisticRegression(
            max_iter=1000,
            random_state=RANDOM_SEED,
            class_weight="balanced",
            solver="lbfgs",
        )),
    ])


def extract_signals(pipeline, text: str, top_n: int = 5) -> list[str]:
    """Extrai os termos mais relevantes que influenciaram a previsão."""
    tfidf = pipeline.named_steps["tfidf"]
    clf = pipeline.named_steps["clf"]
    feature_names = tfidf.get_feature_names_out()
    X = tfidf.transform([text])
    coefficients = clf.coef_[0]
    scores = X.toarray()[0] * coefficients
    top_indices = scores.argsort()[-top_n:][::-1]
    return [feature_names[i] for i in top_indices if scores[i] != 0]


def train(dataset_path: str, output_dir: str):
    """Treina o modelo, avalia e salva artefatos."""
    df = load_dataset(dataset_path)

    X_train, X_test, y_train, y_test = train_test_split(
        df["text"], df["label"],
        test_size=0.2,
        stratify=df["label"],
        random_state=RANDOM_SEED,
    )

    print(f"\nTreino: {len(X_train)} | Teste: {len(X_test)}")

    pipeline = build_pipeline()
    pipeline.fit(X_train, y_train)

    y_pred = pipeline.predict(X_test)
    report = classification_report(y_test, y_pred, output_dict=True)
    cm = confusion_matrix(y_test, y_pred, labels=["GOLPE", "LEGITIMO"])

    print("\n=== Relatório de Classificação ===")
    print(classification_report(y_test, y_pred, target_names=["GOLPE", "LEGITIMO"]))
    print("=== Matriz de Confusao ===")
    print(f"  GOLPE->GOLPE:      {cm[0][0]}")
    print(f"  GOLPE->LEGITIMO:   {cm[0][1]}")
    print(f"  LEGITIMO->GOLPE:   {cm[1][0]}")
    print(f"  LEGITIMO->LEGITIMO: {cm[1][1]}")

    # Salvar modelo
    os.makedirs(output_dir, exist_ok=True)
    model_path = os.path.join(output_dir, f"{MODEL_VERSION}.joblib")
    joblib.dump(pipeline, model_path)
    print(f"\nModelo salvo em: {model_path}")

    # Salvar metadados
    metadata = {
        "model_version": MODEL_VERSION,
        "trained_at": datetime.now(timezone.utc).isoformat(),
        "dataset_path": dataset_path,
        "dataset_size": len(df),
        "train_size": len(X_train),
        "test_size": len(X_test),
        "metrics": {
            "accuracy": report["accuracy"],
            "golpe_precision": report["GOLPE"]["precision"],
            "golpe_recall": report["GOLPE"]["recall"],
            "golpe_f1": report["GOLPE"]["f1-score"],
            "legitimo_precision": report["LEGITIMO"]["precision"],
            "legitimo_recall": report["LEGITIMO"]["recall"],
            "legitimo_f1": report["LEGITIMO"]["f1-score"],
        },
        "confusion_matrix": cm.tolist(),
        "random_seed": RANDOM_SEED,
        "note": "Dataset sintético. Métricas não representam desempenho real.",
    }
    metadata_path = os.path.join(output_dir, f"{MODEL_VERSION}_metadata.json")
    with open(metadata_path, "w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2, ensure_ascii=False)
    print(f"Metadados salvos em: {metadata_path}")

    # Exemplo de sinais extraídos
    print("\n=== Exemplos de Sinais Extraídos ===")
    examples = [
        "Seu cartão foi bloqueado clique urgente",
        "Reunião remarcada para amanhã às 14h",
    ]
    for ex in examples:
        signals = extract_signals(pipeline, ex)
        pred = pipeline.predict([ex])[0]
        prob = pipeline.predict_proba([ex]).max()
        print(f"  '{ex}' → {pred} ({prob:.2f}) sinais: {signals}")

    return metadata


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Treinar classificador de golpes")
    parser.add_argument("--dataset", default="data/synthetic_dataset.csv", help="Caminho do CSV")
    parser.add_argument("--output", default="models/", help="Diretório de saída")
    args = parser.parse_args()
    train(args.dataset, args.output)