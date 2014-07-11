/**
 * 
 */
package mekhq.campaign.mission;

import java.io.PrintWriter;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.Vector;

import megamek.client.RandomNameGenerator;
import megamek.client.RandomSkillsGenerator;
import megamek.client.RandomUnitGenerator;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.bot.princess.HomeEdge;
import megamek.common.Board;
import megamek.common.Compute;
import megamek.common.Crew;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.IStartingPositions;
import megamek.common.Mech;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.PlanetaryConditions;
import megamek.common.Player;
import megamek.common.UnitType;
import megamek.common.loaders.EntityLoadingException;
import mekhq.MekHQ;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.force.Lance;
import mekhq.campaign.market.UnitMarket;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.Planets;
import mekhq.campaign.universe.UnitTableData;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Neoancient
 *
 */
public class AtBScenario extends Scenario {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1148105510264408943L;
	
	public static final int BASEATTACK = 0;
	public static final int EXTRACTION = 1;
	public static final int CHASE = 2;
	public static final int HOLDTHELINE = 3;
	public static final int BREAKTHROUGH = 4;
	public static final int HIDEANDSEEK = 5;	
	public static final int STANDUP = 6;
	public static final int RECONRAID = 7;
	public static final int PROBE = 8;
	
	public static final int SPECIALMISSIONS = 9;
	public static final int OFFICERDUEL = 9;
	public static final int ACEDUEL = 10;
	public static final int AMBUSH = 11;
	public static final int CIVILIANHELP = 12;
	public static final int ALLIEDTRAITORS = 13;
	public static final int PRISONBREAK = 14;
	public static final int STARLEAGUECACHE1 = 15;
	public static final int STARLEAGUECACHE2 = 16;
	
	public static final int BIGBATTLES = 17;
	public static final int ALLYRESCUE = 17;
	public static final int CIVILIANRIOT = 18;
	public static final int CONVOYRESCUE = 19;
	public static final int CONVOYATTACK = 20;
	public static final int PIRATEFREEFORALL = 21;
	
	public static final String[] battleTypes = {"Base Attack", "Extraction",
		"Chase", "Hold the Line", "Breakthrough", "Hide and Seek", "Stand Up",
		"Recon Raid", "Probe", "Officer Duel", "Ace Duel", "Ambush",
		"Civilian Help", "Allied Traitors", "Prison Break",
		"Star League Cache 1", "Star League Cache 2", "Ally Rescue",
		"Civilian Riot", "Convoy Rescue", "Convoy Attack",
		"Pirates Free-for-All"};
	
	public static final int TER_HILLS = 0;
	public static final int TER_BADLANDS = 1;
	public static final int TER_WETLANDS = 2;
	public static final int TER_LIGHTURBAN = 3;
	public static final int TER_FLATLANDS = 4;
	public static final int TER_WOODED = 5;
	public static final int TER_HEAVYURBAN = 6;
	public static final int TER_COASTAL = 7;
	public static final int TER_MOUNTAINS = 8;
	public static final String[] terrainTypes = {"Hills", "Badlands", "Wetlands",
		"Light Urban", "Flatlands", "Wooded", "Heavy Urban", "Coastal",
		"Mountains"
	};	

	public static final int FORCE_MEK = 0;
	public static final int FORCE_VEHICLE = 1;
	public static final int FORCE_MIXED = 2;
	public static final int FORCE_NOVA = 3;
	public static final int FORCE_VEENOVA = 4;
	public static final int FORCE_INFANTRY = 5;
	public static final int FORCE_BA = 6;
	public static final int FORCE_AERO = 7;
	public static final int FORCE_PROTOMEK = 8;
	public static final String[] forceTypeNames = {
		"Mek", "Vehicle", "Mixed", "Nova", "Nova", "Infantry",
		"Battle Armor", "Aerospace", "ProtoMek"
	};
	
	/* The starting position chart in the AtB rules includes the four
	 * corner positions as well, but this creates some conflict with
	 * setting the home edge for the bot, which only includes the four
	 * sides.
	 */
	private static final int [] startPos = {
		Board.START_N, Board.START_E, Board.START_S, Board.START_W
	};

	public static final String[][][] lanceWeights = {
		//IS Lance
		{{"LLLL", "LLLL", "LLLL", "LLLM", "LLLM", "LLMM"},
			{"LLMM", "LMMMM", "LMMM", "MMMM", "MMMM", "MMMH"},
			{"MMHH", "MHHH", "MHHH", "HHHH", "HHHH", "HHHA"},
			{"HHAA", "HHAA", "HAAA", "HAAA", "HAAA", "AAAA"}},
		//Clan Star
		{{"LLLLL", "LLLLL", "LLLLM", "LLLLM", "LLLMM", "LLLMM"},
			{"LLMMM", "LMMMM", "MMMMM", "MMMMM", "MMMMH", "MMMHH"},
			{"MMHHH", "MHHHH", "MHHHH", "HHHHH", "HHHHH", "HHHHA"},
			{"MHHAA", "HHHAA", "HHAAA", "HHAAA", "HHAAA", "AAAAA"}}, 
		//ComStar/WoB Level II
		{{"LLLLLL", "LLLLLL", "LLLLLL", "LLLLMM", "LLLLMM", "LLLMMM"},
			{"LLLMMM", "LLMMMMM", "LMMMM", "MMMMMM", "MMMMMH", "MMMMHH"},
			{"MMMHHH", "MMHHHH", "MHHHHH", "HHHHHH", "HHHHHA", "HHHHAA"},
			{"HHHAAA", "HHHAAA", "HHAAAAA", "HAAAAA", "HAAAAA", "AAAAAA"}}
	};
	
	private int battleType;
	private boolean attacker;
	private int lanceForceId;
	private int terrainType;
	private int light;
	private int weather;
	private int wind;
	private int fog;
	private int atmosphere;
	private float gravity;
	private int start;
	private int deploymentDelay;
	private int mapSizeX, mapSizeY;
	private String map;
	private int lanceCount;	
	private int rerollsRemaining;
		
	ArrayList<Entity> alliesPlayer;
	ArrayList<BotForce> botForces;
	ArrayList<String> alliesPlayerStub;
	ArrayList<BotForceStub> botForceStubs;
		
	/* Special missions cannot generate the enemy until the unit is
	 * added, but needs the Campaign object which is not passed
	 * by addForce or addUnit. Instead we generate all possibilities
	 * (one for each weight class) when the scenario is created and
	 * choose the correct one when a unit is deployed.
	 */
	
	ArrayList<ArrayList<Entity>> specMissionEnemies;
	
	/* Big battles have a similar problem for attached allies. Though
	 * we could generate the maximum number (4) and remove them as
	 * the player deploys additional units, they would be lost if
	 * any units are undeployed.
	 */
	
	ArrayList<Entity> bigBattleAllies;
	
	/* Units that need to be tracked for possible contract breaches
	 * (for destruction), or bonus rolls (for survival).
	 */
	ArrayList<UUID> attachedUnits;
	ArrayList<UUID> survivalBonus;
	
	HashMap<UUID, Entity> entityIds;
	
	public AtBScenario () {
		super();
		lanceForceId = -1;
		alliesPlayer = new ArrayList<Entity>();
		botForces = new ArrayList<BotForce>();
		alliesPlayerStub = new ArrayList<String>();
		botForceStubs = new ArrayList<BotForceStub>();
		attachedUnits = new ArrayList<UUID>();
		survivalBonus = new ArrayList<UUID>();
		entityIds = new HashMap<UUID, Entity>();
		
		light = PlanetaryConditions.L_DAY;
		weather = PlanetaryConditions.WE_NONE;
		wind = PlanetaryConditions.WI_NONE;
		fog = PlanetaryConditions.FOG_NONE;
		atmosphere = PlanetaryConditions.ATMO_STANDARD;
		gravity = (float)1.0;
		deploymentDelay = 0;
		lanceCount = 0;
		rerollsRemaining = 0;
	}

	public AtBScenario (Campaign c, Lance lance, int type, boolean attacker, Date date) {
		super(battleTypes[type] + (attacker?" (Attacker)":" (Defender)"));
		battleType = type;
		this.attacker = attacker;
		if (null == lance) {
			lanceForceId = -1;
		} else {
			this.lanceForceId = lance.getForceId();
		}
		alliesPlayer = new ArrayList<Entity>();
		botForces = new ArrayList<BotForce>();
		alliesPlayerStub = new ArrayList<String>();
		botForceStubs = new ArrayList<BotForceStub>();
		attachedUnits = new ArrayList<UUID>();
		survivalBonus = new ArrayList<UUID>();
		entityIds = new HashMap<UUID, Entity>();
		if (null != lance) {
			for (UUID id : c.getForce(lance.getForceId()).getAllUnits()) {
				entityIds.put(id, c.getUnit(id).getEntity());
			}
		}
		
		light = PlanetaryConditions.L_DAY;
		weather = PlanetaryConditions.WE_NONE;
		wind = PlanetaryConditions.WI_NONE;
		fog = PlanetaryConditions.FOG_NONE;
		atmosphere = PlanetaryConditions.ATMO_STANDARD;
		gravity = (float)1.0;
		deploymentDelay = 0;
		setDate(date);
		lanceCount = 0;
		rerollsRemaining = 0;
		initBattle(c);
	}

	private void initBattle(Campaign c) {
		setTerrain();
		if (c.getCampaignOptions().getUseLightConditions()) {
			setLightConditions();
		}
		if (c.getCampaignOptions().getUseWeatherConditions()) {
			setWeather();
		}
		if (c.getCampaignOptions().getUsePlanetaryConditions() &&
				null != c.getMission(getId())) {
			Planet p = Planets.getInstance().getPlanets().get(c.getMission(getId()).getPlanetName());
			if (null != p) {
				atmosphere = p.getPressure();
				gravity = (float)p.getGravity();
			}
		}
		setMapSize();
		setMapFile();
		if (battleType < SPECIALMISSIONS) {
			lanceCount = 1;
		} else if (battleType >= BIGBATTLES) {
			lanceCount = 2;
		}

		if (null != getLance(c)) {
			getLance(c).refreshCommander(c);
			if (null != getLance(c).getCommander(c).getSkill(SkillType.S_TACTICS)) {
				rerollsRemaining = getLance(c).getCommander(c).getSkill(SkillType.S_TACTICS).getLevel();
			}
		}
	}

	public void setTerrain() {
		final int[] terrainChart = {
				TER_HILLS, TER_BADLANDS, TER_WETLANDS, TER_LIGHTURBAN,
				TER_HILLS, TER_FLATLANDS, TER_WOODED, TER_HEAVYURBAN,
				TER_COASTAL, TER_WOODED, TER_MOUNTAINS
		};
		if (battleType == BASEATTACK)
			terrainType = (Compute.d6() < 4)?TER_LIGHTURBAN:TER_HEAVYURBAN;
		else do {
			terrainType = terrainChart[Compute.d6(2) - 2];
		} while (
				(battleType == PROBE && terrainType == TER_HEAVYURBAN) ||
				(battleType == HIDEANDSEEK &&
					(terrainType == TER_WETLANDS || terrainType == TER_COASTAL ||
						terrainType == TER_FLATLANDS))
			);
	}

	public void setLightConditions() {
		if (battleType == OFFICERDUEL || battleType == ACEDUEL) {
			light = PlanetaryConditions.L_DAY;
			return;
		}
		int roll = Compute.randomInt(10) + 1;
		if (roll < 6) light = PlanetaryConditions.L_DAY;
		else if (roll < 8) light = PlanetaryConditions.L_DUSK;
		else if (roll == 8) light = PlanetaryConditions.L_FULL_MOON;
		else if (roll == 9) light = PlanetaryConditions.L_MOONLESS;
		else light = PlanetaryConditions.L_PITCH_BLACK;
	}

