// src/FilterContext.js
import React, { createContext, useState } from 'react';

export const FilterContext = createContext();

export function FilterProvider({ children }) {
  const [selectedCategories, setSelectedCategories] = useState([]); // e.g. ["Art","Culture"]
  const [selectedGenres, setSelectedGenres] = useState([]);         // e.g. ["Rock","Comedy"]

  return (
    <FilterContext.Provider value={{
      selectedCategories,
      setSelectedCategories,
      selectedGenres,
      setSelectedGenres
    }}>
      {children}
    </FilterContext.Provider>
  );
}
