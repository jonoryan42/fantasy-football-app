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

dotenv.config();

const app = express();
app.use(cors());
app.use(express.json());

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
  "F442", "F433", "F451", "F532", "F523", "F541", "F343", "F352"
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
  const s = String(v).trim().toUpperCase();
  return FORMATION_KEYS.includes(s) ? s : null;
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

  // Unique Indexes for the users and leaderboard for users and their teams.
  await usersCollection.createIndex({ email: 1 }, { unique: true });
  await leaderboardCollection.createIndex({ userId: 1 }, { unique: true });

  console.log(`Connected to MongoDB`);
  console.log(`   DB: ${DB_NAME}`);
  console.log(`   Collections: leaderboard, users, players, player_gameweek_stats, teams, fixtures`);
}

// Health check (easy to verify server is running)
app.get("/health", (req, res) => {
  res.json({ ok: true, service: "fantasy-football-backend" });
});

//Calculating total points for a player
function calculatePlayerPoints(p) {
  const cleanSheetPts =
    (p.position === "GK" || p.position === "DEF")
      ? (p.cleansheets || 0) * 4
      : 0;

  return (
    (p.goals || 0) * 5 +
    (p.assists || 0) * 3 +
    cleanSheetPts -
    (p.yellows || 0) * 1 -
    (p.reds || 0) * 3 -
    (p.owngoals || 0) * 2
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

//Res.statuses
//200 = Ok request succeeded
//201 = New resource created
//400 = Bad Request
//500 = Internal Server Error

//GET leaderboard with calculated points
app.get("/api/leaderboard", async (req, res) => {
  try {
    const season = req.query.season || "2025";
    const teams = await leaderboardCollection.find({}).toArray();

    const computed = await Promise.all(
      teams.map(async (team) => {
        //Calculating points for the team based on the players in the starting XI and their stats so far for the season
    const starterIds = Object.entries(team.slotPlayerIds || {})
      .filter(([slot, playerId]) => !slot.startsWith("BENCH") && playerId != null)
      .map(([_, playerId]) => Number(playerId));
            const stats = await gameweekStatsCollection
              .find({ season, playerId: { $in: starterIds } })
              .toArray();

          //Adding up total points for the team based on the players in the team and their stats so far for the season
        const points = stats.reduce((sum, s) => sum + (s.points || 0), 0);

        return {
          ...team,
          points,
        };
      })
    );

    computed.sort((a, b) => b.points - a.points);
    res.json(computed);
  } catch (err) {
    console.error("GET /api/leaderboard error:", err);
    res.status(500).json({ message: "Failed to fetch leaderboard" });
  }
});

//Get user team
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

//GET current user
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

// GET teams
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

//POST new team (save from Confirm button)
app.post("/api/leaderboard", requireAuth, async (req, res) => {
  console.log("Content-Type:", req.headers["content-type"]);
  console.log("Body:", req.body);
  console.log("playerIds type:", typeof req.body.playerIds);
  console.log("playerIds isArray:", Array.isArray(req.body.playerIds));

  try {
    let { teamName, playerIds } = req.body;
    const userId = new ObjectId(req.auth.userId);

    // --- Validate team name ---
    if (!teamName || typeof teamName !== "string" || !teamName.trim()) {
      return res.status(400).json({ message: "Team Name is required" });
    }

    // --- Validate playerIds ---
    if (!Array.isArray(playerIds) || playerIds.length !== TEAM_SIZE) {
      return res
        .status(400)
        .json({ message: `playerIds must be an array of ${TEAM_SIZE} items` });
    }

    // Force numeric IDs (handles ["1","2"] etc.)
    playerIds = playerIds.map((x) => Number(x));

    // Validate numeric + integers
    if (playerIds.some((x) => !Number.isInteger(x))) {
      return res.status(400).json({ message: "playerIds must be integers" });
    }

    // Validate no duplicates
    if (new Set(playerIds).size !== playerIds.length) {
      return res.status(400).json({ message: "playerIds must be unique" });
    }

    // Optional: friendly check (unique index still protects you)
    const existing = await leaderboardCollection.findOne({ userId });
    if (existing) {
      return res.status(409).json({ message: "User already has a team" });
    }

    //For managing bench and starter players
    const squadPlayerIds = playerIds; // for now reuse your existing variable name
    let slots = Object.fromEntries(SLOT_KEYS.map((k) => [k, null]));
    if (req.body.slotPlayerIds !== undefined) {
      const normalized = normalizeSlotMap(req.body.slotPlayerIds);
      if (normalized === null) {
        return res.status(400).json({ message: "slotPlayerIds invalid" });
      }
      slots = normalized;
    }
    slots = mergeSquadAndSlots(squadPlayerIds, slots);

    //For the formation for Pick Team
    let formationKey = normalizeFormationKey(req.body.formationKey) || "F442";

    const doc = {
      userId,
      teamName: teamName.trim(),
      squadPlayerIds,
      slotPlayerIds: slots,
      createdAt: new Date(),
      updatedAt: new Date(),
      points: 0,
      formationKey,
    };

    const result = await leaderboardCollection.insertOne(doc);

    res.status(201).json({ ...doc, _id: result.insertedId });
  } catch (err) {
    // Duplicate key (e.g. unique index on userId)
    if (err?.code === 11000) {
      return res.status(409).json({ message: "User already has a team" });
    }

    console.error("POST /api/leaderboard error:", err);
    res.status(500).json({ message: "Failed to save team" });
  }
});

//Signup
app.post("/api/auth/register", async (req, res) => {
  try {
    let { fname, lname, email, password } = req.body;

    if (!fname || !lname || !email || !password) {
      return res.status(400).json({ message: "fname, lname, email, password are required" });
    }

    email = email.trim().toLowerCase();

    if (!email.includes("@") || password.length < 6) {
      return res.status(400).json({ message: "Invalid email or password too short" });
    }

    const passwordHash = await bcrypt.hash(password, 10);

    const doc = {
      fname: fname.trim(),
      lname: lname.trim(),
      email,
      passwordHash,
      createdAt: new Date(),
    };

    const result = await usersCollection.insertOne(doc);

    const user = { _id: result.insertedId, fname: doc.fname, lname: doc.lname, email: doc.email };
    const token = signToken(user);

    res.status(201).json({ token, user });
  } catch (err) {
    // duplicate email error
    if (err?.code === 11000) {
      return res.status(409).json({ message: "Email already registered" });
    }
    console.error("POST /api/auth/register error:", err);
    res.status(500).json({ message: "Failed to register" });
  }
});

// Register + create team in one go (final step)
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

    // --- Validate email/password ---
    email = String(email).trim().toLowerCase();
    if (!email.includes("@") || String(password).length < 6) {
      return res.status(400).json({ message: "Invalid email or password too short" });
    }

    // --- Validate teamName ---
    if (typeof teamName !== "string" || !teamName.trim()) {
      return res.status(400).json({ message: "Team Name is required" });
    }

    // --- Validate playerIds ---
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

    // --- Transaction (best practice) ---
    // Note: transactions require a replica set (Atlas supports this).

    let initialSlots = null;

    if (slotPlayerIds !== undefined) {
      const normalized = normalizeSlotMap(slotPlayerIds);
      if (normalized === null) {
        return res.status(400).json({ message: "slotPlayerIds invalid" });
      }
      initialSlots = normalized;
    }

    const result = await session.withTransaction(async () => {
    const passwordHash = await bcrypt.hash(password, 10);

      // 1) Create user
      const userDoc = {
        fname: fname.trim(),
        lname: lname.trim(),
        email,
        passwordHash,
        teamName: teamName.trim(), // optional but useful
        createdAt: new Date(),
      };

      const userInsert = await usersCollection.insertOne(userDoc, { session });
      const userId = userInsert.insertedId;

     // 2) Create leaderboard team
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

      // 3) Create auth response
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

