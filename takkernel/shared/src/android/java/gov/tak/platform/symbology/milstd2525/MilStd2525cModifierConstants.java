package gov.tak.platform.symbology.milstd2525;

import java.util.HashMap;
import java.util.Map;

import armyc2.c2sd.renderer.utilities.ModifiersTG;
import armyc2.c2sd.renderer.utilities.ModifiersUnits;

interface MilStd2525cModifierConstants
{
    String X_ALTITUDE_DEPTH = ModifiersTG.getModifierLetterCode(ModifiersTG.X_ALTITUDE_DEPTH);
    String W_DTG_1 = ModifiersTG.getModifierLetterCode(ModifiersTG.W_DTG_1);
    String W1_DTG_2 = ModifiersTG.getModifierLetterCode(ModifiersTG.W1_DTG_2);
    String AM_DISTANCE = ModifiersTG.getModifierLetterCode(ModifiersTG.AM_DISTANCE);
    String AN_AZIMUTH = ModifiersTG.getModifierLetterCode(ModifiersTG.AN_AZIMUTH);
    String D_TASK_FORCE_INDICATOR  = ModifiersUnits.getModifierLetterCode(ModifiersUnits.D_TASK_FORCE_INDICATOR);
    String Q_DIRECTION_OF_MOVEMENT = ModifiersUnits.getModifierLetterCode(ModifiersUnits.Q_DIRECTION_OF_MOVEMENT);
    String R_MOBILITY_INDICATOR  = ModifiersUnits.getModifierLetterCode(ModifiersUnits.R_MOBILITY_INDICATOR);
    String S_HQ_STAFF_OR_OFFSET_INDICATOR  = ModifiersUnits.getModifierLetterCode(ModifiersUnits.S_HQ_STAFF_OR_OFFSET_INDICATOR);
    String AB_FEINT_DUMMY_INDICATOR  = ModifiersUnits.getModifierLetterCode(ModifiersUnits.AB_FEINT_DUMMY_INDICATOR);
    String AC_INSTALLATION  = ModifiersUnits.getModifierLetterCode(ModifiersUnits.AC_INSTALLATION);
    String AG_AUX_EQUIP_INDICATOR  = ModifiersUnits.getModifierLetterCode(ModifiersUnits.AG_AUX_EQUIP_INDICATOR);
    String AH_AREA_OF_UNCERTAINTY  = ModifiersUnits.getModifierLetterCode(ModifiersUnits.AH_AREA_OF_UNCERTAINTY);
    String AI_DEAD_RECKONING_TRAILER  = ModifiersUnits.getModifierLetterCode(ModifiersUnits.AI_DEAD_RECKONING_TRAILER);
    String AJ_SPEED_LEADER = ModifiersUnits.getModifierLetterCode(ModifiersUnits.AJ_SPEED_LEADER);
    String AK_PAIRING_LINE = ModifiersUnits.getModifierLetterCode(ModifiersUnits.AK_PAIRING_LINE);
    //String AQ_GUARDED_UNIT = ModifiersUnits.getModifierLetterCode(ModifiersUnits.AQ_GUARDED_UNIT);
    String F_REINFORCED_REDUCED = ModifiersUnits.getModifierLetterCode(ModifiersUnits.F_REINFORCED_REDUCED);
    String G_STAFF_COMMENTS = ModifiersUnits.getModifierLetterCode(ModifiersUnits.G_STAFF_COMMENTS);
    String H_ADDITIONAL_INFO_1 = ModifiersUnits.getModifierLetterCode(ModifiersUnits.H_ADDITIONAL_INFO_1);
    String J_EVALUATION_RATING = ModifiersUnits.getModifierLetterCode(ModifiersUnits.J_EVALUATION_RATING);
    String K_COMBAT_EFFECTIVENESS = ModifiersUnits.getModifierLetterCode(ModifiersUnits.K_COMBAT_EFFECTIVENESS);
    String M_HIGHER_FORMATION = ModifiersUnits.getModifierLetterCode(ModifiersUnits.M_HIGHER_FORMATION);
    String P_IFF_SIF = ModifiersUnits.getModifierLetterCode(ModifiersUnits.P_IFF_SIF);
    String T_UNIQUE_DESIGNATION_1 = ModifiersUnits.getModifierLetterCode(ModifiersUnits.T_UNIQUE_DESIGNATION_1);
    String Y_LOCATION = ModifiersUnits.getModifierLetterCode(ModifiersUnits.Y_LOCATION);
    String Z_SPEED = ModifiersUnits.getModifierLetterCode(ModifiersUnits.Z_SPEED);
    String AA_SPECIAL_C2_HQ = ModifiersUnits.getModifierLetterCode(ModifiersUnits.AA_SPECIAL_C2_HQ);
    String AL_OPERATIONAL_CONDITION = ModifiersUnits.getModifierLetterCode(ModifiersUnits.AL_OPERATIONAL_CONDITION);
    String WIDTH = ModifiersTG.getModifierLetterCode(ModifiersTG.WIDTH);
    String LENGTH = ModifiersTG.getModifierLetterCode(ModifiersTG.LENGTH);
    String RADIUS = ModifiersTG.getModifierLetterCode(ModifiersTG.RADIUS);
    Map<String, Integer> MaxLength = new HashMap<String, Integer>() {{
        put(F_REINFORCED_REDUCED, 3);
        put(G_STAFF_COMMENTS, 20);
        put(H_ADDITIONAL_INFO_1, 20);
        put(J_EVALUATION_RATING, 2);
        put(K_COMBAT_EFFECTIVENESS, 5);
        put(M_HIGHER_FORMATION, 21);
        put(P_IFF_SIF, 5);
        put(T_UNIQUE_DESIGNATION_1, 21);
        put(W_DTG_1, 16);
        put(X_ALTITUDE_DEPTH, 14);
        put(Y_LOCATION, 19);
        put(Z_SPEED, 8);
        put(AA_SPECIAL_C2_HQ, 9);
    }};
}