	public void setWeather() {
		if (battleType == OFFICERDUEL || battleType == ACEDUEL) {
			weather = PlanetaryConditions.WE_NONE;
			wind = PlanetaryConditions.WI_NONE;
			fog = PlanetaryConditions.FOG_NONE;
			return;
		}
		int roll = Compute.randomInt(10) + 1;
		int r2 = Compute.d6();
		if (roll < 6) return;
		else if (roll == 6) {
			if (r2 < 4) weather = PlanetaryConditions.WE_LIGHT_RAIN;
			else if (r2 < 6) weather = PlanetaryConditions.WE_MOD_RAIN;
			else weather = PlanetaryConditions.WE_HEAVY_RAIN;
		} else if (roll == 7) {
			if (r2 < 4) weather = PlanetaryConditions.WE_LIGHT_SNOW;
			else if (r2 < 6) weather = PlanetaryConditions.WE_MOD_SNOW;
			else weather = PlanetaryConditions.WE_HEAVY_SNOW;
		} else if (roll == 8) {
			if (r2 < 4) wind = PlanetaryConditions.WI_LIGHT_GALE;
			else if (r2 < 6) wind = PlanetaryConditions.WI_MOD_GALE;
			else wind = PlanetaryConditions.WI_STRONG_GALE;
		} else if (roll == 9) {
			if (r2 == 1) wind = PlanetaryConditions.WI_STORM;
			else if (r2 == 2) weather = PlanetaryConditions.WE_DOWNPOUR;
			else if (r2 == 3) weather = PlanetaryConditions.WE_SLEET;
			else if (r2 == 4) weather = PlanetaryConditions.WE_ICE_STORM;
			else if (r2 == 5) weather = PlanetaryConditions.WI_TORNADO_F13;
			else if (r2 == 6) weather = PlanetaryConditions.WI_TORNADO_F4;
		} else {
			if (r2 < 5) fog = PlanetaryConditions.FOG_LIGHT;
			else fog = PlanetaryConditions.FOG_HEAVY;
		}
	}

	public void setMapSize() {
		int roll = Compute.randomInt(20) + 1;
		if (roll < 6) {
			mapSizeX = 20;
			mapSizeY = 10;
		} else if (roll < 11) {
			mapSizeX = 10;
			mapSizeY = 20;
		} else if (roll < 13) {
			mapSizeX = 30;
			mapSizeY = 10;
		} else if (roll < 15) {
			mapSizeX = 10;
			mapSizeY = 30;
		} else if (roll < 19) {
			mapSizeX = 20;
			mapSizeY = 20;
		} else if (roll == 19) {
			mapSizeX = 40;
			mapSizeY = 10;
		} else {
			mapSizeX = 10;
			mapSizeY = 40;
		}
	}
	
	public int getMapX() {
		if (battleType == BREAKTHROUGH || battleType == CHASE)
			return 18;
		if (battleType == PRISONBREAK || battleType == STARLEAGUECACHE1 ||
				battleType == STARLEAGUECACHE2)
			return 20;
		if (battleType == PIRATEFREEFORALL)
			return 50;
		if (battleType ==ALLYRESCUE || battleType == CONVOYRESCUE ||
				battleType == CIVILIANRIOT)
			return 65;
		if (battleType == CONVOYATTACK)
			return 45;
		
		int base = mapSizeX + 5 * lanceCount;

		if (battleType == BASEATTACK)
			base += 10;
		if (battleType == HIDEANDSEEK)
			base -= 10;
		return (base > 20)? base:20;
	}
	
	public int getMapY() {
		if (battleType == BREAKTHROUGH)
			return 50;
		if (battleType == CHASE)
			return 70;
		if (battleType == PRISONBREAK)
			return 30;
		if (battleType == STARLEAGUECACHE1 ||
				battleType == STARLEAGUECACHE2)
			return 35;
		if (battleType == PIRATEFREEFORALL)
			return 50;
		if (battleType ==ALLYRESCUE || battleType == CONVOYRESCUE)
			return 45;
		if (battleType == CONVOYATTACK || battleType == CIVILIANRIOT)
			return 65;
		
		int base = mapSizeY + 5 * lanceCount;

		if (battleType == BASEATTACK)
			base += 10;
		if (battleType == HIDEANDSEEK)
			base -= 10;
		return (base > 20)? base:20;
	}
	
	public void setMapFile() {
		final String[][] maps = {
			{"Sandy-hills", "Hills-craters", "Hills",
				"Wooded-hills", "Cliffs", "Town-hills"}, //hills
			{"Sandy-valley", "Rocky-valley", "Light-craters",
				"Heavy-craters", "Rubble-mountain", "Cliffs"}, //badlands
			{"Muddy-swamp", "Lake-marsh", "Lake-high",
				"Wooded-lake", "Swamp", "Wooded-swamp"}, //wetlands
			{"Town-mining", "Town-wooded", "Town-generic",
				"Town-farming", "Town-ruin", "Town-mountain"}, //light urban
			{"Savannah", "Dust-bowl", "Sandy-hills",
				"Town-ruin", "Town-generic", "Some-trees"}, //flatlands
			{"Some-trees", "Wooded-lake", "Woods-medium",
				"Wooded-hills", "Woods-deep", "Wooded-valley"}, //wooded
			{"Fortress-city", "Fortress-city", "Town-concrete",
				"Town-concrete", "City-high", "City-dense"}, //heavy urban
			{"River-huge", "Woods-river", "Sandy-river",
				"Rubble-river", "Seaport", "River-wetlands"}, //coastal
			{"Mountain-lake", "Cliffs-lake", "Rubble-mountain",
				"Cliffs", "Mountain-medium", "Mountain-high"} //mountains
		};
		
		if (battleType == OFFICERDUEL || battleType == ACEDUEL ||
				battleType == AMBUSH) {
			map = "Savannah";
			terrainType = TER_FLATLANDS;
		} else if (battleType == STARLEAGUECACHE1 || battleType == STARLEAGUECACHE2) {
			map = "Brian-cache";
			terrainType = TER_HEAVYURBAN;
		} else if (battleType == ALLYRESCUE) {
			map = "Ally-rescue";
		} else if (battleType == CONVOYRESCUE || battleType == CONVOYATTACK) {
			map = "Convoy";
		} else
			map = maps[terrainType][Compute.d6() - 1];
	}
	
	public boolean canRerollTerrain() {
		return canRerollMap();
	}
	
	public boolean canRerollMapSize() {
		return battleType != BREAKTHROUGH && battleType != CHASE &&
				battleType != PRISONBREAK && battleType != PIRATEFREEFORALL &&
				battleType != STARLEAGUECACHE1 && battleType != STARLEAGUECACHE2 &&
				battleType != CONVOYRESCUE && battleType != CONVOYATTACK &&
				battleType != ALLYRESCUE && battleType != CIVILIANRIOT;
	}
	
	public boolean canRerollMap() {
		return battleType != OFFICERDUEL && battleType != ACEDUEL &&
				battleType != STARLEAGUECACHE1 && battleType != STARLEAGUECACHE2 &&
				battleType != ALLYRESCUE && battleType != CONVOYRESCUE &&
				battleType != CONVOYATTACK;
	}
	
	public boolean canRerollLight() {
		return battleType != OFFICERDUEL && battleType != ACEDUEL;
	}
	
	public boolean canRerollWeather() {
		return battleType != OFFICERDUEL && battleType != ACEDUEL;
	}
	
	/* Determines whether an additional unit is qualified to deploy
	 * to this scenario
	 */
	public boolean canDeploy(Unit unit, Campaign campaign) {
		final String[] antiRiotWeapons = {
			"ISERSmallLaser", "Small Laser", "Small Laser Prototype",
			"ISSmallPulseLaser", "ISSmallXPulseLaser", "Small Re-engineered Laser",
			"ISSmallVSPLaser", "CLERMicroLaser", "CLERSmallLaser", 
			"ER Small Laser (CP)", "CLHeavySmallLaser",
			"CLImprovedSmallHeavyLaser", "ClSmall Laser",
			"CLERSmallPulseLaser", "CLMicroPulseLaser", "CLSmallPulseLaser",
			"CLSmallChemLaser",
			"Heavy Machine Gun", "Light Machine Gun", "Machine Gun",
			"Heavy Rifle", "Light Rifle", "Medium Rifle",
			"CLHeavyMG", "CLLightMG", "CLMG",
			"Flamer", "ER Flamer", "CLFlamer", "CLERFlamer",
			"Vehicle Flamer", "Heavy Flamer", "CLVehicleFlamer", "CLHeavyFlamer"
		};

		if (battleType >= BIGBATTLES) {
			if (getForces(campaign).getAllUnits().size() > 7) {
				return false;
			}
			if (battleType == CIVILIANRIOT) {
				for (Part p : unit.getParts()) {
					if (p instanceof EquipmentPart) {
						for (String weapon : antiRiotWeapons) {
							if (((EquipmentPart)p).getType().getInternalName().equals(weapon)) {
								return true;
							}
						}
					}
				}
				return false;
			}
		} else if (battleType > SPECIALMISSIONS) {
			if (getForces(campaign).getAllUnits().size() > 0) {
				return false;
			}
			if (battleType == OFFICERDUEL && unit.getCommander().getRank().isOfficer()) {
				return true;
			}
			if (battleType == ACEDUEL && !unit.getCommander().getRank().isOfficer()) {
				return true;
			}
			if (battleType == PRISONBREAK &&
					unit.getEntity().getWeightClass() > EntityWeightClass.WEIGHT_MEDIUM) {
				return false;
			}
		}
		return true;
	}
	
	public boolean canDeploy(Force force, Campaign c) {
		Vector<UUID> units = force.getAllUnits();
		if (battleType >= BIGBATTLES &&
				getForces(c).getAllUnits().size() + units.size() > 8) {
			return false;
		} else if (battleType >= SPECIALMISSIONS &&
				getForces(c).getAllUnits().size() + units.size() > 0) {
			return false;
		}
		for (UUID id : units) {
			if (!canDeploy(c.getUnit(id), c)) {
				return false;
			}
		}
		return true;
	}
	
	public boolean canDeployUnits(Vector<Unit> units, Campaign c) {
		if (battleType >= BIGBATTLES &&
				getForces(c).getAllUnits().size() + units.size() > 8) {
			return false;
		} else if (battleType >= SPECIALMISSIONS && battleType < BIGBATTLES &&
				getForces(c).getAllUnits().size() + units.size() > 1) {
			return false;
		}
		for (Unit unit : units) {
			if (!canDeploy(unit, c)) {
				return false;
			}
		}
		return true;
	}
	
	public boolean canDeployForces(Vector<Force> forces, Campaign c) {
		int total = 0;
		for (Force force : forces) {
			Vector<UUID> units = force.getAllUnits();
			total += units.size();
			if (battleType >= BIGBATTLES &&
					getForces(c).getAllUnits().size() + units.size() > 8) {
				return false;
			} else if (battleType >= SPECIALMISSIONS &&
					getForces(c).getAllUnits().size() + units.size() > 0) {
				return false;
			}
			for (UUID id : units) {
				if (!canDeploy(c.getUnit(id), c)) {
					return false;
				}
			}
		}
		if (battleType >= BIGBATTLES &&
				getForces(c).getAllUnits().size() + total > 8) {
			return false;
		} else if (battleType >= SPECIALMISSIONS &&
				getForces(c).getAllUnits().size() + total > 0) {
			return false;
		}
		return true;
	}
	
	/* Corrects the enemy (special missions) and allies (big battles)
	 * as necessary based on player deployments. This ought to be called
	 * when the scenario details are displayed or the scenario is started.
	 */
	public void refresh(Campaign c) {
		if (battleType < SPECIALMISSIONS) {
			return;
		}
		Vector<UUID> deployed = getForces(c).getAllUnits();
		if (battleType >= BIGBATTLES) {
			int numAllies = Math.min(4, 8 - deployed.size());
			alliesPlayer.clear();
			for (int i = 0; i < numAllies; i++) {
				alliesPlayer.add(bigBattleAllies.get(i));
			}
		} else {
			if (deployed.size() == 0) {
				return;
			}
			int weight = c.getUnit(deployed.get(0)).getEntity().getWeightClass() - 1;
			botForces.get(0).setEntityList(specMissionEnemies.get(weight));
		}
	}
	
	public void setForces(Campaign c) {
		if (battleType < SPECIALMISSIONS) {
			setStandardBattleForces(c);
		} else if (battleType < BIGBATTLES) {
			setSpecialMissionForces(c);
		} else {
			setBigBattleForces(c);
		}
	}
	
