const express = require("express");
const cors = require("cors");
const dotenv = require("dotenv");
const { MongoClient } = require("mongodb");
//User Auth
const bcrypt = require("bcrypt");
const jwt = require("jsonwebtoken");
const { ObjectId } = require("mongodb");

// const token = jwt.sign(
//   { userId: user._id },
//   process.env.JWT_SECRET,
//   { expiresIn: "7d" }
// );

//.env
dotenv.config();

//uses express and cors
const app = express();
app.use(cors());
app.use(express.json());

//Port and Database
const PORT = process.env.PORT || 8080;
const DB_NAME = process.env.DB_NAME || "fantasy_football";

if (!process.env.MONGODB_URI) {
  console.error("Missing MONGODB_URI in .env");
  process.exit(1);
}

if (!process.env.JWT_SECRET) {
  console.error("Missing JWT_SECRET in .env");
  process.exit(1);
}


//Mongo Collections
let client;
let leaderboardCollection;
let playersCollection;
let gameweekStatsCollection;
let usersCollection;
let teamsCollection;
let fixturesCollection;
let userGameweekScoresCollection;

const TEAM_SIZE = 15;

// All possible UI slot keys (max formation support)
const SLOT_KEYS = [
  "GK1", "GK2",
  "DEF1", "DEF2", "DEF3", "DEF4", "DEF5",
  "MID1", "MID2", "MID3", "MID4", "MID5",
  "STR1", "STR2", "STR3",
  "BENCH1", "BENCH2", "BENCH3", "BENCH4",
];

const FORMATION_KEYS = [
  "442", "433", "451", "532", "523", "541", "343", "352"
];


//Accounting for bench and starter players in the backend
function normalizeSlotMap(slotMap) {
  if (!slotMap || typeof slotMap !== "object") return null;

  const out = {};
  for (const k of SLOT_KEYS) {
    const v = slotMap[k];
    if (v == null) {
      out[k] = null;
    } else {
      const n = Number(v);
      if (!Number.isInteger(n)) return null;
      out[k] = n;
    }
  }
  return out;
}

//Merging the squad and slots with constraints (must be in squad and no duplicates in slots) - ensures data integrity if frontend sends bad data or user tampers with request
function mergeSquadAndSlots(squadPlayerIds, slotPlayerIds) {
  const squadSet = new Set(squadPlayerIds);

  const merged = {};
  const used = new Set();

  for (const k of SLOT_KEYS) {
    const pid = slotPlayerIds?.[k] ?? null;

    if (pid === null) {
      merged[k] = null;
      continue;
    }

    // must be in squad
    if (!squadSet.has(pid)) {
      merged[k] = null;
      continue;
    }

    // must not appear twice
    if (used.has(pid)) {
      merged[k] = null;
      continue;
    }

    merged[k] = pid;
    used.add(pid);
  }

  return merged;
}

function normalizeFormationKey(v) {
  if (v == null) return null;

  let s = String(v).trim().toUpperCase();

  if (s.startsWith("F")) {
    s = s.slice(1);
  }

  return FORMATION_KEYS.includes(s) ? s : null;
}

//Used in frontend and backend
function detectFormationFromSlotMap(slotMap) {
  if (!slotMap || typeof slotMap !== "object") return null;

  let gk = 0;
  let def = 0;
  let mid = 0;
  let str = 0;

  for (const k of SLOT_KEYS) {
    if (k.startsWith("BENCH")) continue;

    const playerId = slotMap[k];
    if (playerId == null) continue;

    if (k.startsWith("GK")) gk++;
    else if (k.startsWith("DEF")) def++;
    else if (k.startsWith("MID")) mid++;
    else if (k.startsWith("STR")) str++;
  }

  if (gk !== 1) return null;

  const derived = `${def}${mid}${str}`;
  return FORMATION_KEYS.includes(derived) ? derived : null;
}

