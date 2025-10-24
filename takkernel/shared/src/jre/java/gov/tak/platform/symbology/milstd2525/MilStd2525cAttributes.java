package gov.tak.platform.symbology.milstd2525;

import ArmyC2.C2SD.Utilities.MilStdAttributes;

interface MilStd2525cAttributes
{
    String LineColor = "lineColor";
    String LineWidth = "lineThickness"; // from https://github.com/missioncommand/mil-sym-java/blob/334bef609fa4a92954fda215714843b2727ec31a/renderer/mil-sym-renderer/src/main/java/sec/web/renderer/MultiPointHandler.java#L81C51-L81C64
    String PixelSize = MilStdAttributes.PixelSize;
    String DrawAsIcon = MilStdAttributes.DrawAsIcon;
    String KeepUnitRatio = MilStdAttributes.KeepUnitRatio;
    String FillColor = MilStdAttributes.FillColor;
    String TextColor = MilStdAttributes.TextColor;
}