	private void setStandardBattleForces(Campaign campaign) {
		/* Find the number of attached units required by the command rights clause */
		int attachedUnitWeight = UnitTableData.WT_MEDIUM;
		if (getLance(campaign).getRole() == Lance.ROLE_SCOUT || getLance(campaign).getRole() == Lance.ROLE_TRAINING) {
			attachedUnitWeight = UnitTableData.WT_LIGHT;
		}
		int numAttachedPlayer = 0;
		int numAttachedBot = 0;
		if (getContract(campaign).getMissionType() == AtBContract.MT_CADREDUTY) {
			numAttachedPlayer = 3;
		} else if (campaign.getFactionCode().equals("MERC")) {
			if (getContract(campaign).getCommandRights() == Contract.COM_INTEGRATED) {
				numAttachedBot = 2;
			}
			if (getContract(campaign).getCommandRights() == Contract.COM_HOUSE) {
				numAttachedBot = 1;
			}
			if (getContract(campaign).getCommandRights() == Contract.COM_LIAISON) {
				numAttachedPlayer = 1;
			}
		}
		
		/* The entities in the attachedAllies list will be added to the player's forces
		 * in MM and don't require a separate BotForce */
		for (int i = 0; i < numAttachedPlayer; i++) {
			Entity en = getEntity(getContract(campaign).getEmployerCode(),
					getContract(campaign).getAllySkill(), getContract(campaign).getAllyQuality(),
					UnitTableData.UNIT_MECH, attachedUnitWeight, campaign);
			if (null != en) {
				alliesPlayer.add(en);
				attachedUnits.add(UUID.fromString(en.getExternalIdAsString()));
			} else {
				System.out.println("Entity for player-controlled allies is null");
			}
		}
		
		/* The allyBot list will be passed to the BotForce constructor */
		ArrayList<Entity> allyBot = new ArrayList<Entity>();
		for (int i = 0; i < numAttachedBot; i++) {
			Entity en = getEntity(getContract(campaign).getEmployerCode(),
					getContract(campaign).getAllySkill(), getContract(campaign).getAllyQuality(),
					UnitTableData.UNIT_MECH, attachedUnitWeight, campaign);
			if (null != en) {
				allyBot.add(en);
				attachedUnits.add(UUID.fromString(en.getExternalIdAsString()));
			} else {
				System.err.println("Entity for ally bot is null");
			}
		}
		
		/* The enemy list represents the main enemy force and will be
		 * filled according to the parameters of each battle type. It
		 * is maintained separately from the generic botForce so
		 * that reinforcement may be added at the end of the method.
		 */
		ArrayList<Entity> enemy = new ArrayList<Entity>();
		int enemyStart;
		int otherStart;
		ArrayList<Entity> otherForce;
		BotForce botForce;
		
		switch (battleType) {
		case HIDEANDSEEK:
			if (attacker) {
				start = startPos[Compute.randomInt(4)];
				enemyStart = Board.START_CENTER;
			} else {
				start = Board.START_CENTER;
				enemyStart = startPos[Compute.randomInt(4)];
			}
			if (allyBot.size() > 0) {
				botForces.add(getAllyBotForce(getContract(campaign), start, allyBot));
			}
			if (attacker) {
				addEnemyForce(enemy, getLance(campaign).getWeightClass(campaign),
						UnitTableData.WT_ASSAULT, 2, 0, campaign);
			} else {
				addEnemyForce(enemy, getLance(campaign).getWeightClass(campaign),	
						UnitTableData.WT_HEAVY, 0, 0, campaign);
			}
			botForces.add(getEnemyBotForce(getContract(campaign), enemyStart, enemy));
			break;
		case HOLDTHELINE:
			if (attacker) {
				start = startPos[Compute.randomInt(4)];
				enemyStart = Board.START_CENTER;
			} else {
				start = Board.START_CENTER;
				enemyStart = startPos[Compute.randomInt(4)];
			}

			if (allyBot.size() > 0) {
				botForces.add(getAllyBotForce(getContract(campaign), start, allyBot));
			}
			if (attacker) {
				addEnemyForce(enemy, getLance(campaign).getWeightClass(campaign),
						UnitTableData.WT_ASSAULT, 0, 0, campaign);
			} else {
				addEnemyForce(enemy, getLance(campaign).getWeightClass(campaign),
						UnitTableData.WT_ASSAULT, 4, 0, campaign);
			}
			botForces.add(getEnemyBotForce(getContract(campaign), enemyStart, enemy));
			break;
		case EXTRACTION:
			if (attacker) {
				start = startPos[Compute.randomInt(4)];
				enemyStart = Board.START_CENTER;
				otherStart = start + 4;
			} else {
				start = Board.START_CENTER;
				enemyStart = startPos[Compute.randomInt(4)];
				otherStart = enemyStart + 4;
			}
			if (otherStart > 8) {
				otherStart -= 8;
			}

			if (allyBot.size() > 0) {
				botForces.add(getAllyBotForce(getContract(campaign), start, allyBot));
			}
			addEnemyForce(enemy, getLance(campaign).getWeightClass(campaign), campaign);
			botForces.add(getEnemyBotForce(getContract(campaign), enemyStart, enemy));
			
			otherForce = new ArrayList<Entity>();
			addCivilianUnits(otherForce, 4, campaign);
			if (attacker) {
				botForces.add(new BotForce("Civilians", 1,
						otherStart, otherForce));
				for (Entity en : otherForce) {
					survivalBonus.add(UUID.fromString(en.getExternalIdAsString()));
				}
			} else {
				botForces.add(new BotForce("Civilians", 2,
						otherStart, otherForce));
			}
			break;
		case BREAKTHROUGH:
			if (attacker) {
				start = Board.START_S;
				enemyStart = Board.START_CENTER;
			} else {
				start = Board.START_CENTER;
				enemyStart = Board.START_S;
			}

			if (allyBot.size() > 0) {
				botForces.add(getAllyBotForce(getContract(campaign), start, allyBot));
			}
						
			addEnemyForce(enemy, getLance(campaign).getWeightClass(campaign), campaign);
			botForce = getEnemyBotForce(getContract(campaign), enemyStart, enemy);
			if (!attacker) {
				BehaviorSettings bs = botForce.getBehaviorSettings();
				bs.setForcedWithdrawal(true);
				bs.setAutoFlee(true);
				bs.setHomeEdge(HomeEdge.NORTH);
			}
			botForces.add(botForce);
			
			break;
		case CHASE:
			start = Board.START_S;
			enemyStart = Board.START_S;

			if (allyBot.size() > 0) {
				botForces.add(getAllyBotForce(getContract(campaign), start, allyBot));
			}

			addEnemyForce(enemy, getLance(campaign).getWeightClass(campaign),
					UnitTableData.WT_ASSAULT, 0, -1, campaign);
			addEnemyForce(enemy, getLance(campaign).getWeightClass(campaign),
					UnitTableData.WT_ASSAULT, 0, -1, campaign);
			botForce = getEnemyBotForce(getContract(campaign), enemyStart, enemy);
			if (!attacker) {
				BehaviorSettings bs = botForce.getBehaviorSettings();
				bs.setForcedWithdrawal(true);
				bs.setAutoFlee(true);
				bs.setHomeEdge(HomeEdge.NORTH);
			}
			botForces.add(botForce);
			
			break;
		case PROBE:
			start = startPos[Compute.randomInt(4)];
			enemyStart = start + 4;
			if (enemyStart > 8) {
				enemyStart -= 8;
			}

			if (allyBot.size() > 0) {
				botForces.add(getAllyBotForce(getContract(campaign), start, allyBot));
			}
			addEnemyForce(enemy, getLance(campaign).getWeightClass(campaign),
					UnitTableData.WT_MEDIUM, 0, 0, campaign);
			botForces.add(getEnemyBotForce(getContract(campaign), enemyStart, enemy));
			break;
		case RECONRAID:
			if (attacker) {
				start = startPos[Compute.randomInt(4)];
				enemyStart = Board.START_CENTER;
			} else {
				start = Board.START_CENTER;
				enemyStart = startPos[Compute.randomInt(4)];
			}

			if (allyBot.size() > 0) {
				botForces.add(getAllyBotForce(getContract(campaign), start, allyBot));
			}
			addEnemyForce(enemy, getLance(campaign).getWeightClass(campaign),
					UnitTableData.WT_MEDIUM, 0, 0, campaign);
			botForces.add(getEnemyBotForce(getContract(campaign), enemyStart, enemy));
			break;			
		case BASEATTACK:
			if (attacker) {
				start = startPos[Compute.randomInt(4)];
				enemyStart = Board.START_CENTER;
			} else {
				start = Board.START_CENTER;
				enemyStart = startPos[Compute.randomInt(4)];
			}

			/* Ally deploys 2 lances of a lighter weight class than the player */
			int allyForce = Math.max(getLance(campaign).getWeightClass(campaign) - 1, 0);
			addLance(allyBot, getContract(campaign).getEmployerCode(),
					getContract(campaign).getAllySkill(), getContract(campaign).getAllyQuality(),
					allyForce, campaign);
			addLance(allyBot, getContract(campaign).getEmployerCode(),
					getContract(campaign).getAllySkill(), getContract(campaign).getAllyQuality(),
					allyForce, campaign);
			botForces.add(getAllyBotForce(getContract(campaign), start, allyBot));
			
			/* Roll 2x on bot lances roll */
			addEnemyForce(enemy, getLance(campaign).getWeightClass(campaign), campaign);
			addEnemyForce(enemy, getLance(campaign).getWeightClass(campaign), campaign);
			botForces.add(getEnemyBotForce(getContract(campaign), enemyStart, enemy));
						
			otherForce = new ArrayList<Entity>();
			addCivilianUnits(otherForce, 10, campaign);
			botForces.add(new BotForce("Civilians", attacker?2:1,
					attacker?enemyStart:start, otherForce));
			//TODO: add 6 turrets to defender
			break;
		case STANDUP:
		default:
			start = startPos[Compute.randomInt(4)];
			enemyStart = start + 4;
			if (enemyStart > 8) {
				enemyStart -= 8;
			}
			if (allyBot.size() > 0) {
				botForces.add(getAllyBotForce(getContract(campaign), start, allyBot));
			}
			addEnemyForce(enemy, getLance(campaign).getWeightClass(campaign), campaign);
			botForces.add(getEnemyBotForce(getContract(campaign), enemyStart, enemy));
			break;
		}
		/* Possible enemy reinforcements */
		int roll = Compute.d6();
		if (roll == 6) {
			addLance(enemy, getContract(campaign).getEnemyCode(),
					getContract(campaign).getEnemySkill(), getContract(campaign).getEnemyQuality(),
					UnitTableData.WT_MEDIUM, UnitTableData.WT_ASSAULT, campaign, 8);
		} else if (roll > 3) {
			addLance(enemy, getContract(campaign).getEnemyCode(),
					getContract(campaign).getEnemySkill(), getContract(campaign).getEnemyQuality(),
					UnitTableData.WT_LIGHT, UnitTableData.WT_ASSAULT, campaign, 6);
		}
		
		if (campaign.getCampaignOptions().getUseDropShips()) {
			roll = Compute.d6();
			if ((battleType == STANDUP && roll <= 2) ||
					(battleType == HOLDTHELINE && attacker && roll == 1) ||
					(battleType == BREAKTHROUGH && !attacker && roll == 1) ||
					(battleType == RECONRAID && attacker && roll <= 3)) {
				boolean dropshipFound = false;
				for (UUID id : campaign.getForces().getAllUnits()) {
					if ((campaign.getUnit(id).getEntity().getEntityType() & Entity.ETYPE_DROPSHIP) != 0 &&
							campaign.getUnit(id).isAvailable()) {
						addUnit(id);
						campaign.getUnit(id).setScenarioId(getId());
						dropshipFound = true;
						break;
					}
				}
				if (!dropshipFound) {
					Entity dropship = getDropship("Leopard (2537)",
							getContract(campaign).getEmployerCode(), campaign);
					alliesPlayer.add(dropship);
				}
				for (int i = 0; i < Compute.d6() - 3; i++) {
					addLance(enemy, getContract(campaign).getEnemyCode(),
							getContract(campaign).getEnemySkill(), getContract(campaign).getEnemyQuality(),
							UnitMarket.getRandomWeight(UnitTableData.UNIT_MECH, getContract(campaign).getEnemyCode(),
								campaign.getCampaignOptions().getRegionalMechVariations()),
							UnitTableData.WT_ASSAULT, campaign, 6);
				}
			}
		}
	}
	
