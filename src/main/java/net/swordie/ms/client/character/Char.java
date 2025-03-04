package net.swordie.ms.client.character;

import net.swordie.ms.Server;
import net.swordie.ms.ServerConfig;
import net.swordie.ms.client.Account;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.LinkSkill;
import net.swordie.ms.client.User;
import net.swordie.ms.client.alliance.Alliance;
import net.swordie.ms.client.alliance.AllianceResult;
import net.swordie.ms.client.anticheat.OffenseManager;
import net.swordie.ms.client.character.avatar.AvatarData;
import net.swordie.ms.client.character.avatar.AvatarLook;
import net.swordie.ms.client.character.cards.MonsterBookInfo;
import net.swordie.ms.client.character.damage.DamageCalc;
import net.swordie.ms.client.character.damage.DamageSkinSaveData;
import net.swordie.ms.client.character.info.*;
import net.swordie.ms.client.character.items.*;
import net.swordie.ms.client.character.keys.FuncKeyMap;
import net.swordie.ms.client.character.monsterbattle.MonsterBattleLadder;
import net.swordie.ms.client.character.monsterbattle.MonsterBattleMobInfo;
import net.swordie.ms.client.character.monsterbattle.MonsterBattleRankInfo;
import net.swordie.ms.client.character.potential.CharacterPotential;
import net.swordie.ms.client.character.potential.CharacterPotentialMan;
import net.swordie.ms.client.character.quest.Quest;
import net.swordie.ms.client.character.quest.QuestManager;
import net.swordie.ms.client.character.runestones.RuneStone;
import net.swordie.ms.client.character.skills.*;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.friend.Friend;
import net.swordie.ms.client.friend.FriendFlag;
import net.swordie.ms.client.friend.FriendRecord;
import net.swordie.ms.client.friend.FriendshipRingRecord;
import net.swordie.ms.client.guild.Guild;
import net.swordie.ms.client.guild.GuildMember;
import net.swordie.ms.client.guild.result.GuildResult;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.client.jobs.JobManager;
import net.swordie.ms.client.jobs.legend.Evan;
import net.swordie.ms.client.jobs.resistance.Demon;
import net.swordie.ms.client.jobs.resistance.WildHunterInfo;
import net.swordie.ms.client.jobs.sengoku.Kanna;
import net.swordie.ms.client.party.Party;
import net.swordie.ms.client.party.PartyMember;
import net.swordie.ms.client.party.PartyResult;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.db.DatabaseManager;
import net.swordie.ms.connection.db.InlinedIntArrayConverter;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.enums.*;
import net.swordie.ms.handlers.ClientSocket;
import net.swordie.ms.handlers.EventManager;
import net.swordie.ms.handlers.PsychicLock;
import net.swordie.ms.life.*;
import net.swordie.ms.life.Merchant.EmployeeTrunk;
import net.swordie.ms.life.Merchant.Merchant;
import net.swordie.ms.life.Merchant.MerchantItem;
import net.swordie.ms.life.drop.Drop;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.pet.Pet;
import net.swordie.ms.loaders.EtcData;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.loaders.containerclasses.AndroidInfo;
import net.swordie.ms.loaders.containerclasses.ItemInfo;
import net.swordie.ms.scripts.ScriptInfo;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.scripts.ScriptType;
import net.swordie.ms.util.*;
import net.swordie.ms.world.Channel;
import net.swordie.ms.world.World;
import net.swordie.ms.world.field.*;
import net.swordie.ms.world.field.fieldeffect.FieldEffect;
import net.swordie.ms.world.gach.GachaponManager;
import net.swordie.ms.world.shop.NpcShopDlg;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.persistence.*;
import java.awt.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;
import static net.swordie.ms.enums.ChatType.SpeakerChannel;
import static net.swordie.ms.enums.ChatType.SystemNotice;
import static net.swordie.ms.enums.InvType.EQUIP;
import static net.swordie.ms.enums.InvType.EQUIPPED;
import static net.swordie.ms.enums.InventoryOperation.*;
import static net.swordie.ms.world.field.FieldInstanceType.CHANNEL;

/**
 * Created on 11/17/2017.
 */
@Entity
@Table(name = "characters")
public class Char {

	@Transient
	private static final Logger log = Logger.getLogger(Char.class);

	@Transient
	private Client client;
	private int rewardPoints;
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private int id;

	@Column(name = "accId")
	private int accId;

	@JoinColumn(name = "questManager")
	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	private QuestManager questManager;

	@JoinColumn(name = "equippedInventory")
	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	private Inventory equippedInventory = new Inventory(EQUIPPED, 52);

	@JoinColumn(name = "equipInventory")
	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	private Inventory equipInventory = new Inventory(EQUIP, 52);

	@JoinColumn(name = "consumeInventory")
	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	private Inventory consumeInventory = new Inventory(InvType.CONSUME, 52);

	@JoinColumn(name = "etcInventory")
	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	private Inventory etcInventory = new Inventory(InvType.ETC, 52);

	@JoinColumn(name = "installInventory")
	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	private Inventory installInventory = new Inventory(InvType.INSTALL, 52);

	@JoinColumn(name = "cashInventory")
	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	private Inventory cashInventory = new Inventory(InvType.CASH, 52);

	@JoinColumn(name = "avatarData")
	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	private AvatarData avatarData;

	@OneToMany(cascade = CascadeType.ALL)
	@JoinColumn(name = "charId")
	@OrderColumn(name = "ord")
	private List<FuncKeyMap> funcKeyMaps;

	@JoinColumn(name = "charId")
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	private Set<Skill> skills;

	@JoinColumn(name = "ownerID")
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	private Set<Friend> friends;

	@JoinColumn(name = "charID")
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	private Set<CharacterPotential> potentials;

	@JoinColumn(name = "charID")
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	private Set<Familiar> familiars;

	@JoinColumn(name = "charID")
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Macro> macros = new ArrayList<>();

	@JoinColumn(name = "guild")
	@OneToOne(cascade = CascadeType.ALL)
	private Guild guild;

	@JoinColumn(name = "monsterBook")
	@OneToOne(cascade = CascadeType.ALL)
	private MonsterBookInfo monsterBookInfo;

	@JoinColumn(name = "charID")
	@OneToMany(cascade = CascadeType.ALL)
	private Set<StolenSkill> stolenSkills;

