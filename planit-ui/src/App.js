import React, { useState, useEffect } from 'react'
import axios from 'axios'
import { Calendar, dateFnsLocalizer, Views } from 'react-big-calendar'
import { format, parseISO, startOfWeek, getDay } from 'date-fns'
import { toZonedTime } from 'date-fns-tz'
import 'react-big-calendar/lib/css/react-big-calendar.css'

const locales = {
  'en-US': require('date-fns/locale/en-US'),
}
const localizer = dateFnsLocalizer({ format, parse: parseISO, startOfWeek, getDay, locales })
const tz = Intl.DateTimeFormat().resolvedOptions().timeZone

// hard-coded these for MVP;
const ALL_CATEGORIES = ['Music', 'Sports']
const GENRES_BY_CATEGORY = {
  Music:    ['Rock', 'Jazz', 'Hip Hop', 'Latin Music'],
  Sports:   ['Football', 'Basketball', 'Soccer', 'Baseball']
}

export default function App() {
  const [allEvents, setAllEvents]           = useState([])
  const [category, setCategory]             = useState(ALL_CATEGORIES[0])
  const [selectedGenres, setSelectedGenres] = useState([])

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
              category: evt.category,
              genre:    evt.genre,
              start:    toZonedTime(utcStart, tz),
              end:      toZonedTime(utcEnd,   tz),
              allDay:   false,
            }
          })
        setAllEvents(mapped)
      })
      .catch(console.error)
  }, [])

  // toggle a genre on/off
  function toggleGenre(genre) {
    setSelectedGenres(gs =>
      gs.includes(genre)
        ? gs.filter(g => g !== genre)
        : [...gs, genre]
    )
  }


  const filteredEvents = allEvents.filter(evt =>
    evt.category === category
    && (
      selectedGenres.length === 0
      || selectedGenres.includes(evt.genre)
    )
  )

  return (
    <div style={{ display: 'flex', height: '100vh' }}>
      <div style={{ flex: 1, padding: 20 }}>
        <h1>PlanIT Weekly Calendar</h1>
        <Calendar
          localizer={localizer}
          events={filteredEvents}
          defaultView={Views.WEEK}
          views={[Views.WEEK]}
          step={30}
          showMultiDayTimes
          defaultDate={new Date()}
          style={{ height: '90%' }}
        />
      </div>

      <div style={{
        width: 240,
        borderLeft: '1px solid #ddd',
        padding: 20,
        boxSizing: 'border-box'
      }}>
        <h2>Choose a category</h2>
        <select
          value={category}
          onChange={e => setCategory(e.target.value)}
          style={{ width: '100%', marginBottom: 20 }}
        >
          {ALL_CATEGORIES.map(cat =>
            <option key={cat} value={cat}>{cat}</option>
          )}
        </select>

        <h3>Pick one or more genres</h3>
        {GENRES_BY_CATEGORY[category].map(genre =>
          <button
            key={genre}
            onClick={() => toggleGenre(genre)}
            style={{
              display: 'block',
              width: '100%',
              textAlign: 'left',
              margin: '4px 0',
              padding: '8px',
              background: selectedGenres.includes(genre) ? '#2563eb' : '#e5e7eb',
              color: selectedGenres.includes(genre) ? 'white' : 'black',
              border: 'none',
              borderRadius: 4,
              cursor: 'pointer'
            }}
          >
            {genre}
          </button>
        )}
      </div>
    </div>
  )
}