async function connectToMongo() {
  client = new MongoClient(process.env.MONGODB_URI);
  await client.connect();

  const db = client.db(DB_NAME);
  leaderboardCollection = db.collection("leaderboard");
  playersCollection = db.collection("players");
  gameweekStatsCollection = db.collection("player_gameweek_stats");
  usersCollection = db.collection("users");
  teamsCollection = db.collection("teams");
  fixturesCollection = db.collection("fixtures");
  userGameweekScoresCollection = db.collection("user_gameweek_scores");

  // Unique Indexes for the users and leaderboard for users and their teams.
  await usersCollection.createIndex({ email: 1 }, { unique: true });
  await leaderboardCollection.createIndex({ userId: 1 }, { unique: true });

  console.log(`Connected to MongoDB`);
  console.log(`   DB: ${DB_NAME}`);
  console.log(`   Collections: leaderboard, users, players, player_gameweek_stats, teams, fixtures, user_gameweek_scores`);
}

//Calculating total points for a player
function calculatePlayerPoints(p) {
  const minutesPts =
    (p.minutes || 0) >= 60 ? 2 :
    (p.minutes || 0) > 0 ? 1 : 0;

  const cleanSheetPts =
    (p.position === "GK" || p.position === "DEF")
      ? (p.cleansheets ? 4 : 0)
      : 0;

  return (
    minutesPts +
    (p.goals || 0) * 5 +
    (p.assists || 0) * 3 +
    cleanSheetPts -
    (p.yellows || 0) * 1 -
    (p.reds || 0) * 3 -
    (p.ownGoals || 0) * 2
  );
}

//User Auth Functions
function signToken(user) {
  // keep payload small
  return jwt.sign(
    { userId: user._id.toString(), email: user.email },
    process.env.JWT_SECRET,
    { expiresIn: "7d" }
  );
}

function requireAuth(req, res, next) {
  const header = req.headers.authorization || "";
  const [type, token] = header.split(" ");

  if (type !== "Bearer" || !token) {
    return res.status(401).json({ message: "Missing auth token" });
  }

  try {
    const decoded = jwt.verify(token, process.env.JWT_SECRET);
    req.auth = decoded; // { userId, email, iat, exp }
    next();
  } catch (e) {
    return res.status(401).json({ message: "Invalid or expired token" });
  }
}

