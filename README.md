# Fantasy Football Android App

## Overview

This project is a Fantasy Football mobile application developed using Kotlin (Android) with a Node.js/Express backend and MongoDB database.

The app allows users to:

* Register and create a fantasy team
* Select and manage a squad of players
* Set a starting lineup and formation
* Make transfers
* View leaderboard standings
* Track gameweek points

---

## Tech Stack

**Frontend:**

* Kotlin (Android Studio)
* RecyclerView, Activities, ViewBinding

**Backend:**

* Node.js with Express
* REST API

**Database:**

* MongoDB (Atlas)

---

## Features

* User authentication (JWT-based)
* Team creation during registration
* Dynamic formation system (e.g. 4-4-2, 4-3-3)
* Player transfers and squad management
* Gameweek scoring system
* Leaderboard based on gameweek snapshots
* View other users' teams

---

## Running the Application

### Backend

1. Navigate to the backend folder:

```
cd backend
```

2. Install dependencies:

```
npm install
```

3. Start the server:

```
node server.js
```

---

### Android App

1. Open the project in Android Studio
2. Connect a physical Android device (required for demo)
3. Ensure the BASE_URL in `ApiClient.kt` matches your local IP address
4. Run the application

---

## Notes

* The backend runs locally and must be accessible via the device’s network (e.g. `192.168.x.x`)
* Some data (fixtures, players) is pre-seeded for demonstration purposes
* Leaderboard is based on stored gameweek snapshots rather than live calculation

---

## Future Improvements

* Player images and profiles
* Fixtures/results UI improvements
* More detailed statistics
* Improved transfer validation and UX
* Cloud deployment of backend

---

## Author

Jonathon Ryan
