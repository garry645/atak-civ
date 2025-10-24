package gov.tak.platform.symbology.milstd2525;

import java.util.HashMap;
import java.util.Map;

import ArmyC2.C2SD.Utilities.ModifiersTG;
import ArmyC2.C2SD.Utilities.ModifiersUnits;

interface MilStd2525cModifierConstants
{
    // unit modifiers
    String A_SYMBOL_ICON = ModifiersUnits.A_SYMBOL_ICON;
    String B_ECHELON = ModifiersUnits.B_ECHELON;
    String C_QUANTITY = ModifiersUnits.C_QUANTITY;
    String D_TASK_FORCE_INDICATOR = ModifiersUnits.D_TASK_FORCE_INDICATOR;
    String E_FRAME_SHAPE_MODIFIER = ModifiersUnits.E_FRAME_SHAPE_MODIFIER;
    String F_REINFORCED_REDUCED = ModifiersUnits.F_REINFORCED_REDUCED;
    String G_STAFF_COMMENTS = ModifiersUnits.G_STAFF_COMMENTS;
    String H_ADDITIONAL_INFO_1 = ModifiersUnits.H_ADDITIONAL_INFO_1;
    String H1_ADDITIONAL_INFO_2 = ModifiersUnits.H1_ADDITIONAL_INFO_2;
    String H2_ADDITIONAL_INFO_3 = ModifiersUnits.H2_ADDITIONAL_INFO_3;
    String J_EVALUATION_RATING = ModifiersUnits.J_EVALUATION_RATING;
    String K_COMBAT_EFFECTIVENESS = ModifiersUnits.K_COMBAT_EFFECTIVENESS;
    String L_SIGNATURE_EQUIP = ModifiersUnits.L_SIGNATURE_EQUIP;
    String M_HIGHER_FORMATION = ModifiersUnits.M_HIGHER_FORMATION;
    String N_HOSTILE = ModifiersUnits.N_HOSTILE;
    String P_IFF_SIF = ModifiersUnits.P_IFF_SIF;
    String Q_DIRECTION_OF_MOVEMENT = ModifiersUnits.Q_DIRECTION_OF_MOVEMENT;
    String R_MOBILITY_INDICATOR = ModifiersUnits.R_MOBILITY_INDICATOR;
    String R2_SIGNIT_MOBILITY_INDICATOR = ModifiersUnits.R2_SIGNIT_MOBILITY_INDICATOR;
    String S_HQ_STAFF_OR_OFFSET_INDICATOR = ModifiersUnits.S_HQ_STAFF_OR_OFFSET_INDICATOR;
    String T_UNIQUE_DESIGNATION_1 = ModifiersUnits.T_UNIQUE_DESIGNATION_1;
    String T1_UNIQUE_DESIGNATION_2 = ModifiersUnits.T1_UNIQUE_DESIGNATION_2;
    String V_EQUIP_TYPE = ModifiersUnits.V_EQUIP_TYPE;
    String W_DTG_1 = ModifiersUnits.W_DTG_1;
    String W1_DTG_2 = ModifiersUnits.W1_DTG_2;
    String X_ALTITUDE_DEPTH = ModifiersUnits.X_ALTITUDE_DEPTH;
    String Y_LOCATION = ModifiersUnits.Y_LOCATION;
    String Z_SPEED = ModifiersUnits.Z_SPEED;
    String AA_SPECIAL_C2_HQ = ModifiersUnits.AA_SPECIAL_C2_HQ;
    String AB_FEINT_DUMMY_INDICATOR = ModifiersUnits.AB_FEINT_DUMMY_INDICATOR;
    String AC_INSTALLATION = ModifiersUnits.AC_INSTALLATION;
    String AD_PLATFORM_TYPE = ModifiersUnits.AD_PLATFORM_TYPE;
    String AE_EQUIPMENT_TEARDOWN_TIME = ModifiersUnits.AE_EQUIPMENT_TEARDOWN_TIME;
    String AF_COMMON_IDENTIFIER = ModifiersUnits.AF_COMMON_IDENTIFIER;
    String AG_AUX_EQUIP_INDICATOR = ModifiersUnits.AG_AUX_EQUIP_INDICATOR;
    String AH_AREA_OF_UNCERTAINTY = ModifiersUnits.AH_AREA_OF_UNCERTAINTY;
    String AI_DEAD_RECKONING_TRAILER = ModifiersUnits.AI_DEAD_RECKONING_TRAILER;
    String AJ_SPEED_LEADER = ModifiersUnits.AJ_SPEED_LEADER;
    String AK_PAIRING_LINE = ModifiersUnits.AK_PAIRING_LINE;
    String AL_OPERATIONAL_CONDITION = ModifiersUnits.AL_OPERATIONAL_CONDITION;
    String AO_ENGAGEMENT_BAR = ModifiersUnits.AO_ENGAGEMENT_BAR;
    String CC_COUNTRY_CODE = ModifiersUnits.CC_COUNTRY_CODE;
    String CN_CPOF_NAME_LABEL = ModifiersUnits.CN_CPOF_NAME_LABEL;
    String SCC_SONAR_CLASSIFICATION_CONFIDENCE = ModifiersUnits.SCC_SONAR_CLASSIFICATION_CONFIDENCE;
    // TG modifiers
    String AM_DISTANCE = ModifiersTG.AM_DISTANCE;
    String AN_AZIMUTH = ModifiersTG.AN_AZIMUTH;
    String LENGTH = ModifiersTG.LENGTH;
    String WIDTH = ModifiersTG.WIDTH;
    String RADIUS = ModifiersTG.RADIUS;
    String ANGLE = ModifiersTG.ANGLE;

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

