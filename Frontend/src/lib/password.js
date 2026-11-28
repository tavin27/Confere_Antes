const CHARACTER_SETS = [
  "ABCDEFGHJKLMNPQRSTUVWXYZ",
  "abcdefghijkmnopqrstuvwxyz",
  "23456789",
  "!@#$%&*+-=?_",
];
const ALL_CHARACTERS = CHARACTER_SETS.join("");

function randomIndex(cryptoApi, max) {
  const limit = 256 - (256 % max);
  const byte = new Uint8Array(1);
  do {
    cryptoApi.getRandomValues(byte);
  } while (byte[0] >= limit);
  return byte[0] % max;
}

export function generatePassword(length = 20) {
  if (!Number.isInteger(length) || length < CHARACTER_SETS.length || length > 72) {
    throw new RangeError("O tamanho da senha deve estar entre 4 e 72 caracteres.");
  }

  const cryptoApi = globalThis.crypto;
  if (!cryptoApi || typeof cryptoApi.getRandomValues !== "function") {
    throw new Error("Este navegador não oferece geração criptograficamente segura.");
  }

  const characters = CHARACTER_SETS.map((set) => set[randomIndex(cryptoApi, set.length)]);
  while (characters.length < length) {
    characters.push(ALL_CHARACTERS[randomIndex(cryptoApi, ALL_CHARACTERS.length)]);
  }

  for (let index = characters.length - 1; index > 0; index -= 1) {
    const swapIndex = randomIndex(cryptoApi, index + 1);
    [characters[index], characters[swapIndex]] = [characters[swapIndex], characters[index]];
  }

  return characters.join("");
}
