// src/SettingsPage.js
import React, { useContext } from 'react';
import { FilterContext } from './FilterContext';

const ALL_CATEGORIES = ['Art', 'Culture', 'Movies', 'Sports'];
const ALL_GENRES     = ['Rock', 'Hip-Hop', 'Comedy', 'Jazz'];

export default function SettingsPage() {
  const {
    selectedCategories, setSelectedCategories,
    selectedGenres,     setSelectedGenres
  } = useContext(FilterContext);

  const toggle = (list, setList, value) => {
    setList(list.includes(value)
      ? list.filter(x => x !== value)
      : [...list, value]);
  };

  return (
    <div style={{ padding: 20 }}>
      <h2>Filter Events</h2>

      <section>
        <h3>Categories</h3>
        {ALL_CATEGORIES.map(cat => (
          <label key={cat} style={{ marginRight: 12 }}>
            <input
              type="checkbox"
              checked={selectedCategories.includes(cat)}
              onChange={() => toggle(selectedCategories, setSelectedCategories, cat)}
            />
            {' '}{cat}
          </label>
        ))}
      </section>

      <section style={{ marginTop: 24 }}>
        <h3>Genres</h3>
        {ALL_GENRES.map(genre => (
          <label key={genre} style={{ marginRight: 12 }}>
            <input
              type="checkbox"
              checked={selectedGenres.includes(genre)}
              onChange={() => toggle(selectedGenres, setSelectedGenres, genre)}
            />
            {' '}{genre}
          </label>
        ))}
      </section>
    </div>
  );
}
