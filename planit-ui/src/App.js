// src/App.js
import React, { useState, useEffect } from 'react'
import axios from 'axios'
import { Calendar, dateFnsLocalizer, Views } from 'react-big-calendar'
import { format, parseISO, startOfWeek, getDay } from 'date-fns'
import { toZonedTime } from 'date-fns-tz'
import 'react-big-calendar/lib/css/react-big-calendar.css'

const locales = { 'en-US': require('date-fns/locale/en-US') }
const localizer = dateFnsLocalizer({ format, parse: parseISO, startOfWeek, getDay, locales })
const tz = Intl.DateTimeFormat().resolvedOptions().timeZone

// 1) Define your category → subcategory map
const CATEGORIES = {
  Music: ['Jazz', 'Rock', 'Hip Hop', 'Latin Music'],
  Sport: ['Football', 'Basketball', 'Soccer', 'Hockey'],
}

export default function App() {
  const [events, setEvents] = useState([])
  const [cat, setCat] = useState('')                    // selected top‐level
  const [subs, setSubs] = useState(new Set())           // selected subgenres

  useEffect(() => {
    axios.get('/api/events').then(res => {
      const mapped = res.data
        .filter(e => e.startTime && e.endTime)
        .map(e => {
          const s = toZonedTime(parseISO(e.startTime + 'Z'), tz)
          const t = toZonedTime(parseISO(e.endTime   + 'Z'), tz)
          return {
            id:       e.id,
            title:    e.title,
            start:    s,
            end:      t,
            category: e.category || 'Uncategorized',
            genre:    e.genre    || 'Unknown',
          }
        })
      setEvents(mapped)
    })
  }, [])


  function toggleSub(sub) {
    setSubs(prev => {
      const next = new Set(prev)
      next.has(sub) ? next.delete(sub) : next.add(sub)
      return next
    })
  }


  const visible = events.filter(e => {
    if (!cat) return true                    // no top‐level filter => show all
    if (e.category !== cat) return false     // must match top‐level
    if (subs.size === 0) return true         // no subfilter => show all in this category
    return subs.has(e.genre)                 // otherwise must match one of the subgenres
  })

  return (
    <div style={{ display: 'flex', height: '100vh' }}>
      {/* ——— Calendar ——— */}
      <div style={{ flexGrow: 1, padding: 20 }}>
        <h1>PlanIT Weekly Calendar</h1>
        <Calendar
          localizer={localizer}
          events={visible}
          defaultView={Views.WEEK}
          views={[Views.WEEK]}
          step={30}
          showMultiDayTimes
          defaultDate={new Date()}
          style={{ height: '85%' }}
        />
      </div>

      {/* ——— Sidebar Filters ——— */}
      <aside style={{
        width: 240,
        borderLeft: '1px solid #ddd',
        padding: '1rem',
        overflowY: 'auto'
      }}>
        <h3>Choose a category</h3>
        <select
          value={cat}
          onChange={e => { setCat(e.target.value); setSubs(new Set()); }}
          style={{ width: '100%', marginBottom: '1rem' }}
        >
          <option value="">— all events —</option>
          {Object.keys(CATEGORIES).map(key => (
            <option key={key} value={key}>{key}</option>
          ))}
        </select>

        {cat && (
          <>
            <h4>Pick one or more:</h4>
            {CATEGORIES[cat].map(sub => (
              <button
                key={sub}
                onClick={() => toggleSub(sub)}
                style={{
                  display: 'block',
                  width: '100%',
                  margin: '0.25rem 0',
                  padding: '0.5rem',
                  background: subs.has(sub) ? '#007aff' : '#eee',
                  color: subs.has(sub) ? 'white' : '#333',
                  border: 'none',
                  borderRadius: 4,
                  textAlign: 'left',
                  cursor: 'pointer'
                }}
              >
                {sub}
              </button>
            ))}
          </>
        )}
      </aside>
    </div>
  )
}