	private void setSpecialMissionForces(Campaign campaign) {
		//enemy must always be the first on the botforce list so we can find it on refresh()
		specMissionEnemies = new ArrayList<ArrayList<Entity>>();
		ArrayList<Entity> enemy;
		int weight;
		int enemyStart;
		ArrayList<Entity> otherForce;
		
		switch (battleType) {
		case OFFICERDUEL: case ACEDUEL:
			start = startPos[Compute.randomInt(4)];
			enemyStart = start + 4;
			if (enemyStart > 8) {
				enemyStart -= 8;
			}
			for (weight = UnitTableData.WT_LIGHT; weight <= UnitTableData.WT_ASSAULT; weight++) {
				enemy = new ArrayList<Entity>();
				Entity en = getEntity(getContract(campaign).getEnemyCode(),
						getContract(campaign).getEnemySkill(), getContract(campaign).getEnemyQuality(),
						UnitTableData.UNIT_MECH, 
						Math.min(weight + 1, UnitTableData.WT_ASSAULT),
						campaign);
				if (weight == UnitTableData.WT_ASSAULT) {
					en.getCrew().setGunnery(en.getCrew().getGunnery() - 1);
					en.getCrew().setPiloting(en.getCrew().getPiloting() - 1);
				}
				enemy.add(en);
				specMissionEnemies.add(enemy);
			}
			botForces.add(getEnemyBotForce(getContract(campaign), enemyStart, specMissionEnemies.get(0)));
			break;
		case AMBUSH:
			start = Board.START_CENTER;
			enemyStart = Board.START_CENTER;
			for (weight = UnitTableData.WT_LIGHT; weight <= UnitTableData.WT_ASSAULT; weight++) {
				enemy = new ArrayList<Entity>();
				if (weight == UnitTableData.WT_LIGHT) {
					enemy.add(getEntity(getContract(campaign).getEnemyCode(),
							getContract(campaign).getEnemySkill(), getContract(campaign).getEnemyQuality(),
							UnitTableData.UNIT_MECH, weight, campaign));
					enemy.add(getEntity(getContract(campaign).getEnemyCode(),
							getContract(campaign).getEnemySkill(), getContract(campaign).getEnemyQuality(),
							UnitTableData.UNIT_MECH, weight, campaign));
				} else {
				for (int i = 0; i < 3; i++)
					enemy.add(getEntity(getContract(campaign).getEnemyCode(),
							getContract(campaign).getEnemySkill(), getContract(campaign).getEnemyQuality(),
							UnitTableData.UNIT_MECH, weight - 1, campaign));
				}
				specMissionEnemies.add(enemy);
			}
			botForces.add(getEnemyBotForce(getContract(campaign), enemyStart, specMissionEnemies.get(0)));
			break;
		case CIVILIANHELP:
			start = startPos[Compute.randomInt(4)];
			enemyStart = start + 4;
			if (enemyStart > 8) {
				enemyStart -= 8;
			}
			for (weight = UnitTableData.WT_LIGHT; weight <= UnitTableData.WT_ASSAULT; weight++) {
				enemy = new ArrayList<Entity>();
				for (int i = 0; i < 3; i++)
					enemy.add(getEntity(getContract(campaign).getEnemyCode(),
							getContract(campaign).getEnemySkill(), getContract(campaign).getEnemyQuality(),
							UnitTableData.UNIT_MECH, weight, campaign));
				specMissionEnemies.add(enemy);
			}
			botForces.add(getEnemyBotForce(getContract(campaign), enemyStart, specMissionEnemies.get(0)));
			
			otherForce = new ArrayList<Entity>();
			addCivilianUnits(otherForce, 4, campaign);
			for (Entity e : otherForce) {
				survivalBonus.add(UUID.fromString(e.getExternalIdAsString()));
			}
			botForces.add(new BotForce("Civilians", 1, start, otherForce));
			break;
		case ALLIEDTRAITORS:
			start = Board.START_CENTER;
			enemyStart = Board.START_CENTER;
			
			for (weight = UnitTableData.WT_LIGHT; weight <= UnitTableData.WT_ASSAULT; weight++) {
				enemy = new ArrayList<Entity>();
				enemy.add(getEntity(getContract(campaign).getEmployerCode(),
						getContract(campaign).getAllySkill(), getContract(campaign).getAllyQuality(),
						UnitTableData.UNIT_MECH, weight, campaign));
				enemy.add(getEntity(getContract(campaign).getEmployerCode(),
						getContract(campaign).getAllySkill(), getContract(campaign).getAllyQuality(),
						UnitTableData.UNIT_MECH, weight, campaign));
				specMissionEnemies.add(enemy);
			}
			botForces.add(new BotForce(getContract(campaign).getAllyBotName(),
					2, enemyStart, specMissionEnemies.get(0)));
			break;
		case PRISONBREAK:
			start = Board.START_CENTER;
			enemyStart = startPos[Compute.randomInt(4)];

			for (weight = UnitTableData.WT_LIGHT; weight <= UnitTableData.WT_ASSAULT; weight++) {
				enemy = new ArrayList<Entity>();
				for (int i = 0; i < 3; i++)
					enemy.add(getEntity(getContract(campaign).getEnemyCode(),
							getContract(campaign).getEnemySkill(), getContract(campaign).getEnemyQuality(),
							UnitTableData.UNIT_MECH, weight, campaign));
				specMissionEnemies.add(enemy);
			}
			botForces.add(new BotForce("Guards", 2, enemyStart, specMissionEnemies.get(0)));
			
			otherForce = new ArrayList<Entity>();
			addCivilianUnits(otherForce, 4, campaign);
			for (Entity e : otherForce) {
				survivalBonus.add(UUID.fromString(e.getExternalIdAsString()));			}
			botForces.add(new BotForce("Prisoners", 1, start, otherForce));
			break;
		case STARLEAGUECACHE1:
			start = Board.START_CENTER;
			enemyStart = Board.START_N;
			
			int roll = Compute.d6();
			String rat;
			if (roll == 1) {
				rat = "CivilianUnits_PrimMech";
			} else {
				for (weight = UnitTableData.WT_LIGHT; weight <= UnitTableData.WT_ASSAULT; weight++) {
					enemy = new ArrayList<Entity>();
					for (int i = 0; i < 3; i++) {
						enemy.add(getEntity(getContract(campaign).getEnemyCode(),
								getContract(campaign).getEnemySkill(), getContract(campaign).getEnemyQuality(),
								UnitTableData.UNIT_MECH, weight, campaign));
					}
					specMissionEnemies.add(enemy);
				}
				botForces.add(getEnemyBotForce(getContract(campaign), enemyStart, specMissionEnemies.get(0)));

				/* Bad form to override the settings, but if none of the rats in
				 * the options include Star League tables, the point of this mission
				 * is thwarted.
				 */
				UnitTableData.FactionTables ft = UnitTableData.getInstance().getRAT("Xotl", 2750, "SL");
				if (null == ft) {
					ft = UnitTableData.getInstance().getBestRAT(campaign.getCampaignOptions().getRATs(),
						2780, "SL", UnitTableData.UNIT_MECH);
				}
				rat = ft.getTable(UnitTableData.UNIT_MECH, UnitMarket.getRandomMechWeight(),
						(roll == 6)?IUnitRating.DRAGOON_A:IUnitRating.DRAGOON_D);
			}

			otherForce = new ArrayList<Entity>();
			Entity e = getUnitFromRat(rat, campaign.getFactionCode(),
					RandomSkillsGenerator.L_REG, campaign);
			otherForce.add(e);
			//TODO: During SW offer a choice between an employer exchange or a contract breach
			Loot loot = new Loot();
			loot.addUnit(e);
			getLoot().add(loot);
			botForces.add(new BotForce("Tech", 1, start, otherForce));
			
			break;
		case STARLEAGUECACHE2:
			start = Board.START_N;
			enemyStart = Board.START_S;
			
			UnitTableData.FactionTables ft = UnitTableData.getInstance().getRAT("Xotl", 2750, "SL");
			if (null == ft) {
				ft = UnitTableData.getInstance().getBestRAT(campaign.getCampaignOptions().getRATs(),
						2780, "SL", UnitTableData.UNIT_MECH);
			}

			for (weight = UnitTableData.WT_LIGHT; weight <= UnitTableData.WT_ASSAULT; weight++) {
				enemy = new ArrayList<Entity>();
				rat = ft.getTable(UnitTableData.UNIT_MECH,
						(battleType == STARLEAGUECACHE1)?UnitMarket.getRandomMechWeight():weight,
								(Compute.d6() == 6)?IUnitRating.DRAGOON_A:IUnitRating.DRAGOON_D);
	
				enemy.add(getUnitFromRat(rat, getContract(campaign).getEnemyCode(),
							getContract(campaign).getEnemySkill(), campaign));
				specMissionEnemies.add(enemy);
			}
			botForces.add(getEnemyBotForce(getContract(campaign), enemyStart, specMissionEnemies.get(0)));
			break;
		}
	}

	private void setBigBattleForces(Campaign campaign) {
		ArrayList<Entity> enemy = new ArrayList<Entity>();
		ArrayList<Entity> otherForce;
		
		final int attached = 4;

		switch (battleType) {
		case ALLYRESCUE:
			start = Board.START_S;
			deploymentDelay = 12;
			for (int i = 0; i < attached; i++) {
				alliesPlayer.add(getEntity(getContract(campaign).getEmployerCode(),
						getContract(campaign).getAllySkill(), getContract(campaign).getAllyQuality(),
						UnitTableData.UNIT_MECH,
						UnitMarket.getRandomWeight(UnitTableData.UNIT_MECH, getContract(campaign).getEmployerCode(),
								campaign.getCampaignOptions().getRegionalMechVariations()),
						campaign));
			}
			
			otherForce = new ArrayList<Entity>();
			for (int i = 0; i < 8; i++) {
				otherForce.add(getEntity(getContract(campaign).getEmployerCode(),
						getContract(campaign).getAllySkill(), getContract(campaign).getAllyQuality(),
						UnitTableData.UNIT_MECH, UnitMarket.getRandomAeroWeight(), // max heavy
						campaign));
			}
			botForces.add(new BotForce(getContract(campaign).getAllyBotName(),
					1, Board.START_CENTER, otherForce));
			
			for (int i = 0; i < 12; i++) {
				enemy.add(getEntity(getContract(campaign).getEnemyCode(),
						getContract(campaign).getEnemySkill(), getContract(campaign).getEnemyQuality(),
						UnitTableData.UNIT_MECH,
						UnitMarket.getRandomAeroWeight() + 1, // no light 'Mechs
						campaign));
			}
			botForces.add(getEnemyBotForce(getContract(campaign), Board.START_N, enemy));
			break;
		case CIVILIANRIOT:
			start = Board.START_S;
			//TODO: only units with machine guns, flamers, or sm lasers
			for (int i = 0; i < attached; i++) {
				alliesPlayer.add(getEntity(getContract(campaign).getEmployerCode(),
						getContract(campaign).getAllySkill(), getContract(campaign).getAllyQuality(),
						UnitTableData.UNIT_MECH,
						(Compute.randomInt(7) < 3)?UnitTableData.WT_LIGHT:UnitTableData.WT_MEDIUM,
						campaign));
			}
			
			otherForce = new ArrayList<Entity>();
			addCivilianUnits(otherForce, 8, campaign);
			for (Entity e : otherForce) {
				survivalBonus.add(UUID.fromString(e.getExternalIdAsString()));
			}
			botForces.add(new BotForce("Loyalists", 1, Board.START_CENTER, otherForce));

			otherForce = new ArrayList<Entity>();
			addCivilianUnits(otherForce, 12, campaign);
			botForces.add(new BotForce("Rioters", 2, Board.START_CENTER, otherForce));

			for (int i = 0; i < 3; i++) {
				//3 mech rebel lance, use employer RAT, enemy skill
				enemy.add(getEntity(getContract(campaign).getEmployerCode(),
						getContract(campaign).getEnemySkill(), IUnitRating.DRAGOON_F,
						UnitTableData.UNIT_MECH,
						Compute.d6() < 4?UnitTableData.WT_LIGHT:UnitTableData.WT_MEDIUM,
						campaign));
			}
			botForces.add(new BotForce("Rebels", 2, Board.START_N, enemy));
			break;
		case CONVOYRESCUE:
			start = Board.START_N;
			deploymentDelay = 7;
			for (int i = 0; i < attached; i++) {
				alliesPlayer.add(getEntity(getContract(campaign).getEmployerCode(),
						getContract(campaign).getAllySkill(), getContract(campaign).getAllyQuality(),
						UnitTableData.UNIT_MECH, UnitTableData.WT_LIGHT, campaign));
			}
			
			otherForce = new ArrayList<Entity>();
			addCivilianUnits(otherForce, 12, campaign);
			for (Entity e : otherForce) {
				survivalBonus.add(UUID.fromString(e.getExternalIdAsString()));
			}
			botForces.add(new BotForce("Convoy", 1, Board.START_CENTER, otherForce));
			
			for (int i = 0; i < 12; i++) {
				enemy.add(getEntity(getContract(campaign).getEnemyCode(),
						getContract(campaign).getEnemySkill(), getContract(campaign).getEnemyQuality(),
						UnitTableData.UNIT_MECH,
						UnitMarket.getRandomWeight(UnitTableData.UNIT_MECH, getContract(campaign).getEnemyCode(),
								campaign.getCampaignOptions().getRegionalMechVariations()),
						campaign));
			}
			botForces.add(getEnemyBotForce(getContract(campaign), Board.START_S, enemy));
			break;
		case CONVOYATTACK:
			start = Board.START_S;

			for (int i = 0; i < attached; i++) {
				alliesPlayer.add(getEntity(getContract(campaign).getEmployerCode(),
						getContract(campaign).getAllySkill(), getContract(campaign).getAllyQuality(),
						UnitTableData.UNIT_MECH, UnitTableData.WT_LIGHT, campaign));
			}
			
			otherForce = new ArrayList<Entity>();
			addCivilianUnits(otherForce, 12, campaign);
			botForces.add(new BotForce("Convoy", 2, Board.START_CENTER, otherForce));
			
			for (int i = 0; i < 8; i++) {
				enemy.add(getEntity(getContract(campaign).getEnemyCode(),
						getContract(campaign).getEnemySkill(), getContract(campaign).getEnemyQuality(),
						UnitTableData.UNIT_MECH,
						UnitMarket.getRandomWeight(UnitTableData.UNIT_MECH, getContract(campaign).getEnemyCode(),
								campaign.getCampaignOptions().getRegionalMechVariations()),
						campaign));
			}
			botForces.add(getEnemyBotForce(getContract(campaign), Board.START_CENTER, enemy));
			break;
		case PIRATEFREEFORALL:
			start = Board.START_CENTER;
			for (int i = 0; i < attached; i++) {
				alliesPlayer.add(getEntity(getContract(campaign).getEmployerCode(),
						getContract(campaign).getAllySkill(), getContract(campaign).getAllyQuality(),
						UnitTableData.UNIT_MECH, UnitMarket.getRandomAeroWeight(), // max heavy
						campaign));
			}
			for (int i = 0; i < 12; i++) {
				enemy.add(getEntity(getContract(campaign).getEnemyCode(),
						getContract(campaign).getEnemySkill(), getContract(campaign).getEnemyQuality(),
						UnitTableData.UNIT_MECH, 
						UnitMarket.getRandomWeight(UnitTableData.UNIT_MECH, getContract(campaign).getEnemyCode(),
								campaign.getCampaignOptions().getRegionalMechVariations()),
						campaign));
			}
			botForces.add(getEnemyBotForce(getContract(campaign), Board.START_N, enemy));

			otherForce = new ArrayList<Entity>();
			for (int i = 0; i < 12; i++) {
				otherForce.add(getEntity("PIR", 
								RandomSkillsGenerator.L_REG, IUnitRating.DRAGOON_C,
								UnitTableData.UNIT_MECH,
								UnitMarket.getRandomMechWeight(),
								campaign));
			}
			botForces.add(new BotForce("Pirates", 3, Board.START_S, enemy));
			break;
		}
		bigBattleAllies = new ArrayList<Entity>();
		for (Entity en : alliesPlayer) {
			bigBattleAllies.add(en);
		}
	}
	