	@JoinColumn(name = "charID")
	@OneToMany(cascade = CascadeType.ALL)
	private Set<ChosenSkill> chosenSkills;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "hyperrockfields", joinColumns = @JoinColumn(name = "CharID"))
	@Column(name = "fieldid")
	@OrderColumn(name = "ord")
	private int[] hyperrockfields = new int[13];

	@Column(name = "monsterparkcount")
	private byte monsterParkCount;

	private int partyID = 0; // Just for DB purposes
	private int previousFieldID;

	@Transient
	private int previousPortalID; // not super important so we wont save to db

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "skillcooltimes", joinColumns = @JoinColumn(name = "charID"))
	@MapKeyColumn(name = "skillid")
	@Column(name = "nextusabletime")
	private Map<Integer, Long> skillCoolTimes;
	@ElementCollection
	@CollectionTable(name = "itemsbuylimit", joinColumns = @JoinColumn(name = "charID"))
	@MapKeyColumn(name = "shopitemid")
	@Column(name = "amountbought")
	private Map<Long, Integer> itemBoughtAmounts;

	@Transient
	private CharacterPotentialMan potentialMan;

	@Transient
	private Ranking ranking;
	@Transient
	private int combatOrders;
	@Transient
	private List<ItemPot> itemPots;
	@Transient
	private List<Pet> pets;
	@Transient
	private List<FriendRecord> friendRecords;
	@Transient
	private List<ExpConsumeItem> expConsumeItems;
	@Transient
	private List<MonsterBattleMobInfo> monsterBattleMobInfos;
	@Transient
	private MonsterBattleLadder monsterBattleLadder;
	@Transient
	private MonsterBattleRankInfo monsterBattleRankInfo;
	@Transient
	private Position position;
	@Transient
	private Position oldPosition;
	@Transient
	private Field field;
	@Transient
	private byte moveAction;
	@Transient
	private TemporaryStatManager temporaryStatManager;
	@Transient
	private GachaponManager gachaponManager;
	@Transient
	private Job jobHandler;
	@Transient
	private MarriageRecord marriageRecord;
	@Transient
	private WildHunterInfo wildHunterInfo;
	@Transient
	private ZeroInfo zeroInfo;
	@Transient
	private int nickItem;
	@Transient
	private DamageSkinSaveData damageSkin = new DamageSkinSaveData(18, 2433063, false, "Je moeder");
	@Transient
	private DamageSkinSaveData premiumDamageSkin = new DamageSkinSaveData();
	@Transient
	private boolean partyInvitable;
	@Transient
	private ScriptManagerImpl scriptManagerImpl = new ScriptManagerImpl(this);
	@Transient
	private int driverID;
	@Transient
	private int passengerID;
	@Transient
	private int chocoCount;
	@Transient
	private int activeEffectItemID;
	@Transient
	private int monkeyEffectItemID;
	@Transient
	private int completedSetItemID;
	@Transient
	private short fieldSeatID;
	@Transient
	private int portableChairID;
	@Transient
	private String portableChairMsg;
	@Transient
	private short foothold;
	@Transient
	private int tamingMobLevel;
	@Transient
	private int tamingMobExp;
	@Transient
	private int tamingMobFatigue;
	@Transient
	private MiniRoom miniRoom;
	@Transient
	private String ADBoardRemoteMsg;
	@Transient
	private boolean inCouple;
	@Transient
	private CoupleRecord couple;
	@Transient
	private FriendshipRingRecord friendshipRingRecord;
	@Transient
	private int evanDragonGlide;
	@Transient
	private int kaiserMorphRotateHueExtern;
	@Transient
	private int kaiserMorphPrimiumBlack;
	@Transient
	private int kaiserMorphRotateHueInnner;
	@Transient
	private int makingMeisterSkillEff;
	@Transient
	private FarmUserInfo farmUserInfo;
	@Transient
	private int customizeEffect;
	@Transient
	private String customizeEffectMsg;
	@Transient
	private byte soulEffect;
	@Transient
	private FreezeHotEventInfo freezeHotEventInfo;
	@Transient
	private int eventBestFriendAID;
	@Transient
	private int mesoChairCount;
	@Transient
	private boolean beastFormWingOn;
	@Transient
	private int activeNickItemID;
	@Transient
	private int mechanicHue;
	@Transient
	private boolean online;
	@Transient
	private Party party;
	@Transient
	private FieldInstanceType fieldInstanceType;
	@Transient
	private Map<Integer, Field> fields = new HashMap<>();
	@Transient
	private int bulletIDForAttack;
	@Transient
	private NpcShopDlg shop;
	@Transient // yes
	private User user;
	@Transient // yes
	private Account account;
	@Transient
	private Client chatClient;
	@Transient
	private DamageCalc damageCalc;
	@Transient
	private boolean buffProtector;
	@Transient
	private int comboCounter;
	@Transient
	private ScheduledFuture comboKillResetTimer;
	@Transient
	private ScheduledFuture timeLimitTimer;
	@Transient
	private int deathCount = -1;
	@Transient
	private long runeStoneCooldown;
	@Transient
	private MemorialCubeInfo memorialCubeInfo;
	@Transient
	private Familiar activeFamiliar;
	@Transient
	private boolean skillCDBypass = false;
	// TODO Move this to CharacterStat?
	@Transient
	private Map<BaseStat, Long> baseStats = new HashMap<>();
	@Transient
	private boolean changingChannel;
	@Transient
	private TownPortal townPortal;
	@Transient
	private TradeRoom tradeRoom;
	@Transient
	private boolean battleRecordOn;
	@Transient
	private long nextRandomPortalTime;
    @Transient
    private Map<Integer, Integer> currentDirectionNode;
	@Transient
	private String lieDetectorAnswer = "";
	@Transient
	private long lastLieDetector = 0;
	// TOOD: count and log lie detector passes and fails
    @Transient
	private boolean tutor = false;
	@Transient
	private int transferField = 0;
	@Transient
	private int transferFieldReq = 0;
	@Transient
	private String blessingOfFairy = null;
	@Transient
	private String blessingOfEmpress = null;
	@Transient
	private Map<Integer, Integer> hyperPsdSkillsCooltimeR = new HashMap<>();
	@Transient
	private boolean isInvincible = false;
	@Convert(converter = InlinedIntArrayConverter.class)
	private List<Integer> quickslotKeys;
	@Transient
	private Android android;
	@Transient
	private Map<Integer, PsychicArea> psychicAreas;
	@Transient
	private Map<Integer, PsychicLock> psychicLocks;
	@Transient
	private Map<Integer, PsychicLockBall> psychicLockBalls;
	@Transient
	private Instance instance;
	@Transient
	private Merchant merchant;
	@Transient
	private Merchant visitingmerchant;


	public Char() {
		this(0, "", 0, 0, 0, (short) 0, (byte) -1, (byte) -1, 0, 0, new int[]{});
	}

	public Char(int accId, String name, int keySettingType, int eventNewCharSaleJob, int job, short curSelectedSubJob,
				byte gender, byte skin, int face, int hair, int[] items) {
		this.accId = accId;
		avatarData = new AvatarData();
		avatarData.setAvatarLook(new AvatarLook());
		AvatarLook avatarLook = avatarData.getAvatarLook();
		avatarLook.setGender(gender);
		avatarLook.setSkin(skin);
		avatarLook.setFace(face);
		avatarLook.setHair(hair);
		List<Integer> hairEquips = new ArrayList<>();
		for (int itemId : items) {
			Equip equip = ItemData.getEquipDeepCopyFromID(itemId, false);
			if (equip != null && ItemConstants.isEquip(itemId)) {
				hairEquips.add(itemId);
				if ("Wp".equals(equip.getiSlot())) {
					if (!equip.isCash()) {
						avatarLook.setWeaponId(itemId);
					} else {
						avatarLook.setWeaponStickerId(itemId);
					}
				}
			}
		}
		avatarLook.setHairEquips(hairEquips);
		avatarLook.setJob(job);
		CharacterStat characterStat = new CharacterStat(name, job);
		getAvatarData().setCharacterStat(characterStat);
		characterStat.setGender(gender);
		characterStat.setSkin(skin);
		characterStat.setFace(items.length > 0 ? items[0] : 0);
		characterStat.setHair(items.length > 1 ? items[1] : 0);
		characterStat.setSubJob(curSelectedSubJob);
		setFieldInstanceType(CHANNEL);
		ranking = new Ranking();
		pets = new ArrayList<>();
		questManager = new QuestManager(this);
		itemPots = new ArrayList<>();
		friendRecords = new ArrayList<>();
		expConsumeItems = new ArrayList<>();
		skills = new HashSet<>();
		temporaryStatManager = new TemporaryStatManager(this);
		gachaponManager = new GachaponManager();
		friends = new HashSet<>();
		monsterBookInfo = new MonsterBookInfo();
		potentialMan = new CharacterPotentialMan(this);
		skillCoolTimes = new HashMap<>();
		familiars = new HashSet<>();
		hyperrockfields = new int[]{
				999999999,
				999999999,
				999999999,

				999999999,
				999999999,
				999999999,

				999999999,
				999999999,
				999999999,

				999999999,
				999999999,
				999999999,

				999999999,
		};
		monsterParkCount = 0;
		currentDirectionNode = new HashMap<>();
		potentials = new HashSet<>();
//        monsterBattleMobInfos = new ArrayList<>();
//        monsterBattleLadder = new MonsterBattleLadder();
//        monsterBattleRankInfo = new MonsterBattleRankInfo();
		psychicAreas = new HashMap<>();
		psychicLocks = new HashMap<>();
		psychicLockBalls = new HashMap<>();
		funcKeyMaps = new ArrayList<FuncKeyMap>();
	}

	public static Char getFromDBById(int userId) {
		return (Char) DatabaseManager.getObjFromDB(Char.class, userId);
	}

	public static Char getFromDBByName(String name) {
		log.info(String.format("%s: Trying to get Char by name (%s).", LocalDateTime.now(), name));
		// DAO?
		Session session = DatabaseManager.getSession();
		Transaction transaction = session.beginTransaction();
		Query query = session.createQuery("FROM Char chr WHERE chr.avatarData.characterStat.name = :name");
		query.setParameter("name", name);
		List l = ((org.hibernate.query.Query) query).list();
		Char chr = null;
		if (l != null && l.size() > 0) {
			chr = (Char) l.get(0);
		}
		transaction.commit();
		session.close();
		return chr;
	}

	public static Char getFromDBByNameAndWorld(String name, int worldId) {
		Session session = DatabaseManager.getSession();
		Transaction transaction = session.beginTransaction();
		Query query = session.createQuery("FROM Char chr " +
				"WHERE chr.avatarData.characterStat.name = :name AND chr.avatarData.characterStat.worldIdForLog = :world");
		query.setParameter("name", name);
		query.setParameter("world", worldId);
		List l = ((org.hibernate.query.Query) query).list();
		Char chr = null;
		if (l != null && l.size() > 0) {
			chr = (Char) l.get(0);
		}
		transaction.commit();
		session.close();
		return chr;
	}

	public AvatarData getAvatarData() {
		return avatarData;
	}

	public Ranking getRanking() {
		return ranking;
	}

	public int getId() {
		return id;
	}

	public void setAvatarData(AvatarData avatarData) {
		this.avatarData = avatarData;
	}

	public void setRanking(Ranking ranking) {
		this.ranking = ranking;
	}

	public int getAccId() {
		return accId;
	}

	public void setAccId(int accId) {
		this.accId = accId;
	}

	public Inventory getEquippedInventory() {
		return equippedInventory;
	}

	public void addItemToInventory(InvType type, Item item, boolean hasCorrectBagIndex) {
		if (item == null) {
			return;
		}
		Inventory inventory = getInventoryByType(type);
		ItemInfo ii = ItemData.getItemInfoByID(item.getItemId());
		int quantity = item.getQuantity();
		if (inventory != null) {
			Item existingItem = inventory.getItemByItemIDAndStackable(item.getItemId());
			boolean rec = false;
			if (existingItem != null && existingItem.getInvType().isStackable() && existingItem.getQuantity() < ii.getSlotMax()) {
				if (quantity + existingItem.getQuantity() > ii.getSlotMax()) {
					quantity = ii.getSlotMax() - existingItem.getQuantity();
					item.setQuantity(item.getQuantity() - quantity);
					rec = true;
				}
				existingItem.addQuantity(quantity);
				write(WvsContext.inventoryOperation(true, false,
						UpdateQuantity, (short) existingItem.getBagIndex(), (byte) -1, 0, existingItem));
				Item copy = item.deepCopy();
				copy.setQuantity(quantity);
				if (rec) {
					addItemToInventory(item);
				}
			} else {
				if (!hasCorrectBagIndex) {
					item.setBagIndex(inventory.getFirstOpenSlot());
				}
				Item itemCopy = null;
				if (item.getInvType().isStackable() && ii != null && item.getQuantity() > ii.getSlotMax()) {
					itemCopy = item.deepCopy();
					quantity = quantity - ii.getSlotMax();
					itemCopy.setQuantity(quantity);
					item.setQuantity(ii.getSlotMax());
					rec = true;
				}
				inventory.addItem(item);
				write(WvsContext.inventoryOperation(true, false,
                        Add, (short) item.getBagIndex(), (byte) -1, 0, item));
				if (rec) {
					addItemToInventory(itemCopy);
				}
			}
			setBulletIDForAttack(calculateBulletIDForAttack(1));
		}
	}

	public void addItemToInventory(Item item) {
		addItemToInventory(item.getInvType(), item, false);
	}

	public void setEquippedInventory(Inventory equippedInventory) {
		this.equippedInventory = equippedInventory;
	}

	public Inventory getEquipInventory() {
		return equipInventory;
	}

	public void setEquipInventory(Inventory equipInventory) {
		this.equipInventory = equipInventory;
	}

	public Inventory getConsumeInventory() {
		return consumeInventory;
	}

	public void setConsumeInventory(Inventory consumeInventory) {
		this.consumeInventory = consumeInventory;
	}

	public Inventory getEtcInventory() {
		return etcInventory;
	}

	public void setEtcInventory(Inventory etcInventory) {
		this.etcInventory = etcInventory;
	}

	public Inventory getInstallInventory() {
		return installInventory;
	}

	public void setInstallInventory(Inventory installInventory) {
		this.installInventory = installInventory;
	}

	public Inventory getCashInventory() {
		return cashInventory;
	}

	public void setCashInventory(Inventory cashInventory) {
		this.cashInventory = cashInventory;
	}

	/**
	 * Encodes this Char's info inside a given {@link OutPacket}, with given info.
	 *
	 * @param outPacket The OutPacket this method should encode to.
	 * @param mask      Which info should be encoded.
	 */
	public void encode(OutPacket outPacket, DBChar mask) {

		// CharacterData::Decode
		outPacket.encodeLong(mask.get());
		outPacket.encodeByte(getCombatOrders());
		for (int i = 0; i < GameConstants.MAX_PET_AMOUNT; i++) {
			if (i < getPets().size()) {
				outPacket.encodeInt(getPets().get(i).getActiveSkillCoolTime());
			} else {
				outPacket.encodeInt(0);
			}
		}
		outPacket.encodeByte(0); // unk, not in kmst
		byte sizeByte = 0;
		outPacket.encodeByte(sizeByte);
		for (int i = 0; i < sizeByte; i++) {
			outPacket.encodeInt(0);
		}

		int sizee = 0;
		outPacket.encodeInt(sizee);
		for (int i = 0; i < sizee; i++) {
			outPacket.encodeInt(0); // nKey
			outPacket.encodeLong(0); // pInfo
		}
		outPacket.encodeByte(0); // again unsure
		if (mask.isInMask(DBChar.Character)) {
			getAvatarData().getCharacterStat().encode(outPacket);
			outPacket.encodeByte(getFriendRecords().size());
			boolean hasBlessingOfFairy = getBlessingOfFairy() != null;
			outPacket.encodeByte(hasBlessingOfFairy);
			if (hasBlessingOfFairy) {
				outPacket.encodeString(getBlessingOfFairy());
			}
			boolean hasBlessingOfEmpress = getBlessingOfEmpress() != null;
			outPacket.encodeByte(hasBlessingOfEmpress);
			if (hasBlessingOfEmpress) {
				outPacket.encodeString(getBlessingOfEmpress());
			}
			outPacket.encodeByte(false); // ultimate explorer, deprecated
		}
		if (mask.isInMask(DBChar.Money)) {
			outPacket.encodeLong(getMoney());
		}
		if (mask.isInMask(DBChar.ItemSlotConsume) || mask.isInMask(DBChar.ExpConsumeItem)) {
			outPacket.encodeInt(getExpConsumeItems().size());
			for (ExpConsumeItem eci : getExpConsumeItems()) {
				eci.encode(outPacket);
			}
		}
		if (mask.isInMask(DBChar.ItemSlotConsume) || mask.isInMask(DBChar.ShopBuyLimit)) {
			int size = 0;
			outPacket.encodeInt(size);
			for (int i = 0; i < size; i++) {
				outPacket.encodeInt(0);
				outPacket.encodeInt(0);
				outPacket.encodeInt(0);
				outPacket.encodeInt(0);
				outPacket.encodeInt(0);
				outPacket.encodeLong(0);
				outPacket.encodeLong(0);
			}
		}
		if (mask.isInMask(DBChar.MonsterBattleInfo)) {
			int count = 0; // MonsterBattle_MobInfo
			outPacket.encodeInt(count);
			if (getMonsterBattleMobInfos() != null) {
				for (MonsterBattleMobInfo mbmi : getMonsterBattleMobInfos()) {
					mbmi.encode(outPacket);
					// TODO int int int int int int byte int int
				}
			}
			outPacket.encodeInt(getId());

			outPacket.encodeInt(0);
			outPacket.encodeInt(0);
			outPacket.encodeInt(0);
			outPacket.encodeInt(0);
			outPacket.encodeInt(0);
			outPacket.encodeInt(0);
			outPacket.encodeInt(0);

			boolean hasMonsterBattleLadder = getMonsterBattleLadder() != null;
			outPacket.encodeByte(hasMonsterBattleLadder);
			if (hasMonsterBattleLadder) {
				getMonsterBattleLadder().encode(outPacket); // TODO GW_MonsterBattleLadder_UserInfo::Decode
			}
			boolean hasMonsterBattleRankInfo = getMonsterBattleRankInfo() != null;
			outPacket.encodeByte(hasMonsterBattleRankInfo);
			if (hasMonsterBattleRankInfo) {
				getMonsterBattleRankInfo().encode(outPacket); // TODO GW_MonsterBattleRankInfo::Decode(&dummyBLD, nSlotHyper);
			}
			outPacket.encodeByte(hasMonsterBattleRankInfo);
			// again?
			if (hasMonsterBattleRankInfo) {
				getMonsterBattleRankInfo().encode(outPacket); // TODO GW_MonsterBattleRankInfo::Decode(&dummyBLD, nSlotHyper);
			}
		}
		if (mask.isInMask(DBChar.InventorySize)) {
			outPacket.encodeByte(getEquipInventory().getSlots());
			outPacket.encodeByte(getConsumeInventory().getSlots());
			outPacket.encodeByte(getEtcInventory().getSlots());
			outPacket.encodeByte(getInstallInventory().getSlots());
			outPacket.encodeByte(getCashInventory().getSlots());
		}

		if (mask.isInMask(DBChar.AdminShopCount)) {
			outPacket.encodeInt(0); // ???
			outPacket.encodeInt(0);
		}
		if (mask.isInMask(DBChar.ItemSlotEquip)) {
			outPacket.encodeByte(0); // ?
			List<Item> equippedItems = new ArrayList<>(getEquippedInventory().getItems());
			equippedItems.sort(Comparator.comparingInt(Item::getBagIndex));
			// Normal equipped items
			for (Item item : equippedItems) {
				Equip equip = (Equip) item;
				if (item.getBagIndex() > BodyPart.BPBase.getVal() && item.getBagIndex() < BodyPart.BPEnd.getVal()) {
					outPacket.encodeShort(equip.getBagIndex());
					equip.encode(outPacket);
				}
			}
			outPacket.encodeShort(0);
			// Cash equipped items
			for (Item item : getEquippedInventory().getItems()) {
				Equip equip = (Equip) item;
				if (item.getBagIndex() >= BodyPart.CBPBase.getVal() && item.getBagIndex() <= BodyPart.CBPEnd.getVal()) {
					outPacket.encodeShort(equip.getBagIndex() - 100);
					equip.encode(outPacket);
				}
			}
			outPacket.encodeShort(0);
			// Equip inventory
			for (Item item : getEquipInventory().getItems()) {
				Equip equip = (Equip) item;
				outPacket.encodeShort(equip.getBagIndex());
				equip.encode(outPacket);
			}
			outPacket.encodeShort(0);
			// NonBPEquip::Decode (Evan)
			for (Item item : getEquippedInventory().getItems()) {
				Equip equip = (Equip) item;
				if (item.getBagIndex() >= BodyPart.EvanBase.getVal() && item.getBagIndex() < BodyPart.EvanEnd.getVal()) {
					outPacket.encodeShort(equip.getBagIndex());
					equip.encode(outPacket);
				}
			}
			outPacket.encodeShort(0);
			// VirtualEquipInventory::Decode (Android)
			// >= 20k < 200024?
			for (Item item : getEquippedInventory().getItems()) {
				Equip equip = (Equip) item;
				if (item.getBagIndex() >= BodyPart.MechBase.getVal() && item.getBagIndex() < BodyPart.MechEnd.getVal()) {
					outPacket.encodeShort(equip.getBagIndex());
					equip.encode(outPacket);
				}
			}
			outPacket.encodeShort(0);
			// Guessing pet consume items, could very well be wrong
			for (Item item : getEquippedInventory().getItems()) {
				Equip equip = (Equip) item;
				if (item.getBagIndex() >= 200 && item.getBagIndex() <= 300) {
					outPacket.encodeShort(equip.getBagIndex());
					equip.encode(outPacket);
				}
			}
			outPacket.encodeShort(0);
			// Android
			for (Item item : getEquippedInventory().getItems()) {
				Equip equip = (Equip) item;
				if (item.getBagIndex() >= BodyPart.APBase.getVal() && item.getBagIndex() <= BodyPart.APEnd.getVal()) {
					outPacket.encodeShort(equip.getBagIndex());
					equip.encode(outPacket);
				}
			}
			outPacket.encodeShort(0);
			// Angelic Buster
			for (Item item : getEquippedInventory().getItems()) {
				Equip equip = (Equip) item;
				if (item.getBagIndex() >= BodyPart.DUBase.getVal() && item.getBagIndex() < BodyPart.DUEnd.getVal()) {
					outPacket.encodeShort(equip.getBagIndex());
					equip.encode(outPacket);
				}
			}
			outPacket.encodeShort(0);
			// Bits
			for (Item item : getEquippedInventory().getItems()) {
				Equip equip = (Equip) item;
				if (item.getBagIndex() >= BodyPart.BitsBase.getVal() && item.getBagIndex() < BodyPart.BitsEnd.getVal()) {
					outPacket.encodeShort(equip.getBagIndex());
					equip.encode(outPacket);
				}
			}
			outPacket.encodeShort(0);
			// Zero
			for (Item item : getEquippedInventory().getItems()) {
				Equip equip = (Equip) item;
				if (item.getBagIndex() >= BodyPart.ZeroBase.getVal() && item.getBagIndex() < BodyPart.ZeroEnd.getVal()) {
					outPacket.encodeShort(equip.getBagIndex());
					equip.encode(outPacket);
				}
			}
			outPacket.encodeShort(0);
			// Totems
			for (Item item : getEquippedInventory().getItems()) {
				Equip equip = (Equip) item;
				if (item.getBagIndex() >= BodyPart.TotemBase.getVal() && item.getBagIndex() < BodyPart.TotemEnd.getVal()) {
					outPacket.encodeShort(equip.getBagIndex());
					equip.encode(outPacket);
				}
			}
			outPacket.encodeShort(0);
			// Maybe zero beta cash?
			for (Item item : getEquippedInventory().getItems()) {
				Equip equip = (Equip) item;
				if (item.getBagIndex() >= BodyPart.MBPBase.getVal() && item.getBagIndex() < BodyPart.MBPEnd.getVal()) {
					outPacket.encodeShort(equip.getBagIndex());
					equip.encode(outPacket);
				}
			}
			outPacket.encodeShort(0);
			// Haku
			for (Item item : getEquippedInventory().getItems()) {
				Equip equip = (Equip) item;
				if (item.getBagIndex() >= BodyPart.HakuStart.getVal() && item.getBagIndex() < BodyPart.HakuEnd.getVal()) {
					outPacket.encodeShort(equip.getBagIndex());
					equip.encode(outPacket);
				}
			}
			outPacket.encodeShort(0);
			outPacket.encodeShort(0);
			outPacket.encodeShort(0);
		}
		if (mask.isInMask(DBChar.ItemSlotConsume)) {
			for (Item item : getConsumeInventory().getItems()) {
				outPacket.encodeByte(item.getBagIndex());
				item.encode(outPacket);
			}
			outPacket.encodeByte(0);
		}
		if (mask.isInMask(DBChar.ItemSlotInstall)) {
			for (Item item : getInstallInventory().getItems()) {
				outPacket.encodeByte(item.getBagIndex());
				item.encode(outPacket);
			}
			outPacket.encodeByte(0);
		}
		if (mask.isInMask(DBChar.ItemSlotEtc)) {
			for (Item item : getEtcInventory().getItems()) {
				outPacket.encodeByte(item.getBagIndex());
				item.encode(outPacket);
			}
			outPacket.encodeByte(0);
		}
		if (mask.isInMask(DBChar.ItemSlotCash)) {
			for (Item item : getCashInventory().getItems()) {
				outPacket.encodeByte(item.getBagIndex());
				item.encode(outPacket);
			}
			outPacket.encodeByte(0);
		}
		// BagDatas
		if (mask.isInMask(DBChar.ItemSlotConsume)) {
			// TODO
			outPacket.encodeInt(0);
		}
		if (mask.isInMask(DBChar.ItemSlotInstall)) {
			// TODO
			outPacket.encodeInt(0);
		}
		if (mask.isInMask(DBChar.ItemSlotEtc)) {
			// TODO
			outPacket.encodeInt(0);
		}
		if (mask.isInMask(DBChar.ItemSlotCash)) {
			// TODO
			outPacket.encodeInt(0);
		}
		// End bagdatas
		if (mask.isInMask(DBChar.CoreAura)) {
			int val = 0;
			outPacket.encodeInt(val);
			for (int i = 0; i < val; i++) {
				outPacket.encodeInt(0);
				outPacket.encodeLong(0);
			}
		}
		if (mask.isInMask(DBChar.ItemPot)) {
			boolean hasItemPot = getItemPots() != null;
			outPacket.encodeByte(hasItemPot);
			if (hasItemPot) {
				for (int i = 0; i < getItemPots().size(); i++) {
					getItemPots().get(i).encode(outPacket);
					outPacket.encodeByte(i != getItemPots().size() - 1);
				}
			}
		}

		if (mask.isInMask(DBChar.SkillRecord)) {
			boolean encodeSkills = getSkills().size() > 0;
			outPacket.encodeByte(encodeSkills);
			if (encodeSkills) {
				Set<LinkSkill> linkSkills = getLinkSkills();
				short size = (short) (getSkills().size() + linkSkills.size());
				outPacket.encodeShort(size);
				for (Skill skill : getSkills()) {
					outPacket.encodeInt(skill.getSkillId());
					outPacket.encodeInt(skill.getCurrentLevel());
					outPacket.encodeFT(FileTime.fromType(FileTime.Type.MAX_TIME));
					if (SkillConstants.isSkillNeedMasterLevel(skill.getSkillId())) {
						outPacket.encodeInt(skill.getMasterLevel());
					}
				}
				for (LinkSkill linkSkill : linkSkills) {
					outPacket.encodeInt(linkSkill.getLinkSkillID());
					outPacket.encodeInt(linkSkill.getOwnerID());
					outPacket.encodeFT(FileTime.fromType(FileTime.Type.MAX_TIME));
					if (SkillConstants.isSkillNeedMasterLevel(linkSkill.getLinkSkillID())) {
						outPacket.encodeInt(3); // whatevs
					}
				}
				outPacket.encodeShort(linkSkills.size());
				for (LinkSkill linkSkill : linkSkills) {
					outPacket.encodeInt(linkSkill.getLinkSkillID()); // another nCount
					outPacket.encodeShort(linkSkill.getLevel() - 1); // idk
				}
			} else {
				short size = 0;
				outPacket.encodeShort(size);
				for (int i = 0; i < size; i++) {
					outPacket.encodeInt(0); // nTI
					outPacket.encodeInt(0); // sValue
				}
				short size2 = 0;
				outPacket.encodeShort(size2);
				for (int i = 0; i < size2; i++) {
					outPacket.encodeInt(0); // nTI
				}

				short size3 = 0;
				outPacket.encodeShort(size3);
				for (int i = 0; i < size3; i++) {
					outPacket.encodeInt(0); // nTI
					outPacket.encodeFT(new FileTime(0)); // pInfo
				}
				short size4 = 0;
				outPacket.encodeShort(size2);
				for (int i = 0; i < size2; i++) {
					outPacket.encodeInt(0); // nTI
				}

				short size5 = 0;
				outPacket.encodeShort(size);
				for (int i = 0; i < size; i++) {
					outPacket.encodeInt(0); // nTI
					outPacket.encodeInt(0); // sValue
				}
				short size6 = 0;
				outPacket.encodeShort(size2);
				for (int i = 0; i < size2; i++) {
					outPacket.encodeInt(0); // nTI
				}
			}
		}

		if (mask.isInMask(DBChar.SkillCooltime)) {
			long curTime = System.currentTimeMillis();
			Map<Integer, Long> cooltimes = new HashMap<>();
			getSkillCoolTimes().forEach((key, value) -> {
				if (value - curTime > 0) {
					cooltimes.put(key, value);
				}
			});
			outPacket.encodeShort(cooltimes.size());
			for (Map.Entry<Integer, Long> cooltime : cooltimes.entrySet()) {
				outPacket.encodeInt(cooltime.getKey()); // nSkillId
				outPacket.encodeInt((int) ((cooltime.getValue() - curTime) / 1000)); // nSkillCooltime
			}
		}
		if (mask.isInMask(DBChar.QuestRecord)) {
			// modified/deleted, not completed anyway
			boolean removeAllOldEntries = true;
			outPacket.encodeByte(removeAllOldEntries);
			short size = (short) getQuestManager().getQuestsInProgress().size();
			outPacket.encodeShort(size);
			for (Quest quest : getQuestManager().getQuestsInProgress()) {
				outPacket.encodeInt(quest.getQRKey());
				outPacket.encodeString(quest.getQRValue());
			}
			if (!removeAllOldEntries) {
				// blacklisted quests
				short size2 = 0;
				outPacket.encodeShort(size2);
				for (int i = 0; i < size2; i++) {
					outPacket.encodeInt(0); // nQRKey
				}
			}
			size = 0;
			outPacket.encodeShort(size);
			// Not sure what this is for
			for (int i = 0; i < size; i++) {
				outPacket.encodeString("");
				outPacket.encodeString("");
			}
		}
		if (mask.isInMask(DBChar.QuestComplete)) {
			boolean removeAllOldEntries = true;
			outPacket.encodeByte(removeAllOldEntries);
			Set<Quest> completedQuests = getQuestManager().getCompletedQuests();
			outPacket.encodeShort(completedQuests.size());
			for (Quest quest : completedQuests) {
				outPacket.encodeInt(quest.getQRKey());
				outPacket.encodeInt(0); // Timestamp of completion
			}
			if (!removeAllOldEntries) {
				short size = 0;
				outPacket.encodeShort(size);
				for (int i = 0; i < size; i++) {
					outPacket.encodeInt(0); // nQRKey?
				}
			}
		}
		if (mask.isInMask(DBChar.MinigameRecord)) {
			int size = 0;
			outPacket.encodeShort(size);
			for (int i = 0; i < size; i++) {
				new MiniGameRecord().encode(outPacket);
			}
		}
		if (mask.isInMask(DBChar.CoupleRecord)) {
			int coupleSize = 0;
			outPacket.encodeShort(coupleSize);
			for (int i = 0; i < coupleSize; i++) {
				new CoupleRecord().encode(outPacket);
			}
			int friendSize = 0;
			outPacket.encodeShort(friendSize);
			for (int i = 0; i < friendSize; i++) {
				new FriendRecord().encode(outPacket);
			}
			int marriageSize = 0;
			outPacket.encodeShort(marriageSize);
			for (int i = 0; i < marriageSize; i++) {
				new MarriageRecord().encode(outPacket);
			}
		}

		if (mask.isInMask(DBChar.MapTransfer)) {
			for (int i = 0; i < 5; i++) {
				outPacket.encodeInt(0);
			}
			for (int i = 0; i < 10; i++) {
				outPacket.encodeInt(0);
			}
			for (int i = 0; i < 13; i++) {
				outPacket.encodeInt(0);
			}
			for (int i = 0; i < 13; i++) {
				outPacket.encodeInt(0);
			}
		}
		if (mask.isInMask(DBChar.MonsterBookCover)) {
			outPacket.encodeInt(getMonsterBookInfo().getCoverID());
		}
		if (mask.isInMask(DBChar.MonsterBookCard)) {
			boolean isCompleted = false;
			outPacket.encodeByte(isCompleted);
			if (!isCompleted) {
				short size = (short) getMonsterBookInfo().getCards().size();
				outPacket.encodeShort(size);
				for (int card : getMonsterBookInfo().getCards()) {
					outPacket.encodeShort(card);
					outPacket.encodeByte(true); // bEnabled?
				}
			} else {
				outPacket.encodeShort(0); // card list size
				short encSize = 0;
				outPacket.encodeShort(encSize);
				outPacket.encodeArr(new byte[encSize]);
				encSize = 0;
				outPacket.encodeShort(encSize);
				outPacket.encodeArr(new byte[encSize]);
			}
			outPacket.encodeInt(getMonsterBookInfo().getSetID()); // monsterbook set
		}
		if (mask.isInMask(DBChar.QuestCompleteOld)) {
			short size = 0;
			outPacket.encodeShort(size);
			for (int i = 0; i < size; i++) {
				outPacket.encodeShort(0);
			}
		}
		if (mask.isInMask(DBChar.Familiar)) {
			outPacket.encodeInt(getFamiliars().size());
			for (Familiar familiar : getFamiliars()) {
				familiar.encode(outPacket);
			}
		}
		if (mask.isInMask(DBChar.NewYearCard)) {
			short size = 0;
			outPacket.encodeShort(size);
			for (int i = 0; i < size; i++) {
				outPacket.encodeInt(0);
				outPacket.encodeInt(0);
				outPacket.encodeString("");
				outPacket.encodeByte(0);
				outPacket.encodeLong(0);
				outPacket.encodeInt(0);
				outPacket.encodeString("");
				outPacket.encodeByte(0);
				outPacket.encodeByte(0);
				outPacket.encodeLong(0);
				outPacket.encodeString("");
			}
		}
		if (mask.isInMask(DBChar.QuestRecordEx)) {
			outPacket.encodeShort(getQuestManager().getEx().size());
			for (Quest quest : getQuestManager().getEx()) {
				outPacket.encodeInt(quest.getQRKey());
				outPacket.encodeString(quest.getQRValue());
			}
		}
		if (mask.isInMask(DBChar.Avatar)) {

			short size = 0;
			outPacket.encodeShort(size);
			for (int i = 0; i < size; i++) {
				outPacket.encodeInt(0); // sValue
				new AvatarLook().encode(outPacket);
			}
		}
		if (mask.isInMask(DBChar.MapTransfer)) {
			int size = 0;
			outPacket.encodeInt(0);
			for (int i = 0; i < size; i++) {
				outPacket.encodeInt(0);
				outPacket.encodeInt(0);
			}
		}
		if (mask.isInMask(DBChar.WildHunterInfo)) {
			if (JobConstants.isWildHunter(getAvatarData().getCharacterStat().getJob())) {
				getWildHunterInfo().encode(outPacket); // GW_WildHunterInfo::Decode
			}
		}
		if (mask.isInMask(DBChar.ZeroInfo)) {
			if (JobConstants.isZero(getAvatarData().getCharacterStat().getJob())) {
				if (getZeroInfo() == null) {
					initZeroInfo();
				}
				getZeroInfo().encode(outPacket); // ZeroInfo::Decode
			}
		}
		if (mask.isInMask(DBChar.ShopBuyLimit)) {
			short size = 0;
			outPacket.encodeShort(size);
			for (int i = 0; i < size; i++) {
				// Encode shop buy limit
			}
		}
		if (mask.isInMask(DBChar.StolenSkills)) {
			if (JobConstants.isPhantom(getAvatarData().getCharacterStat().getJob())) {
				for (int i = 0; i < 15; i++) {
					StolenSkill stolenSkill = getStolenSkillByPosition(i);
					outPacket.encodeInt(stolenSkill == null ? 0 : stolenSkill.getSkillid());
				}
			} else {
				outPacket.encodeInt(0);
				outPacket.encodeInt(0);
				outPacket.encodeInt(0);
				outPacket.encodeInt(0);

				outPacket.encodeInt(0);
				outPacket.encodeInt(0);
				outPacket.encodeInt(0);
				outPacket.encodeInt(0);

				outPacket.encodeInt(0);
				outPacket.encodeInt(0);
				outPacket.encodeInt(0);

				outPacket.encodeInt(0);
				outPacket.encodeInt(0);

				outPacket.encodeInt(0);
				outPacket.encodeInt(0);
			}
		}
		if (mask.isInMask(DBChar.ChosenSkills)) {
			if (JobConstants.isPhantom(getAvatarData().getCharacterStat().getJob())) {
				for (int i = 1; i <= 5; i++) { //Shifted by +1 to accomodate the Skill Management Tabs
					ChosenSkill chosenSkill = getChosenSkillByPosition(i);
					outPacket.encodeInt(chosenSkill == null
							? 0
							: isChosenSkillInStolenSkillList(chosenSkill.getSkillId())
							? chosenSkill.getSkillId()
							: 0
					);
				}
			} else {
				for (int i = 0; i < 5; i++) {
					outPacket.encodeInt(0);
				}
			}
		}
		if (mask.isInMask(DBChar.CharacterPotentialSkill)) {
			outPacket.encodeShort(getPotentials().size());
			for (CharacterPotential cp : getPotentials()) {
				cp.encode(outPacket);
			}
		}
		if (mask.isInMask(DBChar.SoulCollection)) {
			short size = 0;
			outPacket.encodeShort(size);
			for (int i = 0; i < size; i++) {
				outPacket.encodeInt(0); //
				outPacket.encodeInt(0); //
			}
		}
		sizee = 0;
		outPacket.encodeInt(sizee);
		for (int i = 0; i < sizee; i++) {
			outPacket.encodeString("");
			// sub_73A1A0
			outPacket.encodeInt(0);
			outPacket.encodeString("");
			int size = 0;
			outPacket.encodeInt(size);
			for (int j = 0; j < size; j++) {
				outPacket.encodeByte(0);
			}
		}
		outPacket.encodeByte(0); // idk
		if (mask.isInMask(DBChar.Character)) {
			outPacket.encodeInt(0); // honor level, deprecated
			outPacket.encodeInt(getHonorExp()); // honor exp
		}
		if (mask.isInMask(DBChar.Money)) {
			boolean shouldIEncodeThis = true;
			outPacket.encodeByte(shouldIEncodeThis);
			if (shouldIEncodeThis) {
				short size = 0;
				outPacket.encodeShort(size);
				for (int i = 0; i < size; i++) {
					short category = 0;
					outPacket.encodeShort(category);
					short size2 = 0;
					outPacket.encodeShort(size2);
					for (int i2 = 0; i2 < size2; i2++) {
						outPacket.encodeInt(0); // nItemId
						outPacket.encodeInt(0); // nCount
					}
				}
			} else {
				short size2 = 0;
				outPacket.encodeShort(size2);
				for (int i2 = 0; i2 < size2; i2++) {
					outPacket.encodeShort(0); // nCategory
					outPacket.encodeInt(0); // nItemId
					outPacket.encodeInt(0); // nCount
				}

			}
		}
		if (mask.isInMask(DBChar.ReturnEffectInfo)) {
//            getReturnEffectInfo().encode(outPacket); // ReturnEffectInfo::Decode
			outPacket.encodeByte(0);
		}
		if (mask.isInMask(DBChar.DressUpInfo)) {
			new DressUpInfo().encode(outPacket); // GW_DressUpInfo::Decode
		}
		if (mask.isInMask(DBChar.MonsterCollection)) {
			outPacket.encodeInt(1);
			outPacket.encodeInt(0);
			outPacket.encodeLong(0);
			outPacket.encodeString("");
			outPacket.encodeInt(0);
		}
		if (mask.isInMask(DBChar.CoreInfo)) {
			// GW_Core
			short size = 0;
			outPacket.encodeShort(size);
			for (int i = 0; i < size; i++) {
				outPacket.encodeShort(0); // nPos
				outPacket.encodeInt(0); // nCoreID
				outPacket.encodeInt(0); // nLeftCount
			}

			size = 0;
			outPacket.encodeShort(size);
			for (int i = 0; i < size; i++) {
				outPacket.encodeShort(0); // nPos
				outPacket.encodeInt(0); // nCoreID
				outPacket.encodeInt(0); // nLeftCount
			}
		}
		if (mask.isInMask(DBChar.FarmPotential)) {
			new FarmPotential().encode(outPacket); // FARM_POTENTIAL::Decode
		}
		if (mask.isInMask(DBChar.FarmUserInfo)) {
			new FarmUserInfo().encode(outPacket); // FarmUserInfo::Decode
			outPacket.encodeInt(0);
			outPacket.encodeInt(0);
		}
		if (mask.isInMask(DBChar.MemorialCubeInfo)) {
			new MemorialCubeInfo().encode(outPacket); // MemorialCubeInfo::Decode
		}
		if (mask.isInMask(DBChar.LikePoint)) {
			new LikePoint().encode(outPacket);
		}
		if (mask.isInMask(DBChar.RunnerGameRecord)) {
			new RunnerGameRecord().encode(outPacket); // RunnerGameRecord::Decode
		}
		short sizeO = 0;
		outPacket.encodeShort(sizeO);
		for (int i = 0; i < sizeO; i++) {
			outPacket.encodeInt(0);
			outPacket.encodeString("");
		}
		if (mask.isInMask(DBChar.MonsterCollection)) {
			Set<MonsterCollectionExploration> mces = getAccount().getMonsterCollection().getMonsterCollectionExplorations();
			outPacket.encodeShort(mces.size());
			for (MonsterCollectionExploration mce : mces) {
				outPacket.encodeInt(mce.getPosition());
				outPacket.encodeString(mce.getValue(true));
			}
		}
		boolean farmOnline = false;
		outPacket.encodeByte(farmOnline);
		int sizeInt = 0;
		// CharacterData::DecodeTextEquipInfo
		outPacket.encodeInt(sizeInt);
		for (int i = 0; i < sizeInt; i++) {
			outPacket.encodeInt(0);
			outPacket.encodeString("");
		}

		if (mask.isInMask(DBChar.VisitorLog4)) {
			// mushy
			outPacket.encodeByte(1);
			outPacket.encodeByte(0);
			outPacket.encodeInt(1);
			outPacket.encodeInt(0);
			outPacket.encodeInt(100);
			outPacket.encodeFT(FileTime.fromType(FileTime.Type.MAX_TIME));
			outPacket.encodeShort(0);
			outPacket.encodeShort(0);
		}

		if (mask.isInMask(DBChar.Unk4)) {
			outPacket.encodeByte(0);
		}

		if (mask.isInMask(DBChar.Unk)) {
			outPacket.encodeInt(0);
			outPacket.encodeInt(0);
		}

		if (mask.isInMask(DBChar.CoreAura)) {
			outPacket.encodeInt(0);
			outPacket.encodeInt(0);
			outPacket.encodeInt(0);
			outPacket.encodeInt(0);
			outPacket.encodeInt(0);

			outPacket.encodeInt(0);
			outPacket.encodeInt(0);
			outPacket.encodeInt(0);
			outPacket.encodeInt(0);
			outPacket.encodeInt(0);
			outPacket.encodeInt(0);

			outPacket.encodeInt(0);
			outPacket.encodeInt(0);
			outPacket.encodeInt(0);
			outPacket.encodeInt(0);

			outPacket.encodeLong(0);
			outPacket.encodeByte(0);

			outPacket.encodeByte(1);
		}

		if (mask.isInMask(DBChar.EquipExt)) {
			short size = 0;
			outPacket.encodeShort(size);
			for (int i = 0; i < size; i++) {
				outPacket.encodeShort(0);
				outPacket.encodeShort(0);
			}
		}

		if (mask.isInMask(DBChar.RedLeafInfo)) {
			// red leaf information
			outPacket.encodeInt(getAccId());
			outPacket.encodeInt(getId());
			outPacket.encodeInt(0);
			outPacket.encodeInt(0);
		}
		outPacket.encodeArr(new byte[32]); // real

	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Char && ((Char) other).getId() == getId();
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	private String getBlessingOfEmpress() {
		return blessingOfEmpress;
	}

	public void setBlessingOfEmpress(String blessingOfEmpress) {
		this.blessingOfEmpress = blessingOfEmpress;
	}

	private String getBlessingOfFairy() {
		return blessingOfFairy;
	}

	public void setBlessingOfFairy(String blessingOfFairy) {
		this.blessingOfFairy = blessingOfFairy;
	}

	public void setCombatOrders(int combatOrders) {
		this.combatOrders = combatOrders;
	}

	public int getCombatOrders() {
		return combatOrders;
	}

	public QuestManager getQuestManager() {
		if (questManager.getChr() == null) {
			questManager.setChr(this);
		}
		return questManager;
	}

	public void setQuests(QuestManager questManager) {
		this.questManager = questManager;
	}

	public List<ItemPot> getItemPots() {
		return null;
	}

	public void setItemPots(List<ItemPot> itemPots) {
		this.itemPots = itemPots;
	}

	public List<Pet> getPets() {
		return pets;
	}

	public void setPets(List<Pet> pets) {
		this.pets = pets;
	}

	public List<FriendRecord> getFriendRecords() {
		return friendRecords;
	}

	public void setFriendRecords(List<FriendRecord> friendRecords) {
		this.friendRecords = friendRecords;
	}

	public long getMoney() {
		return getAvatarData().getCharacterStat().getMoney();
	}

	public List<ExpConsumeItem> getExpConsumeItems() {
		return expConsumeItems;
	}

	public void setExpConsumeItems(List<ExpConsumeItem> expConsumeItems) {
		this.expConsumeItems = expConsumeItems;
	}

	public List<MonsterBattleMobInfo> getMonsterBattleMobInfos() {
		return monsterBattleMobInfos;
	}

	public void setMonsterBattleMobInfos(List<MonsterBattleMobInfo> monsterBattleMobInfos) {
		this.monsterBattleMobInfos = monsterBattleMobInfos;
	}

	public MonsterBattleLadder getMonsterBattleLadder() {
		return monsterBattleLadder;
	}

	public void setMonsterBattleLadder(MonsterBattleLadder monsterBattleLadder) {
		this.monsterBattleLadder = monsterBattleLadder;
	}

	public MonsterBattleRankInfo getMonsterBattleRankInfo() {
		return monsterBattleRankInfo;
	}

	public void setMonsterBattleRankInfo(MonsterBattleRankInfo monsterBattleRankInfo) {
		this.monsterBattleRankInfo = monsterBattleRankInfo;
	}

	public List<Inventory> getInventories() {
		return new ArrayList<>(Arrays.asList(getEquippedInventory(), getEquipInventory(),
				getConsumeInventory(), getEtcInventory(), getInstallInventory(), getCashInventory()));
	}

	public Inventory getInventoryByType(InvType invType) {
		switch (invType) {
			case EQUIPPED:
				return getEquippedInventory();
			case EQUIP:
				return getEquipInventory();
			case CONSUME:
				return getConsumeInventory();
			case ETC:
				return getEtcInventory();
			case INSTALL:
				return getInstallInventory();
			case CASH:
				return getCashInventory();
			default:
				return null;
		}
	}

	public Client getClient() {
		return client;
	}

	public void setClient(Client client) {
		this.client = client;
	}

	public int getFieldID() {
		return (int) getAvatarData().getCharacterStat().getPosMap();
	}

	private void setFieldID(int fieldID) {
		getAvatarData().getCharacterStat().setPosMap(fieldID);
	}

	public Position getPosition() {
		return position;
	}

	public void setPosition(Position position) {
		this.position = position;
	}

	public void setField(Field field) {
		this.field = field;
		setFieldID(field.getId());
	}

	public Field getField() {
		return field;
	}

	/**
	 * Sets the job of this Char with a given id. Does nothing if the id is invalid.
	 * If it is valid, will set this Char's job, add all Skills that the job should have by default,
	 * and sends the info to the client.
	 *
	 * @param id
	 */
	public void setJob(int id) {
		JobConstants.JobEnum job = JobConstants.JobEnum.getJobById((short) id);
		if (job == null) {
			return;
		}
		getAvatarData().getCharacterStat().setJob(id);
		setJobHandler(JobManager.getJobById((short) id, this));
		List<Skill> skills = SkillData.getSkillsByJob((short) id);
		skills.forEach(skill -> addSkill(skill, true));
		getClient().write(WvsContext.changeSkillRecordResult(skills, true, false, false, false));
		notifyChanges();
	}

	public short getJob() {
		return getAvatarData().getCharacterStat().getJob();
	}

	/**
	 * Sets the SP to the current job level.
	 *
	 * @param num The new SP amount.
	 */
	public void setSpToCurrentJob(int num) {
		if (JobConstants.isExtendSpJob(getJob())) {
			byte jobLevel = (byte) JobConstants.getJobLevel(getJob());
			getAvatarData().getCharacterStat().getExtendSP().setSpToJobLevel(jobLevel, num);
		} else {
			getAvatarData().getCharacterStat().setSp(num);
		}
	}

	/**
	 * Sets the SP to the job level according to the current level.
	 *
	 * @param num The amount of SP to add
	 */
	public void addSpToJobByCurrentLevel(int num) {
		CharacterStat cs = getAvatarData().getCharacterStat();
		if (JobConstants.isExtendSpJob(getJob())) {
			byte jobLevel = (byte) JobConstants.getJobLevelByCharLevel(getJob(), getLevel());
			num += cs.getExtendSP().getSpByJobLevel(jobLevel);
			getAvatarData().getCharacterStat().getExtendSP().setSpToJobLevel(jobLevel, num);
		} else {
			num += cs.getSp();
			getAvatarData().getCharacterStat().setSp(num);
		}
	}

	public Set<Skill> getSkills() {
		return skills;
	}

	public void setSkills(Set<Skill> skills) {
		this.skills = skills;
	}

	/**
	 * Adds a {@link Skill} to this Char. Changes the old Skill if the Char already has a Skill
	 * with the same id. Removes the skill if the given skill's id is 0.
	 *
	 * @param skill The Skill this Char should get.
	 */
	public void addSkill(Skill skill) {
		addSkill(skill, false);
	}

	/**
	 * Adds a {@link Skill} to this Char. Changes the old Skill if the Char already has a Skill
	 * with the same id. Removes the skill if the given skill's id is 0.
	 *
	 * @param skill The Skill this Char should get.
	 * @param addRegardlessOfLevel if this is true, the skill will not be removed from the char, even if the cur level
	 *                             of the given skill is 0.
	 */
	public void addSkill(Skill skill, boolean addRegardlessOfLevel) {
		if (!addRegardlessOfLevel && skill.getCurrentLevel() == 0) {
			removeSkill(skill.getSkillId());
			return;
		}
		skill.setCharId(getId());
		boolean isPassive = SkillConstants.isPassiveSkill(skill.getSkillId());
		boolean isChanged;
		if (getSkills().stream().noneMatch(s -> s.getSkillId() == skill.getSkillId())) {
			getSkills().add(skill);
			isChanged = true;
		} else {
			Skill oldSkill = getSkill(skill.getSkillId());
			isChanged = oldSkill.getCurrentLevel() != skill.getCurrentLevel();
			if (isPassive && isChanged) {
				removeFromBaseStatCache(oldSkill);
			}
			oldSkill.setCurrentLevel(skill.getCurrentLevel());
			oldSkill.setMasterLevel(skill.getMasterLevel());
		}
		// Change cache accordingly
		if (isPassive && isChanged) {
			addToBaseStatCache(skill);
		}
	}

	/**
	 * Removes a Skill from this Char.
	 * @param skillID the id of the skill that should be removed
	 */
	public void removeSkill(int skillID) {
		Skill skill = Util.findWithPred(getSkills(), s -> s.getSkillId() == skillID);
		if (skill != null) {
			if (SkillConstants.isPassiveSkill(skillID)) {
				removeFromBaseStatCache(skill);
			}
			getSkills().remove(skill);

		}
	}

    /**
     * Removes a Skill from this Char.
     * Sends change skill record to remove the skill from the client.
     * @param skillID the id of the skill that should be removed
     */
	public void removeSkillAndSendPacket(int skillID) {
        Skill skill = getSkill(skillID);
        if (skill != null) {
            removeSkill(skillID);
            skill.setCurrentLevel(-1);
            skill.setMasterLevel(-1);
            write(WvsContext.changeSkillRecordResult(Collections.singletonList(skill), true, false, false, false));
        }
    }

	/**
	 * Initializes the BaseStat cache, by going through all the needed passive stat changers.
	 */
	public void initBaseStats() {
		getBaseStats().clear();
		Map<BaseStat, Long> stats = getBaseStats();
		stats.put(BaseStat.cr, 5L);
		stats.put(BaseStat.minCd, 20L);
		stats.put(BaseStat.maxCd, 50L);
		stats.put(BaseStat.pdd, 9L);
		stats.put(BaseStat.mdd, 9L);
		stats.put(BaseStat.acc, 11L);
		stats.put(BaseStat.eva, 8L);
		stats.put(BaseStat.buffTimeR, 100L);
		getSkills().stream().filter(skill -> SkillConstants.isPassiveSkill_NoPsdSkillsCheck(skill.getSkillId())).
				forEach(this::addToBaseStatCache);
	}

	/**
	 * Adds a Skill's info to the current base stat cache.
	 *
	 * @param skill The skill to add
	 */
	public void addToBaseStatCache(Skill skill) {
		SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
		if(SkillConstants.isPassiveSkill(skill.getSkillId())) {
			Map<BaseStat, Integer> stats = si.getBaseStatValues(this, skill.getCurrentLevel());
			stats.forEach(this::addBaseStat);
		}
		if (si.isPsd() && si.getSkillStatInfo().containsKey(SkillStat.coolTimeR)) {
			for(int psdSkill : si.getPsdSkills()) {
				getHyperPsdSkillsCooltimeR().put(psdSkill, si.getValue(SkillStat.coolTimeR, 1));
			}
		}
	}

	/**
	 * Removes a Skill's info from the current base stat cache.
	 *
	 * @param skill The skill to remove
	 */
	public void removeFromBaseStatCache(Skill skill) {
		SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
		Map<BaseStat, Integer> stats = si.getBaseStatValues(this, skill.getCurrentLevel());
		stats.forEach(this::removeBaseStat);
	}

	/**
	 * Returns whether or not this Char has a {@link Skill} with a given id.
	 *
	 * @param id The id of the Skill.
	 * @return Whether or not this Char has a Skill with the given id.
	 */
	public boolean hasSkill(int id) {
		return getSkills().stream().anyMatch(s -> s.getSkillId() == id) && getSkill(id, false).getCurrentLevel() > 0;
	}

	/**
	 * Gets a {@link Skill} of this Char with a given id.
	 *
	 * @param id The id of the requested Skill.
	 * @return The Skill corresponding to the given id of this Char, or null if there is none.
	 */
	public Skill getSkill(int id) {
		return getSkill(id, false);
	}

	/**
	 * Gets a {@link Skill} with a given ID. If <code>createIfNull</code> is true, creates the Skill
	 * if it doesn't exist yet.
	 * If it is false, will return null if this Char does not have the given Skill.
	 *
	 * @param id           The id of the requested Skill.
	 * @param createIfNull Whether or not this method should create the Skill if it doesn't exist.
	 * @return The Skill that the Char has, or <code>null</code> if there is no such skill and
	 * <code>createIfNull</code> is false.
	 */
	public Skill getSkill(int id, boolean createIfNull) {
		for (Skill s : getSkills()) {
			if (s.getSkillId() == id) {
				return s;
			}
		}
		return createIfNull ? createAndReturnSkill(id) : null;
	}

	public int getSkillLevel(int skillID) {
		Skill skill = getSkill(skillID);
		if (skill != null) {
			return skill.getCurrentLevel();
		}
		return 0;
	}

	public int getRemainRecipeUseCount(int recipeID) {
		if (SkillConstants.isMakingSkillRecipe(recipeID)) {
			return getSkillLevel(recipeID);
		}
		return 0;
	}

	/**
	 * Creates a new {@link Skill} for this Char.
	 *
	 * @param id The skillID of the Skill to be created.
	 * @return The new Skill.
	 */
	private Skill createAndReturnSkill(int id) {
		Skill skill = SkillData.getSkillDeepCopyById(id);
		addSkill(skill);
		return skill;
	}

	public void setStat(Stat charStat, int value) {
		CharacterStat cs = getAvatarData().getCharacterStat();
		switch (charStat) {
			case str:
				cs.setStr(value);
				break;
			case dex:
				cs.setDex(value);
				break;
			case inte:
				cs.setInt(value);
				break;
			case luk:
				cs.setLuk(value);
				break;
			case hp:
				cs.setHp(value);
				break;
			case mhp:
				cs.setMaxHp(value);
				if (JobConstants.isDemonAvenger(getJob())) {
					((Demon) getJobHandler()).sendHpUpdate();
				}
				break;
			case mp:
				cs.setMp(value);
				break;
			case mmp:
				cs.setMaxMp(value);
				break;
			case ap:
				cs.setAp(value);
				break;
			case level:
				cs.setLevel(value);
				notifyChanges();
				break;
			case skin:
				cs.setSkin(value);
				break;
			case face:
				cs.setFace(value);
				break;
			case hair:
				cs.setHair(value);
				break;
			case pop:
				cs.setPop(value);
				break;
			case charismaEXP:
				cs.setCharismaExp(value);
				break;
			case charmEXP:
				cs.setCharmExp(value);
				break;
			case craftEXP:
				cs.setCraftExp(value);
				break;
			case insightEXP:
				cs.setInsightExp(value);
				break;
			case senseEXP:
				cs.setSenseExp(value);
				break;
			case willEXP:
				cs.setWillExp(value);
				break;
			case fatigue:
				cs.setFatigue(value);
				break;
			case subJob:
				cs.setSubJob(value);
				break;
		}
	}

	/**
	 * Notifies all groups (such as party, guild) about all your changes, such as level and job.
	 */
	private void notifyChanges() {
		Party party = getParty();
		if (party != null) {
			party.updatePartyMemberInfoByChr(this);
			party.broadcast(WvsContext.partyResult(PartyResult.userMigration(party)));
		}
		Guild guild = getGuild();
		if (guild != null) {
			GuildMember gm = guild.getMemberByCharID(getId());
			gm.setLevel(getLevel());
			gm.setJob(getJob());
			guild.broadcast(WvsContext.guildResult(GuildResult.changeLevelOrJob(guild, gm)));
			Alliance ally = guild.getAlliance();
			if (ally != null) {
				ally.broadcast(WvsContext.allianceResult(AllianceResult.changeLevelOrJob(ally, guild, gm)));
			}
		}
	}

	/**
	 * Gets a raw Stat from this Char, unaffected by things such as equips and skills.
	 *
	 * @param charStat The requested Stat
	 * @return the requested stat's value
	 */
	public int getStat(Stat charStat) {
		CharacterStat cs = getAvatarData().getCharacterStat();
		switch (charStat) {
			case str:
				return cs.getStr();
			case dex:
				return cs.getDex();
			case inte:
				return cs.getInt();
			case luk:
				return cs.getLuk();
			case hp:
				return cs.getHp();
			case mhp:
				return cs.getMaxHp();
			case mp:
				return cs.getMp();
			case mmp:
				return cs.getMaxMp();
			case ap:
				return cs.getAp();
			case level:
				return cs.getLevel();
			case skin:
				return cs.getSkin();
			case face:
				return cs.getFace();
			case hair:
				return cs.getHair();
			case pop:
				return cs.getPop();
			case charismaEXP:
				return cs.getCharismaExp();
			case charmEXP:
				return cs.getCharmExp();
			case craftEXP:
				return cs.getCraftExp();
			case insightEXP:
				return cs.getInsightExp();
			case senseEXP:
				return cs.getSenseExp();
			case willEXP:
				return cs.getWillExp();
			case fatigue:
				return cs.getFatigue();
			case subJob:
				return cs.getSubJob();
		}
		return -1;
	}

	/**
	 * Adds a Stat to this Char.
	 *
	 * @param charStat which Stat to add
	 * @param amount   the amount of Stat to add
	 */
	public void addStat(Stat charStat, int amount) {
		setStat(charStat, getStat(charStat) + amount);
	}

	/**
	 * Adds a Stat to this Char, and immediately sends the packet to the client notifying the change.
	 *
	 * @param charStat which Stat to change
	 * @param amount   the amount of Stat to add
	 */
	public void addStatAndSendPacket(Stat charStat, int amount) {
		setStatAndSendPacket(charStat, getStat(charStat) + amount);
	}

	/**
	 * Adds a Stat to this Char, and immediately sends the packet to the client notifying the change.
	 *
	 * @param charStat which Stat to change
	 * @param value    the value of Stat to set
	 */
	public void setStatAndSendPacket(Stat charStat, int value) {
		setStat(charStat, value);
		Map<Stat, Object> stats = new HashMap<>();
		switch (charStat) {
			case level:
			case skin:
			case fatigue:
				stats.put(charStat, (byte) getStat(charStat));
				break;
			case str:
			case dex:
			case inte:
			case luk:
			case ap:
			case subJob:
				stats.put(charStat, (short) getStat(charStat));
				break;
			case hp:
			case mhp:
			case mp:
			case mmp:
			case face:
			case hair:
			case pop:
			case charismaEXP:
			case insightEXP:
			case willEXP:
			case craftEXP:
			case senseEXP:
			case charmEXP:
			case eventPoints:
				stats.put(charStat, getStat(charStat));
				break;
		}
		write(WvsContext.statChanged(stats));
	}

	/**
	 * Adds a certain amount of money to the current character. Also sends the
	 * packet to update the client's state.
	 *
	 * @param amount The amount of money to add. May be negative.
	 */
	public void addMoney(long amount) {
		CharacterStat cs = getAvatarData().getCharacterStat();
		long money = cs.getMoney();
		long newMoney = money + amount;
		if (newMoney >= 0) {
			newMoney = Math.min(GameConstants.MAX_MONEY, newMoney);
			Map<Stat, Object> stats = new HashMap<>();
			cs.setMoney(newMoney);
			stats.put(Stat.money, newMoney);
			write(WvsContext.statChanged(stats));
		}
	}

	/**
	 * The same as addMoney, but negates the amount.
	 *
	 * @param amount The money to deduct. May be negative.
	 */
	public void deductMoney(long amount) {
		addMoney(-amount);
	}

	public Position getOldPosition() {
		return oldPosition;
	}

	public void setOldPosition(Position oldPosition) {
		this.oldPosition = oldPosition;
	}

	public void setMoveAction(byte moveAction) {
		this.moveAction = moveAction;
	}

	public byte getMoveAction() {
		return moveAction;
	}

	/**
	 * Sends a message to this Char through the ScriptProgress packet.
	 *
	 * @param msg The message to display.
	 */
	public void chatScriptMessage(String msg) {
		write(UserPacket.scriptProgressMessage(msg));
	}

	/**
	 * Sends a message to this Char with a default colour {@link ChatType#SystemNotice}.
	 *
	 * @param msg The message to display.
	 */
	public void chatMessage(String msg) {
		chatMessage(SystemNotice, msg);
	}

	/**
	 * Sends a formatted message to this Char with a default color {@link ChatType#SystemNotice}.
	 * @param msg The message to display
	 * @param args The format arguments
	 */
	public void chatMessage(String msg, Object... args) {
		chatMessage(SystemNotice, msg, args);
	}

	/**
	 * Sends a formatted message to this Char with a given {@link ChatType colour}.
	 *
	 * @param clr The Colour this message should be in.
	 * @param msg The message to display.
	 * @param args The format arguments
	 */
	public void chatMessage(ChatType clr, String msg, Object... args) {
		write(UserLocal.chatMsg(clr, String.format(msg, args)));
	}

	/**
	 * Sends a message to this Char with a given {@link ChatType colour}.
	 *
	 * @param clr The Colour this message should be in.
	 * @param msg The message to display.
	 */
	public void chatMessage(ChatType clr, String msg) {
		getClient().write(UserLocal.chatMsg(clr, msg));
	}

    /**
     * Sends a message to the character if the debug config flag is turned on.
     *
     * @param message message to send
     */
	public void dbgChatMsg(String message) {
	    if (ServerConfig.DEBUG_MODE)
	        chatMessage(message);
    }

	/**
	 * Unequips an {@link Item}. Ensures that the hairEquips and both inventories get updated.
	 *
	 * @param item The Item to equip.
	 */
	public void unequip(Item item) {
		AvatarLook al = getAvatarData().getAvatarLook();
		int itemID = item.getItemId();
		getInventoryByType(EQUIPPED).removeItem(item);
		getInventoryByType(EQUIP).addItem(item);
		al.removeItem(itemID);
		byte maskValue = AvatarModifiedMask.AvatarLook.getVal();
		getField().broadcastPacket(UserRemote.avatarModified(this, maskValue, (byte) 0), this);
		if (getTemporaryStatManager().hasStat(SoulMP) && ItemConstants.isWeapon(item.getItemId())) {
			getTemporaryStatManager().removeStat(SoulMP, false);
			getTemporaryStatManager().removeStat(FullSoulMP, false);
			getTemporaryStatManager().sendResetStatPacket();
		}
        List<Skill> skills = new ArrayList<>();
        for (ItemSkill itemSkill : ItemData.getEquipById(item.getItemId()).getItemSkills()) {
            Skill skill = getSkill(itemSkill.getSkill());
            skill.setCurrentLevel(0);
            removeSkill(itemSkill.getSkill());
            skill.setCurrentLevel(-1); // workaround to remove skill from window without a cc
            skills.add(skill);
        }
        if (skills.size() > 0) {
            getClient().write(WvsContext.changeSkillRecordResult(skills, true, false, false, false));
        }
		int equippedSummonSkill = ItemConstants.getEquippedSummonSkillItem(item.getItemId(), getJob());
		if (equippedSummonSkill != 0) {
			getField().removeSummon(equippedSummonSkill, getId());

            getTemporaryStatManager().removeStatsBySkill(equippedSummonSkill);
            getTemporaryStatManager().removeStatsBySkill(getTemporaryStatManager().getOption(RepeatEffect).rOption);
		}
		if (JobConstants.isDemonAvenger(getJob())) {
			((Demon) getJobHandler()).sendHpUpdate();
		}
		if (ItemConstants.isAndroid(itemID) || ItemConstants.isMechanicalHeart(itemID)) {
			if (getAndroid() != null) {
				getField().removeLife(getAndroid());
			}
			setAndroid(null);
		}
	}

	/**
	 * Equips an {@link Item}. Ensures that the hairEquips and both inventories get updated.
	 *
	 * @param item The Item to equip.
	 */
	public boolean equip(Item item) {
		Equip equip = (Equip) item;
		if (equip.hasSpecialAttribute(EquipSpecialAttribute.Vestige)) {
			return false;
		}
		if (equip.isEquipTradeBlock()) {
			equip.setTradeBlock(true);
			equip.setEquipTradeBlock(false);
			equip.setEquippedDate(FileTime.currentTime());
			equip.addAttribute(EquipAttribute.Untradable);
		}
		if (equip.getCharmEXP() > 0) {
			addStatAndSendPacket(Stat.charmEXP, equip.getCharmEXP());
			equip.setCharmEXP(0);
			equip.setiCraft((short) 0);
			equip.addAttribute(EquipAttribute.NoNonCombatStatGain);
		}
		AvatarLook al = getAvatarData().getAvatarLook();
		int itemID = item.getItemId();
		getInventoryByType(EQUIP).removeItem(item);
		getInventoryByType(EQUIPPED).addItem(item);
		List<Integer> hairEquips = getAvatarData().getAvatarLook().getHairEquips();
		if (item.getBagIndex() < BodyPart.APBase.getVal() || item.getBagIndex() > BodyPart.APEnd.getVal()){
			// only add if not part of your own body
			if (ItemConstants.isWeapon(itemID)) {
				al.setWeaponId(itemID);
			}
			if (!hairEquips.contains(itemID)) {
				hairEquips.add(itemID);
			}
		}
		if (!equip.hasAttribute(EquipAttribute.NoNonCombatStatGain) && equip.getCharmEXP() != 0) {
			addStatAndSendPacket(Stat.charmEXP, equip.getCharmEXP());
			equip.addAttribute(EquipAttribute.NoNonCombatStatGain);
		}
		List<Skill> skills = new ArrayList<>();
        for (ItemSkill itemSkill : ItemData.getEquipById(equip.getItemId()).getItemSkills()) {
            Skill skill = SkillData.getSkillDeepCopyById(itemSkill.getSkill());
            byte slv = itemSkill.getSlv();
            // support for Tower of Oz rings
            if (equip.getLevel() > 0) {
                slv = (byte) Math.min(equip.getLevel(), skill.getMaxLevel());
            }
            skill.setCurrentLevel(slv);
            skills.add(skill);
            addSkill(skill);
        }
        if (skills.size() > 0) {
            getClient().write(WvsContext.changeSkillRecordResult(skills, true, false, false, false));
        }
		int equippedSummonSkill = ItemConstants.getEquippedSummonSkillItem(equip.getItemId(), getJob());
        if (equippedSummonSkill != 0) {
			getJobHandler().handleSkill(getClient(), equippedSummonSkill, (byte) 1, null);
		}
		byte maskValue = AvatarModifiedMask.AvatarLook.getVal();
		getField().broadcastPacket(UserRemote.avatarModified(this, maskValue, (byte) 0), this);
		initSoulMP();
		if (JobConstants.isDemonAvenger(getJob())) {
			((Demon) getJobHandler()).sendHpUpdate();
		}
		// check android status
		if (ItemConstants.isAndroid(itemID) || ItemConstants.isMechanicalHeart(itemID)) {
			initAndroid(true);
			if (getAndroid() != null) {
				getField().spawnLife(getAndroid(), null);
			}
		}
		return true;
	}

	public TemporaryStatManager getTemporaryStatManager() {
		return temporaryStatManager;
	}

	public void setTemporaryStatManager(TemporaryStatManager temporaryStatManager) {
		this.temporaryStatManager = temporaryStatManager;
	}

	public GachaponManager getGachaponManager() {
		return gachaponManager;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setJobHandler(Job jobHandler) {
		this.jobHandler = jobHandler;
	}

	public Job getJobHandler() {
		return jobHandler;
	}

	public FuncKeyMap getFuncKeyMap() {
		return funcKeyMaps.get(0);
	}

	public List<FuncKeyMap> getFuncKeyMaps() {
		return funcKeyMaps;
	}

	public void initFuncKeyMaps(int keySettingType, boolean beastTamer) {
		int amount = beastTamer ? 5 : 1;
		for (int i = 0; i < amount; i++) {
			FuncKeyMap funcKeyMap = FuncKeyMap.getDefaultMapping(keySettingType);
			funcKeyMaps.add(funcKeyMap);
		}
	}

	/**
	 * Creates a {@link Rect} with regard to this character. Adds all values to this Char's
	 * position.
	 *
	 * @param rect The rectangle to use.
	 * @return The new rectangle.
	 */
	public Rect getRectAround(Rect rect) {
		int x = getPosition().getX();
		int y = getPosition().getY();
		return new Rect(x + rect.getLeft(), y + rect.getTop(), x + rect.getRight(), y + rect.getBottom());
	}

	/**
	 * Returns the Equip equipped at a certain {@link BodyPart}.
	 *
	 * @param bodyPart The requested bodyPart.
	 * @return The Equip corresponding to <code>bodyPart</code>. Null if there is none.
	 */
	public Item getEquippedItemByBodyPart(BodyPart bodyPart) {
		List<Item> items = getEquippedInventory().getItemsByBodyPart(bodyPart);
		return items.size() > 0 ? items.get(0) : null;
	}

	public boolean isLeft() {
		return moveAction > 0 && (moveAction % 2) == 1;
	}

	public MarriageRecord getMarriageRecord() {
		return marriageRecord;
	}

	public void setMarriageRecord(MarriageRecord marriageRecord) {
		this.marriageRecord = marriageRecord;
	}

	/**
	 * Returns a {@link Field} based on the current {@link FieldInstanceType} of this Char (channel,
	 * expedition,
	 * party or solo).
	 *
	 * @return The Field corresponding to the current FieldInstanceType.
	 */
	public Field getOrCreateFieldByCurrentInstanceType(int fieldID) {
		Field res;
		if (getInstance() == null) {
			res = getClient().getChannelInstance().getField(fieldID);
		} else {
			res = getInstance().getField(fieldID);
			res.setRuneStone(null);
		}
		return res;
	}

	/**
	 * Warps this Char to a given field at the starting portal.
	 *
	 * @param fieldId the ID of the field to warp to
	 */
	public void warp(int fieldId) {
		warp(getOrCreateFieldByCurrentInstanceType(fieldId));
	}

	/**
	 * Warps this Char to a given field at the given portal. If the portal doesn't exist, takes the starting portal.
	 *
	 * @param fieldId the ID of the field to warp to
	 * @param portalId the ID of the portal where the Char should spawn
	 */
	public void warp(int fieldId, int portalId) {
		Field field = getOrCreateFieldByCurrentInstanceType(fieldId);
		Portal portal = field.getPortalByID(portalId);
		if (portal == null) {
			portal = field.getDefaultPortal();
		}
		warp(field, portal);
	}

	/**
	 * Warps this character to a given field, at the starting position.
	 * See {@link #warp(Field, Portal) warp}.
	 *
	 * @param toField The field to warp to.
	 */
	public void warp(Field toField) {
		warp(toField, toField.getPortalByName("sp"), false, true);
	}

	/**
	 * Warps this Char to a given {@link Field}, with the Field's "sp" portal as spawn position.
	 *
	 * @param toField       The Field to warp to.
	 * @param characterData Whether or not the character data should be encoded.
	 */
	public void warp(Field toField, boolean characterData) {
		if (toField == null) {
			toField = getOrCreateFieldByCurrentInstanceType(100000000);
		}
		warp(toField, toField.getPortalByName("sp"), characterData, true);
	}

	public void warp(int fieldId, int portalId, boolean saveReturnMap) {
		Field field = getOrCreateFieldByCurrentInstanceType(fieldId);
		Portal portal = field.getPortalByID(portalId);
		if (portal == null) {
			portal = field.getDefaultPortal();
		}
		warp(field, portal, false, saveReturnMap);
	}

	/**
	 * Warps this Char to a given {@link Field} and {@link Portal}. Will not include character data.
	 *
	 * @param toField  The Field to warp to.
	 * @param toPortal The Portal to spawn at.
	 */
	public void warp(Field toField, Portal toPortal) {
		warp(toField, toPortal, false, true);
	}

	/**
	 * Sets the return portal to the nearest current portal.
	 */
	public void setNearestReturnPortal() {
		Rect rect = new Rect(
				new Position(
						getPosition().getX() - 30,
						getPosition().getY() - 30),
				new Position(
						getPosition().getX() + 50, // wide girth
						getPosition().getY() + 50)
		);

		List<Portal> portals = getField().getClosestPortal(rect);

		if (portals.size() > 0) {
			setPreviousPortalID(portals.get(0).getId());
		} else {
			setPreviousPortalID(0);
		}
	}

	/**
	 * Warps this character to a given field, at a given portal.
	 * Ensures that the previous map does not contain this Char anymore, and that the new field
	 * does.
	 * Ensures that all Lifes are immediately spawned for the new player.
	 *
	 * @param toField The {@link Field} to warp to.
	 * @param portal  The {@link Portal} where to spawn at.
	 */
	public void warp(Field toField, Portal portal, boolean characterData, boolean saveReturnMap) {
		if (toField == null) {
			return;
		}
		TemporaryStatManager tsm = getTemporaryStatManager();
		for (AffectedArea aa : tsm.getAffectedAreas()) {
			tsm.removeStatsBySkill(aa.getSkillID());
		}
		Field currentField = getField();

		if (currentField != null) {
			if (saveReturnMap) {
				setPreviousFieldID(currentField.getId()); // this may be a bad idea in some cases? idk
				setNearestReturnPortal();
			}
			currentField.removeChar(this);
		}

		setField(toField);
		toField.addChar(this);
		getAvatarData().getCharacterStat().setPortal(portal.getId());
		setPosition(new Position(portal.getX(), portal.getY()));
		getClient().write(Stage.setField(this, toField, getClient().getChannel(), false, 0, characterData, hasBuffProtector(),
				(byte) (portal != null ? portal.getId() : 0), false, 100, null, true, -1));
		showProperUI(currentField != null ? currentField.getId() : -1, toField.getId());
		if (characterData) {
			initSoulMP();
			Party party = getParty();
			if (party != null) {
				write(WvsContext.partyResult(PartyResult.loadParty(party)));
			}
			if (getGuild() != null) {
				write(WvsContext.guildResult(GuildResult.loadGuild(getGuild())));
				if (getGuild().getAlliance() != null) {
					write(WvsContext.allianceResult(AllianceResult.loadDone(getGuild().getAlliance())));
					write(WvsContext.allianceResult(AllianceResult.loadGuildDone(getGuild().getAlliance())));
				}
			}
			for (Friend f : getFriends()) {
				f.setFlag(getClient().getWorld().getCharByID(f.getFriendID()) != null
						? FriendFlag.FriendOnline
						: FriendFlag.FriendOffline);
			}
			for (Friend f : getAccount().getFriends()) {
				f.setFlag(getClient().getWorld().getAccountByID(f.getFriendAccountID()) != null
						? FriendFlag.AccountFriendOnline
						: FriendFlag.AccountFriendOffline);
			}
		}
		toField.spawnLifesForChar(this);

		if (JobConstants.isEvan(getJob()) && getJob() != JobConstants.JobEnum.EVAN_NOOB.getJobId()) {
			((Evan) getJobHandler()).spawnMir();
		}
		if (JobConstants.isKanna(getJob())) {
			((Kanna) getJobHandler()).spawnHaku();
		}
		if (tsm.hasStat(IndieEmpty)) {
			for (Iterator<Option> iterator = tsm.getCurrentStats().getOrDefault(IndieEmpty, new ArrayList<>()).iterator(); iterator.hasNext(); ) {
				Summon summon = iterator.next().summon;
				if (summon != null) {
					if (summon.getMoveAbility().changeFieldWithOwner()) {
						summon.setObjectId(getField().getNewObjectID());
						getField().spawnSummon(summon);
					} else {
						iterator.remove();
					}
				}
			}
		}
		for (int skill : Job.REMOVE_ON_WARP) {
			if (tsm.hasStatBySkillId(skill)) {
				tsm.removeStatsBySkill(skill);
			}
		}
		if (tsm.hasStat(Flying) && !toField.isFly()) {
			tsm.removeStat(Flying, false);
		}
		notifyChanges();
		toField.execUserEnterScript(this);
		initPets();
		if (toField.getTimeLimit() > 0) {
			Field warpTo = getOrCreateFieldByCurrentInstanceType(toField.getReturnMap());
			if (warpTo != null && toField.getReturnMap() != toField.getId()) {
				if (timeLimitTimer != null && !timeLimitTimer.isDone()) {
					timeLimitTimer.cancel(true);
				}
				new Clock(ClockType.SecondsClock, getField(), toField.getTimeLimit());
				timeLimitTimer = EventManager.addEvent(() -> warp(warpTo), toField.getTimeLimit(), TimeUnit.SECONDS);
			}
		}
		if (getDeathCount() > 0) {
			write(UserLocal.deathCountInfo(getDeathCount()));
		}
		if (field.getEliteState() == EliteState.EliteBoss) {
			write(FieldPacket.eliteState(EliteState.EliteBoss, true, GameConstants.ELITE_BOSS_BGM, null, null));
		}
		if (getActiveFamiliar() != null) {
			getField().broadcastPacket(CFamiliar.familiarEnterField(getId(), true, getActiveFamiliar(), true, false));
		}
		Dragon dragon = getDragon();
		if (dragon != null) {
			toField.spawnLife(dragon, null);
		}
		Android android = getAndroid();
		if (android != null) {
			toField.spawnLife(android, null);
		}
		for (Mob mob : toField.getMobs()) {
			mob.addObserver(getScriptManager());
		}
		if (getFieldInstanceType() == CHANNEL) {
			write(FieldPacket.setQuickMoveInfo(GameConstants.getQuickMoveInfos().stream().filter(qmi -> !qmi.isNoInstances() || getField().isChannelField()).collect(Collectors.toList())));
		}
		if (JobConstants.isAngelicBuster(getJob())) {
			write(UserLocal.setDressChanged(false, true));
		}
	}

	/**
	 * Adds a given amount of exp to this Char. Immediately checks for level-up possibility, and
	 * sends the updated
	 * stats to the client. Allows multi-leveling.
	 *
	 * @param amount The amount of exp to add.
	 */
	public void addExp(long amount) {
		ExpIncreaseInfo eii = new ExpIncreaseInfo();
		eii.setLastHit(true);
		eii.setIncEXP(Util.maxInt(amount));
		addExp(amount, eii);
	}

	/**
	 * Adds exp to this Char. Will calculate the extra exp gained from buffs and the exp rate of the server.
	 * Also takes an argument to show this info to the client. Will not send anything if this argument (eii) is null.
	 *
	 * @param amount The amount of exp to add
	 * @param eii    The info to send to the client
	 */
	public void addExp(long amount, ExpIncreaseInfo eii) {
		if (amount <= 0) {
			return;
		}
		if (getGuild() != null) {
			getGuild().addCommitmentToChar(this, (int) Math.min(amount, Integer.MAX_VALUE)); // independant of any xp buffs
		}
		int expFromExpR = (int) (amount * (getTotalStat(BaseStat.expR) / 100D));
		amount += expFromExpR;
		int level = getLevel();
		CharacterStat cs = getAvatarData().getCharacterStat();
		long curExp = cs.getExp();
		if (level >= GameConstants.charExp.length - 1) {
			return;
		}
		long newExp = curExp + amount;
		Map<Stat, Object> stats = new HashMap<>();
		while (newExp >= GameConstants.charExp[level] && level < GameConstants.charExp.length) {
			newExp -= GameConstants.charExp[level];
			addStat(Stat.level, 1);
			stats.put(Stat.level, (byte) getStat(Stat.level));
			getJobHandler().handleLevelUp();
			level++;
			getField().broadcastPacket(UserRemote.effect(getId(), Effect.levelUpEffect()));
			heal(getMaxHP());
			healMP(getMaxMP());
		}
		cs.setExp(newExp);
		stats.put(Stat.exp, newExp);
		if (eii != null) {
			eii.setIndieBonusExp(expFromExpR);
			write(WvsContext.incExpMessage(eii));
		}
		getClient().write(WvsContext.statChanged(stats));
	}

	/**
	 * Adds a given amount of exp to this Char, however it does not display the Exp Message.
	 * Immediately checks for level-up possibility, and sends the updated
	 * stats to the client. Allows multi-leveling.
	 *
	 * @param amount The amount of exp to add.
	 */
	public void addExpNoMsg(long amount) {
		addExp(amount, null);
	}

	public void addTraitExp(Stat traitStat, int amount) {
		if (amount <= 0) {
			return;
		}
		Map<Stat, Object> stats = new HashMap<>();
		addStat(traitStat, amount);
		stats.put(traitStat, getStat(traitStat));
		stats.put(Stat.dayLimit, getAvatarData().getCharacterStat().getNonCombatStatDayLimit());
		write(WvsContext.statChanged(stats));
		write(WvsContext.incNonCombatStatEXPMessage(traitStat, amount));
	}

	/**
	 * Writes a packet to this Char's client.
	 *
	 * @param outPacket The OutPacket to write.
	 */
	public void write(OutPacket outPacket) {
		if (getClient() != null) {
			getClient().write(outPacket);
		}
	}

	public ExpIncreaseInfo getExpIncreaseInfo() {
		return new ExpIncreaseInfo();
	}

	public WildHunterInfo getWildHunterInfo() {
		return wildHunterInfo;
	}


	public void setWildHunterInfo(WildHunterInfo wildHunterInfo) {
		this.wildHunterInfo = wildHunterInfo;
	}

	public ZeroInfo getZeroInfo() {
		return zeroInfo;
	}

	public void setZeroInfo(ZeroInfo zeroInfo) {
		this.zeroInfo = zeroInfo;
	}

	public int getNickItem() {
		return nickItem;
	}

	public void setNickItem(int nickItem) {
		this.nickItem = nickItem;
	}

	public void setDamageSkin(int itemID) {
		setDamageSkin(new DamageSkinSaveData(ItemConstants.getDamageSkinIDByItemID(itemID), itemID, false,
				StringData.getItemStringById(itemID)));
	}

	public void setDamageSkin(DamageSkinSaveData damageSkin) {
		this.damageSkin = damageSkin;
	}

	public DamageSkinSaveData getDamageSkin() {
		return damageSkin;
	}

	public DamageSkinSaveData getPremiumDamageSkin() {
		return premiumDamageSkin;
	}

	public void setPremiumDamageSkin(DamageSkinSaveData premiumDamageSkin) {
		this.premiumDamageSkin = premiumDamageSkin;
	}

	public void setPremiumDamageSkin(int itemID) {
		setPremiumDamageSkin(new DamageSkinSaveData(ItemConstants.getDamageSkinIDByItemID(itemID), itemID, false,
				StringData.getItemStringById(itemID)));
	}

	public void setPartyInvitable(boolean partyInvitable) {
		this.partyInvitable = partyInvitable;
	}

	/**
	 * Returns if this Char can be invited to a party.
	 *
	 * @return Whether or not this Char can be invited to a party.
	 */
	public boolean isPartyInvitable() {
		return partyInvitable;
	}

	/**
	 * Returns if this character is currently in its beta state.
	 *
	 * @return true if this Char is in a beta state.
	 */
	public boolean isZeroBeta() {
		return getZeroInfo() != null && getZeroInfo().isZeroBetaState();
	}

	/**
	 * Zero only.
	 * Goes into Beta form if Alpha, and into Alpha if Beta.
	 */
	public void swapZeroState() {
		if (!(JobConstants.isZero(getJob())) || getZeroInfo() == null) {
			return;
		}
		ZeroInfo oldInfo = getZeroInfo().deepCopy();
		ZeroInfo currentInfo = getZeroInfo();
		CharacterStat cs = getAvatarData().getCharacterStat();
		currentInfo.setZeroBetaState(!oldInfo.isZeroBetaState());
		currentInfo.setSubHP(cs.getHp());
		currentInfo.setSubMHP(cs.getMaxHp());
		currentInfo.setSubMP(cs.getMp());
		currentInfo.setSubMMP(cs.getMaxMp());
		cs.setHp(oldInfo.getSubHP());
		cs.setMaxHp(oldInfo.getSubMHP());
		cs.setMp(oldInfo.getSubMP());
		cs.setMaxMp(oldInfo.getSubMMP());
		Map<Stat, Object> updatedStats = new HashMap<>();
		updatedStats.put(Stat.hp, cs.getHp());
		updatedStats.put(Stat.mhp, cs.getMaxHp());
		updatedStats.put(Stat.mp, cs.getMp());
		updatedStats.put(Stat.mmp, cs.getMaxMp());
		write(WvsContext.statChanged(updatedStats));
//        write(WvsContext.zeroInfo(currentInfo));
	}

	/**
	 * Initializes zero info with HP values.
	 */
	public void initZeroInfo() {
		ZeroInfo zeroInfo = new ZeroInfo();
		CharacterStat cs = getAvatarData().getCharacterStat();
		zeroInfo.setSubHP(cs.getHp());
		zeroInfo.setSubMHP(cs.getMaxHp());
		zeroInfo.setSubMP(cs.getMp());
		zeroInfo.setSubMMP(cs.getMaxMp());
		setZeroInfo(zeroInfo);
	}

	public ScriptManagerImpl getScriptManager() {
		return scriptManagerImpl;
	}

	/**
	 * Adds a {@link Drop} to this Char.
	 *
	 * @param drop The Drop that has been picked up.
	 */
	public boolean addDrop(Drop drop) {
		if (drop.isMoney()) {
			addMoney(drop.getMoney());
			getQuestManager().handleMoneyGain(drop.getMoney());
			write(WvsContext.dropPickupMessage(drop.getMoney(), (short) 0, (short) 0));
			dispose();
			return true;
		} else {
			Item item = drop.getItem();
			int itemID = item.getItemId();
			boolean isConsume = false;
			boolean isRunOnPickUp = false;
			if (itemID == GameConstants.BLUE_EXP_ORB_ID || itemID == GameConstants.PURPLE_EXP_ORB_ID ||
					itemID == GameConstants.RED_EXP_ORB_ID) {
				long expGain = (long) (drop.getMobExp() * GameConstants.getExpOrbExpModifierById(itemID));

				write(UserPacket.effect(Effect.fieldItemConsumed((int) (expGain > Integer.MAX_VALUE ? Integer.MAX_VALUE : expGain))));
				addExpNoMsg(expGain);

				// Exp Orb Buff On Pickup
				TemporaryStatManager tsm = getTemporaryStatManager();
				ItemBuffs.giveItemBuffsFromItemID(this, tsm, itemID);
			}
			if (!ItemConstants.isEquip(itemID)) {
				ItemInfo ii = ItemData.getItemInfoByID(itemID);
				isConsume = ii.getSpecStats().getOrDefault(SpecStat.consumeOnPickup, 0) != 0;
				isRunOnPickUp = ii.getSpecStats().getOrDefault(SpecStat.runOnPickup, 0) != 0;
			}
			if (isConsume) {
				consumeItemOnPickup(item);
				dispose();
				return true;
			} else if (isRunOnPickUp) {
				String script = String.valueOf(itemID);
				ItemInfo ii = ItemData.getItemInfoByID(itemID);
				if (ii.getScript() != null && !"".equals(ii.getScript())) {
					script = ii.getScript();
				}
				getScriptManager().startScript(itemID, script, ScriptType.Item);
				return true;
			} else if (getInventoryByType(item.getInvType()).canPickUp(item)) {
				if (item instanceof Equip) {
					Equip equip = (Equip) item;
					if (equip.hasAttribute(EquipAttribute.UntradableAfterTransaction)) {
						equip.removeAttribute(EquipAttribute.UntradableAfterTransaction);
						equip.addAttribute(EquipAttribute.Untradable);
					}
				}
				addItemToInventory(item);
				write(WvsContext.dropPickupMessage(item, (short) item.getQuantity()));
				return true;
			} else {
				write(WvsContext.dropPickupMessage(0, (byte) -1, (short) 0, (short) 0, (short) 0));
				return false;
			}
		}
	}

	private void consumeItemOnPickup(Item item) {
		int itemID = item.getItemId();
		if (ItemConstants.isMobCard(itemID)) {
			MonsterBookInfo mbi = getMonsterBookInfo();
			int id = 0;
			if (!mbi.hasCard(itemID)) {
				mbi.addCard(itemID);
				id = itemID;
			}
			write(WvsContext.monsterBookSetCard(id));
		}
	}

	/**
	 * Returns the Char's name.
	 *
	 * @return The Char's name.
	 */
	public String getName() {
		return getAvatarData().getCharacterStat().getName();
	}

	/**
	 * Checks whether or not this Char has a given quest in progress.
	 *
	 * @param questReq The quest ID of the requested quest.
	 * @return Whether or not this char is in progress with the quest.
	 */
	public boolean hasQuestInProgress(int questReq) {
		return getQuestManager().hasQuestInProgress(questReq);
	}

	/**
	 * Disposes this Char, allowing it to send packets to the server again.
	 */
	public void dispose() {
		write(WvsContext.exclRequest());
	}

	/**
	 * Returns the current HP of this Char.
	 *
	 * @return the current HP of this Char.
	 */
	public int getHP() {
		return getStat(Stat.hp);
	}

	/**
	 * Returns the current MP of this Char.
	 *
	 * @return the current MP of this Char.
	 */
	public int getMP() {
		return getStat(Stat.mp);
	}

	/**
	 * Gets the max hp of this Char.
	 *
	 * @return The max hp of this Char
	 */
	public int getMaxHP() {
		return getTotalStat(BaseStat.mhp);
	}

	/**
	 * Gets the max mp of this Char.
	 *
	 * @return The max mp of this Char
	 */
	public int getMaxMP() {
		return getTotalStat(BaseStat.mmp);
	}

	/**
	 * Heals character's MP and HP completely.
	 */
	public void healHPMP() {
		heal(getMaxHP(), true);
		healMP(getMaxMP());
	}

	/**
	 * Heals this Char's HP for a certain amount. Caps off at maximum HP.
	 *
	 * @param amount The amount to heal.
	 */
	public void heal(int amount, boolean whilstDeath) {
		int curHP = getHP();
		int maxHP = getMaxHP();
		int newHP = curHP + amount > maxHP ? maxHP : curHP + amount;
		Map<Stat, Object> stats = new HashMap<>();

		if(whilstDeath || getHP() > 0) {
			setStat(Stat.hp, newHP);
			stats.put(Stat.hp, newHP);
			write(WvsContext.statChanged(stats));
		}
		if (getParty() != null) {
			getParty().broadcast(UserRemote.receiveHP(this), this);
		}
	}

	public void heal(int amount) {
		heal(amount, false);
	}

	/**
	 * "Heals" this Char's MP for a certain amount. Caps off at maximum MP.
	 *
	 * @param amount The amount to heal.
	 */
	public void healMP(int amount) {
		int curMP = getMP();
		int maxMP = getMaxMP();
		int newMP = curMP + amount > maxMP ? maxMP : curMP + amount;
		Map<Stat, Object> stats = new HashMap<>();
		setStat(Stat.mp, newMP);
		stats.put(Stat.mp, newMP);
		write(WvsContext.statChanged(stats));
	}

	/**
	 * Consumes a single {@link Item} from this Char's {@link Inventory}. Will remove the Item if it
	 * has a quantity of 1.
	 *
	 * @param item The Item to consume, which is currently in the Char's inventory.
	 */
	public void consumeItem(Item item) {
		Inventory inventory = getInventoryByType(item.getInvType());
		// data race possible
		if (item.getQuantity() <= 1 && !ItemConstants.isThrowingItem(item.getItemId())) {
			item.setQuantity(0);
			inventory.removeItem(item);
			short bagIndex = (short) item.getBagIndex();
			if (item.getInvType() == EQUIPPED) {
				getAvatarData().getAvatarLook().removeItem(item.getItemId());
				bagIndex = (short) -bagIndex;
			}
			write(WvsContext.inventoryOperation(true, false,
					Remove, bagIndex, (byte) 0, 0, item));
		} else {
			item.setQuantity(item.getQuantity() - 1);
			write(WvsContext.inventoryOperation(true, false,
					UpdateQuantity, (short) item.getBagIndex(), (byte) -1, 0, item));
		}
		setBulletIDForAttack(calculateBulletIDForAttack(1));
	}

	/**
	 * Consumes an item of this Char with the given id. Will do nothing if the Char doesn't have the
	 * Item.
	 * Only works for non-Equip (i.e., type is not EQUIPPED or EQUIP, CASH is fine) items.
	 * Calls {@link #consumeItem(Item)} if an Item is found.
	 *
	 * @param id       The Item's id.
	 * @param quantity The amount to consume.
	 */
	public void consumeItem(int id, int quantity) {
		Item checkItem = ItemData.getItemDeepCopy(id);
		if (checkItem != null) {
			Item item = getInventoryByType(checkItem.getInvType()).getItemByItemID(id);
			if (item != null) {
				int itemQuantity = item.getQuantity();
				int consumed = quantity > itemQuantity ? 0 : itemQuantity - quantity;
				item.setQuantity(consumed + 1); // +1 because 1 gets consumed by consumeItem(item)
				consumeItem(item);
				if (quantity > itemQuantity) {
					consumeItem(id, quantity - itemQuantity);
				}
			}
		}
	}

	public boolean hasItem(int itemID) {
		return getInventories().stream().anyMatch(inv -> inv.containsItem(itemID));
	}

	public boolean hasItemCount(int itemID, int count) {
		Inventory inv = getInventoryByType(ItemData.getItemDeepCopy(itemID).getInvType());
		return inv.getItems().stream()
				.filter(i -> i.getItemId() == itemID)
				.mapToInt(Item::getQuantity)
				.sum() >= count;
	}

	public short getLevel() {
		return getAvatarData().getCharacterStat().getLevel();
	}

	public boolean isMarried() {
		// TODO
		return false;
	}

	public Guild getGuild() {
		return guild;
	}

	public void setGuild(Guild guild) {
		if (guild != null) {
			// to ensure that the same instance of a guild is retrieved for all characters
			this.guild = getClient().getWorld().getGuildByID(guild.getId());
		} else {
			this.guild = null;
		}
	}

	public int getTotalChuc() {
		return getInventoryByType(EQUIPPED).getItems().stream().mapToInt(i -> ((Equip) i).getChuc()).sum();
	}

	public int getDriverID() {
		return driverID;
	}

	public void setDriverID(int driverID) {
		this.driverID = driverID;
	}

	public int getPassengerID() {
		return passengerID;
	}

	public void setPassengerID(int passengerID) {
		this.passengerID = passengerID;
	}

	public int getChocoCount() {
		return chocoCount;
	}

	public void setChocoCount(int chocoCount) {
		this.chocoCount = chocoCount;
	}

	public int getActiveEffectItemID() {
		return activeEffectItemID;
	}

	public void setActiveEffectItemID(int activeEffectItemID) {
		this.activeEffectItemID = activeEffectItemID;
	}

	public int getMonkeyEffectItemID() {
		return monkeyEffectItemID;
	}

	public void setMonkeyEffectItemID(int monkeyEffectItemID) {
		this.monkeyEffectItemID = monkeyEffectItemID;
	}

	public int getCompletedSetItemID() {
		return completedSetItemID;
	}

	public void setCompletedSetItemID(int completedSetItemID) {
		this.completedSetItemID = completedSetItemID;
	}

	public short getFieldSeatID() {
		return -1;
	}

	public void setFieldSeatID(short fieldSeatID) {
		this.fieldSeatID = fieldSeatID;
	}

	public int getPortableChairID() {
		return portableChairID;
	}

	public void setPortableChairID(int portableChairID) {
		this.portableChairID = portableChairID;
	}

	public String getPortableChairMsg() {
		return portableChairMsg;
	}

	public void setPortableChairMsg(String portableChairMsg) {
		this.portableChairMsg = portableChairMsg;
	}

	public short getFoothold() {
		return foothold;
	}

	public void setFoothold(short foothold) {
		this.foothold = foothold;
	}

	public int getTamingMobLevel() {
		return tamingMobLevel;
	}

	public void setTamingMobLevel(int tamingMobLevel) {
		this.tamingMobLevel = tamingMobLevel;
	}

	public int getTamingMobExp() {
		return tamingMobExp;
	}

	public void setTamingMobExp(int tamingMobExp) {
		this.tamingMobExp = tamingMobExp;
	}

	public int getTamingMobFatigue() {
		return tamingMobFatigue;
	}

	public void setTamingMobFatigue(int tamingMobFatigue) {
		this.tamingMobFatigue = tamingMobFatigue;
	}

	public MiniRoom getMiniRoom() {
		return miniRoom;
	}

	public void setMiniRoom(MiniRoom miniRoom) {
		this.miniRoom = miniRoom;
	}

	public String getADBoardRemoteMsg() {
		return ADBoardRemoteMsg;
	}

	public void setADBoardRemoteMsg(String ADBoardRemoteMsg) {
		this.ADBoardRemoteMsg = ADBoardRemoteMsg;
	}

	public boolean isInCouple() {
		return inCouple;
	}

	public void setInCouple(boolean inCouple) {
		this.inCouple = inCouple;
	}

	public CoupleRecord getCouple() {
		return couple;
	}

	public void setCouple(CoupleRecord couple) {
		this.couple = couple;
	}

	public boolean hasFriendshipItem() {
		return false;
	}

	public FriendshipRingRecord getFriendshipRingRecord() {
		return friendshipRingRecord;
	}

	public void setFriendshipRingRecord(FriendshipRingRecord friendshipRingRecord) {
		this.friendshipRingRecord = friendshipRingRecord;
	}

	public int getComboCounter() {
		return comboCounter;
	}

	public void setComboCounter(int comboCounter) {
		this.comboCounter = comboCounter;
	}

	public int getEvanDragonGlide() {
		return evanDragonGlide;
	}

	public void setEvanDragonGlide(int evanDragonGlide) {
		this.evanDragonGlide = evanDragonGlide;
	}

	public int getKaiserMorphRotateHueExtern() {
		return kaiserMorphRotateHueExtern;
	}

	public void setKaiserMorphRotateHueExtern(int kaiserMorphRotateHueExtern) {
		this.kaiserMorphRotateHueExtern = kaiserMorphRotateHueExtern;
	}

	public int getKaiserMorphPrimiumBlack() {
		return kaiserMorphPrimiumBlack;
	}

	public void setKaiserMorphPrimiumBlack(int kaiserMorphPrimiumBlack) {
		this.kaiserMorphPrimiumBlack = kaiserMorphPrimiumBlack;
	}

	public int getKaiserMorphRotateHueInnner() {
		return kaiserMorphRotateHueInnner;
	}

	public void setKaiserMorphRotateHueInnner(int kaiserMorphRotateHueInnner) {
		this.kaiserMorphRotateHueInnner = kaiserMorphRotateHueInnner;
	}

	public int getMakingMeisterSkillEff() {
		return makingMeisterSkillEff;
	}

	public void setMakingMeisterSkillEff(int makingMeisterSkillEff) {
		this.makingMeisterSkillEff = makingMeisterSkillEff;
	}

	public FarmUserInfo getFarmUserInfo() {
		if (farmUserInfo == null) {
			return new FarmUserInfo();
		}
		return farmUserInfo;
	}

	public void setFarmUserInfo(FarmUserInfo farmUserInfo) {
		this.farmUserInfo = farmUserInfo;
	}

	public int getCustomizeEffect() {
		return customizeEffect;
	}

	public void setCustomizeEffect(int customizeEffect) {
		this.customizeEffect = customizeEffect;
	}

	public String getCustomizeEffectMsg() {
		return customizeEffectMsg;
	}

	public void setCustomizeEffectMsg(String customizeEffectMsg) {
		this.customizeEffectMsg = customizeEffectMsg;
	}

	public byte getSoulEffect() {
		return soulEffect;
	}

	public void setSoulEffect(byte soulEffect) {
		this.soulEffect = soulEffect;
	}

	public FreezeHotEventInfo getFreezeHotEventInfo() {
		if (freezeHotEventInfo == null) {
			return new FreezeHotEventInfo();
		}
		return freezeHotEventInfo;
	}

	public void setFreezeHotEventInfo(FreezeHotEventInfo freezeHotEventInfo) {
		this.freezeHotEventInfo = freezeHotEventInfo;
	}

	public int getEventBestFriendAID() {
		return eventBestFriendAID;
	}

	public void setEventBestFriendAID(int eventBestFriendAID) {
		this.eventBestFriendAID = eventBestFriendAID;
	}

	public int getMesoChairCount() {
		return mesoChairCount;
	}

	public void setMesoChairCount(int mesoChairCount) {
		this.mesoChairCount = mesoChairCount;
	}

	public boolean isBeastFormWingOn() {
		return beastFormWingOn;
	}

	public void setBeastFormWingOn(boolean beastFormWingOn) {
		this.beastFormWingOn = beastFormWingOn;
	}

	public int getActiveNickItemID() {
		return activeNickItemID;
	}

	public void setActiveNickItemID(int activeNickItemID) {
		this.activeNickItemID = activeNickItemID;
	}

	public int getMechanicHue() {
		return mechanicHue;
	}

	public void setMechanicHue(int mechanicHue) {
		this.mechanicHue = mechanicHue;
	}

	public boolean isOnline() {
		return online;
	}

	public void setOnline(boolean online) {
		if (getGuild() != null) {
			setGuild(getGuild()); // Hack to ensure that all chars have the same instance of a guild
			Guild g = getGuild();
			GuildMember gm = g.getMemberByCharID(getId());
			gm.setOnline(online);
			gm.setChr(online ? this : null);
			Alliance ally = getGuild().getAlliance();
			if (ally != null) {
				ally.broadcast(WvsContext.allianceResult(
						AllianceResult.notifyLoginOrLogout(ally, g, gm, !this.online && online)), this);
			} else {
				getGuild().broadcast(WvsContext.guildResult(
						GuildResult.notifyLoginOrLogout(g, gm, online, online)), this);
			}
		}
		this.online = online;
		if (getParty() != null) {
			PartyMember pm = getParty().getPartyMemberByID(getId());
			if (pm != null) {
				pm.setChr(online ? this : null);
				pm.updateInfoByChar(this);
				getParty().updateFull();
			}
		}
	}

	public void setParty(Party party) {
		if (party != null) {
			setPartyID(party.getId());
		} else {
			setPartyID(0);
		}
		this.party = party;
	}

	public Party getParty() {
		return party;
	}

	public void logout() {
		punishLieDetectorEvasion();
		log.info("Logging out " + getName());
		if (getField().getForcedReturn() != GameConstants.NO_MAP_ID) {
			setFieldID(getField().getForcedReturn());
		}
		if (getTradeRoom() != null) {
			Char other = getTradeRoom().getOther();
			getTradeRoom().cancelTrade();
			other.chatMessage("Your trade partner disconnected.");
		}
		getScriptManager().getScripts().values().forEach(ScriptInfo::reset);
		getWorld().getConnectedChatClients().remove(getAccId());
		setOnline(false);
		getJobHandler().handleCancelTimer(this);
		getField().removeChar(this);
		getUser().setCurrentChr(null);
		if (!isChangingChannel()) {
			getClient().getChannelInstance().removeChar(this);
			Server.getInstance().removeUser(getUser()); // don't unstuck, as that would save the account (twice)
		} else {
			getClient().setChr(null);
		}
		DatabaseManager.saveToDB(getAccount());
	}

	public int getSubJob() {
		return getAvatarData().getCharacterStat().getSubJob();
	}

	public FieldInstanceType getFieldInstanceType() {
		return fieldInstanceType;
	}

	public void setFieldInstanceType(FieldInstanceType fieldInstanceType) {
		this.fieldInstanceType = fieldInstanceType;
	}

	/**
	 * Returns the current Set of Fields that this Char holds as personal instances.
	 *
	 * @return the list of personal Field instances.
	 */
	public Map<Integer, Field> getFields() {
		return fields;
	}

	public void addField(Field field) {
		getFields().put(field.getId(), field);
	}

	public Field getPersonalById(int id) {
		return getFields().get(id);
	}

	public void setInstance(Instance instance) {
		if (this.instance != null && this.instance.getChars().size() == 0 && instance == null) {
			this.instance.stopEvents();
		}
		this.instance = instance;
	}

	public Instance getInstance() {
		if (party != null && party.getInstance() != null && party.getInstance().getChars().contains(this)) {
			return party.getInstance();
		}
		return instance;
	}

	private void showProperUI(int fromField, int toField) {
		if (GameConstants.getMaplerunnerField(toField) > 0 && GameConstants.getMaplerunnerField(fromField) <= 0) {
			write(FieldPacket.openUI(UIType.UI_PLATFORM_STAGE_LEAVE));
		} else if (GameConstants.getMaplerunnerField(fromField) > 0 && GameConstants.getMaplerunnerField(toField) <= 0) {
			write(FieldPacket.closeUI(UIType.UI_PLATFORM_STAGE_LEAVE));
		}
	}

	public int calculateBulletIDForAttack(int requiredAmount) {
		Item weapon = getEquippedInventory().getFirstItemByBodyPart(BodyPart.Weapon);
		if (weapon == null) {
			return 0;
		}
		Predicate<Item> kindOfBulletPred;
		int id = weapon.getItemId();

		if (ItemConstants.isClaw(id)) {
			kindOfBulletPred = i -> ItemConstants.isThrowingStar(i.getItemId());
		} else if (ItemConstants.isBow(id)) {
			kindOfBulletPred = i -> ItemConstants.isBowArrow(i.getItemId());
		} else if (ItemConstants.isXBow(id)) {
			kindOfBulletPred = i -> ItemConstants.isXBowArrow(i.getItemId());
		} else if (ItemConstants.isGun(id)) {
			kindOfBulletPred = i -> ItemConstants.isBullet(i.getItemId());
		} else {
			return 0;
		}
		Item i = getConsumeInventory().getItems().stream().sorted(Comparator.comparing(Item::getBagIndex)).filter(kindOfBulletPred).filter(item -> item.getQuantity() >= requiredAmount).findFirst().orElse(null);
		return i != null ? i.getItemId() : 0;
	}

	public int getBulletIDForAttack() {
		return bulletIDForAttack;
	}

	public void setBulletIDForAttack(int bulletIDForAttack) {
		this.bulletIDForAttack = bulletIDForAttack;
	}

	public void setShop(NpcShopDlg shop) {
		this.shop = shop;
	}

	public NpcShopDlg getShop() {
		return shop;
	}

	/**
	 * Checks if this Char can hold an Item in their inventory, assuming that its quantity is 1.
	 *
	 * @param id the item's itemID
	 * @return whether or not this Char can hold an item in their inventory
	 */
	public boolean canHold(int id) {
		boolean canHold;
		if (ItemConstants.isEquip(id)) {  //Equip
			canHold = getEquipInventory().getSlots() > getEquipInventory().getItems().size();
		} else {    //Item
			ItemInfo ii = ItemData.getItemInfoByID(id);
			Inventory inv = getInventoryByType(ii.getInvType());
			Item curItem = inv.getItemByItemID(id);
			canHold = (curItem != null && curItem.getQuantity() + 1 < ii.getSlotMax()) || inv.getSlots() > inv.getItems().size();
		}
		return canHold;
	}

	public boolean canHold(int id, int quantity) {
		int slotMax = ItemData.getItemInfoByID(id).getSlotMax();
		List<Item> items = new ArrayList<>();
		for(int i = quantity; i > 0; i -= slotMax){
			Item item = ItemData.getItemDeepCopy(id);
			item.setQuantity(i > slotMax ? slotMax : i);
			items.add(item);
		}
		return canHold(items);
	}

	/**
	 * Recursive function that checks if this Char can hold a list of items in their inventory.
	 *
	 * @param items the list of items this char should be able to hold
	 * @return whether or not this Char can hold the list of items
	 */
	public boolean canHold(List<Item> items) {
		return canHold(items, deepCopyForInvCheck());
	}

	private boolean canHold(List<Item> items, Char deepCopiedChar) {
		// explicitly use a Char param to avoid accidentally adding items
		if (items.size() == 0) {
			return true;
		}
		Item item = items.get(0);
		if (canHold(item.getItemId())) {
			Inventory inv = deepCopiedChar.getInventoryByType(item.getInvType());
			inv.addItem(item);
			items.remove(item);
			return deepCopiedChar.canHold(items, deepCopiedChar);
		} else {
			return false;
		}

	}

	private Char deepCopyForInvCheck() {
		Char chr = new Char();
		chr.setEquippedInventory(getEquippedInventory().deepCopy());
		chr.setEquipInventory(getEquipInventory().deepCopy());
		chr.setConsumeInventory(getConsumeInventory().deepCopy());
		chr.setEtcInventory(getEtcInventory().deepCopy());
		chr.setInstallInventory(getInstallInventory().deepCopy());
		chr.setCashInventory(getCashInventory().deepCopy());
		return chr;
	}

	/**
	 * Returns the set of personal (i.e., non-account) friends of this Char.
	 *
	 * @return The set of personal friends
	 */
	public Set<Friend> getFriends() {
		return friends;
	}

	public void setFriends(Set<Friend> friends) {
		this.friends = friends;
	}

	/**
	 * Returns the total list of friends of this Char + the owning Account's friends.
	 *
	 * @return The total list of friends
	 */
	public Set<Friend> getAllFriends() {
		Set<Friend> res = new HashSet<>(getFriends());
		res.addAll(getAccount().getFriends());
		return res;
	}

	public Friend getFriendByCharID(int charID) {
		return getFriends().stream().filter(f -> f.getFriendID() == charID).findAny().orElse(null);
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
	}

	public void removeFriend(Friend friend) {
		if (friend != null) {
			getFriends().remove(friend);
		}
	}

	public void removeFriendByID(int charID) {
		removeFriend(getFriendByCharID(charID));
	}

	public void addFriend(Friend friend) {
		if (getFriendByCharID(friend.getFriendID()) == null) {
			getFriends().add(friend);
		}
	}

	public void setChatClient(Client chatClient) {
		this.chatClient = chatClient;
	}

	public Client getChatClient() {
		return chatClient;
	}

	public List<Macro> getMacros() {
		return macros;
	}

	public void setMacros(List<Macro> macros) {
		this.macros = macros;
	}

	public void encodeDamageSkins(OutPacket outPacket) {
		outPacket.encodeByte(true); // hasDamageSkins. Always true in this design.
		// check ida for structure
		getDamageSkin().encode(outPacket);
		getPremiumDamageSkin().encode(outPacket);
		outPacket.encodeShort(getAccount().getDamageSkins().size() + 10); // slotCount
		outPacket.encodeShort(getAccount().getDamageSkins().size());
		for (DamageSkinSaveData dssd : getAccount().getDamageSkins()) {
			dssd.encode(outPacket);
		}
	}

	public boolean canAddMoney(long reqMoney) {
		return getMoney() + reqMoney > 0 && getMoney() + reqMoney < GameConstants.MAX_MONEY;
	}

	public void addPet(Pet pet) {
		getPets().add(pet);
	}

	public void removePet(Pet pet) {
		getPets().remove(pet);
	}

	public void initPets() {
		for (PetItem pi : getCashInventory().getItems()
				.stream()
				.filter(i -> i instanceof PetItem && ((PetItem) i).getActiveState() > 0)
				.map(i -> (PetItem) i).collect(Collectors.toList())) {
			Pet p = getPets().stream().filter(pet -> pet.getItem().equals(pi)).findAny().orElse(null);
			if (p == null) {
				// only create a new pet if the active state is > 0 (active), but isn't added to our own list yet
				p = pi.createPet(this);
				addPet(p);
			}
			getField().broadcastPacket(UserLocal.petActivateChange(p, true, (byte) 0));
		}
	}

	public Pet getPetByIdx(int idx) {
		return getPets().stream()
				.filter(p -> p.getIdx() == idx)
				.findAny()
				.orElse(null);
	}

	public int getFirstPetIdx() {
		int chosenIdx = -1;
		for (int i = 0; i < GameConstants.MAX_PET_AMOUNT; i++) {
			Pet p = getPetByIdx(i);
			if (p == null) {
				chosenIdx = i;
				break;
			}
		}
		return chosenIdx;
	}

	/**
	 * Initializes the equips' enchantment stats.
	 */
	public void initEquips() {
		for (Equip e : getEquippedInventory().getItems().stream().map(e -> (Equip) e).collect(Collectors.toList())) {
			e.recalcEnchantmentStats();
		}
		for (Equip e : getEquipInventory().getItems().stream().map(e -> (Equip) e).collect(Collectors.toList())) {
			e.recalcEnchantmentStats();
		}
	}

	public void initSoulMP() {
		Equip weapon = (Equip) getEquippedItemByBodyPart(BodyPart.Weapon);
		TemporaryStatManager tsm = getTemporaryStatManager();
		if (weapon != null && weapon.getSoulSocketId() != 0 && !tsm.hasStat(SoulMP)) {
			Option o = new Option();
			o.rOption = ItemConstants.getSoulSkillFromSoulID(weapon.getSoulOptionId());
			o.xOption = ItemConstants.MAX_SOUL_CAPACITY;
			tsm.putCharacterStatValue(SoulMP, o);
			tsm.sendSetStatPacket();
		}
	}

	public MonsterBookInfo getMonsterBookInfo() {
		return monsterBookInfo;
	}

	public void setMonsterBookInfo(MonsterBookInfo monsterBookInfo) {
		this.monsterBookInfo = monsterBookInfo;
	}

	public void setDamageCalc(DamageCalc damageCalc) {
		this.damageCalc = damageCalc;
	}

	public DamageCalc getDamageCalc() {
		return damageCalc;
	}

	/**
	 * Gets the current amount of a given stat the character has. Includes things such as skills, items, etc...
	 *
	 * @param baseStat the requested stat
	 * @return the amount of stat
	 */
	private double getTotalStatAsDouble(BaseStat baseStat) {
		// TODO cache this completely
		double stat = 0;
		// Stat allocated by sp
		stat += baseStat.toStat() == null ? 0 : getStat(baseStat.toStat());
		// Stat gained by passives
		stat += getBaseStats().getOrDefault(baseStat, 0L);
		// Stat gained by buffs
		int ctsStat = getTemporaryStatManager().getBaseStats().getOrDefault(baseStat, 0);
		stat += ctsStat;
		// Stat gained by the stat's corresponding "per level" value
		if (baseStat.getLevelVar() != null) {
			stat += getTotalStatAsDouble(baseStat.getLevelVar()) * getLevel();
		}
		// Stat gained by equips
		for (Item item : getEquippedInventory().getItems()) {
			Equip equip = (Equip) item;
			stat += equip.getBaseStat(baseStat);
		}
		// Stat gained by the stat's corresponding rate value
		if (baseStat.getRateVar() != null) {
			stat += stat * (getTotalStat(baseStat.getRateVar()) / 100D);
		}
		// Stat gained by set effects
		stat += getStatAmountSetEffect(baseStat);
		// --- Everything below this doesn't get affected by the rate var
		// Character potential
		for (CharacterPotential cp : getPotentials()) {
			Skill skill = cp.getSkill();
			SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
			Map<BaseStat, Integer> stats = si.getBaseStatValues(this, skill.getCurrentLevel());
			stat += stats.getOrDefault(baseStat, 0);
		}
		return stat;
	}

	public int getTotalStat(BaseStat stat) {
		return (int) getTotalStatAsDouble(stat);
	}

	/**
	 * Gets a total list of basic stats that a character has, including from skills, items, etc...
	 *
	 * @return the total list of basic stats
	 */
	public Map<BaseStat, Integer> getTotalBasicStats() {
		Map<BaseStat, Integer> stats = new HashMap<>();
		for (BaseStat bs : BaseStat.values()) {
			stats.put(bs, getTotalStat(bs));
		}
		return stats;
	}

	/**
	 * Sets whether or not this user has chosen to use up an item to protect their buffs upon next respawn.
	 *
	 * @param buffProtector buff protectability
	 */
	public void setBuffProtector(boolean buffProtector) {
		this.buffProtector = buffProtector;
	}

	/**
	 * Returns whether this user has chosen to activate a buff protector for their next respawn.
	 *
	 * @return buff protectability
	 */
	public boolean hasBuffProtector() {
		return buffProtector;
	}

	/**
	 * Returns the item the user has for protecting buffs.
	 *
	 * @return the Item the user has for prtoecting buffs, or null if there is none.
	 */
	public Item getBuffProtectorItem() {
		int[] buffItems = {5133000, 5133001, 4143000};
		Item item = null;
		for (int id : buffItems) {
			item = getConsumeInventory().getItemByItemID(id);
			if (item == null) {
				item = getCashInventory().getItemByItemID(id);
			}
			if (item != null) {
				// just break when an item was found.
				break;
			}
		}
		return item;
	}

	/**
	 * Resets the combo kill's timer. Interrupts the previous timer if there was one.
	 */
	public void comboKillResetTimer() {
		if (comboKillResetTimer != null && !comboKillResetTimer.isDone()) {
			comboKillResetTimer.cancel(true);
		}
		comboKillResetTimer = EventManager.addEvent(() -> setComboCounter(0), GameConstants.COMBO_KILL_RESET_TIMER, TimeUnit.SECONDS);
	}

	public Map<Integer, Long> getSkillCoolTimes() {
		return skillCoolTimes;
	}

	public void addSkillCoolTime(int skillId, long nextusabletime) {
		getSkillCoolTimes().put(skillId, nextusabletime);
	}

	public void removeSkillCoolTime(int skillId) {
		getSkillCoolTimes().remove(skillId);
	}

	public void resetSkillCoolTime(int skillId) {
		if(hasSkillOnCooldown(skillId)) {
			addSkillCoolTime(skillId, 0);
			write(UserLocal.skillCooltimeSetM(skillId, 0));
		}
	}

	public void reduceSkillCoolTime(int skillId, long amountInMS) {
		if (hasSkillOnCooldown(skillId)) {
			long nextUsableTime = getSkillCoolTimes().get(skillId);
			addSkillCoolTime(skillId, nextUsableTime - amountInMS);
			write(UserLocal.skillCooltimeSetM(skillId, (int) ((nextUsableTime - amountInMS) - System.currentTimeMillis() < 0 ? 0 : (nextUsableTime - amountInMS) - System.currentTimeMillis())));
		}
	}

	public long getRemainingCoolTime(int skillId) {
		if (hasSkillOnCooldown(skillId)) {
			return getSkillCoolTimes().getOrDefault(skillId, System.currentTimeMillis()) - System.currentTimeMillis();
		}
		return 0L;
	}

	/**
	 * Checks whether or not a skill is currently on cooldown.
	 *
	 * @param skillID the skill's id to check
	 * @return whether or not a skill is currently on cooldown
	 */
	public boolean hasSkillOnCooldown(int skillID) {
		return System.currentTimeMillis() < getSkillCoolTimes().getOrDefault(skillID, 0L);
	}

	/**
	 * Checks if a skill is allowed to be cast, according to its cooltime. If it is allowed, it immediately sets
	 * the cooltime and stores the next moment where the skill is allowed. Skills without cooltime are always allowed.
	 *
	 * @param skillID the skill id of the skill to put on cooldown
	 * @return whether or not the skill was allowed
	 */
	public boolean checkAndSetSkillCooltime(int skillID) {
		if (hasSkillOnCooldown(skillID)) {
			return false;
		} else {
			Skill skill = getSkill(skillID);
			if (skill != null && SkillData.getSkillInfoById(skillID).hasCooltime() && !SkillConstants.isKeydownCDSkill(skillID)) {
				setSkillCooldown(skillID, (byte) skill.getCurrentLevel());
			}
			return true;
		}
	}
	public void addSkillCooldown(int skillId, int time) {
		addSkillCoolTime(skillId, System.currentTimeMillis() + time);
		write(UserLocal.skillCooltimeSetM(skillId, time));
	}

	/**
	 * Sets a skill's cooltime according to their property in the WZ files, and stores the moment where the skill
	 * comes off of cooldown.
	 *
	 * @param skillID the skill's id to set
	 * @param slv     the current skill level
	 */
	public void setSkillCooldown(int skillID, byte slv) {
		SkillInfo si = SkillData.getSkillInfoById(skillID);
		if (si != null) {
			int cdInSec = si.getValue(SkillStat.cooltime, slv);
			int cdInMillis = cdInSec > 0 ? cdInSec * 1000 : si.getValue(SkillStat.cooltimeMS, slv);
			int alteredcd = getJobHandler().alterCooldownSkill(skillID);
			if (alteredcd >= 0) {
				cdInMillis = alteredcd;
			}
			// RuneStone of Skill
			if (getTemporaryStatManager().hasStatBySkillId(RuneStone.LIBERATE_THE_RUNE_OF_SKILL) && cdInMillis > 5000 && !si.isNotCooltimeReset()) {
				cdInMillis = 5000;
			}


			// Customized  skill cooldowns
			int fixedSkillCD = SkillConstants.getSkillCooldown(skillID);
			if(fixedSkillCD != -1) {
				cdInMillis = fixedSkillCD;
			}

			if (!hasSkillCDBypass() && cdInMillis > 0) {
				addSkillCoolTime(skillID, System.currentTimeMillis() + cdInMillis);
				write(UserLocal.skillCooltimeSetM(skillID, cdInMillis));
			}
		}
	}

	public CharacterPotentialMan getPotentialMan() {
		return potentialMan;
	}

	public Set<CharacterPotential> getPotentials() {
		return potentials;
	}

	public void setPotentials(Set<CharacterPotential> potentials) {
		this.potentials = potentials;
	}

	public int getHonorExp() {
		return getAvatarData().getCharacterStat().getHonorExp();
	}

	public void setHonorExp(int honorExp) {
		getAvatarData().getCharacterStat().setHonorExp(honorExp);
	}

	/**
	 * Adds honor exp to this Char, and sends a packet to the client with the new honor exp.
	 * Honor exp added may be negative, but the total honor exp will never go below 0.
	 *
	 * @param exp the exp to add (may be negative)
	 */
	public void addHonorExp(int exp) {
		setHonorExp(Math.max(0, getHonorExp() + exp));
		write(WvsContext.characterHonorExp(getHonorExp()));
	}

	public int getDeathCount() {
		return deathCount;
	}

	public void setDeathCount(int deathCount) {
		this.deathCount = deathCount;
	}

	public Set<LinkSkill> getLinkSkills() {
		return getAccount().getLinkSkills().stream()
				.filter(ls -> ls.getOwnerID() != getId())
				.collect(Collectors.toSet());
	}

	/**
	 * Adds a skill to this Char. If the Char already has this skill, just changes the levels.
	 *
	 * @param skillID      the skill's id to add
	 * @param currentLevel the current level of the skill
	 * @param masterLevel  the master level of the skill
	 */
	public void addSkill(int skillID, int currentLevel, int masterLevel) {
		Skill skill = SkillData.getSkillDeepCopyById(skillID);
		if (skill == null && !SkillConstants.isMakingSkillRecipe(skillID)) {
			log.error("No such skill found.");
			return;
		}
		skill.setCurrentLevel(currentLevel);
		skill.setMasterLevel(masterLevel);
		List<Skill> list = new ArrayList<>();
		list.add(skill);
		addSkill(skill);
		write(WvsContext.changeSkillRecordResult(list, true, false, false, false));
	}

	public long getRuneCooldown() {
		return runeStoneCooldown;
	}

	public void setRuneCooldown(long runeCooldown) {
		this.runeStoneCooldown = runeCooldown;
	}

	public MemorialCubeInfo getMemorialCubeInfo() {
		return memorialCubeInfo;
	}

	public void setMemorialCubeInfo(MemorialCubeInfo memorialCubeInfo) {
		this.memorialCubeInfo = memorialCubeInfo;
	}

	public Set<Familiar> getFamiliars() {
		return familiars;
	}

	public void setFamiliars(Set<Familiar> familiars) {
		this.familiars = familiars;
	}

	public boolean hasFamiliar(int familiarID) {
		return getFamiliars().stream().anyMatch(f -> f.getFamiliarID() == familiarID);
	}

	public Familiar getFamiliarByID(int familiarID) {
		return getFamiliars().stream().filter(f -> f.getFamiliarID() == familiarID).findAny().orElse(null);
	}

	public void addFamiliar(Familiar familiar) {
		getFamiliars().add(familiar);
	}

	public void removeFamiliarByID(int familiarID) {
		removeFamiliar(getFamiliarByID(familiarID));
	}

	public void removeFamiliar(Familiar familiar) {
		if (familiar != null) {
			getFamiliars().remove(familiar);
		}
	}

	public void setActiveFamiliar(Familiar activeFamiliar) {
		this.activeFamiliar = activeFamiliar;
	}

	public Familiar getActiveFamiliar() {
		return activeFamiliar;
	}

	public boolean hasSkillCDBypass() {
		return skillCDBypass;
	}

	public void setSkillCDBypass(boolean skillCDBypass) {
		this.skillCDBypass = skillCDBypass;
	}


	public Set<StolenSkill> getStolenSkills() {
		return stolenSkills;
	}

	public void setStolenSkills(Set<StolenSkill> stolenSkills) {
		this.stolenSkills = stolenSkills;
	}

	public void addStolenSkill(StolenSkill stolenSkill) {
		getStolenSkills().add(stolenSkill);
	}

	public void removeStolenSkill(StolenSkill stolenSkill) {
		if (stolenSkill != null) {
			getStolenSkills().remove(stolenSkill);
		}
	}

	public StolenSkill getStolenSkillByPosition(int position) {
		return getStolenSkills().stream().filter(ss -> ss.getPosition() == position).findAny().orElse(null);
	}

	public StolenSkill getStolenSkillBySkillId(int skillId) {
		return getStolenSkills().stream().filter(ss -> ss.getSkillid() == skillId).findAny().orElse(null);
	}


	public Set<ChosenSkill> getChosenSkills() {
		return chosenSkills;
	}

	public void setChosenSkills(Set<ChosenSkill> chosenSkills) {
		this.chosenSkills = chosenSkills;
	}

	public void addChosenSkill(ChosenSkill chosenSkill) {
		getChosenSkills().add(chosenSkill);
	}

	public void removeChosenSkill(ChosenSkill chosenSkill) {
		if (chosenSkill != null) {
			getChosenSkills().remove(chosenSkill);
		}
	}

	public ChosenSkill getChosenSkillByPosition(int position) {
		return getChosenSkills().stream().filter(ss -> ss.getPosition() == position).findAny().orElse(null);
	}

	public boolean isChosenSkillInStolenSkillList(int skillId) {
		return getStolenSkills().stream().filter(ss -> ss.getSkillid() == skillId).findAny().orElse(null) != null;
	}

	public Map<BaseStat, Long> getBaseStats() {
		return baseStats;
	}

	/**
	 * Adds a BaseStat's amount to this Char's BaseStat cache.
	 *
	 * @param bs     The BaseStat
	 * @param amount the amount of BaseStat to add
	 */
	public void addBaseStat(BaseStat bs, int amount) {
		getBaseStats().put(bs, getBaseStats().getOrDefault(bs, 0L) + amount);
	}

	/**
	 * Removes a BaseStat's amount from this Char's BaseStat cache.
	 *
	 * @param bs     The BaseStat
	 * @param amount the amount of BaseStat to remove
	 */
	public void removeBaseStat(BaseStat bs, int amount) {
		addBaseStat(bs, -amount);
	}

	public void addItemToInventory(int id, int quantity) {
		if (ItemConstants.isEquip(id)) {  //Equip
			Equip equip = ItemData.getEquipDeepCopyFromID(id, false);
			addItemToInventory(equip.getInvType(), equip, false);
			getClient().write(WvsContext.inventoryOperation(true, false,
                    Add, (short) equip.getBagIndex(), (byte) -1, 0, equip));

		} else {    //Item
			Item item = ItemData.getItemDeepCopy(id);
			item.setQuantity(quantity);
			addItemToInventory(item);
			getClient().write(WvsContext.inventoryOperation(true, false,
                    Add, (short) item.getBagIndex(), (byte) -1, 0, item));

		}
	}

	public int getSpentHyperSp() {
		int sp = 0;
		for (int skillID = 80000400; skillID <= 80000418; skillID++) {
			Skill skill = getSkill(skillID);
			if (skill != null) {
				sp += SkillConstants.getTotalNeededSpForHyperStatSkill(skill.getCurrentLevel());
			}
		}
		return sp;
	}

	public int getRewardPoints() {
		return rewardPoints;
	}

	public void setRewardPoints(int rewardPoints) {
		this.rewardPoints = rewardPoints;
	}

	public int[] getHyperRockFields() {
		return hyperrockfields;
	}

	public void setHyperRockFields(int[] hyperrockfields) {
		this.hyperrockfields = hyperrockfields;
	}

	public boolean isChangingChannel() {
		return changingChannel;
	}

	public void setChangingChannel(boolean changingChannel) {
		this.changingChannel = changingChannel;
	}

	public int getPartyID() {
		return partyID;
	}

	public void setPartyID(int partyID) {
		this.partyID = partyID;
	}

	public byte getMonsterParkCount() {
		return monsterParkCount;
	}

	public void setMonsterParkCount(byte monsterParkCount) {
		this.monsterParkCount = monsterParkCount;
	}

	public TownPortal getTownPortal() {
		return townPortal;
	}

	public void setTownPortal(TownPortal townPortal) {
		this.townPortal = townPortal;
	}

	public TradeRoom getTradeRoom() {
		return tradeRoom;
	}

	public void setTradeRoom(TradeRoom tradeRoom) {
		this.tradeRoom = tradeRoom;
	}

	public void damage(int damage) {
		HitInfo hi = new HitInfo();
		hi.hpDamage = damage;
		getJobHandler().handleHit(getClient(), hi);
	}

	public void changeChannel(byte channelId) {
		changeChannelAndWarp(channelId, getFieldID());
	}

	public void changeChannelAndWarp(byte channelId, int fieldId) {
		logout();
		setChangingChannel(true);
		Field field = getField();
		if (getFieldID() != fieldId) {
			setField(getOrCreateFieldByCurrentInstanceType(fieldId));
		}
		DatabaseManager.saveToDB(getAccount());
		int worldID = getClient().getChannelInstance().getWorldId();
		World world = Server.getInstance().getWorldById(worldID);
		field.removeChar(this);
		Channel channel = world.getChannelById(channelId);
		channel.addClientInTransfer(channelId, getId(), getClient());
		short port = (short) channel.getPort();
		write(ClientSocket.migrateCommand(true, port));
	}

	@Override
	public String toString() {
		return "Char{" +
				"(" + super.toString() +
				")id=" + id +
				", accId=" + accId +
				", name=" + getName() +
				'}';
	}

	public void setBattleRecordOn(boolean battleRecordOn) {
		this.battleRecordOn = battleRecordOn;
	}

	public boolean isBattleRecordOn() {
		return battleRecordOn;
	}

	public void checkAndRemoveExpiredItems() {
		Inventory[] inventories = new Inventory[]{getEquippedInventory(), getEquipInventory(), getConsumeInventory(),
				getEtcInventory(), getInstallInventory(), getCashInventory()};
		Set<Item> expiredItems = new HashSet<>();
		for (Inventory inv : inventories) {
			expiredItems.addAll(
					inv.getItems().stream()
							.filter(item -> item.getDateExpire().isExpired())
							.collect(Collectors.toSet())
			);
		}
		List<Integer> expiredItemIDs = expiredItems.stream().map(Item::getItemId).collect(Collectors.toList());
		write(WvsContext.message(MessageType.GENERAL_ITEM_EXPIRE_MESSAGE, expiredItemIDs));
		for (Item item : expiredItems) {
			consumeItem(item);
		}
	}

	public boolean isGuildMaster() {
		return getGuild() != null && getGuild().getLeaderID() == getId();
	}

	/**
	 * Checks if this Char has any of the given quests in progress. Also true if the size of the given set is 0.
	 *
	 * @param quests the set of quest ids to check
	 * @return whether or not this Char has any of the given quests
	 */
	public boolean hasAnyQuestsInProgress(Set<Integer> quests) {
		return quests.size() == 0 || quests.stream().anyMatch(this::hasQuestInProgress);
	}

	public int getPreviousFieldID() {
		return previousFieldID == 0 || previousFieldID == 999999999 ? 100000000 : previousFieldID;
	}

	public void setPreviousFieldID(int previousFieldID) {
		this.previousFieldID = previousFieldID;
	}

	public int getPreviousPortalID() {
		return previousPortalID;
	}

	public void setPreviousPortalID(int portalId) { previousPortalID = portalId; }

	public long getNextRandomPortalTime() {
		return nextRandomPortalTime;
	}

	public void setNextRandomPortalTime(long nextRandomPortalTime) {
		this.nextRandomPortalTime = nextRandomPortalTime;
	}

	public void clearCurrentDirectionNode() { this.currentDirectionNode.clear(); }

	public int getCurrentDirectionNode(int node) {
		Integer direction = currentDirectionNode.getOrDefault(node, null);
		if (direction == null) {
			currentDirectionNode.put(node, 0);
		}
		return currentDirectionNode.get(node);
	}

	public void increaseCurrentDirectionNode(int node) {
		Integer direction = currentDirectionNode.getOrDefault(node, null);
		if (direction == null) {
			currentDirectionNode.put(node, 1);
		} else {
			currentDirectionNode.put(node, direction + 1);
		}
	}

	public void punishLieDetectorEvasion() {
		if (getLieDetectorAnswer().length() > 0) {
			failedLieDetector();
		}
	}

	public String getLieDetectorAnswer() {
		return lieDetectorAnswer;
	}

	public void setLieDetectorAnswer(String answer) {
		lieDetectorAnswer = answer;
	}

	public void failedLieDetector() {
		setLieDetectorAnswer("");
		chatMessage(SpeakerChannel, "You have failed the Lie Detector test.");

		getClient().write(WvsContext.antiMacroResult(null, AntiMacro.AntiMacroResultType.AntiMacroRes_Fail.getVal(), AntiMacro.AntiMacroType.AntiMacroFieldRequest.getVal()));

		// TODO: handle fail
	}

	public void passedLieDetector() {
		setLieDetectorAnswer("");
		chatMessage(SpeakerChannel, "You have passed the Lie Detector test!");

		getClient().write(WvsContext.antiMacroResult(null, AntiMacro.AntiMacroResultType.AntiMacroRes_Success.getVal(), AntiMacro.AntiMacroType.AntiMacroFieldRequest.getVal()));

		// TODO: handle pass
	}

	public boolean sendLieDetector() {
		return sendLieDetector(false);
	}

	public boolean sendLieDetector(boolean force) {
		// LD ran too recently (15 min)
		if (!force && lastLieDetector != 0 && System.currentTimeMillis() - lastLieDetector < 900_000L) {
			return false;
		}

		// TODO: don't allow more than 3 refreshes

		lieDetectorAnswer = "";
		String font = AntiMacro.FONTS[Util.getRandom(AntiMacro.FONTS.length - 1)];

		String options = "abcdefghijklmnopqrstuvwxyz0123456789";

		for (int i = 1; i <= 6; i++) {
			if (Util.getRandom(1) == 0) {
				options = options.toUpperCase();
			} else {
				options = options.toLowerCase();
			}

			lieDetectorAnswer += options.charAt(Util.getRandom(options.length() - 1));
		}

		try {
			AntiMacro am = new AntiMacro(font, lieDetectorAnswer);
			lastLieDetector = System.currentTimeMillis();

			byte[] image = am.generateImage(196, 44, Color.BLACK, AntiMacro.getRandomColor());
			getClient().write(WvsContext.antiMacroResult(image, AntiMacro.AntiMacroResultType.AntiMacroRes.getVal(), AntiMacro.AntiMacroType.AntiMacroFieldRequest.getVal()));
		} catch (IOException|FontFormatException e) {
			e.printStackTrace();

			return false;
		}

		return true;
	}

    public OffenseManager getOffenseManager() {
        return getUser().getOffenseManager();
    }

	/**
	 * Applies the mp consumption of a skill.
	 * @param skillID the skill's id
	 * @param slv the current skill level
	 * @return whether the consumption was successful (unsuccessful = not enough mp)
	 */
	public boolean applyMpCon(int skillID, byte slv) {
		int curMp = getStat(Stat.mp);
		SkillInfo si = SkillData.getSkillInfoById(skillID);
		if (si == null) {
			return true;
		}
		int mpCon = si.getValue(SkillStat.mpCon, slv);
		boolean hasEnough = curMp >= mpCon;
		if (hasEnough) {
			addStatAndSendPacket(Stat.mp, -mpCon);
		}
		return hasEnough;
	}

	public boolean applyBulletCon(int skillID, byte slv) {
		if (getTemporaryStatManager().hasStat(NoBulletConsume) || JobConstants.isPhantom(getJob())) {
			return true;
		}
		SkillInfo si = SkillData.getSkillInfoById(skillID);
		if (si == null) {
			return true;
		}
		int bulletCon = si.getValue(SkillStat.bulletCount, slv) + si.getValue(SkillStat.bulletConsume, slv);
		if (bulletCon <= 0) {
			return true;
		}
		int bulletItemId = getBulletIDForAttack();
		if (bulletItemId == 0) {
			return false;
		}
		if (!hasItemCount(getBulletIDForAttack(), bulletCon)) {
			setBulletIDForAttack(calculateBulletIDForAttack(bulletCon));
		}
		boolean hasEnough = hasItemCount(bulletItemId, bulletCon);
		if (hasEnough) {
			consumeItem(bulletItemId, bulletCon);
		}
		return hasEnough;
	}

	public boolean hasTutor() {
		return tutor;
	}

	public void hireTutor(boolean set) {
		tutor = set;
		write(UserLocal.hireTutor(set));
	}

	/**
	 * Shows tutor automated message (the client is taking the message information from wz).
	 * @param id the id of the message.
	 * @param duration message duration
	 */
	public void tutorAutomatedMsg(int id, int duration) {
		if (!tutor) {
			hireTutor(true);
		}
		write(UserLocal.tutorMsg(id, duration));
	}

	/**
	 * Shows tutor custom message (you decide which message the tutor will say).
	 * @param message your custom message
	 * @param width size of the message box
	 * @param duration message duration
	 */
	public void tutorCustomMsg(String message, int width, int duration) {
		if (!tutor) {
			hireTutor(true);
		}
		write(UserLocal.tutorMsg(message, width, duration));
	}

	public void setTransferField(int fieldID) {
		this.transferField = fieldID;
		this.transferFieldReq = fieldID == 0 ? 0 : getField().getId();
	}

	public int getTransferField() {
		return transferField;
	}

	public int getTransferFieldReq() {
		return transferFieldReq;
	}

	public void setMakingSkillLevel(int skillID, int level) {
		Skill skill = getSkill(skillID);
		if (skill != null) {
			skill.setCurrentLevel((level << 24) + getMakingSkillProficiency(skillID));
			addSkill(skill);
			write(WvsContext.changeSkillRecordResult(skill));
		}
	}

	public int getMakingSkillLevel(int skillID) {
		return (getSkillLevel(skillID) >> 24) <= 0 ? 0 : getSkillLevel(skillID) >> 24;
	}

	public void setMakingSkillProficiency(int skillID, int proficiency) {
		Skill skill = getSkill(skillID);
		if (skill != null) {
			skill.setCurrentLevel((getMakingSkillLevel(skillID) << 24) + proficiency);
			addSkill(skill);
			write(WvsContext.changeSkillRecordResult(skill));
		}
	}

	public int getMakingSkillProficiency(int skillID) {
		return (getSkillLevel(skillID) & 0xFFFFFF) <= 0 ? 0 : getSkillLevel(skillID) & 0xFFFFFF;
	}

	public void addMakingSkillProficiency(int skillID, int amount) {
		int makingSkillID = SkillConstants.recipeCodeToMakingSkillCode(skillID);
		int level = getMakingSkillLevel(makingSkillID);

		int neededExp = SkillConstants.getNeededProficiency(level);
		if (neededExp <= 0) {
			return;
		}
		int exp = getMakingSkillProficiency(makingSkillID);
		if (exp >= neededExp) {
			write(UserLocal.chatMsg(ChatType.GameDesc, "You can't gain any more Herbalism mastery until you level your skill."));
			write(UserLocal.chatMsg(ChatType.GameDesc, "See the appropriate NPC in Ardentmill to level up."));
			setMakingSkillProficiency(makingSkillID, neededExp);
			return;
		}
		int newExp = exp + amount;
		write(UserLocal.chatMsg(ChatType.GameDesc, SkillConstants.getMakingSkillName(makingSkillID) + "'s mastery increased. (+" + amount + ")"));
		if (newExp >= neededExp) {
			write(UserLocal.noticeMsg("You've accumulated " + SkillConstants.getMakingSkillName(makingSkillID) + " mastery. See an NPC in town to level up.", true));
			setMakingSkillProficiency(makingSkillID, neededExp);
		} else {
			setMakingSkillProficiency(makingSkillID, newExp);
		}
	}

	public void makingSkillLevelUp(int skillID) {
		int level = getMakingSkillLevel(skillID);
		int neededExp = SkillConstants.getNeededProficiency(level);
		if (neededExp <= 0) {
			return;
		}
		int exp = getMakingSkillProficiency(skillID);
		if (exp >= neededExp) {
			setMakingSkillProficiency(skillID, 0);
			setMakingSkillLevel(skillID, level + 1);
			Stat trait = Stat.craftEXP;
			switch (skillID) {
				case 92000000:
					trait = Stat.senseEXP;
					break;
				case 92010000:
					trait = Stat.willEXP;
					break;
			}
			addTraitExp(trait, (int) Math.pow(2, (level + 1) + 2));
			write(FieldPacket.fieldEffect(FieldEffect.playSound("profession/levelup", 100)));
		}
	}

	public void addNx(int nx) {
		getAccount().addNXCredit(nx);
		chatScriptMessage("You have gained " + nx + " NX.");
		write(WvsContext.setMaplePoint(getAccount().getNxCredit()));
	}

	public void initBlessingSkillNames() {
		Account account = getAccount();
		Char fairyChar = null;
		for (Char chr : account.getCharacters()) {
			if (!chr.equals(this)
					&& chr.getLevel() >= 10
					&& (fairyChar == null || chr.getLevel() > fairyChar.getLevel())) {
				fairyChar = chr;
			}
		}
		if (fairyChar != null) {
			setBlessingOfFairy(fairyChar.getName());
		}
		Char empressChar = null;
		for (Char chr : account.getCharacters()) {
			if (!chr.equals(this)
					&& (JobConstants.isCygnusKnight(chr.getJob()) || JobConstants.isMihile(chr.getJob())
					&& chr.getLevel() >= 5
					&& (empressChar == null || chr.getLevel() > empressChar.getLevel()))) {
				empressChar = chr;
			}
		}
		if (empressChar != null) {
			setBlessingOfEmpress(empressChar.getName());
		}
	}

	public void initBlessingSkills() {
		Char fairyChar = getAccount().getCharByName(getBlessingOfFairy());
		if (fairyChar != null) {
			addSkill(SkillConstants.getFairyBlessingByJob(getJob()),
					Math.min(20, fairyChar.getLevel() / 10), 20);
		}
		Char empressChar = getAccount().getCharByName(getBlessingOfEmpress());
		if (empressChar != null) {
			addSkill(SkillConstants.getEmpressBlessingByJob(getJob()),
					Math.min(30, empressChar.getLevel() / 5), 30);
		}
	}

	public Map<Integer, Integer> getHyperPsdSkillsCooltimeR() {
		return hyperPsdSkillsCooltimeR;
	}

	public void setHyperPsdSkillsCooltimeR(Map<Integer, Integer> hyperPsdSkillsCooltimeR) {
		this.hyperPsdSkillsCooltimeR = hyperPsdSkillsCooltimeR;
	}

	public boolean isInvincible() {
		return isInvincible;
	}

	public void setInvincible(boolean invincible) {
		isInvincible = invincible;
	}

	public void setQuickslotKeys(List<Integer> quickslotKeys) {
		this.quickslotKeys = quickslotKeys;
	}

	public List<Integer> getQuickslotKeys() {
		return quickslotKeys;
	}

	public Dragon getDragon() {
		Dragon dragon = null;
		if (getJobHandler() instanceof Evan) {
			dragon = ((Evan) getJobHandler()).getDragon();
		}
		return dragon;
	}

	/**
	 * Checks if this Char has a skill with at least a given level.
	 * @param skillID the skill to get
	 * @param slv the minimum skill level
	 * @return whether or not this Char has the skill with the given skill level
	 */
	public boolean hasSkillWithSlv(int skillID, short slv) {
		Skill skill = getSkill(skillID);
		return skill != null && skill.getCurrentLevel() >= slv;
	}

	public World getWorld() {
		return getClient().getWorld();
	}

    public Android getAndroid() {
        return android;
    }

    public void setAndroid(Android android) {
        this.android = android;
    }

	/**
	 * Initializes this Char's Android according to their heart + android equips. Will not do anything if an Android
	 * already exists.
	 * @param override Whether or not to override the old Android if one exists.
	 */
	public void initAndroid(boolean override) {
		if (getAndroid() == null || override) {
			Item heart = getEquippedItemByBodyPart(BodyPart.MechanicalHeart);
			Item android = getEquippedItemByBodyPart(BodyPart.Android);
			if (heart != null && android != null && ((Equip) heart).getAndroidGrade() + 3 >= ((Equip) android).getAndroidGrade()) {
				int androidId = ((Equip) android).getAndroid();
				AndroidInfo androidInfo = EtcData.getAndroidInfoById(androidId);
				if (getAndroid() != null) {
					getField().removeLife(getAndroid());
				}
				Android newAndroid = new Android(this, androidInfo);
				if (getPosition() != null) {
					newAndroid.setPosition(getPosition().deepCopy());
				}
				setAndroid(newAndroid);
			}
		}
	}

	public void useStatChangeItem(Item item, boolean consume) {
		TemporaryStatManager tsm = getTemporaryStatManager();
		int itemID = item.getItemId();
		Map<SpecStat, Integer> specStats = ItemData.getItemInfoByID(itemID).getSpecStats();
		if (specStats.size() > 0) {
			ItemBuffs.giveItemBuffsFromItemID(this, tsm, itemID);
		} else {
			switch (itemID) {
				case 2050004: // All cure
					tsm.removeAllDebuffs();
					break;
				default:
					chatMessage(ChatType.Mob, String.format("Unhandled stat change item %d", itemID));
			}
		}
		if (consume) {
			consumeItem(item);
		}
		dispose();
	}

	public int getSpentActiveHyperSkillSp() {
		int sp = 0;
		for (Skill skill : getSkills()) {
			SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
			if (si.getHyper() == 2) {
				sp += skill.getCurrentLevel();
			}
		}
		return sp;
	}

	public int getSpentPassiveHyperSkillSp() {
		int sp = 0;
		for (Skill skill : getSkills()) {
			SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
			if (si.getHyper() == 1) {
				sp += skill.getCurrentLevel();
			}
		}
		return sp;
	}
	public PsychicArea addPsychicArea(PsychicArea pa) {
		psychicAreas.put(pa.localPsychicAreaKey, pa);
		return pa;
	}

	public void removePsychicArea(int psychicAreaKey) {
		this.psychicAreas.remove(psychicAreaKey);
	}

	public PsychicArea getPsychicArea(int psychicAreaKey) {
		return psychicAreas.getOrDefault(psychicAreaKey, null);
	}

	public void addPsychicLock(PsychicLock pl) {
		psychicLocks.put(pl.key, pl);
	}

	public void removePsychicLock(int key) {
		this.psychicLocks.remove(key);
	}

	public Map<Long, Integer> getItemBoughtAmounts() {
		return itemBoughtAmounts;
	}

	public void addItemBoughtAmount(long itemId, int amount) {
		getItemBoughtAmounts().put(itemId, amount);
	}

	public void increaseGolluxStack() {
		int maxStack = 5;
		TemporaryStatManager tsm = getTemporaryStatManager();
		int stack = tsm.getCurrentStats().get(Stigma) != null ? tsm.getCurrentStats().get(Stigma).get(0).nOption : 0;
		stack++;
		Option o = new Option();
		if (stack >= maxStack) {
			this.damage(getHP());
			stack = maxStack;
		}
		o.nOption = stack;
		o.rOption = 800;
		o.bOption = maxStack;
		tsm.putCharacterStatValue(Stigma, o);
		// no tOption  as it would probably be permanent (till death)
		tsm.sendSetStatPacket();
	}

	public Merchant getVisitingmerchant() {
		return visitingmerchant;
	}

	public void setVisitingmerchant(Merchant visitingmerchant) {
		this.visitingmerchant = visitingmerchant;
	}

	public Merchant getMerchant() {
		if (merchant == null) {
			findMerchant();
		}
		return merchant;
	}

	public void setMerchant(Merchant merchant) {
		this.merchant = merchant;
	}

	public void findMerchant() {
		ArrayList<Merchant> allmerchants = this.getWorld().getMerchants();
		for (Merchant m : allmerchants) {
			if (m.getOwnerID() == this.getId()) {
				this.setMerchant(m);
				break;
			}
		}
	}

	public void getItemsFromEmployeeTrunk() {
		EmployeeTrunk employeeTrunk = getAccount().getEmployeeTrunk();
		long earnings = employeeTrunk.getMoney();
		if (getMerchant() != null) {
			chatMessage("You have still got an open merchant at room: " + getMerchant().getField().getId() % 10 + " at channel: " + getMerchant().getField().getChannel());
			return;
		}
		if (!canAddMoney(earnings)) {
			chatMessage("You cannot hold that much mesos.");
			return;
		}
		List<MerchantItem> itemsMoved = new ArrayList<MerchantItem>();
		for (MerchantItem mi : employeeTrunk.getItems()) {
			Item i = mi.item.deepCopy();
			i.setQuantity(mi.bundles * i.getQuantity());
			if (getInventoryByType(i.getInvType()).canPickUp(i)) {
				if (mi.bundles > 0) {    //remove MerchantItem from merchant and database but if bundles <= 0 but don't add to char inv
					addItemToInventory(i);
				}
				itemsMoved.add(mi);
			}
		}
		employeeTrunk.getItems().removeAll(itemsMoved);
		if (getMerchant() != null) { //merchant can be null after server restart
			merchant.getItems().removeAll(itemsMoved);
		}
		addMoney(earnings);
		employeeTrunk.setMoney(0);
		DatabaseManager.saveToDB(employeeTrunk);
	}

	public Map<ScrollStat, Integer> getStatsBySetEffects() {
		HashMap<ScrollStat, Integer> stats = new HashMap<>();
		HashMap<Integer, Integer> setIdToLevel = new HashMap<>();
		for (Item item : getEquippedInventory().getItems()) {
			Equip equip = (Equip) item;
			int setItemId = equip.getSetItemID();
			if (setItemId > 0) {
				int level = setIdToLevel.getOrDefault(setItemId, 0);
				level++;
				setIdToLevel.put(setItemId, level);
			}
		}
		for (Map.Entry<Integer, Integer> entry : setIdToLevel.entrySet()) {
			int setId = entry.getKey();
			int setLevel = entry.getValue();
			SetEffect setEffect = EtcData.getSetEffectInfoById(setId);
			for (int i = 1; i <= setLevel; i++) {
				if (setEffect.getStatsByLevel(i) == null) {
					continue;
				}
				for (Object effect : setEffect.getStatsByLevel(i)) {
					if (effect instanceof net.swordie.ms.util.container.Tuple) {
						ScrollStat ss = (ScrollStat) (((net.swordie.ms.util.container.Tuple) effect).getLeft());
						int amount = (int) (((net.swordie.ms.util.container.Tuple) effect).getRight());
						stats.put(ss, amount);
					}
				}
			}
		}
		return stats;
	}

	public List<ItemOption> getItemOptionsBySetEffects() {
		List<ItemOption> options = new ArrayList<>();
		HashMap<Integer, Integer> setIdToLevel = new HashMap<>();
		for (Item item : getEquippedInventory().getItems()) {
			Equip equip = (Equip) item;
			int setItemId = equip.getSetItemID();
			if (setItemId > 0) {
				int level = setIdToLevel.getOrDefault(setItemId, 0);
				level++;
				setIdToLevel.put(setItemId, level);
			}
		}
		for (Map.Entry<Integer, Integer> entry : setIdToLevel.entrySet()) {
			int setId = entry.getKey();
			int setLevel = entry.getValue();
			SetEffect setEffect = EtcData.getSetEffectInfoById(setId);
			for (int i = 1; i <= setLevel; i++) {
				if (setEffect.getStatsByLevel(i) == null) {
					continue;
				}
				for (Object effect : setEffect.getStatsByLevel(i)) {
					if (effect instanceof ItemOption) {
						options.add((ItemOption) effect);
					}
				}
			}
		}
		return options;
	}



	public int getStatAmountSetEffect(BaseStat baseStat) {
		int amount = 0;
		Map<ScrollStat, Integer> stats = getStatsBySetEffects();
		for (Map.Entry<ScrollStat, Integer> entry : stats.entrySet()) {
			if (entry.getKey().getBaseStat() == baseStat) {
				amount += entry.getValue();
			}
		}

		List<ItemOption> options = getItemOptionsBySetEffects();
		for (ItemOption option : options) {
			int id = option.getId();
			int level = option.getReqLevel();
			ItemOption io = ItemData.getItemOptionById(id);
			if (io != null) {
				Map<BaseStat, Double> valMap = io.getStatValuesByLevel(level);
				amount += valMap.getOrDefault(baseStat, 0D);
			}
		}

		return amount;
	}

	public void addMaplePoint(int maplePoint) {
		getUser().addMaplePoints(maplePoint);
		chatScriptMessage("You have gained " + maplePoint + " MaplePoint.");
		getClient().write(WvsContext.setMaplePoint(getUser().getMaplePoints()));
	}
}