    static String getModifierName(String modifier)
    {
        switch(modifier)
        {
            case A_SYMBOL_ICON:
                return "Symbol Icon";
            case B_ECHELON:
                return "Echelon";
            case C_QUANTITY:
                return "Quantity";
            case D_TASK_FORCE_INDICATOR:
                return "Task Force Indicator";
            case E_FRAME_SHAPE_MODIFIER:
                return "Frame Shape Modifier";
            case F_REINFORCED_REDUCED:
                return "Reinforce Reduced";
            case G_STAFF_COMMENTS:
                return "Staff Comments";
            case H_ADDITIONAL_INFO_1:
                return "Additional Info 1";
            case H1_ADDITIONAL_INFO_2:
                return "Additional Info 2";
            case H2_ADDITIONAL_INFO_3:
                return "Additional Info 3";
            case J_EVALUATION_RATING:
                return "Evaluation Rating";
            case K_COMBAT_EFFECTIVENESS:
                return "Combat Effectiveness";
            case L_SIGNATURE_EQUIP:
                return "Signature Equipment";
            case M_HIGHER_FORMATION:
                return "Higher Formation";
            case N_HOSTILE:
                return "Hostile";
            case P_IFF_SIF:
                return "IFF SIF";
            case Q_DIRECTION_OF_MOVEMENT:
                return "Direction of Movement";
            case R_MOBILITY_INDICATOR:
                return "Mobility Indicator";
            case R2_SIGNIT_MOBILITY_INDICATOR:
                return "Signals Intelligence Mobility Indicator";
            case S_HQ_STAFF_OR_OFFSET_INDICATOR:
                return "HQ Staff / Offset Indicator";
            case T_UNIQUE_DESIGNATION_1:
                return "Unique Designation 1";
            case T1_UNIQUE_DESIGNATION_2:
                return "Unique Designation 2";
            case V_EQUIP_TYPE:
                return "Equipment Type";
            case W_DTG_1:
                return "Date Time Group 1";
            case W1_DTG_2:
                return "Date Time Group 2";
            case X_ALTITUDE_DEPTH:
                return "Altitude Depth";
            case Y_LOCATION:
                return "Location";
            case Z_SPEED:
                return "Speed";
            case AA_SPECIAL_C2_HQ:
                return "Special C2 HQ";
            case AB_FEINT_DUMMY_INDICATOR:
                return "Feint Dummy Indicator";
            case AC_INSTALLATION:
                return "Installation";
            case AD_PLATFORM_TYPE:
                return "Platform Type";
            case AE_EQUIPMENT_TEARDOWN_TIME:
                return "Equipment Teardown Time";
            case AF_COMMON_IDENTIFIER:
                return "Common Identifier";
            case AG_AUX_EQUIP_INDICATOR:
                return "Auxiliary Equipment Indicator";
            case AH_AREA_OF_UNCERTAINTY:
                return "Area of Uncertainty";
            case AI_DEAD_RECKONING_TRAILER:
                return "Dead Reckoning Trailer";
            case AJ_SPEED_LEADER:
                return "Speed Leader";
            case AK_PAIRING_LINE:
                return "Pairing Line";
            case AL_OPERATIONAL_CONDITION:
                return "Operational Condition";
            case AO_ENGAGEMENT_BAR:
                return "Engagement Bar";//*/
            case SCC_SONAR_CLASSIFICATION_CONFIDENCE:
                return "Sonar Classification Confidence";
            case AM_DISTANCE :
                return "Distance";
            case AN_AZIMUTH :
                return "Azimuth";
            case LENGTH :
                return "Length";
            case WIDTH :
                return "Width";
            case RADIUS :
                return "Radius";
            case ANGLE :
                return "Angle";
            default:
                return "";

        }
    }
}
