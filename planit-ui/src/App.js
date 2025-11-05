// src/App.jsx
import React, { useState, useEffect } from 'react'
import axios from 'axios'
import { Calendar, dateFnsLocalizer, Views } from 'react-big-calendar'
import { format, parseISO, startOfWeek, getDay, addWeeks } from 'date-fns'
import { toZonedTime } from 'date-fns-tz'
import 'react-big-calendar/lib/css/react-big-calendar.css'

import planitLogo from './icons/planit-p.svg'

const BRAND_YELLOW = '#FDE68A'
const BRAND_TEXT_DARK = '#0B1220'

const locales = { 'en-US': require('date-fns/locale/en-US') }
const localizer = dateFnsLocalizer({ format, parse: parseISO, startOfWeek, getDay, locales })
const tz = Intl.DateTimeFormat().resolvedOptions().timeZone

function clamp(n, min, max) { return Math.max(min, Math.min(max, n)); }

export default function App() {
  const [allEvents, setAllEvents]                   = useState([])
  const [categories, setCategories]                 = useState(['All'])
  const [category, setCategory]                     = useState('All')
  const [selectedByCategory, setSelectedByCategory] = useState({})

  // NEW: control visible date (the calendar will center this date)
  const [currentDate, setCurrentDate] = useState(new Date())

  const selectedGenres = selectedByCategory[category] || []

  const genresForCurrentCategory = Array.from(
    new Set(
      allEvents
        .filter(e => category === 'All' || e.category === category)
        .map(e => e.genre)
    )
  ).filter(Boolean)

  function toggleGenre(genre) {
    setSelectedByCategory(prev => {
      const current = prev[category] || []
      const next = current.includes(genre)
        ? current.filter(g => g !== genre)
        : [...current, genre]
      return { ...prev, [category]: next }
    })
  }

   useEffect(() => {
      axios.get('/api/events')
        .then(res => {
          const mapped = res.data
            .filter(evt => evt.startTime && evt.endTime)
            .map(evt => {
              const utcStart = parseISO(evt.startTime + 'Z')
              const utcEnd   = parseISO(evt.endTime   + 'Z')
              return {
                id:       evt.id,
                title:    evt.title,
                category: evt.category || 'Uncategorized',
                genre:    evt.genre,
                start:    toZonedTime(utcStart, tz),
                end:      toZonedTime(utcEnd,   tz),
                allDay:   false,
              }
            })


        setAllEvents(mapped)
      })
      .catch(err => console.error('GET /api/events failed:', err))
  }, []) // load once

   // Recompute categories whenever events change; reset invalid selection
   useEffect(() => {
     // avoid re-creating Set work if you like
     // import { useMemo } from 'react'
     const catsFromData = Array.from(new Set(allEvents.map(e => e.category))).filter(Boolean);


     setCategories(['All', ...catsFromData]);

     if (category !== 'All' && !catsFromData.includes(category)) {
       setCategory('All');
     }
   }, [allEvents, category]);

  // Client-side filtering
  const filteredEvents = allEvents.filter(evt => {
    const categoryMatch = category === 'All' || evt.category === category
    const genreMatch    = selectedGenres.length === 0 || selectedGenres.includes(evt.genre)
    return categoryMatch && genreMatch
  })

  // NAVIGATION helpers (week +/- 1)
  function goPrevWeek() { setCurrentDate(prev => addWeeks(prev, -1)) }
  function goNextWeek() { setCurrentDate(prev => addWeeks(prev, 1)) }
  function goToday() { setCurrentDate(new Date()) }

  // ----------- dynamic min/max/scrollToTime based on filteredEvents -------------
  const DEFAULT_MIN_HOUR = 8   // fallback start hour
  const DEFAULT_MAX_HOUR = 22  // fallback end hour
  const HOUR_BUFFER = 1        // extra hour padding

  let earliestHour = 24
  let latestHour = -1

  filteredEvents.forEach(evt => {
    if (evt.start instanceof Date) {
      const sh = evt.start.getHours()
      earliestHour = Math.min(earliestHour, sh)
    }
    if (evt.end instanceof Date) {
      const eh = evt.end.getHours()
      if (evt.end.getMinutes() > 0) latestHour = Math.max(latestHour, eh + 1)
      else latestHour = Math.max(latestHour, eh)
    }
  })

  const minHour = earliestHour <= 23 ? clamp(earliestHour - HOUR_BUFFER, 0, 23) : DEFAULT_MIN_HOUR
  const maxHour = latestHour >= 0 ? clamp(latestHour + HOUR_BUFFER, 1, 24) : DEFAULT_MAX_HOUR

  const dayForMinMax = new Date(currentDate) // use currently-centered day
  const minDate = new Date(dayForMinMax)
  minDate.setHours(minHour, 0, 0, 0)

  const maxDate = new Date(dayForMinMax)
  if (maxHour >= 24) {
    maxDate.setHours(23, 59, 59, 999)
  } else {
    maxDate.setHours(maxHour, 0, 0, 0)
  }

  const scrollToTime = minDate
  // ---------------------------------------------------------------------------

  return (
    <div style={{ display: 'flex', height: '100vh' }}>
      <div style={{ flex: 1, padding: 20 }}>
        {/* Logo + heading + nav controls */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 12 }}>
          <img src={planitLogo} alt="PlanIT" style={{ width: 64, height: 64, borderRadius: 10 }} />
          <h1 style={{ margin: 0 }}>PlanIT Weekly Calendar</h1>

          {/* Buttons row */}
          <div style={{ marginLeft: 20, display: 'flex', gap: 8 }}>
            <button onClick={goToday} style={{ padding: '8px 12px', borderRadius: 6, border: '1px solid #ddd', background: '#fff' }}>Today</button>
            <button onClick={goPrevWeek} style={{ padding: '8px 12px', borderRadius: 6, border: '1px solid #ddd', background: '#fff' }}>Back</button>
            <button onClick={goNextWeek} style={{ padding: '8px 12px', borderRadius: 6, border: '1px solid #ddd', background: '#fff' }}>Next</button>
          </div>
        </div>

       <Calendar
         localizer={localizer}
         events={filteredEvents}
         defaultView={Views.WEEK}
         views={[Views.WEEK]}
         step={30}
         showMultiDayTimes
         date={currentDate}
         onNavigate={date => setCurrentDate(date)}
         toolbar={false}
         min={minDate}
         max={maxDate}
         scrollToTime={scrollToTime}
         style={{ height: '90%' }}
       />
      </div>

      <div style={{ width: 260, borderLeft: '1px solid #ddd', padding: 20, boxSizing: 'border-box' }}>
        <h2>Category</h2>
        <select
          value={category}
          onChange={e => setCategory(e.target.value)}
          style={{ width: '100%', marginBottom: 16 }}
        >
          {categories.map(cat => <option key={cat} value={cat}>{cat}</option>)}
        </select>

        <h3>Genres</h3>
        {genresForCurrentCategory.length === 0 && <div style={{color:'#666'}}>No genres for this selection.</div>}
        {genresForCurrentCategory.map(genre => {
          const isSelected = selectedGenres.includes(genre)
          return (
            <button
              key={genre}
              onClick={() => toggleGenre(genre)}
              style={{
                display: 'block',
                width: '100%',
                textAlign: 'left',
                margin: '4px 0',
                padding: '8px',
                background: isSelected ? BRAND_YELLOW : '#e5e7eb',
                color: isSelected ? BRAND_TEXT_DARK : 'black',
                border: 'none',
                borderRadius: 4,
                cursor: 'pointer'
              }}
            >
              {genre}
            </button>
          )
        })}
      </div>
    </div>
  )
}
