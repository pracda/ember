// Pure helper for the pickup board: which ready ids are newly arrived since the
// last render (so their tiles can pulse / chime).
export function diffNewIds(previous: readonly number[], current: readonly number[]): number[] {
  const seen = new Set(previous);
  return current.filter((id) => !seen.has(id));
}
