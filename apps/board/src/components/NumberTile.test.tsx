import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { NumberTile } from './NumberTile';

describe('NumberTile', () => {
  it('a ready tile is a labelled button that collects on tap', async () => {
    const user = userEvent.setup();
    const onCollect = vi.fn();
    render(<NumberTile ticketNumber={42} variant="ready" onCollect={onCollect} />);

    const tile = screen.getByRole('button', { name: /Collect order 42/ });
    expect(tile).toHaveTextContent('42');
    await user.click(tile);
    expect(onCollect).toHaveBeenCalledTimes(1);
  });

  it('a preparing tile is not interactive', () => {
    render(<NumberTile ticketNumber={7} variant="preparing" />);
    expect(screen.queryByRole('button')).toBeNull();
    expect(screen.getByLabelText(/Preparing order 7/)).toHaveTextContent('7');
  });
});
