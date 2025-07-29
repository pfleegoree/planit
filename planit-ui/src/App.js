import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { Calendar, dateFnsLocalizer, Views } from 'react-big-calendar';
import { format, parse, startOfWeek, getDay } from 'date-fns';
import 'react-big-calendar/lib/css/react-big-calendar.css';

const locales = {
  'en-US': require('date-fns/locale/en-US'),
};
const localizer = dateFnsLocalizer({ format, parse, startOfWeek, getDay, locales });

function App() {
  const [events, setEvents] = useState([]);

  useEffect(() => {
    axios.get('/api/events')           // thanks to the proxy
      .then(res => {
        const mapped = res.data.map(evt => ({
          id: evt.id,
          title: evt.title,
          start: evt.startTime ? new Date(evt.startTime) : new Date(),
          end:   evt.endTime   ? new Date(evt.endTime)   : new Date(),
          allDay: false,
        }));
        setEvents(mapped);
      })
      .catch(err => console.error('Error fetching events:', err));
  }, []);

  return (
    <div style={{ height: '100vh', padding: 20 }}>
      <h1>PlanIT Weekly Calendar</h1>
      <Calendar
        localizer={localizer}
        events={events}
        defaultView={Views.WEEK}
        views={[ Views.WEEK ]}
        step={30}
        showMultiDayTimes
        defaultDate={new Date()}
        style={{ height: '90%' }}
      />
    </div>
  );
}

export default App;