//used to calculate and save user points for a specific gameweek and season (called from admin route after each gameweek)
async function saveUserGameweekScore({ userTeam, season, gameweek }) {
  const squadPlayerIds = userTeam.squadPlayerIds || userTeam.playerIds || [];

  const rawSlotPlayerIds = normalizeSlotMap(userTeam.slotPlayerIds) || {};
  const mergedSlotPlayerIds = mergeSquadAndSlots(squadPlayerIds, rawSlotPlayerIds);

  const formationKey =
    normalizeFormationKey(userTeam.formationKey) ||
    detectFormationFromSlotMap(mergedSlotPlayerIds) ||
    "442";

  const STARTER_SLOTS = new Set([
    "GK1",
    "DEF1", "DEF2", "DEF3", "DEF4", "DEF5",
    "MID1", "MID2", "MID3", "MID4", "MID5",
    "STR1", "STR2", "STR3"
  ]);

  // Get starting XI 
  const starterPlayerIds = Object.entries(mergedSlotPlayerIds)
    .filter(([slotKey, playerId]) =>
      STARTER_SLOTS.has(slotKey) && playerId != null
    )
    .map(([_, playerId]) => Number(playerId));

  const idsToScore = starterPlayerIds.length > 0 ? starterPlayerIds : squadPlayerIds;

  // Fetch stats/players
  const stats = await gameweekStatsCollection.find({
    season,
    gameweek,
    playerId: { $in: idsToScore }
  }).toArray();

  const players = await playersCollection.find({
    id: { $in: idsToScore }
  }).toArray();

  const statsByPlayerId = new Map(stats.map(s => [s.playerId, s]));
  const positionByPlayerId = new Map(players.map(p => [p.id, p.position]));
  const nameByPlayerId = new Map(players.map(p => [p.id, p.name]));

  //Calculate points 
  let points = 0;

  console.log("------ TEAM DEBUG START ------");
  console.log("Team:", userTeam.teamName);
  console.log("Starter IDs:", starterPlayerIds, "count:", starterPlayerIds.length);

  for (const playerId of idsToScore) {
    const s = statsByPlayerId.get(playerId);
    if (!s) {
      console.log("Missing stats for:", playerId);
      continue;
    }

    const statForCalc = {
      position: positionByPlayerId.get(playerId) || null,
      minutes: s.minutes || 0,
      goals: s.goals || 0,
      assists: s.assists || 0,
      cleansheets: !!s.cleansheet,
      yellows: s.yellows || 0,
      reds: s.reds || 0,
      ownGoals: s.ownGoals || 0
    };

    //Points calculation
    const playerPoints = calculatePlayerPoints(statForCalc);
    const playerName = nameByPlayerId.get(playerId) || "UNKNOWN";

    console.log(
      `${playerName.padEnd(20)} | ${statForCalc.position} | ${playerPoints} pts`,
      statForCalc
    );

    points += playerPoints;
  }

  console.log("TOTAL CALCULATED:", points);
  console.log("Formation:", formationKey);
  console.log("------ TEAM DEBUG END ------");

  const now = new Date();

  await userGameweekScoresCollection.updateOne(
    {
      userId: userTeam.userId,
      season,
      gameweek
    },
    {
      $set: {
        teamName: userTeam.teamName,
        points,
        squadPlayerIds,
        slotPlayerIds: mergedSlotPlayerIds,
        formationKey,
        updatedAt: now
      },
      $setOnInsert: {
        createdAt: now
      }
    },
    { upsert: true }
  );

  return points;
}

//Res.statuses
//200 = Ok request succeeded
//201 = New resource created
//400 = Bad Request
//500 = Internal Server Error

// --- Routes ---
// GETTERS

// Health check (easy to verify server is running)
app.get("/health", (req, res) => {
  res.json({ ok: true, service: "fantasy-football-backend" });
});

//GET leaderboard with calculated points
app.get("/api/leaderboard", async (req, res) => {
  try {
    const season = String(req.query.season || "2025");
    const gameweek = Number(req.query.gameweek || 1);

    //Get ALL teams
    const teams = await leaderboardCollection.find({}).toArray();

    //Get scores for that GW
    const scores = await userGameweekScoresCollection
      .find({ season, gameweek })
      .toArray();

    //Build a map: userId -> points
    const scoreMap = new Map(
      scores.map(s => [String(s.userId), s.points])
    );

    //Merge (default to 0 if missing)
    const leaderboard = teams.map(team => ({
      userId: team.userId,
      teamName: team.teamName,
      points: scoreMap.get(String(team.userId)) ?? 0,
      gameweek,
      season
    }))
    .sort((a, b) => b.points - a.points);

    res.json(leaderboard);

  } catch (err) {
    console.error("GET /api/leaderboard error:", err);
    res.status(500).json({ message: "Failed to fetch leaderboard" });
  }
});

//Get current user team
app.get("/api/leaderboard/me", requireAuth, async (req, res) => {
  try {
    const userId = new ObjectId(req.auth.userId);
    const team = await leaderboardCollection.findOne({ userId });
    if (!team) return res.status(404).json({ message: "No team found for user" });
    res.json(team);
  } catch (err) {
    console.error("GET /api/leaderboard/me error:", err);
    res.status(500).json({ message: "Failed to fetch team" });
  }
});

