/** Pi compares with `>`; reserve an extra token to trigger at exactly 80%. */
export function automaticCompactionSettings(contextWindow: number) {
  const window = Math.max(1, Math.floor(contextWindow));
  return {
    enabled: true,
    reserveTokens: window - Math.max(1, Math.floor(window * 0.8)) + 1,
    keepRecentTokens: Math.min(20_000, Math.max(1, Math.floor(window * 0.25))),
  };
}
