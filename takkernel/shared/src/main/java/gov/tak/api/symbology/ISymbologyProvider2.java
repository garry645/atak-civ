package gov.tak.api.symbology;


import gov.tak.api.annotation.NonNull;
import gov.tak.api.annotation.Nullable;

public interface ISymbologyProvider2 extends ISymbologyProvider {

    int MASK_HEADQUARTERS = 0x1;
    int MASK_DUMMY_FEINT = 0x2;
    int MASK_TASKFORCE = 0x4;

    @NonNull Status getStatus(String symbolCode);
    String setStatus(String symbolCode, @NonNull Status status);
    @Nullable Amplifier getAmplifier(String symbolCode);
    String setAmplifier(String symbolCode, @Nullable Amplifier amplifier);

    int getHeadquartersTaskForceDummyMask(String symbolCode);
    String setHeadquartersTaskForceDummyMask(String symbolCode, int mask);
}