//GET players (read from MongoDB)
app.get("/api/players", async (req, res) => {
  try {
    const players = await playersCollection.find({}).sort({ id: 1 }).toArray();

    const withPoints = players.map((p) => ({
      ...p,
      points: calculatePlayerPoints(p),
    }));

    res.json(withPoints);
  } catch (err) {
    console.error("GET /api/players error:", err);
    res.status(500).json({ message: "Failed to fetch players" });
  }
});

//GET player gameweek stats
app.get("/api/gameweeks/:gw", async (req, res) => {
  try {
    //convert from string to number, decimal parcing (10)
    const gw = parseInt(req.params.gw, 10);
    //current season
    const season = req.query.season || "2025";

    //convert playerIds from query string to array of integers
    const playerIds = (req.query.playerIds || "")
      .split(",")
      .map((x) => parseInt(x, 10))
      .filter((n) => Number.isInteger(n));

    if (!Number.isInteger(gw) || playerIds.length === 0) {
      return res.status(400).json({ message: "gw and playerIds are required" });
    }

    const stats = await gameweekStatsCollection
      .find({ season, gameweek: gw, playerId: { $in: playerIds } })
      .toArray();

    res.json(stats);
  } catch (err) {
    console.error("GET /api/gameweeks/:gw error:", err);
    res.status(500).json({ message: "Failed to fetch gameweek stats" });
  }
});

//Route to get all gameweek stats for a player across the season (for stats page)
app.get("/api/gameweeks", async (req, res) => {
  try {
    const season = req.query.season || "2025";

    const playerIds = (req.query.playerIds || "")
      .split(",")
      .map((x) => parseInt(x, 10))
      .filter((n) => Number.isInteger(n));

    if (playerIds.length === 0) {
      return res.status(400).json({ message: "playerIds are required" });
    }

    const stats = await gameweekStatsCollection
      .find({ season, playerId: { $in: playerIds } })
      .toArray();

    res.json(stats);
  } catch (err) {
    console.error("GET /api/gameweeks error:", err);
    res.status(500).json({ message: "Failed to fetch all gameweek stats" });
  }
});

//Getting all of the user gameweek scores for a specific gameweek and season (for leaderboard and stats reference)
app.get("/api/gameweeks/:gameweek/scores", async (req, res) => {
  try {
    const gameweek = Number(req.params.gameweek);
    const season = String(req.query.season || "2025");

    const scores = await userGameweekScoresCollection.find({
      gameweek,
      season
    }).toArray();

    res.json(scores.map(s => ({
    userId: s.userId,
    teamName: s.teamName,
    points: s.points,
    squadPlayerIds: s.squadPlayerIds,
    slotPlayerIds: s.slotPlayerIds,
    formationKey: s.formationKey,
    gameweek: s.gameweek,
    season: s.season
  })));
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Failed to fetch GW scores" });
  }
});

//Getting the current user's gameweek score for a specific gameweek and season
app.get("/api/gameweeks/:gameweek/me", requireAuth, async (req, res) => {
  try {
    const gameweek = Number(req.params.gameweek);
    const season = String(req.query.season || "2025");
    const userId = new ObjectId(req.auth.userId);

    const doc = await userGameweekScoresCollection.findOne({
      userId,
      gameweek,
      season
    });

    res.json(doc || null);
  } catch (err) {
    console.error("GET /api/gameweeks/:gameweek/me error:", err);
    res.status(500).json({ message: "Failed to fetch user gameweek score" });
  }
});

//Getting another user's gameweek score for a specific gameweek and season (for stats page reference when viewing other teams)
app.get("/api/gameweeks/:gameweek/user/:userId", async (req, res) => {
  try {
    const gameweek = Number(req.params.gameweek);
    const season = String(req.query.season || "2025");
    const userId = new ObjectId(req.params.userId);

    const doc = await userGameweekScoresCollection.findOne({
      userId,
      gameweek,
      season
    });

    res.json(doc || null);
  } catch (err) {
    console.error("GET /api/gameweeks/:gameweek/user/:userId error:", err);
    res.status(500).json({ message: "Failed to fetch viewed user gameweek score" });
  }
});

