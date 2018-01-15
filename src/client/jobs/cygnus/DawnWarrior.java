package client.jobs.cygnus;

import client.Client;
import client.character.Char;
import client.character.HitInfo;
import client.character.skills.*;
import client.jobs.Job;
import client.life.Mob;
import client.life.MobTemporaryStat;
import connection.InPacket;
import constants.JobConstants;
import enums.ChatMsgColour;
import enums.MobStat;
import loaders.SkillData;
import packet.WvsContext;
import util.Util;

import java.util.Arrays;

import static client.character.skills.CharacterTemporaryStat.Booster;
import static client.character.skills.CharacterTemporaryStat.IndieStatR;
import static client.character.skills.SkillStat.prop;
import static client.character.skills.SkillStat.time;
import static client.character.skills.SkillStat.x;

/**
 * Created on 12/14/2017.
 */
public class DawnWarrior extends Job {

    public static final int SOUL_ELEMENT = 11001022; //Buff  (Immobility Debuff)

    public static final int SOUL_SPEED = 11101024; //Buff
    public static final int FALLING_MOON = 11101022; //Buff (Unlimited Duration)

    public static final int RISING_SUN = 11111022; //Buff (Unlimited Duration)
    public static final int TRUE_SIGHT = 11111023; //Buff (Mob Def Debuff & Final DmgUp Debuff)

    public static final int EQUINOX_CYCLE = 11121012; //Buff
    public static final int IMPALING_RAYS = 11121004; //Special Attack (Incapacitate Debuff)
    public static final int CALL_OF_CYGNUS_DW = 11121000; //Buff

    private int[] buffs = new int[] {
            SOUL_ELEMENT,
            SOUL_SPEED,
            FALLING_MOON,
            RISING_SUN,
            TRUE_SIGHT,
            EQUINOX_CYCLE,
            CALL_OF_CYGNUS_DW,
    };

    public DawnWarrior(Char chr) {
        super(chr);
    }

    public void handleBuff(Client c, InPacket inPacket, int skillID, byte slv) {
        Char chr = c.getChr();
        SkillInfo si = SkillData.getSkillInfoById(skillID);
        TemporaryStatManager tsm = c.getChr().getTemporaryStatManager();
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        switch (skillID) {
            case SOUL_ELEMENT:
                //TODO
                break;
            case SOUL_SPEED:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.putCharacterStatValue(Booster, o1);
                break;
            case FALLING_MOON:
                //TODO
                break;
            case RISING_SUN:
                //TODO
                break;
            case EQUINOX_CYCLE:
                //TODO
                break;
            case CALL_OF_CYGNUS_DW:
                o1.nReason = skillID;
                o1.nValue = si.getValue(x, slv);
                o1.tStart = (int) System.currentTimeMillis();
                o1.tTerm = si.getValue(time, slv);
                tsm.putCharacterStatValue(IndieStatR, o1); //Indie
                break;
        }
        c.write(WvsContext.temporaryStatSet(tsm));
    }

    private boolean isBuff(int skillID) {
        return Arrays.stream(buffs).anyMatch(b -> b == skillID);
    }

    @Override
    public void handleAttack(Client c, AttackInfo attackInfo) {
        Char chr = c.getChr();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Skill skill = chr.getSkill(attackInfo.skillId);
        int skillID = 0;
        SkillInfo si = null;
        boolean hasHitMobs = attackInfo.mobAttackInfo.size() > 0;
        int slv = 0;
        if (skill != null) {
            si = SkillData.getSkillInfoById(skill.getSkillId());
            slv = skill.getCurrentLevel();
            skillID = skill.getSkillId();
        }
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        switch (attackInfo.skillId) {
            case IMPALING_RAYS:
                if (Util.succeedProp(si.getValue(prop, slv))) {
                    for (MobAttackInfo mai : attackInfo.mobAttackInfo) {
                        Mob mob = (Mob) chr.getField().getLifeByObjectID(mai.mobId);
                        MobTemporaryStat mts = mob.getTemporaryStat();
                        o1.nOption = 1;
                        o1.rOption = skill.getSkillId();
                        o1.tOption = si.getValue(time, slv);
                        mts.addStatOptionsAndBroadcast(MobStat.Stun, o1);
                    }
                }
                break;
        }
    }

    @Override
    public void handleSkill(Client c, int skillID, byte slv, InPacket inPacket) {
        Char chr = c.getChr();
        Skill skill = chr.getSkill(skillID);
        SkillInfo si = null;
        if(skill != null) {
            si = SkillData.getSkillInfoById(skillID);
        }
        chr.chatMessage(ChatMsgColour.YELLOW, "SkillID: " + skillID);
        if (isBuff(skillID)) {
            handleBuff(c, inPacket, skillID, slv);
        } else {
            Option o1 = new Option();
            Option o2 = new Option();
            Option o3 = new Option();
            switch(skillID) {
                case TRUE_SIGHT:
                    //TODO
                    // Mob Def = x
                    // Final Dmg on mob = v
                    break;
            }
        }
    }

    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {

    }

    @Override
    public boolean isHandlerOfJob(short id) {
        JobConstants.JobEnum job = JobConstants.JobEnum.getJobById(id);
        switch (job) {
            case NOBLESSE:
            case DAWNWARRIOR1:
            case DAWNWARRIOR2:
            case DAWNWARRIOR3:
            case DAWNWARRIOR4:
                return true;
            default:
                return false;
        }
    }
}
