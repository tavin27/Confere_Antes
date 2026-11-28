const EMERGENCY_TERMS = [
  "familia", "familiares", "parente", "parentes",
  "porta", "arrombar", "arrombada", "arrombando", "arrombamento",
  "perseguicao", "perseguindo", "perseguido", "perseguida", "perseguem", "perseguiram",
  "perseguidor", "perseguidores", "seguindo",
  "ameaca", "ameacando", "agressao", "agredindo",
  "violencia", "violento", "violenta", "arma", "armado", "armada",
  "invadir", "invadiram", "invasao", "invasor", "invasores",
  "perigo", "socorro", "emergencia", "assedio", "abuso", "abusando",
  "sequestro", "sequestraram", "roubo", "roubaram", "assalto", "assaltaram",
];
const EMERGENCY_PATTERN = new RegExp(`(?:^|[^a-z-])(?:${EMERGENCY_TERMS.join("|")})(?=$|[^a-z-])`, "u");

export function hasEmergencyKeywords(text) {
  const normalized = text.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase();
  return EMERGENCY_PATTERN.test(normalized);
}