	private void addEnemyForce(ArrayList<Entity> list, int weightClass, Campaign c) {
		addEnemyForce(list, weightClass, UnitTableData.WT_ASSAULT, 0, 0, c);
	}
	
	private void addEnemyForce(ArrayList<Entity> list, int weightClass,
			int maxWeight, int rollMod, int weightMod, Campaign c) {
		int roll = Compute.randomInt(20) + 1;
		roll += (c.getCampaignOptions().getSkillLevel() - 2) * 5;
		roll += rollMod;
		switch (weightClass) {
		case EntityWeightClass.WEIGHT_ULTRA_LIGHT:
		case EntityWeightClass.WEIGHT_LIGHT:
			if (roll < 1) {
				addEnemyLance(list, UnitTableData.WT_LIGHT + weightMod, maxWeight, c);
			} else if (roll <= 10) {
				addEnemyLance(list, UnitTableData.WT_MEDIUM + weightMod, maxWeight, c);
			} else if (roll < 17) {
				addEnemyLance(list, UnitTableData.WT_LIGHT + weightMod, maxWeight, c);
				addEnemyLance(list, UnitTableData.WT_LIGHT + weightMod, maxWeight, c);
			} else if (roll < 21) {
				addEnemyLance(list, UnitTableData.WT_HEAVY + weightMod, maxWeight, c);
			} else {
				addEnemyLance(list, UnitTableData.WT_MEDIUM + weightMod, maxWeight, c);
				addEnemyLance(list, UnitTableData.WT_LIGHT + weightMod, maxWeight, c);
			}
			break;
		case EntityWeightClass.WEIGHT_MEDIUM:
			if (roll < 1) {
				addEnemyLance(list, UnitTableData.WT_MEDIUM + weightMod, maxWeight, c);
			} else if (roll < 6) {
				addEnemyLance(list, UnitTableData.WT_LIGHT + weightMod, maxWeight, c);
				addEnemyLance(list, UnitTableData.WT_LIGHT + weightMod, maxWeight, c);
			} else if (roll < 11) {
				addEnemyLance(list, UnitTableData.WT_HEAVY + weightMod, maxWeight, c);
			} else if (roll < 21) {
				addEnemyLance(list, UnitTableData.WT_MEDIUM + weightMod, maxWeight, c);
				addEnemyLance(list, UnitTableData.WT_LIGHT + weightMod, maxWeight, c);
			} else {
				addEnemyLance(list, UnitTableData.WT_MEDIUM + weightMod, maxWeight, c);
				addEnemyLance(list, UnitTableData.WT_MEDIUM + weightMod, maxWeight, c);
			}
			break;
		case EntityWeightClass.WEIGHT_HEAVY:
			if (roll < 1) {
				addEnemyLance(list, UnitTableData.WT_MEDIUM + weightMod, maxWeight, c);
				addEnemyLance(list, UnitTableData.WT_LIGHT + weightMod, maxWeight, c);
			} else if (roll < 4) {
				addEnemyLance(list, UnitTableData.WT_LIGHT + weightMod, maxWeight, c);
				addEnemyLance(list, UnitTableData.WT_LIGHT + weightMod, maxWeight, c);
				addEnemyLance(list, UnitTableData.WT_LIGHT + weightMod, maxWeight, c);
			} else if (roll < 8) {
				addEnemyLance(list, UnitTableData.WT_MEDIUM + weightMod, maxWeight, c);
				addEnemyLance(list, UnitTableData.WT_MEDIUM + weightMod, maxWeight, c);
			} else if (roll < 10) {
				addEnemyLance(list, UnitTableData.WT_ASSAULT + weightMod, maxWeight, c);
			} else if (roll < 13) {
				addEnemyLance(list, UnitTableData.WT_HEAVY + weightMod, maxWeight, c);
				addEnemyLance(list, UnitTableData.WT_LIGHT + weightMod, maxWeight, c);
			} else if (roll < 17) {
				addEnemyLance(list, UnitTableData.WT_MEDIUM + weightMod, maxWeight, c);
				addEnemyLance(list, UnitTableData.WT_LIGHT + weightMod, maxWeight, c);
				addEnemyLance(list, UnitTableData.WT_LIGHT + weightMod, maxWeight, c);
			} else if (roll < 21) {
				addEnemyLance(list, UnitTableData.WT_HEAVY + weightMod, maxWeight, c);
				addEnemyLance(list, UnitTableData.WT_MEDIUM + weightMod, maxWeight, c);
			} else {
				addEnemyLance(list, UnitTableData.WT_HEAVY + weightMod, maxWeight, c);
				addEnemyLance(list, UnitTableData.WT_LIGHT + weightMod, maxWeight, c);
				addEnemyLance(list, UnitTableData.WT_LIGHT + weightMod, maxWeight, c);
			}
			break;
		case EntityWeightClass.WEIGHT_ASSAULT:
		case EntityWeightClass.WEIGHT_SUPER_HEAVY:
			if (roll < 1) {
				addEnemyLance(list, UnitTableData.WT_MEDIUM + weightMod, maxWeight, c);
				addEnemyLance(list, UnitTableData.WT_LIGHT + weightMod, maxWeight, c);
				addEnemyLance(list, UnitTableData.WT_LIGHT + weightMod, maxWeight, c);
			} else if (roll < 5) {
				addEnemyLance(list, UnitTableData.WT_MEDIUM + weightMod, maxWeight, c);
				addEnemyLance(list, UnitTableData.WT_MEDIUM + weightMod, maxWeight, c);
				addEnemyLance(list, UnitTableData.WT_LIGHT + weightMod, maxWeight, c);
			} else if (roll < 8) {
				addEnemyLance(list, UnitTableData.WT_HEAVY + weightMod, maxWeight, c);
				addEnemyLance(list, UnitTableData.WT_LIGHT + weightMod, maxWeight, c);
				addEnemyLance(list, UnitTableData.WT_LIGHT + weightMod, maxWeight, c);
			} else if (roll < 10) {
				addEnemyLance(list, UnitTableData.WT_HEAVY + weightMod, maxWeight, c);
				addEnemyLance(list, UnitTableData.WT_HEAVY + weightMod, maxWeight, c);
			} else if (roll < 12) {
				addEnemyLance(list, UnitTableData.WT_ASSAULT + weightMod, maxWeight, c);
				addEnemyLance(list, UnitTableData.WT_LIGHT + weightMod, maxWeight, c);
			} else if (roll < 16) {
				addEnemyLance(list, UnitTableData.WT_MEDIUM + weightMod, maxWeight, c);
				addEnemyLance(list, UnitTableData.WT_MEDIUM + weightMod, maxWeight, c);
				addEnemyLance(list, UnitTableData.WT_MEDIUM + weightMod, maxWeight, c);
			} else if (roll < 19) {
				addEnemyLance(list, UnitTableData.WT_HEAVY + weightMod, maxWeight, c);
				addEnemyLance(list, UnitTableData.WT_MEDIUM + weightMod, maxWeight, c);
				addEnemyLance(list, UnitTableData.WT_LIGHT + weightMod, maxWeight, c);
			} else if (roll < 21) {
				addEnemyLance(list, UnitTableData.WT_ASSAULT + weightMod, maxWeight, c);
				addEnemyLance(list, UnitTableData.WT_MEDIUM + weightMod, maxWeight, c);
			} else {
				addEnemyLance(list, UnitTableData.WT_ASSAULT + weightMod, maxWeight, c);
				addEnemyLance(list, UnitTableData.WT_MEDIUM + weightMod, maxWeight, c);
				addEnemyLance(list, UnitTableData.WT_LIGHT + weightMod, maxWeight, c);
			}
			break;
		}
	}
	
	private void addEnemyLance(ArrayList<Entity> list, int weight, int maxWeight, Campaign c) {
		if (weight < UnitTableData.WT_LIGHT) {
			weight = UnitTableData.WT_LIGHT;
		}
		if (weight > UnitTableData.WT_ASSAULT) {
			weight = UnitTableData.WT_ASSAULT;
		}
		addLance(list, getContract(c).getEnemyCode(),
				getContract(c).getEnemySkill(), getContract(c).getEnemyQuality(),
				weight, maxWeight, c);
		lanceCount++;
	}
	
	private Entity getEntity(String faction, int skill, int quality, int unitType, int weightClass, Campaign c) {
		UnitTableData.FactionTables ft = UnitTableData.getInstance().getBestRAT(c.getCampaignOptions().getRATs(), c.getCalendar().get(Calendar.YEAR), faction, unitType);
		if (null != ft) {
			return getUnitFromRat(ft.getTable(unitType, weightClass, quality),
					faction, skill, c);
		}
		System.err.println("Error finding faction tables for " + faction + ", unit type " + unitType);
		return null;
	}
	
	private Entity getUnitFromRat(String rat, String faction, int skill, Campaign campaign) {
		Entity en = null;
		MechSummary ms = null;
		if (null == rat) {
			MekHQ.logError("Could not locate appropriate RAT while generating scenario");
			return null;
		}

		RandomUnitGenerator.getInstance().setChosenRAT(rat);
		ArrayList<MechSummary> msl = RandomUnitGenerator.getInstance().generate(1);
		if (msl.size() > 0) {
			ms = msl.get(0);
		}
		if (null == ms) {
			MekHQ.logError("Unable to load summary from RAT " + rat + " while generating scenario");
			return null;
		}

		try {
			en = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
		} catch (EntityLoadingException ex) {
			en = null;
			MekHQ.logError("Unable to load entity: " + ms.getSourceFile() + ": " + ms.getEntryName() + ": " + ex.getMessage());
			MekHQ.logError(ex);
			return null;
		}
		
		en.setOwner(campaign.getPlayer());
		en.setGame(campaign.getGame());
		
		Faction f = Faction.getFaction(faction);
		
		RandomNameGenerator rng = RandomNameGenerator.getInstance();
		rng.setChosenFaction(f.getNameGenerator());
		String crewName = rng.generate();

		RandomSkillsGenerator rsg = new RandomSkillsGenerator();
		rsg.setMethod(RandomSkillsGenerator.M_TAHARQA);
		rsg.setLevel(skill);
		
		if (f.isClan()) {
			rsg.setType(RandomSkillsGenerator.T_CLAN);
		}
		int[] skills = rsg.getRandomSkills(en);

		if (f.isClan() && Compute.d6(2) > 8 - getContract(campaign).getEnemySkill()
				+ skills[0] + skills[1]) {
			int phenotype;
			switch (UnitType.determineUnitTypeCode(en)) {
			case UnitType.MEK:
				phenotype = Bloodname.P_MECHWARRIOR;
				break;
			case UnitType.BATTLE_ARMOR:
				phenotype = Bloodname.P_ELEMENTAL;
				break;
			case UnitType.AERO:
				phenotype = Bloodname.P_AEROSPACE;
				break;
			case UnitType.PROTOMEK:
				phenotype = Bloodname.P_PROTOMECH;
				break;
			default:
				phenotype = -1;
			}
			if (phenotype >= 0) {
				crewName += " " + Bloodname.randomBloodname(faction, phenotype, campaign.getCalendar().get(Calendar.YEAR)).getName();
			}
		}

		en.setCrew(new Crew(crewName,
							Compute.getFullCrewSize(en),
							skills[0], skills[1]));
		
		UUID id = UUID.randomUUID();
		while (null != entityIds.get(id)) {
			id = UUID.randomUUID();
		}
		en.setExternalIdAsString(id.toString());
		entityIds.put(id, en);
		
		return en;
	}
	