//GET current user (requires auth, used in frontend to verify token and get user info on app load)
app.get("/api/auth/me", requireAuth, async (req, res) => {
  try {
    const userId = new ObjectId(req.auth.userId);
    const user = await usersCollection.findOne(
      { _id: userId },
      { projection: { passwordHash: 0 } }
    );
    if (!user) return res.status(404).json({ message: "User not found" });

    res.json(user);
  } catch (err) {
    console.error("GET /api/auth/me error:", err);
    res.status(500).json({ message: "Failed to fetch user" });
  }
});

// GET teams (Real life clubs)
app.get("/api/teams", async (req, res) => {
  try {
    //Sorted in the way of a league table
    const teams = await teamsCollection
      .find({})
      .sort({
        points: -1,
        goalDifference: -1,
        goalsFor: -1,
        name: 1
      })
      .toArray();

    res.json(teams);
  } catch (err) {
    console.error("GET /api/teams error:", err);
    res.status(500).json({ message: "Failed to fetch teams" });
  }
});

// GET fixtures for a specific gameweek
app.get("/api/fixtures/gameweek/:gw", async (req, res) => {
  try {
    const gw = parseInt(req.params.gw, 10);
    const season = req.query.season || "2025";

    if (!Number.isInteger(gw)) {
      return res.status(400).json({ message: "Invalid gameweek" });
    }

    const fixtures = await fixturesCollection
      .find({ season, gameweek: gw })
      .sort({ homeTeam: 1 })
      .toArray();

    res.json(fixtures);
  } catch (err) {
    console.error("GET /api/fixtures/gameweek/:gw error:", err);
    res.status(500).json({ message: "Failed to fetch fixtures" });
  }
});

//Needs Work
// GET upcoming fixtures for a specific club - Two routes for fixtures is cleaner
app.get("/api/fixtures/upcoming", async (req, res) => {
  try {
    const team = req.query.team;
    const season = req.query.season || "2025";
    //Stats for gameweek 2 onwards (Current Gameweek)
    const fromGw = parseInt(req.query.fromGw || "2", 10);
    //Next 2 fixtures
    const limit = parseInt(req.query.limit || "2", 10);

    if (!team || typeof team !== "string") {
      return res.status(400).json({ message: "team is required" });
    }

    if (!Number.isInteger(fromGw) || !Number.isInteger(limit)) {
      return res.status(400).json({ message: "fromGw and limit must be valid integers" });
    }

    const fixtures = await fixturesCollection
      .find({
        season,
        gameweek: { $gte: fromGw },
        $or: [
          { homeTeam: team },
          { awayTeam: team }
        ]
      })
      .sort({ gameweek: 1 })
      .limit(limit)
      .toArray();

    res.json(fixtures);
  } catch (err) {
    console.error("GET /api/fixtures/upcoming error:", err);
    res.status(500).json({ message: "Failed to fetch upcoming fixtures" });
  }
});

