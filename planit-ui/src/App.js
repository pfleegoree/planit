 import React, { useState, useEffect } from 'react';
 import axios from 'axios';
 import { Calendar, dateFnsLocalizer, Views } from 'react-big-calendar';
 import { format, parseISO, startOfWeek, getDay } from 'date-fns';
 import 'react-big-calendar/lib/css/react-big-calendar.css';
 import { toZonedTime } from 'date-fns-tz'

 const locales = {
   'en-US': require('date-fns/locale/en-US'),
 };
 const localizer = dateFnsLocalizer({ format, parse: parseISO, startOfWeek, getDay, locales });
 const tz = Intl.DateTimeFormat().resolvedOptions().timeZone;

 function App() {
   const [events, setEvents] = useState([]);

   useEffect(() => {
     axios.get('/api/events')
       .then(res => {
         const mapped = res.data
           // drop any that have no times (or handle all‑day differently)
           .filter(evt => evt.startTime && evt.endTime)
                      .map(evt => {
                       // 1) turn the ISO string into a JS Date
                        const utcStart = parseISO(evt.startTime + 'Z')
                        const utcEnd   = parseISO(evt.endTime + 'Z');

                        // 2) shift that UTC date into the local zone
                        return {
                          id:     evt.id,
                          title:  evt.title,
                          start:  toZonedTime(utcStart, tz),
                          end:    toZonedTime(utcEnd,   tz),
                          allDay: false,
                        };
                      });

         console.log('mapped events →', mapped);
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