	private Entity getDropship(String name, String fName, Campaign campaign) {
		Entity en = Campaign.getBrandNewUndamagedEntity(name);
		
		en.setOwner(campaign.getPlayer());
		en.setGame(campaign.getGame());
		
		Faction faction = Faction.getFaction(fName);
		
		RandomNameGenerator rng = RandomNameGenerator.getInstance();
		rng.setChosenFaction(faction.getNameGenerator());

		RandomSkillsGenerator rsg = new RandomSkillsGenerator();
		rsg.setMethod(RandomSkillsGenerator.M_TAHARQA);
		rsg.setLevel(RandomSkillsGenerator.L_REG);

		if (faction.isClan()) rsg.setType(RandomSkillsGenerator.T_CLAN);
		int[] skills = rsg.getRandomSkills(en);
		en.setCrew(new Crew(rng.generate(),
							Compute.getFullCrewSize(en),
							skills[0], skills[1]));
		
		UUID id = UUID.randomUUID();
		while (null != entityIds.get(id)) {
			id = UUID.randomUUID();
		}
		en.setExternalIdAsString(id.toString());
		entityIds.put(id, en);
		
		return en;
	}
	
	private void addLance(ArrayList<Entity> list, String faction,
			int skill, int quality, int weightClass, Campaign c) {
		addLance(list, faction, skill, quality, weightClass,
				UnitTableData.WT_ASSAULT, c, 0);
	}
	
	private void addLance(ArrayList<Entity> list, String faction,
			int skill, int quality, int weightClass, int maxWeight, Campaign c) {
		addLance(list, faction, skill, quality, weightClass,
				maxWeight, c, 0);
	}
	
	private String adjustForMaxWeight(String weights, int maxWeight) {
		String retVal = weights;
		if (maxWeight == UnitTableData.WT_HEAVY) {
			//Hide and Seek (defender)
			retVal = weights.replaceAll("A", "LM");
		} else if (maxWeight == UnitTableData.WT_MEDIUM) {
			//Probe, Recon Raid (attacker)
			retVal = weights.replaceAll("A", "MM");
			retVal = retVal.replaceAll("H", "LM");
		}
		return retVal;
	}
	
	private String adjustWeightsForFaction(String weights, String faction) {
		String retVal = weights;
		if (faction.equals("DC")) {
			retVal = weights.replaceFirst("MM", "LH");
		}
		if ((faction.equals("LA") ||
				faction.equals("CCO") || faction.equals("CGB"))
				&& weights.matches("[LM]{3,}")) {
			retVal = weights.replaceFirst("M", "H");
		}
		if (faction.equals("FWL") || faction.equals("CIH")) {
			retVal = weights.replaceFirst("HA", "HH");
		}
		return retVal;
	}
	
	private void addLance(ArrayList<Entity> list, String faction, int skill, int quality, int weightClass,
			int maxWeight, Campaign campaign, int arrivalTurn) {
		if (Faction.getFaction(faction).isClan()) {
			addStar(list, faction, skill, quality, weightClass, maxWeight, campaign, arrivalTurn);
			return;
		} 
		if (faction.equals("CS") || faction.equals("WOB")) {
			addLevelII(list, faction, skill, quality, weightClass, maxWeight, campaign, arrivalTurn);
			return;
		}
		
		String weights = adjustForMaxWeight(lanceWeights[0][weightClass][Compute.d6() - 1],
				maxWeight);
		
		int forceType;
		int roll = Compute.d6();
		if (roll < 4) {
			forceType = FORCE_VEHICLE;
		} else if (roll < 6) {
			forceType = FORCE_MIXED;
		} else {
			forceType = FORCE_MEK;
			if (campaign.getCampaignOptions().getRegionalMechVariations()) {
				weights = adjustWeightsForFaction(weights, faction);
			}
		}
		
		int[] unitTypes = new int[weights.length()];
		for (int i = 0; i < unitTypes.length; i++) {
			unitTypes[i] = (forceType == FORCE_VEHICLE)?UnitTableData.UNIT_VEHICLE:UnitTableData.UNIT_MECH;
		}
		/* Distribute vehicles randomly(-ish) through mixed units */
		if (forceType == FORCE_MIXED) {
			for (int i = 0; i < weights.length() / 2; i++) {
				int j = Compute.randomInt(weights.length());
				while (unitTypes[j] == UnitTableData.UNIT_VEHICLE) {
					j++;
					if (j >= weights.length()) {
						j = 0;
					}
				}
				unitTypes[j] = UnitTableData.UNIT_VEHICLE;
			}
		}
		
		for (int i = 0; i < weights.length(); i++) {
			Entity en = getEntity(faction, skill, quality, unitTypes[i],
					decodeWeightStr(weights, i),
					campaign);
			if (null != en) {
				en.setDeployRound(arrivalTurn);
			}
			list.add(en);
			if (unitTypes[i] == UnitTableData.UNIT_VEHICLE && campaign.getCampaignOptions().getDoubleVehicles()) {
				en = getEntity(faction, skill, quality, unitTypes[i],
						decodeWeightStr(weights, i),
						campaign);
				if (null != en) {
					en.setDeployRound(arrivalTurn);
				}
				list.add(en);
			}				
		}
	}
	
	private void addStar(ArrayList<Entity> list, String faction, int skill, int quality, int weightClass, int maxWeight, Campaign campaign, int arrivalTurn) {
		int forceType = FORCE_MEK;
		/* 1 chance in 12 of a Nova, per AtB rules; CHH/CSL
		 * close to 2/3. Added a chance to encounter
		 * a vehicle Star in Clan second-line (rating C or lower) units,
		 * or all unit ratings for CHH/CSL and CBS.
		 */
		int roll = Compute.d6(2);
		int novaTarget = 10;
		if (faction.equals("CHH") || faction.equals("CSL")) {
			novaTarget = 7;
		}
		if (roll > novaTarget) {
			forceType = FORCE_NOVA;
		} else if (campaign.getCampaignOptions().getClanVehicles()) {
			if (!faction.equals("CHH") || !faction.equals("CSL")
					&& !faction.equals("CBS")) {
				roll += quality;
			}
			if (roll <= 4) {
				forceType = FORCE_VEHICLE;
			}
		}
		
		String weights = adjustForMaxWeight(lanceWeights[1][weightClass][Compute.d6() - 1],
				maxWeight);
		
		int unitType = (forceType == FORCE_VEHICLE)?UnitTableData.UNIT_VEHICLE:UnitTableData.UNIT_MECH;
		
		if (campaign.getCampaignOptions().getRegionalMechVariations()) {
			if (unitType == UnitTableData.UNIT_MECH) {
				weights = adjustWeightsForFaction(weights, faction);
			}
			/* medium vees are rare among the Clans, FM:CC, p. 8 */
			if (unitType == UnitTableData.UNIT_VEHICLE) {
				weights = adjustWeightsForFaction(weights, "DC");
			}
		}

		int unitsPerPoint;
		switch (unitType) {
		case UnitTableData.UNIT_VEHICLE:
		case UnitTableData.UNIT_AERO:
			unitsPerPoint = 2;
			break;
		case UnitTableData.UNIT_PROTOMECH:
			unitsPerPoint = 5;
			break;
		case UnitTableData.UNIT_MECH:
		case UnitTableData.UNIT_INFANTRY:
		case UnitTableData.UNIT_BATTLEARMOR:
		default:
			unitsPerPoint = 1;
			break;
		}
		
		/* Ensure Novas use Frontline tables to get Omnis */
		int tmpQuality = quality;
		if (forceType == FORCE_NOVA && quality < IUnitRating.DRAGOON_B) {
			tmpQuality = IUnitRating.DRAGOON_B;
		}
		for (int point = 0; point < weights.length(); point++) {
			for (int unit = 0; unit < unitsPerPoint; unit++) {
				Entity en = getEntity(faction, skill, tmpQuality, unitType,
						decodeWeightStr(weights, point),
						campaign);
				if (null != en) {
					en.setDeployRound(arrivalTurn);
				}
				list.add(en);
			}
		}
		if (forceType == FORCE_NOVA || forceType == FORCE_VEENOVA) {
			unitType = forceType == FORCE_VEENOVA? UnitTableData.UNIT_INFANTRY:UnitTableData.UNIT_BATTLEARMOR;
			for (int i = 0; i < 5; i++) {
				Entity en = getEntity(faction, skill, quality,
						unitType, 0, campaign);
				if (null != en) {
					en.setDeployRound(arrivalTurn);
				}
				list.add(en);
			}
		}
	}
	
	private void addLevelII(ArrayList<Entity> list, String faction, int skill, int quality, int weightClass,
			int maxWeight, Campaign campaign, int arrivalTurn) {
		String weights = adjustForMaxWeight(lanceWeights[2][weightClass][Compute.d6() - 1],
				maxWeight);
		
		int forceType = FORCE_MEK;
		int roll = Compute.d6();
		if (roll < 4) {
			forceType = FORCE_VEHICLE;
		} else if (roll < 6) {
			forceType = FORCE_MIXED;
		}
				
		int[] unitTypes = new int[weights.length()];
		for (int i = 0; i < unitTypes.length; i++) {
			unitTypes[i] = (forceType == FORCE_VEHICLE)?UnitTableData.UNIT_VEHICLE:UnitTableData.UNIT_MECH;
		}
		/* Distribute vehicles randomly(-ish) through mixed units */
		if (forceType == FORCE_MIXED) {
			for (int i = 0; i < weights.length() / 2; i++) {
				int j = Compute.randomInt(weights.length());
				while (unitTypes[j] == UnitTableData.UNIT_VEHICLE) {
					j++;
					if (j >= weights.length()) {
						j = 0;
					}
				}
				unitTypes[j] = UnitTableData.UNIT_VEHICLE;
			}
		}
		
		for (int i = 0; i < weights.length(); i++) {
			Entity en = getEntity(faction, skill, quality, unitTypes[i],
					decodeWeightStr(weights, i),
					campaign);
			if (null != en) {
				en.setDeployRound(arrivalTurn);
			}
			list.add(en);
			if (unitTypes[i] == UnitTableData.UNIT_VEHICLE && campaign.getCampaignOptions().getDoubleVehicles()) {
				en = getEntity(faction, skill, quality, unitTypes[i],
						decodeWeightStr(weights, i),
						campaign);
				if (null != en) {
					en.setDeployRound(arrivalTurn);
				}
				list.add(en);
			}				
		}
	}
	
	private int decodeWeightStr(String s, int i) {
		switch (s.charAt(i)) {
		case 'L': return UnitTableData.WT_LIGHT;
		case 'M': return UnitTableData.WT_MEDIUM;
		case 'H': return UnitTableData.WT_HEAVY;
		case 'A': return UnitTableData.WT_ASSAULT;
		}
		return 0;
	}

	private void addCivilianUnits(ArrayList<Entity> list, int num, Campaign c) {
		for (int i = 0; i < num; i++) {
			list.add(getUnitFromRat("CivilianUnits", "IND", RandomSkillsGenerator.L_GREEN, c));
		}
	}
	
	/* Convenience methods for frequently-used arguments */
	private BotForce getAllyBotForce(AtBContract c, int start, ArrayList<Entity> entities) {
		return new BotForce(c.getAllyBotName(), 1, start, entities,
				c.getAllyCamoCategory(),
				c.getAllyCamoFileName(),
				c.getAllyColorIndex());
	}

	private BotForce getEnemyBotForce(AtBContract c, int start, ArrayList<Entity> entities) {
		return new BotForce(c.getEnemyBotName(), 2, start, entities,
				c.getEnemyCamoCategory(),
				c.getEnemyCamoFileName(),
				c.getEnemyColorIndex());
	}