//Saves past user gameweek scores for reference
app.post("/api/admin/gameweeks/:gameweek/snapshot", async (req, res) => {
  try {
    const gameweek = Number(req.params.gameweek);
    const season = String(req.body.season || "2025");

    const teams = await leaderboardCollection.find({}).toArray();

    for (const team of teams) {
      await saveUserGameweekScore({userTeam: team, season, gameweek});
    }

    res.json({
      ok: true,
      season,
      gameweek,
      count: teams.length
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Failed to snapshot" });
  }
});

// Register + create team in one go
app.post("/api/auth/register-with-team", async (req, res) => {
  const session = client.startSession();

  try {
    let { fname, lname, email, password, teamName, playerIds, slotPlayerIds, formationKey } = req.body;
    // --- Validate required fields ---
    if (!fname || !lname || !email || !password || !teamName || !playerIds) {
      return res.status(400).json({
        message: "fname, lname, email, password, teamName, playerIds are required",
      });
    }

    //Validate email/password
    email = String(email).trim().toLowerCase();
    if (!email.includes("@") || String(password).length < 6) {
      return res.status(400).json({ message: "Invalid email or password too short" });
    }

    //Validate teamName
    if (typeof teamName !== "string" || !teamName.trim()) {
      return res.status(400).json({ message: "Team Name is required" });
    }

    //Validate playerIds
    if (!Array.isArray(playerIds) || playerIds.length !== TEAM_SIZE) {
      return res
        .status(400)
        .json({ message: `playerIds must be an array of ${TEAM_SIZE} items` });
    }

    playerIds = playerIds.map((x) => Number(x));
    if (playerIds.some((x) => !Number.isInteger(x))) {
      return res.status(400).json({ message: "playerIds must be integers" });
    }
    if (new Set(playerIds).size !== playerIds.length) {
      return res.status(400).json({ message: "playerIds must be unique" });
    }

    let initialSlots = null;

    //Putting chosen players in the correct slots on the backend
    if (slotPlayerIds !== undefined) {
      const normalized = normalizeSlotMap(slotPlayerIds);
      if (normalized === null) {
        return res.status(400).json({ message: "slotPlayerIds invalid" });
      }
      initialSlots = normalized;
    }

    const result = await session.withTransaction(async () => {
    const passwordHash = await bcrypt.hash(password, 10);

      //Create user
      const userDoc = {
        fname: fname.trim(),
        lname: lname.trim(),
        email,
        passwordHash,
        teamName: teamName.trim(),
        createdAt: new Date(),
      };

      const userInsert = await usersCollection.insertOne(userDoc, { session });
      const userId = userInsert.insertedId;

     //Create leaderboard team
     let slots = initialSlots ?? Object.fromEntries(SLOT_KEYS.map((k) => [k, null]));

     // Merge constraints (in squad + no duplicates)
     slots = mergeSquadAndSlots(playerIds, slots);

    const teamDoc = {
      userId,
      teamName: teamName.trim(),
      squadPlayerIds: playerIds,
      slotPlayerIds: slots,
      formationKey: formationKey ?? "F442",
      createdAt: new Date(),
      updatedAt: new Date(),
      // points: 0,
    };

      const teamInsert = await leaderboardCollection.insertOne(teamDoc, { session });

      //Create auth response
      const userForToken = { _id: userId, email };
      const token = signToken(userForToken);

      return {
        token,
        user: {
          _id: userId,
          fname: userDoc.fname,
          lname: userDoc.lname,
          email: userDoc.email,
          teamName: userDoc.teamName,
        },
        teamId: teamInsert.insertedId,
      };
    });

    return res.status(201).json({ token: result.token, user: result.user });
  } catch (err) {
    // duplicate email OR duplicate userId in leaderboard (unique indexes)
    if (err?.code === 11000) {
      // Could be users.email or leaderboard.userId
      return res.status(409).json({ message: "Email already registered or team already exists" });
    }

    console.error("POST /api/auth/register-with-team error:", err);
    return res.status(500).json({ message: "Failed to register" });
  } finally {
    await session.endSession();
  }
});

//Login
app.post("/api/auth/login", async (req, res) => {
  try {
    let { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({ message: "email and password are required" });
    }

    email = email.trim().toLowerCase();

    const user = await usersCollection.findOne({ email });
    if (!user) return res.status(401).json({ message: "Invalid credentials" });

    const ok = await bcrypt.compare(password, user.passwordHash);
    if (!ok) return res.status(401).json({ message: "Invalid credentials" });

    const token = signToken(user);

    res.json({
      token,
      user: { _id: user._id, fname: user.fname, lname: user.lname, email: user.email },
    });
  } catch (err) {
    console.error("POST /api/auth/login error:", err);
    res.status(500).json({ message: "Failed to login" });
  }
});

//Update current user's teamName
app.patch("/api/users/me/teamname", requireAuth, async (req, res) => {
  try {
    const { teamName } = req.body;
    if (!teamName || !teamName.trim()) {
      return res.status(400).json({ message: "teamName is required" });
    }

    const userId = new ObjectId(req.auth.userId);

    await usersCollection.updateOne(
      { _id: userId },
      { $set: { teamName: teamName.trim() } }
    );

    res.json({ ok: true, teamName: teamName.trim() });
  } catch (err) {
    console.error("PATCH /api/users/me/teamname error:", err);
    res.status(500).json({ message: "Failed to update team name" });
  }
});


//PATCH update existing team players (save from Transfers Confirm button)
app.patch("/api/leaderboard/me", requireAuth, async (req, res) => {
  console.log("PATCH /api/leaderboard/me Body:", req.body);

  try {
    let { playerIds, squadPlayerIds, slotPlayerIds } = req.body;
    const userId = new ObjectId(req.auth.userId);

   // allow old name (playerIds) for now
let squad = squadPlayerIds ?? playerIds;

// load existing team (needed if client only sent slotPlayerIds)
const existing = await leaderboardCollection.findOne({ userId });
if (!existing) return res.status(404).json({ message: "No team found for user" });

// if squad not provided, fall back to existing squad
if (squad === undefined) {
  squad = existing.squadPlayerIds;
}

// validate squad (either provided by client or taken from existing)
if (!Array.isArray(squad) || squad.length !== TEAM_SIZE) {
  return res.status(400).json({ message: `squadPlayerIds must be an array of ${TEAM_SIZE} items` });
}
squad = squad.map((x) => Number(x));
if (squad.some((x) => !Number.isInteger(x))) {
  return res.status(400).json({ message: "squadPlayerIds must be integers" });
}
if (new Set(squad).size !== squad.length) {
  return res.status(400).json({ message: "squadPlayerIds must be unique" });
}

    // base slots = existing OR empty
    let slots =
    existing.slotPlayerIds && typeof existing.slotPlayerIds === "object"
    ? normalizeSlotMap(existing.slotPlayerIds) // ensures missing keys become null
    : Object.fromEntries(SLOT_KEYS.map((k) => [k, null]));

    // if client sent slots, validate them
    if (slotPlayerIds !== undefined) {
      const normalized = normalizeSlotMap(slotPlayerIds);
      if (normalized === null) {
        return res.status(400).json({ message: "slotPlayerIds invalid" });
      }
      slots = normalized;
    }

    let formationKey = existing.formationKey || "F442";

if (req.body.formationKey !== undefined) {
  const normalized = normalizeFormationKey(req.body.formationKey);
  if (normalized === null) {
    return res.status(400).json({ message: "formationKey invalid" });
  }
  formationKey = normalized;
}

    // merge constraints
    slots = mergeSquadAndSlots(squad, slots);

    // save
    await leaderboardCollection.updateOne(
      { userId },
      {
        $set: {
          squadPlayerIds: squad,
          slotPlayerIds: slots,
          formationKey,
          updatedAt: new Date(),
        },
      }
    );

    res.json({ ok: true, squadPlayerIds: squad, slotPlayerIds: slots, formationKey });
  } catch (err) {
    console.error("PATCH /api/leaderboard/me error:", err);
    res.status(500).json({ message: "Failed to update team" });
  }
});

connectToMongo()
  .then(() => {
    app.listen(PORT, () => {
      console.log(`API running at http://localhost:${PORT}`);
    });
  })
  .catch((err) => {
    console.error("Mongo connection failed:", err);
    process.exit(1);
  });

// Graceful shutdown
process.on("SIGINT", async () => {
  try {
    if (client) await client.close();
  } finally {
    process.exit(0);
  }
});

