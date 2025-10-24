package gov.tak.platform.symbology.milstd2525;

import armyc2.c2sd.renderer.utilities.MilStdAttributes;

interface MilStd2525cAttributes
{
    String LineColor = "LineColor";
    String LineWidth = "lineThickness"; // from https://github.com/missioncommand/mil-sym-java/blob/334bef609fa4a92954fda215714843b2727ec31a/renderer/mil-sym-renderer/src/main/java/sec/web/renderer/MultiPointHandler.java#L81C51-L81C64
    String PixelSize = "PixelSize";
    String DrawAsIcon = "DrawAsIcon";
    String KeepUnitRatio = "KeepUnitRatio";
    String FillColor = "FillColor";
    String TextColor = "TextColor";
}