	public ArrayList<String> generateEntityStub(ArrayList<Entity> entities) {
		ArrayList<String> stub = new ArrayList<String>();
		for (Entity en : entities) {
			if (null == en) {
				stub.add("<html><font color='red'>Unit could not be loaded</font></html>");
			} else {
				stub.add("<html>" + en.getCrew().getName() + " (" +
						en.getCrew().getGunnery() + "/" +
						en.getCrew().getPiloting() + "), " +
						"<i>" + en.getShortName() + "</i>" + 
						((en.getDeployRound() > 0)?" [Reinforcements]":"") +
						"</html>");
			}
		}
		return stub;
	}
	
	public BotForceStub generateBotStub(BotForce bf) {
		return new BotForceStub("<html>" +
					bf.getName() + " <i>" +
					((bf.getTeam() == 1)?"Allied":"Enemy") + "</i>" +
					" Start: " + IStartingPositions.START_LOCATION_NAMES[bf.getStart()] +
					"</html>",
					generateEntityStub(bf.getEntityList()));
	}

	@Override
	public void generateStub(Campaign c) {
		super.generateStub(c);
		for (BotForce bf : botForces) {
			botForceStubs.add(generateBotStub(bf));
		}
		alliesPlayerStub = generateEntityStub(alliesPlayer);
		
		botForces.clear();
		alliesPlayer.clear();
		if (null != bigBattleAllies) {
			bigBattleAllies.clear();
		}
		if (null != specMissionEnemies) {
			specMissionEnemies.clear();
		}
	}
	
	public void doPostResolution(Campaign c, int contractBreaches, int bonusRolls) {
			getContract(c).addPlayerMinorBreaches(contractBreaches);
			for (int i = 0; i < bonusRolls; i++) {
				getContract(c).doBonusRoll(c);
			}
			if (RECONRAID == battleType && attacker &&
					(getStatus() == S_VICTORY || getStatus() == S_MVICTORY)) {
				for (int i = 0; i < Compute.d6() - 2; i++) {
					getContract(c).doBonusRoll(c);
				}
			}
	}
	
