import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { MenuItem } from '@ember/shared';
import { CustomizeModal } from './CustomizeModal';

const burger: MenuItem = {
  id: 'b1',
  name: 'Ember Smash',
  category: 'Burgers',
  basePrice: 6.5,
  mealAvailable: true,
  sizes: [],
  addons: [
    { label: 'Extra cheese', priceDelta: 0.9 },
    { label: 'No onion', priceDelta: 0 },
  ],
};

describe('CustomizeModal', () => {
  it('shows a live price that updates with choices, and returns them on add', async () => {
    const user = userEvent.setup();
    const onAdd = vi.fn();
    render(<CustomizeModal item={burger} onAdd={onAdd} onClose={() => {}} />);

    const addButton = screen.getByRole('button', { name: /Add to ticket/ });
    expect(addButton).toHaveTextContent('$6.50');

    await user.click(screen.getByLabelText(/Extra cheese/));
    expect(addButton).toHaveTextContent('$7.40');

    await user.click(screen.getByLabelText(/Make it a meal/));
    expect(addButton).toHaveTextContent('$10.90');

    await user.click(addButton);
    expect(onAdd).toHaveBeenCalledTimes(1);
    expect(onAdd).toHaveBeenCalledWith(
      expect.objectContaining({ meal: true, addons: ['Extra cheese'], size: undefined }),
    );
  });

  it('closes on Escape', async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();
    render(<CustomizeModal item={burger} onAdd={() => {}} onClose={onClose} />);
    await user.keyboard('{Escape}');
    expect(onClose).toHaveBeenCalled();
  });
});
