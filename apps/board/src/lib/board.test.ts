import { describe, expect, it } from 'vitest';
import { diffNewIds } from './board';

describe('diffNewIds', () => {
  it('returns ids present now but not before', () => {
    expect(diffNewIds([1, 2], [2, 3, 4])).toEqual([3, 4]);
  });

  it('is empty when nothing new', () => {
    expect(diffNewIds([1, 2, 3], [2, 1])).toEqual([]);
    expect(diffNewIds([], [])).toEqual([]);
  });

  it('treats an empty previous as all-new', () => {
    expect(diffNewIds([], [5, 6])).toEqual([5, 6]);
  });
});