	@Override
	protected void writeToXmlEnd(PrintWriter pw1, int indent) {
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "battleType", battleType);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "attacker", attacker);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "lanceForceId", lanceForceId);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "terrainType", terrainType);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "light", light);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "weather", weather);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "wind", wind);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "fog", fog);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "atmosphere", atmosphere);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "gravity", gravity);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "start", start);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "deploymentDelay", deploymentDelay);

		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<mapSize>"
				+ mapSizeX + "," + mapSizeY
				+"</mapSize>");

		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "map", map);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "lanceCount", lanceCount);
		MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "rerollsRemaining", rerollsRemaining);

		if (null != bigBattleAllies && bigBattleAllies.size() > 0) {
			pw1.println(MekHqXmlUtil.indentStr(indent+1) + "<bigBattleAllies>");
			for (Entity entity : bigBattleAllies) {
				pw1.println(writeEntityWithCrewToXmlString(entity, indent+2, bigBattleAllies));			
			}
			pw1.println(MekHqXmlUtil.indentStr(indent+1) + "</bigBattleAllies>");
		} else if (alliesPlayer.size() > 0) {
			pw1.println(MekHqXmlUtil.indentStr(indent+1)+"<alliesPlayer>");
			for (Entity entity : alliesPlayer) {
				pw1.println(writeEntityWithCrewToXmlString(entity, indent+2, alliesPlayer));			
			}
			pw1.println(MekHqXmlUtil.indentStr(indent+1)+"</alliesPlayer>");
		}

		for (BotForce botForce : botForces) {
			pw1.println(MekHqXmlUtil.indentStr(indent+1)+"<botForce>");
			botForce.writeToXml(pw1, indent+1);
			pw1.println(MekHqXmlUtil.indentStr(indent+1)+"</botForce>");
		}
		
		if (alliesPlayerStub.size() > 0) {
			pw1.println(MekHqXmlUtil.indentStr(indent+1) + "<alliesPlayerStub>");
			for (String stub : alliesPlayerStub) {
				MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+2,
						"entityStub", MekHqXmlUtil.escape(stub));
			}
			pw1.println(MekHqXmlUtil.indentStr(indent+1) + "</alliesPlayerStub>");
		}

		for (BotForceStub bot : botForceStubs) {
			pw1.println(MekHqXmlUtil.indentStr(indent+1)
					+ "<botForceStub name=\""
					+ MekHqXmlUtil.escape(bot.name) + "\">");
			for (String entity : bot.getEntityList()) {
				MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+2,
						"entityStub", MekHqXmlUtil.escape(entity));
			}
			pw1.println(MekHqXmlUtil.indentStr(indent+1)
					+ "</botForceStub>");
		}

		if (attachedUnits.size() > 0) {
			MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "attachedUnits", getCsvFromList(attachedUnits));
		}
		if (survivalBonus.size() > 0) {
			MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "survivalBonus", getCsvFromList(survivalBonus));
		}
		
		if (null != specMissionEnemies && specMissionEnemies.size() > 0) {
			pw1.println(MekHqXmlUtil.indentStr(indent+1) + "<specMissionEnemies>");
			for (int i = 0; i < specMissionEnemies.size(); i++) {
				pw1.println(MekHqXmlUtil.indentStr(indent+2) + "<playerWeight class=\"" + i + "\">");
				for (Entity entity : specMissionEnemies.get(i)) {
					pw1.println(writeEntityWithCrewToXmlString(entity, indent+3, specMissionEnemies.get(i)));			
				}
				pw1.println(MekHqXmlUtil.indentStr(indent+2) + "</playerWeight>");
			}
			pw1.println(MekHqXmlUtil.indentStr(indent+1) + "</specMissionEnemies>");
		}

		super.writeToXmlEnd(pw1, indent);
	}
	
	/* MekHqXmlUtil.writeEntityToXmlString does not include the crew,
	 * as crew is handled by the Person class in MekHQ. This utility
	 * function will insert a pilot tag (and also a deployment attribute,
	 * which is also not added by the MekHqXmlUtil method).
	 */
	public static String writeEntityWithCrewToXmlString(Entity tgtEnt, int indentLvl, ArrayList<Entity> list) {
		String retVal = MekHqXmlUtil.writeEntityToXmlString(tgtEnt, indentLvl, list);
		
		String crew = MekHqXmlUtil.indentStr(indentLvl+1)
				+ "<pilot name=\""
				+ MekHqXmlUtil.escape(tgtEnt.getCrew().getName())
				+ "\" size=\""
				+ tgtEnt.getCrew().getSize()
				+ "\" nick=\""
				+ MekHqXmlUtil.escape(tgtEnt.getCrew().getNickname())
				+ "\" gunnery=\""
				+ tgtEnt.getCrew().getGunnery()
				+ "\" piloting=\""
				+ tgtEnt.getCrew().getPiloting();
		
		if (tgtEnt.getCrew().getToughness() != 0) {
			crew += "\" toughness=\""
				+ String.valueOf(tgtEnt.getCrew().getToughness());
		}
		if (tgtEnt.getCrew().getInitBonus() != 0) {
			crew += "\" initB=\""
				+ String.valueOf(tgtEnt.getCrew().getInitBonus());
		}
		if (tgtEnt.getCrew().getCommandBonus() != 0) {
			crew += "\" commandB=\""
				+ String.valueOf(tgtEnt.getCrew().getCommandBonus());
		}
		if (tgtEnt.getCrew().isDead() || tgtEnt.getCrew().getHits() > 5) {
			crew +="\" hits=\"Dead";
		} else if (tgtEnt.getCrew().getHits() > 0) {
			crew += "\" hits=\""
				+ String.valueOf(tgtEnt.getCrew().getHits());
		}
		crew += "\" ejected=\""
				 + String.valueOf(tgtEnt.getCrew().isEjected());
		
		if (tgtEnt instanceof Mech) {
			if (((Mech) tgtEnt).isAutoEject()) {
				crew += "\" autoeject=\"true";
			} else {
				crew += "\" autoeject=\"false";
			}
		}
		crew += "\"/>";
		
		return retVal.replaceFirst(">", " deployment=\"" +
			tgtEnt.getDeployRound() + "\">\n" + crew + "\n");
	}
	
	@Override
	protected void loadFieldsFromXmlNode(Node wn) throws ParseException {
		super.loadFieldsFromXmlNode(wn);
		NodeList nl = wn.getChildNodes();

		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
			if (wn2.getNodeName().equalsIgnoreCase("battleType")) {
				battleType = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("attacker")) {
				attacker = Boolean.parseBoolean(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("lanceForceId")) {
				lanceForceId = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("terrainType")) {
				terrainType = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("light")) {
				light = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("weather")) {
				weather = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("wind")) {
				wind = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("fog")) {
				fog = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("atmosphere")) {
				atmosphere = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("gravity")) {
				gravity = Float.parseFloat(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("start")) {
				start = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("deploymentDelay")) {
				deploymentDelay = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("mapSize")) {
				String []xy = wn2.getTextContent().split(",");
				mapSizeX = Integer.parseInt(xy[0]);
				mapSizeY = Integer.parseInt(xy[1]);
			} else if (wn2.getNodeName().equalsIgnoreCase("map")) {
				map = wn2.getTextContent().trim();
			} else if (wn2.getNodeName().equalsIgnoreCase("lanceCount")) {
				lanceCount = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("rerollsRemaining")) {
				rerollsRemaining = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("alliesPlayer")) {
				NodeList nl2 = wn2.getChildNodes();
				for (int i = 0; i < nl2.getLength(); i++) {
					Node wn3 = nl2.item(i);
					if (wn3.getNodeName().equalsIgnoreCase("entity")) {
						Entity en = null;
						try {
							en = MekHqXmlUtil.getEntityFromXmlString(wn3);
						} catch (Exception e) {
							MekHQ.logError("Error loading allied unit in scenario");
							MekHQ.logError(e);
						}
						alliesPlayer.add(en);
						entityIds.put(UUID.fromString(en.getExternalIdAsString()), en);
					}
				}
			} else if (wn2.getNodeName().equalsIgnoreCase("bigBattleAllies")) {
				bigBattleAllies = new ArrayList<Entity>();
				NodeList nl2 = wn2.getChildNodes();
				for (int i = 0; i < nl2.getLength(); i++) {
					Node wn3 = nl2.item(i);
					if (wn3.getNodeName().equalsIgnoreCase("entity")) {
						Entity en = null;
						try {
							en = MekHqXmlUtil.getEntityFromXmlString(wn3);
						} catch (Exception e) {
							MekHQ.logError("Error loading allied unit in scenario");
							MekHQ.logError(e);
						}
						bigBattleAllies.add(en);
						entityIds.put(UUID.fromString(en.getExternalIdAsString()), en);
					}
				}
			} else if (wn2.getNodeName().equalsIgnoreCase("specMissionEnemies")) {
				specMissionEnemies = new ArrayList<ArrayList<Entity>>();
				for (int i = 0; i < 4; i++) {
					specMissionEnemies.add(new ArrayList<Entity>());
				}
				NodeList nl2 = wn2.getChildNodes();
				for (int i = 0; i < nl2.getLength(); i++) {
					Node wn3 = nl2.item(i);
					if (wn3.getNodeName().equalsIgnoreCase("weight")) {
						int weightClass = Integer.parseInt(wn3.getAttributes().getNamedItem("class").getTextContent());
						NodeList nl3 = wn3.getChildNodes();
						for (int j = 0; j < nl3.getLength(); j++) {
							Node wn4 = nl3.item(j);
							if (wn4.getNodeName().equalsIgnoreCase("entity")) {
								Entity en = null;
								try {
									en = MekHqXmlUtil.getEntityFromXmlString(wn3);
								} catch (Exception e) {
									MekHQ.logError("Error loading allied unit in scenario");
									MekHQ.logError(e);
								}
								specMissionEnemies.get(weightClass).add(en);
								entityIds.put(UUID.fromString(en.getExternalIdAsString()), en);
							}
						}
					}
				}
			} else if (wn2.getNodeName().equalsIgnoreCase("botForce")) {
				BotForce bf = new BotForce();
				try {
					bf.setFieldsFromXmlNode(wn2);
				} catch (Exception e) {
					MekHQ.logError("Error loading allied unit in scenario");
					MekHQ.logError(e);
					bf = null;
				}
				if (null != bf) {
					botForces.add(bf);
				}
			} else if (wn2.getNodeName().equalsIgnoreCase("alliesPlayerStub")) {
				alliesPlayerStub = getEntityStub(wn2);
			} else if (wn2.getNodeName().equalsIgnoreCase("botForceStub")) {
				String name = MekHqXmlUtil.unEscape(wn2.getAttributes().getNamedItem("name").getTextContent());
				ArrayList<String> stub = getEntityStub(wn2);
				botForceStubs.add(new BotForceStub(name, stub));
			} else if (wn2.getNodeName().equalsIgnoreCase("attachedUnits")) {
				String ids[] = wn2.getTextContent().split(",");
				for (String s : ids) {
					attachedUnits.add(UUID.fromString(s));
				}
			} else if (wn2.getNodeName().equalsIgnoreCase("survivalBonus")) {
				String ids[] = wn2.getTextContent().split(",");
				for (String s : ids) {
					survivalBonus.add(UUID.fromString(s));
				}
			}
		}
	}
	
	private ArrayList<String> getEntityStub(Node wn) {
		ArrayList<String> stub = new ArrayList<String>();
		NodeList nl = wn.getChildNodes();
		for (int x = 0; x < nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			if (wn2.getNodeName().equalsIgnoreCase("entityStub")) {
				stub.add(MekHqXmlUtil.unEscape(wn2.getTextContent()));
			}
		}
		return stub;
	}
		
	protected String getCsvFromList(ArrayList<? extends Object> list) {
		String retVal = new String();
		for (int i = 0; i < list.size(); i++) {
			retVal += list.get(i).toString();
			if (i < list.size() - 1) {
				retVal += ",";
			}
		}
		return retVal;
	}
	
	public AtBContract getContract(Campaign c) {
		return (AtBContract)c.getMission(getMissionId());
	}
	
	public int getLanceForceId() {
		return lanceForceId;
	}
	
	public Lance getLance(Campaign c) {
		return c.getLances().get(lanceForceId);
	}

	public void setLance(Lance l) {
		lanceForceId = l.getForceId();
	}

	public int getBattleType() {
		return battleType;
	}

	public void setBattleType(int battleType) {
		this.battleType = battleType;
	}

	public boolean isAttacker() {
		return attacker;
	}

	public void setAttacker(boolean attacker) {
		this.attacker = attacker;
	}
	
	public ArrayList<Entity> getAlliesPlayer() {
		return alliesPlayer;
	}
	
	public ArrayList<UUID> getAttachedUnits() {
		return attachedUnits;
	}
	
	public ArrayList<UUID> getSurvivalBonus() {
		return survivalBonus;
	}

	public Entity getEntity(UUID id) {
		return entityIds.get(id);
	}

	public BotForce getBotForce(int i) {
		return botForces.get(i);
	}
	
	public int getNumBots() {
		if (isCurrent()) {
			return botForces.size();
		} else {
			return botForceStubs.size();
		}
	}
	
	public ArrayList<String> getAlliesPlayerStub() {
		return alliesPlayerStub;
	}
	
	public ArrayList<BotForceStub> getBotForceStubs() {
		return botForceStubs;
	}

	public int getTerrainType() {
		return terrainType;
	}

	public void setTerrainType(int terrainType) {
		this.terrainType = terrainType;
	}

	public int getLight() {
		return light;
	}

	public void setLight(int light) {
		this.light = light;
	}

	public int getWeather() {
		return weather;
	}

	public void setWeather(int weather) {
		this.weather = weather;
	}

	public int getWind() {
		return wind;
	}

	public void setWind(int wind) {
		this.wind = wind;
	}

	public int getFog() {
		return fog;
	}

	public void setFog(int fog) {
		this.fog = fog;
	}
	
	public int getAtmosphere() {
		return atmosphere;
	}
	
	public void setAtmosphere(int atmosphere) {
		this.atmosphere = atmosphere;
	}
	
	public float getGravity() {
		return gravity;
	}
	
	public void setGravity(float gravity) {
		this.gravity = gravity;
	}

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int getDeploymentDelay() {
		return deploymentDelay;
	}

	public void setDeploymentDelay(int delay) {
		this.deploymentDelay = delay;
	}

	public String getMap() {
		return map;
	}

	public void setMap(String map) {
		this.map = map;
	}

	public int getLanceCount() {
		return lanceCount;
	}

	public void setLanceCount(int lanceCount) {
		this.lanceCount = lanceCount;
	}

	public int getRerollsRemaining() {
		return rerollsRemaining;
	}

	public void useReroll() {
		rerollsRemaining--;
	}
	
	public class BotForce implements Serializable, MekHqXmlSerializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 8259058549964342518L;
		
		private String name;
		private ArrayList<Entity> entityList;
		private int team;
		private int start;
		private String camoCategory;
		private String camoFileName;
		private int colorIndex;
		private BehaviorSettings behaviorSettings;
		
		public BotForce() {
			this.entityList = new ArrayList<Entity>();
			behaviorSettings = BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR;
		};
		
		public BotForce(String name, int team, int start, ArrayList<Entity> entityList) {
			this(name, team, start, entityList, Player.NO_CAMO, null, -1);
		}

		public BotForce(String name, int team, int start, ArrayList<Entity> entityList,
				String camoCategory, String camoFileName, int colorIndex) {
			this.name = name;
			this.team = team;
			this.start = start;
			this.entityList = entityList;
		    this.camoCategory = camoCategory;
		    this.camoFileName = camoFileName;
		    this.colorIndex = colorIndex;
			behaviorSettings = BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR;
			behaviorSettings.setHomeEdge(findHomeEdge(start));
		}
		
		/* Convert from MM's Board to Princess's HomeEdge */
		public HomeEdge findHomeEdge(int start) {
			switch (start) {
			case Board.START_N:
				return HomeEdge.NORTH;
			case Board.START_S:
				return HomeEdge.SOUTH;
			case Board.START_E:
				return HomeEdge.EAST;
			case Board.START_W:
				return HomeEdge.WEST;
			case Board.START_NW:
				return (Compute.randomInt(2) == 0)?HomeEdge.NORTH:HomeEdge.WEST;
			case Board.START_NE:
				return (Compute.randomInt(2) == 0)?HomeEdge.NORTH:HomeEdge.EAST;
			case Board.START_SW:
				return (Compute.randomInt(2) == 0)?HomeEdge.SOUTH:HomeEdge.WEST;
			case Board.START_SE:
				return (Compute.randomInt(2) == 0)?HomeEdge.SOUTH:HomeEdge.EAST;
			default:
				return HomeEdge.getHomeEdge(Compute.randomInt(4));
			}
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public ArrayList<Entity> getEntityList() {
			return entityList;
		}

		public void setEntityList(ArrayList<Entity> entityList) {
			this.entityList = entityList;
		}

		public int getTeam() {
			return team;
		}

		public void setTeam(int team) {
			this.team = team;
		}

		public int getStart() {
			return start;
		}

		public void setStart(int start) {
			this.start = start;
		}

		public String getCamoCategory() {
			return camoCategory;
		}

		public void setCamoCategory(String camoCategory) {
			this.camoCategory = camoCategory;
		}

		public String getCamoFileName() {
			return camoFileName;
		}

		public void setCamoFileName(String camoFileName) {
			this.camoFileName = camoFileName;
		}
		
		public int getColorIndex() {
			return colorIndex;
		}
		
		public void setColorIndex(int index) {
			colorIndex = index;
		}

		public BehaviorSettings getBehaviorSettings() {
			return behaviorSettings;
		}

		public void setBehaviorSettings(BehaviorSettings behaviorSettings) {
			this.behaviorSettings = behaviorSettings;
		}
		
		public void setHomeEdge(int i) {
			behaviorSettings.setHomeEdge(findHomeEdge(1));
		}

		public void writeToXml(PrintWriter pw1, int indent) {
			MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "name", MekHqXmlUtil.escape(name));
			MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "team", team);
			MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "start", start);
			MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "camoCategory", MekHqXmlUtil.escape(camoCategory));
			MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "camoFileName", MekHqXmlUtil.escape(camoFileName));
			MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "colorIndex", colorIndex);

			pw1.println(MekHqXmlUtil.indentStr(indent+1) + "<entities>");
			for (Entity en : entityList) {
				pw1.println(writeEntityWithCrewToXmlString(en, indent + 2, entityList));
			}
			pw1.println(MekHqXmlUtil.indentStr(indent+1) + "</entities>");
			
			if (!behaviorSettings.isDefault()) {
				pw1.println(MekHqXmlUtil.indentStr(indent+1) + "<behaviorSettings>");
				MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "forcedWithdrawal", behaviorSettings.isForcedWithdrawal());
				MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "goHome", behaviorSettings.shouldGoHome());
				MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "autoFlee", behaviorSettings.shouldAutoFlee());
				MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "selfPreservationIndex", behaviorSettings.getSelfPreservationIndex());
				MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "fallShameIndex", behaviorSettings.getFallShameIndex());
				MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "hyperAggressionIndex", behaviorSettings.getHyperAggressionIndex());
				MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "homeEdge", behaviorSettings.getHomeEdge().ordinal());
				MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "herdMentalityIndex", behaviorSettings.getHerdMentalityIndex());
				MekHqXmlUtil.writeSimpleXmlTag(pw1, indent+1, "braveryIndex", behaviorSettings.getBraveryIndex());
				pw1.println(MekHqXmlUtil.indentStr(indent+1) + "</behaviorSettings>");
			}
		}

		public void setFieldsFromXmlNode(Node wn) {
			NodeList nl = wn.getChildNodes();
			for (int x = 0; x < nl.getLength(); x++) {
				Node wn2 = nl.item(x);
				if (wn2.getNodeName().equalsIgnoreCase("name")) {
					name = MekHqXmlUtil.unEscape(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("team")) {
					team = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("start")) {
					start = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("camoCategory")) {
					camoCategory = MekHqXmlUtil.unEscape(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("camoFileName")) {
					camoFileName = MekHqXmlUtil.unEscape(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("colorIndex")) {
					colorIndex = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("entities")) {
					NodeList nl2 = wn2.getChildNodes();
					for (int i = 0; i < nl2.getLength(); i++) {
						Node wn3 = nl2.item(i);
						if (wn3.getNodeName().equalsIgnoreCase("entity")) {
							Entity en = null;
							try {
								en = MekHqXmlUtil.getEntityFromXmlString(wn3);
							} catch (Exception e) {
								MekHQ.logError("Error loading allied unit in scenario");
								MekHQ.logError(e);
							}
							entityList.add(en);
							entityIds.put(UUID.fromString(en.getExternalIdAsString()), en);
						}
					}
				} else if (wn2.getNodeName().equalsIgnoreCase("behaviorSettings")) {
					NodeList nl2 = wn2.getChildNodes();
					for (int i = 0; i < nl2.getLength(); i++) {
						Node wn3 = nl2.item(i);
						if (wn2.getNodeName().equalsIgnoreCase("forcedWithdrawal")) {
							behaviorSettings.setForcedWithdrawal(Boolean.parseBoolean(wn2.getTextContent()));
						} else if (wn3.getNodeName().equalsIgnoreCase("goHome")) {
							behaviorSettings.setGoHome(Boolean.parseBoolean(wn2.getTextContent()));
						} else if (wn3.getNodeName().equalsIgnoreCase("autoFlee")) {
							behaviorSettings.setAutoFlee(Boolean.parseBoolean(wn2.getTextContent()));
						} else if (wn3.getNodeName().equalsIgnoreCase("selfPreservationIndex")) {
							behaviorSettings.setSelfPreservationIndex(Integer.parseInt(wn2.getTextContent()));
						} else if (wn3.getNodeName().equalsIgnoreCase("fallShameIndex")) {
							behaviorSettings.setFallShameIndex(Integer.parseInt(wn2.getTextContent()));
						} else if (wn3.getNodeName().equalsIgnoreCase("hyperAggressionIndex")) {
							behaviorSettings.setHyperAggressionIndex(Integer.parseInt(wn2.getTextContent()));
						} else if (wn3.getNodeName().equalsIgnoreCase("homeEdge")) {
							behaviorSettings.setHomeEdge(Integer.parseInt(wn2.getTextContent()));
						} else if (wn3.getNodeName().equalsIgnoreCase("herdMentalityIndex")) {
							behaviorSettings.setHerdMentalityIndex(Integer.parseInt(wn2.getTextContent()));
						} else if (wn3.getNodeName().equalsIgnoreCase("braveryIndex")) {
							behaviorSettings.setBraveryIndex(Integer.parseInt(wn2.getTextContent()));
						}
					}
				}			
			}
		}

	}
	
	public class BotForceStub {
		private String name;
		private ArrayList<String> entityList;
		
		public BotForceStub(String name, ArrayList<String> entityList) {
			this.name = name;
			this.entityList = entityList;
		}
		
		public String getName() {
			return name;
		}
		
		public ArrayList<String> getEntityList() {
			return entityList;
		}
	}

}