
#import "@preview/polylux:0.4.0": *
#import "formatting-atak.typ": *

#show: userguide.with(
    version: "5.5",
)


#tak-slide[
  = 1 Overview
#toolbox.side-by-side(columns:(8fr,6fr))[
The Team Awareness Kit for Android (ATAK) is a Government-off-the-Shelf (GOTS) software application and mapping framework for mobile devices. ATAK has been designed and developed to run on Android smart devices used in a tactical environment. The ATAK software application is an extensible moving map display that integrates imagery, map and overlay information to provide enhanced collaboration and Situational Awareness (SA) over a tactical meshed network. ATAK promotes information flow and communications from the tactical environment to command enterprise locations. The first time ATAK is opened, or after a Clear Content, a passphrase is auto generated to activate data encryption. The user can supply their own passphrase by using Settings > Callsign and Device Preferences > Encryption Password > Change Encryption Passphrase.
][
  #set align(right)
  #image("01_atak_overview.jpg", width: 90%)
]
Following this step, the End User License Agreement (EULA) must be accepted. Next, a prompt will appear to allow ATAK to have access to several areas of the device such as its location, pictures, videos, SMS, etc. A prompt will appear with a TAK Device Setup screen to allow further configuration of ATAK. Changes and imports made here can always be updated later. Finally, the Self-Marker can be placed manually on the map if GPS location is not enabled. Do this by following the instructions located in the Self-Marker widget in the lower right corner of the map.

The toolbar sits at the top of the map display.  The features whose icons reside on the toolbar are discussed in individual sections of this guide. The three bars to the left of the toolbar (or to the right when using the legacy toolbar option) provide access to all ATAK tools and plug-ins.  Long -pressing the map hides the toolbar.

Select the *Magnifier* icons to zoom in or out on the map. The map can also be zoomed by using two fingers on the screen to pinch and spread the map. The Map Scale displays a 1 inch to X mi/km reference on the map. The scale adjusts with the map when zoomed in and out. Alerts and notifications are displayed in the lower left of the map interface. Hint windows are available to alert users to changes or make suggestions about the use of tools the first time they are opened.   
]

#tak-slide[
== Compass Interactions

The Compass appears in the upper left and is used to control map orientation. It has two primary modes: North Up/Track Up (default) and Manual Map Rotation/Lock. While in North Up/Track Up Mode, single press on the *Compass* icon to cycle between the North Up and Track Up map orientation. Long press the *Compass* to call out the additional controls menu where the Manual Rotation/Lock and 3D features are available. Select the *Rotation* icon to enter Manual Map Rotation/Lock Mode. When in Manual Map Rotation/Lock Mode, rotate the map orientation by pressing on the map with two fingers and pivoting them in the desired direction. Single press on the *Compass* to lock the screen orientation, signified by the appearance of the lock icon, and again to unlock the orientation for further adjustment. 3D controls are discussed in a separate section.
#toolbox.side-by-side(columns:(.75fr, 10fr, 1.5fr))[
  #image("01_north_up.svg", width: 90%)
][
  There are several options for interacting with the *Compass* icon. Tapping on the compass icon cycles between North Up and Track Up for map viewing. Selecting North Up keeps the map locked with North always at the top of the screen.

  Track Up allows the map to rotate based on the bearing of the device itself, keeping the Self-Marker facing north. 
][
  #set align(right)
  #image("01_track_up.jpg", width: 79%)
]
Long-pressing the compass opens the additional controls’ menus for Manual Orientation and 3D Modes. Select the Android back button to center the screen on the Self-Marker. Selecting the *Crosshair* icon will pan to the Self-Marker and lock the center of the screen to the Self-Marker’s position. 

*Manual Orientation*
#toolbox.side-by-side(columns:(1.5fr, 10fr, 1.5fr))[
  #image("01_manual_orientation_3D_flyout.jpg", width: 90%)
][
 To place the map into manual orientation, long-press on the *Compass* and select the *Manual Orientation* control option. 
 Once in the manual orientation mode, touch the screen with two fingers and simultaneously rotate both fingers and the map will rotate accordingly. To lock the map in its current rotation, long-press the *Compass* and tap the *Manual Orientation* control icon again.
 ][
  #set align(right)
  #image("01_manual_orientation_locked.jpg", width: 90%)
 ]
]

#tak-slide[
  == Compass Interactions (continued)

*3D View*
#toolbox.side-by-side(columns:(1.5fr, 10fr, 1.5fr))[
#image("01_north_arrow_flyout.jpg", width: 90%)
][
ATAK features 3D viewing of terrain and map items (Elevation Data such as DTED, Shuttle Radar Topography Mission (SRTM), or Quantized Mesh are required to be installed). To enable 3D view, long press on the *North Arrow* to call out the additional controls menu and select *3D*.   
][
#set align(right)
#image("01_north_arrow_flyout_3d_locked.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(4fr, 7fr, 4fr))[
   #image("01_3D_render_view.jpg", width: 95%)
   ][
A tilt angle indicator will appear around the edge of the *North Arrow* when 3D view is active. Touch the screen with two fingers and simultaneously swipe up or down on the screen to tilt the view angle. Once the appropriate viewing angle is set, select the *3D Lock* icon to retain this view while panning the map. While viewing the map from an angle, some map items will appear raised above the map surface if they have defined elevations.  ATAK also allows the view to tilt to a 90-degree angle to look at a marker/image from a straight on perspective.
][
  #set align(right)
 #image("01_3d_flat.jpg", width: 95%)
]
*3D Models*

ATAK supports the use of 3D models. OBJ models and other file types from products such as Pix4D can be imported via the Import Manager or can be manually placed in the atak/overlays folder prior to startup. If using Import Manager browse to the .OBJ file and import only that file or browse to a .ZIP file that contains the .OBJ file (and others) and import only that file. If importing via manual placement, place a .ZIP file containing the .OBJ file (and others) into the atak/overlays directory and they will be imported on startup.
]

#tak-slide[
== Compass Interactions - 3D Model (continued)
#toolbox.side-by-side(columns:(1.5fr, 7fr, 2.5fr))[
  #image("01_3D_progress.jpg", width: 90%)
][
Once imported, a 3D Model icon will appear on the map. Zoom into the area of the icon until a loading ring appears. After the loading process has finished, the 3D model will be projected onto the map. Enable the map 3D View and tilt the view angle to see the 3D modeling. 
Loaded 3D models will appear as their own category in Overlay Manager and can be toggled on/off or removed from there.
][
  #set align(right)
  #image("01_tilted_3D_model.jpg", width: 90%)
]
*First Person View*
#toolbox.side-by-side(columns:(.5fr, 6fr, 2fr))[
  #image("01_first_person_icon.svg", width: 90%)
][
 ATAK provides the ability to view the map from a First-Person perspective.  First-Person simulates the view of a user looking straight ahead towards the horizon rather than from the default overhead map view perspective. The First-Person tool can be activated from the Additional Tools and Settings menu.  

 When selected, the First-Person Tool places buttons at the top of the map and two soft joysticks on the right and left of the map.   
][
  #set align(right)
  #image("01_first_person -map_view.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(1fr, 9fr, 3.5fr, 3.5fr))[
  #image("01_first_person_button.svg", width: 90%)
][
 To enable the First-Person View perspective, select the *First-Person View* button and then tap the desired location on the map. 

 Once the map is in First-Person mode, the soft joysticks can be used to control the camera. The right joystick toggles between driving and tilting the camera view.  The left joystick adjusts the elevation of the First-Person view.  Elevation can be adjusted both above ground level and below ground level for a subterranean view.
][
   #set align(right)
#image("01_map_select.jpg", width: 90%)
][
#set align(right)
#image("01_first_person_view_enabled.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(1fr, 10fr))[
  #image("01_disable_first_person.svg", width: 90%)
][
 Select the *First-Person* icon to disable First-Person mode and return to the default map view.   
]
]


#tak-slide[
= 2 Placement
#toolbox.side-by-side(columns:(.5fr, 10fr))[
  #image("02_point_dropper_icon.svg", width: 90%)
][
Locations of interest can be entered using the Point Dropper tool. Select the *Point Dropper* icon to place internationally standardized markers and other icons on the map, edit the data and share the markers with other network members. 
]
== Self Marker
#toolbox.side-by-side(columns:(1.5fr, 9fr, 2.5fr))[
#image("02_self_marker_icon.svg", width: 90%)  
][
By default, the Self-Marker is displayed as a blue arrowhead with a white outline at the user’s current location. The appearance of the Self-Marker can be customized by navigating to Additional Tools and Plugins > Settings > Display Preferences > My Location Color/Size.  Custom options for the Self-Marker include marker size, main color and outline color.  To apply custom colors, select the *Custom* option and use the sliders under *Change/Create Icon Main Custom Color* and *Change/Create Icon Outline Custom Color* to create the desired color/outline.    
][
  #set align(right)
  #image("02_self_marker_radial.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(9fr, 1.5fr, 1.5fr))[
 Long-press *Fine Adjust* to open the sub-radial and enter Coordinate or MGRS Location.

\
\
 Long-press *Place Marker* to open the sub-radial to place a CoT marker. 
][
  #set align(right)
  #image("02_fine_adjust_pull_out_menu.jpg", width: 90%)
][
  #set align(right)
  #image("02_place_marker_pull_out_menu.jpg", width: 90%)
]
]

#tak-slide[
== Self Marker (continued)
#toolbox.side-by-side(columns:(9fr, 3fr))[
Other TAK users appear on the display as a colored circle. The color of the circle represents the user’s Team affiliation, with additional lettering inside the circle to identify the role of the user on the team.  Team Member markers that include a diagonal line indicate that the GPS location is not available. A solid marker indicates that the user has GPS reception.

Available roles include: Team Member, Team Lead (designated by a TL in the center of the marker), Headquarters (HQ in center), Sniper (S), Medic (+), Forward Observer (FO), RTO (R) or K9 (K9). 

\
][
  #set align(right)
  #image("02_team_member_radial_menu.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(3fr, 9fr, 2.5fr))[
  #image("02_polar_coordinate_entry.jpg", width: 90%)
][
The Polar Coordinate Entry option allows a marker to be placed relative to the current marker. The Polar Coordinate option also allows for a range and bearing line to be placed in addition to the marker (point). 

Long-press or tap *Contact Card* to open the Communications sub-radial. Chat and Send Data Package are always available to communicate with the user. If configured in Settings; Email, SMS Messaging, TAK Chat (XMPP), VOIP and Cell Phone communication options will also appear on the sub-radial. 
][
  #set align(right)
  #image("02_team_member_contact_card_pull_out_menu.jpg", width: 90%)
]
]

#tak-slide[
== Point Dropper
#toolbox.side-by-side(columns:(2fr, 10fr))[
  #image("02_placement_markers.jpg", width: 90%)
][
Select the *Point Dropper* icon to open the Point Dropper menu, which contains several iconsets, recently added points and an Iconset Manager. The basic Markers symbology affiliations are: Unknown, Neutral, Red and Friendly. Select a marker from the pallet, then tap a location on the map to drop the marker. 
]
#toolbox.side-by-side(columns:(10fr, 2.5fr))[
 To apply a particular subtype to a marker, tap on the subtype field to reveal the list of available subtypes.  Make a selection from the list or use the Search option to search for a particular subtype.

 To place a marker using manually entered coordinates, choose the marker type and long-press a location on the map. A window will open allowing for the entry of the desired coordinates (MGRS, Lat./Long, etc.).  
 ][
 #set align(right)
  #image("02_placement_subtypes.jpg", width: 90%)
]
To change the standard naming convention when placing markers, enter values into the custom prefix and index fields. If values are entered, the next marker will be dropped with the prefix name and starting number(s) or letter(s) and every subsequent marker will be assigned the next consecutive number(s) or letter(s). 

\
#toolbox.side-by-side(columns:(2.5fr, 9fr, 2.5fr, 3fr))[
  #image("02_placement_mission.jpg", width: 90%)
][
 Select the mission specific iconset to open marker options including Contact Point (CP), Initial Point (IP), BP/HA, Waypoint, Sensor or Observation Point (OP).  

 Swipe in the iconset area or select the *Iconset Name* field, bringing up the iconset drop-down, to move between iconset pallets.

 The Mission iconset allows the user to place mission-specific markers. For instance, the Sensor marker can be placed with a visible Field of View (FOV) direction and length.  The FOV can be adjusted from the Sensor radial.  
][
  #set align(right)
  #image("02_placement_pallets.jpg")
][
  #set align(right)
 #image("02_sensor_fov.jpg", width: 90%)
]
]

#tak-slide[
  == Point Dropper (continued)
#toolbox.side-by-side(columns:(2fr, 10fr, 2.5fr))[
#image("02_placement_vehicles.jpg", width: 90%) 
][
The Vehicle Models iconset places to-scale 3D models of the selected icon. If the Edit option is selected, modifications to the model can be made in the same manner as the Rubber Sheet feature. 

See the Rubber Sheet section for more details. The Free Rotate / 3D View allows the user to quickly access the map rotation and 3D view of the Vehicle Model or other objects on the map. 
][
  #set align(right)
  #image("02_vehicle_model_radial.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(2fr, 10fr))[
  #image("02_recently_added_points_options.jpg", width: 90%)
][
The last point placed is shown at the bottom of the Point Dropper window. The information for all recently placed points can be accessed by selecting the *Clock* icon. This displays the marker icon, name, coordinates, elevation, and range & bearing information. The user can send, rename or remove any recently added markers by selecting the *Arrows* next to the marker to reveal *SEND*, *RENAME* or *DEL* buttons.  
]
#toolbox.side-by-side(columns:(2fr, 6fr, 3fr, 3fr))[
  #image("02_iconset_manager.jpg", width: 90%)
 
][
 Select the *Iconset Manager* (gear) button to add or delete iconsets or to set the default CoT Mapping. 
][
  #set align(right)
  #image("02_placement_add_iconset.jpg", width: 90%)
][
#set align(right)
#image("02_placement_select_default_markers.jpg", width: 90%)
]
]

#tak-slide[
== Radial Menus
#toolbox.side-by-side(columns:(2fr, 2fr, 2fr, 2fr, 2fr))[
#image("02_unknown_object_marker_radial_menu.jpg", width: 90%)
][
  #set align(right)
#image("02_neutral_object_marker_radial_menu.jpg", width: 90%)
][
  #set align(right)
  #image("02_red_object_marker_radial_menu.jpg", width: 90%)
][
  #set align(right)
  #image("02_friendly_object_marker_radial_menu.jpg", width: 90%)
][
  #set align(right)
  #image("02_spot_marker_radial_menu.jpg", width: 80%)
]
#toolbox.side-by-side(columns:(1fr, 1fr, 1fr, 1fr, 6fr))[
  #image("02_fine_adjust_pull_out_radial_menu.jpg", width: 80%)
][
  #image("02_compass_overlay_pull_out_radial_menu.jpg", width: 80%)
][
  #image("02_details_pull_out_radial_menu.jpg", width: 80%)
][
#image("02_track_breadcrumbs_pull_out_radial_menu.jpg", width:80%)
][
Long-press *Fine Adjust* to open the sub-radial which allows the ability to enter coordinates or MGRS location.  Select *Details* on a marker radial to make desired modifications, including Coordinate, Elevation, Name, Type, Remarks and Status. Selecting Marker Type opens a dialog box with extra categories.  
]
#toolbox.side-by-side(columns:(10fr, 3.5fr))[
Selecting Marker Type opens a dialog box with additional categories to choose. Checking *Show Modifiers* under Status, provides text fields to record additional information about the marker, including the direction of movement, speed, location, etc. Setting a status also displays a status indicator underneath the marker icon when viewing it from the map. 

All markers, except for Mission > BP/HA, Spot Map, and Vehicle Models, can place a FOV on the map by setting the sensor switch to *Enabled*. Selecting *Edit* will bring up a Sensor pane to change FOV range, direction, field of view, sensor endpoint, and configure a video source.
][
#set align(right)
#image("02_cot_marker_status.jpg", width: 90%)
]
 File attachments, including images, can be associated with the object by selecting the *Attachments* (paperclip) icon. Once all the desired modifications have been made, the Marker can be sent to other network members using *Send*. The information can be broadcast to all members or sent to specific recipients. Select the *Auto Send* option to broadcast the marker to other TAK users on the network, with updates automatically sent about once every 60 seconds.
]

#tak-slide[
== Radial Menus (continued)

The following table presents the radial menu items and corresponding descriptions. An *X* indicates if the radial selection appears for each specific marker type. 
#set align(center)
#image("02_placement_icon_table.jpg", width: 60%)
]

#tak-slide[
= 3 Range Tools
#toolbox.side-by-side(columns:(.75fr, 9fr, 2.5fr))[
 #image("03_range_tools_icon.svg", width: 90%) 
][
Range Tools provides several types of measuring tools for measuring direction and distance. Select the *Range Tools* icon from the Additional Tools & Plugins window to open the Range Tools toolbar. The toolbar includes an R&B circle, dynamic measure line, static measure line and a bullseye.  
][
  #set align(right)
  #image("03_range_toolbar.svg", width: 90%)
]
== Range and Bearing Line
#toolbox.side-by-side(columns:(.75fr, 10fr, 2.5fr))[
#image("03_measure_icon.svg", width: 90%)
][
The R&B Line tool calculates the distance between two locations on a map, the distance between an object on the map and another point on the map, or the distance between a point on the map and the Self-Marker. Select the *R&B Line* icon on the toolbar to toggle on (yellow) or off (white). When yellow, tap a point to measure from or long press to measure from the Self-Marker to that point. Once the first point or object is selected, tap another point or object from which to measure.  Once endpoints are set, this line is stationary.
][
#set align(right)
#image("03_range_and_bearing_line_radial_menu.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(.75fr, 10fr, 2.5fr))[
#image("03_dynamic_measure_icon.svg", width: 90%)
][
Select the *Dynamic R&B Line* icon to begin creating a line that can be moved and repositioned by the user. Tap once to place an endpoint, then again for the second endpoint. This R&B Line remains unlocked. Touch and drag an endpoint to move it. When the desired location is established, select the center of the line and use the *Pin* button on the radial menu to pin the position of the bearing line. This pinned R&B Line will remain after the Dynamic R&B Line is toggled off.  
][
#set align(right)
#image("03_dynamic_r&b_line_radial_menu.jpg", width: 90%)
]
]

#tak-slide[
== Range and Bearing Line (continued)
#toolbox.side-by-side(columns:(2fr, 9fr))[
#image("03_r&b_line.jpg", width:90%)
][
The pinned R&B Line will show the azimuth, distance and depression or elevation degree between the two points. To reposition an anchor point, long press on either end of the bearing line, then tap another location. The line will be moved to the new location with an adjusted distance and azimuth.  
]
#toolbox.side-by-side(columns:(3fr, 6fr, 3fr))[
#image("03_dynamic_end_point_radial_menu.jpg", width: 90%)
][
Select either end of the R&B line to display the R&B Line end point radial. 

To make fine adjustments to either end of the line, tap the *Fine Adjust* icon on the radial. Crosshairs appear and the area is magnified. Drag a finger inside the magnified area to finely position the end of the bearing line, then select *Confirm* to accept the new position or *Cancel* to discard it. To delete the bearing line, select the *Delete* icon on the radial and the line will be deleted. 
][
#set align(right)
#image("03_r&b_fine_adjust.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(3fr, 11fr, 2.5fr,))[
#image("03_range_and_bearing_line_radial_menu.jpg", width: 90%)
][
To obtain even further options for the bearing line, tap along the line and the R&B Line radial will display. See Bloodhound (Multiple Bloodhounds) section for more details about the Bloodhound radial option.

Select the *Angle Bearing Units* radial to display additional bearing unit options, including Degrees Grid (measured in relation to the fixed grid lines of the map projection), Mils Grid, Degrees Magnetic, Mils Magnetic, Degrees True or Mils True.
][
#set align(right)
#image("03_angle_bearing_units pull_out_menu.jpg", width: 90%)
]
]

#tak-slide[
== Range and Bearing Line (continued)  
#toolbox.side-by-side(columns:(2fr, 8fr, 4fr))[
#image("03_distance_units_pull_out_menu.jpg", width: 90%)
][
Select the *Distance Units* radial to display additional distance unit options, including Nautical Miles, Meters/Kilometers or Feet/Miles. If the length of the R&B line is longer than approximately 30km, the line will then follow the contour of the ground versus being a straight line from one endpoint to the other.
][
#set align(right)
#image("03_r&b_follow_contour.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(2fr, 8fr, 4fr))[
#image("03_r&b_settings.jpg", width: 90%)
][
To make changes to the parameters of the line, select the *Details* icon on the radial. 

The Elevation Profile option at the bottom of the Details page displays the elevation profile for the selected R&B Line at the bottom of the map when selected. The Elevation Profile provides information on Max Altitude, Min Altitude, Gain and Loss. By checking the *Show Viewshed* checkbox, a viewshed will be shown along the line.  Slide the blue dot left or right to advance the viewshed along the line. The R&B Line can be sent or broadcast to other users from the Details menu. 
][
#set align(right)
#image("03_r&b_line_elevation.jpg", width: 90%)
]
Range & Bearing Tool settings can be customized in Settings > Display Preferences > Basic Display Settings > Unit Display Format Preferences.
]

#tak-slide[
 == Range and Bearing Circle Tool
#toolbox.side-by-side(columns:(.75fr,9fr, 2.5fr, 2fr))[
#image("03_r&b_circle_icon.svg", width: 90%)  
][
The R&B Circle Tool uses one or more range rings to mark a point on the map. Select the *Circle* icon on the toolbar to toggle the Circle Tool on (yellow) and off (white). When yellow, select the desired location on the map for the center of the circle. If the Self-Marker or a marker is selected for either the center or the radius of the circle, the circle’s center or radius will change when the markers are repositioned.

To make further adjustments to circle parameters, tap the center of the circle to display the circle’s radial menu. 

Select the *Details* icon on the radial to modify the circle center location, the radius, the unit of measurement, the number of rings and remarks. After making adjustments, the circles will be redrawn as specified. The R&B Circle can be sent or broadcast to other users.
][
#set align(right)
#image("03_r&b_circle_radial_menu.jpg", width: 90%)
][
#set align(right)
#image("03_r&b_circle.jpg", width: 90%)
]
== Bullseye Tool
#toolbox.side-by-side(columns:(.75fr, 6fr, 3fr, 2.5fr, 2fr))[
#image("03_bullseye_icon.svg", width: 90%)
][
The Bullseye tool is an additional Range & Bearing option that gives more information than the standard R&B Line or R&B circle. The Bullseye provides a circular grid with lines every 30 degrees. The angles can be changed to be either toward the center point (Red) or away from the center point (Green). 

Range rings can also be added. The Bullseye can be sent or broadcast to other users by selecting *Send* from the Details window.
][
  #set align(right)
#image("03_green_or_red_bullseye.jpg", width: 90%)
][
#set align(right)
#image("03_bullseye_radial_menu.jpg", width: 90%)  
][
#set align(right)
#image("03_bullseye_details.jpg", width:93%) 
]
]

#tak-slide[
= 4 Route Planning and Navigation
#toolbox.side-by-side(columns:(.75fr, 9fr, 2.5fr))[
#image("04_routes_icon.svg", width: 90%)
][
Route planning and navigation capabilities allow the user to create or modify routes and set navigation objectives.

The Routes tool allows users to quickly create, view and modify routes. Select the *Routes* icon from the Additional Tools and Plug-ins window to see a list of existing routes, create new routes, import routes, sort, export, delete routes and search for routes. For an individual route, the user can view the details, send, navigate to, edit, reverse or delete the selected route.  
][
#set align(right)
#image("04_routes_menu.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(2fr, 6fr, 2.5fr, 2.5fr))[
#image("04_opacity.jpg", width: 90%)
][
Route line thickness, line style and opacity can be changed as well.  To change route line thickness, select the *Line Thickness* button to the right of the *Color* button and use the slider to adjust to the desired line thickness. 
][
#set align(right)
#image("04_line_thickness.jpg", width: 90%)
][
#set align(right)
#image("04_line_style.jpg", width: 90%)
]
To change the route line style, select the *Line Style* button to the right of the *Line Thickness* button and choose one of the available line style options. Route color opacity can be adjusted by selecting the *Color* button and then adjusting the opacity slider in the Choose Route Color window.  If the *Show All* box in the lower right is unchecked, only routes that are visible in the current map screen will be listed.
#toolbox.side-by-side(columns:(2fr, 8fr, 2fr))[
#image("04_routes_import_choices.jpg", width: 90%)
][
Select the *Import* icon to import a route in one of two ways: from a file or from a line on the map. Choose *File Select* to navigate to the location of the saved routes (in KML or GPX format) and select the desired route. The saved route will be imported and displayed on the map and will be listed in the Overlay Manager under the Navigation category. Tap *Map Select* to choose a line present on the map and have it converted into a route. Examples of lines that can be used include a drawing tool polyline, a drawing tool telestration line or a line within a KML overlay.
][
#set align(right)
#image("04_routes_import.jpg", width: 90%)
]
]


#tak-slide[
== Routes (continued)
#toolbox.side-by-side(columns:(2.5fr, 7fr))[
#image("04_create_route.jpg", width: 90%)
][
To create a new route, tap *+*, select the route type, and follow the onscreen instructions.  Select a location on the map to make it part of the route or long press to create check points along the route. Select *Draw* to manually create a route segment by using a finger drag motion on the map.  
]
#toolbox.side-by-side(columns:(2fr, 9fr, 2.5fr))[
#image("04_route_details.jpg", width: 90%)
][
Once the *End* button is selected, the Route Details window opens. Within the Details window, choose to: navigate to the route by selecting the *GO* button, change the color and opacity of the route, change the line width of the route, change the route details and modify the check points. Select *Undo* to undo any modifications to the check points. 

The Route Details may be changed by selecting the *Primary, Infil* button,  next to the *Line Style* button. Change the method of movement (driving, walking, etc.); Infil or Exfil; Primary or Secondary, and Ascending or Descending check points.
][
#set align(right)
#image("04_route_settings.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(2fr, 9fr, 2.5fr))[
#image("04_route_cues.jpg", width: 90%)  
][
Check points can be modified as follows: Rename, Add Cues, Align (only available when using a route alignment plug-in) or Delete. To rename the check point, select the current check point and a Rename window will appear. After changing the name, select *Done*. To add a cue to a check point, select the box to the right of the distance column. The Cues window will appear, and a preset cue may be selected, or a custom cue may be entered. When the desired cue is added, select *OK*.
][
#set align(right)
#image("04_route_edit_cp.jpg", width: 90%)
]

]

#tak-slide[
== Routes (continued)
#toolbox.side-by-side(columns:(9fr, 2fr))[
The bottom of the route details screen provides the option to Send, Export, Edit, view the Elevation Profile, and view/modify Route Preferences. When the Send option is selected, the route may be sent to selected recipients on the network or broadcast to all available recipients. The route can be exported to a file in KML, GPX, KMZ or a Shapefile format. Exported files can be found in the “/atak/export” folder.  
][
#set align(right)  
#image("04_routes_details_options.jpg", width: 90%)
]
\
#toolbox.side-by-side(columns:(8fr, 4fr))[
The Elevation Profile icon allows the ability to view the elevation profile for that route. The Elevation Profile provides information on Total Distance, Maximum Altitude, Minimum Altitude, Gain and Loss. If the *Show Viewshed* checkbox is checked, a viewshed will appear on the route and trace the route, as the Elevation Profile blue dot is moved along the graph.
][
#set align(right)
#image("04_routes_elevation.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(3fr, 3fr, 8fr))[
#image("04_route_radial_menu.jpg", width: 90%)
][
#image("04_route_creation_check_point _radial_menu.jpg", width: 90%)
][
The Route Tool offers two different sets of radial options depending on which portion of the route has been selected. A line segment or a point can be selected. 
]
]

#tak-slide[
== Navigation
#toolbox.side-by-side(columns:(2.5fr, 10fr, 1fr))[
#image("04_routes_navigation.jpg", width: 90%)
][
Route navigation can be initiated by selecting the route’s navigation radial option or by selecting the route’s *Navigation* icon in the Routes listing. Navigation information will appear in the top left corner.  
][
  #set align(right)
  #image("04_routes_cue_on_screen.jpg", width: 90%)
]
If a cue has been established, when the user gets close to a check point, the cue for that check point will be displayed; if the volume is turned up, the cue will be audible. Voice Cues can be turned off by selecting the *Mute* icon. Speed units can be changed during navigation by tapping the top row of the navigation display to cycle between MPH, KMH, KN and mps. The displayed cue can be collapsed to provide more screen space. 

The arrow buttons on either side of the navigation information allow the user to move forward to the next check point or move back to the previous check point. Selecting the *x* will end navigation. Tapping on the distance or speed displayed will toggle the units between miles/mph, km/kmph, nm/kt, and km/mps.

#toolbox.side-by-side(columns:(9fr, 3.5fr))[
When navigating a route while tilted in 3D mode, a 3D billboard feature can be enabled to pop up an image associated with a marker. As the user approaches a marker with an attached image, the image associated with that marker will pop up, once the user is within the defined billboard render distance. The image remains displayed until the user has travelled past the marker.

The default distance for images to appear during navigation can be changed in Settings > Tool Preferences > Specific Tool Preferences > Route Preferences > Billboard Render Distance. The billboard feature can also be turned off by toggling the Settings > Tool Preferences > Specific Tool Preferences > Route Preferences > Show Attachment Billboards.
][
#set align(right)
#image("04_billboard_with_image.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(.75fr, 13fr))[
#image("04_quick_nav.svg", width: 90%)
][
Selecting the *Quick Nav* icon will begin navigation (a bloodhound) to any point, object or route on the map. A pairing line is drawn, and navigation information is displayed in the lower left portion of the screen. Estimated time to the next check point, speed and distance to the next check point are updated as the user moves. See the Bloodhound section in this document for more information.
]
]

#tak-slide[
= 5 Red X Tool
#toolbox.side-by-side(columns:(.75fr, 9fr, 4fr))[
#image("05_red_x_icon.svg", width: 90%)
][
The Red X Tool provides a quick way for discerning coordinate and elevation information for a point on the map. Select the *White X* on the toolbar to toggle the Red X tool into a movable state. 
][
#set align(right)
#image("05_self_paired_to_red_x.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(.75fr, 13fr))[
#image("05_red_x_unlocked.svg", width: 90%)
][
When first selected, the icon will turn yellow denoting that each time the map is tapped, the Red X will move to that location. The widget in the upper right of the screen will show pertinent location information for the location of the Red X.
]
#toolbox.side-by-side(columns:(.75fr, 10fr, 2.5fr))[
#image("05_red_x_locked.svg", width: 90%)
][
When the icon on the toolbar is tapped again, the Red X will lock to its current location, the icon will remain yellow and a lock icon will appear. Long press the *Red X* icon on the toolbar to disable the tool.  

Selecting the *Red X* on the map will open its radial menu. Sub-radial menus are available for additional functionality. Long press the yellow arrow to access. 


][
#set align(right)
#image("05_red_x_radial_menu.jpg", width: 90%)
]

_*Note:*_ Red X is not persistent. When ATAK is closed and then reopened, the Red X will no longer be present. 
]

#tak-slide[
= 6 Bloodhound
#toolbox.side-by-side(columns:(.75fr, 13fr))[
#image("06_bloodhound_icon.svg", width: 90%)
][
The Bloodhound Tool provides support for tracking and intercepting a map item. It allows for the selection of two points on the map, and/or map objects, and displays Range & Bearing information between the chosen tracker and the target.
]
#toolbox.side-by-side(columns:(7fr, 2fr))[
Select the *Bloodhound* icon to open the Bloodhound Tool. A prompt will appear to choose where to start by tapping the *From Reticle* (default = local device’s Self-Marker) and where to bloodhound (track) to by tapping the *To Reticle*. Use Quick Select DP to quickly select a DP to bloodhound to instead of using the To Reticle.


][
#set align(right)
#image("06_bloodhound_setup.jpg", width: 90%)
]
Targets include map objects like other user’s Self-Markers, DPs, CoT Markers, Shape center points, Range & Bearing endpoints, etc. If a map location is selected instead of an object as the target, Bloodhound will place a waypoint marker there. The Self-Marker will then track towards the waypoint. 

Select *OK* and Bloodhound will be activated.
#toolbox.side-by-side(columns:(9fr, 3fr))[
If either point moves, the green widget in the lower left will show the updated information. As the tracking object begins to navigate toward the target, the Estimated Time of Arrival (ETA) will update accordingly. 

The green line showing the direct path from the tracker to the target will flash when a user-defined ETA outer threshold is reached (default = 6 minutes from target). The line will flash as the tracker continues toward the target until the next ETA threshold is reached (default = 3 minutes). The line will turn a flashing yellow until the final ETA threshold (default = 1 minute) is reached. The line then flashes red until the target is reached. 

Colors and thresholds can be modified in Settings > Tool Preferences > Specific Tool Preferences > Bloodhound Preferences.
][
  #set align(right)
  #image("06_bloodhound_paired.jpg", width: 90%)
]
]

#tak-slide[
== Route Mode
#toolbox.side-by-side(columns:(.75fr, 13fr))[
#image("06_route_mode_icon.svg", width: 90%)
][
Select the *Route Mode* icon in the bottom left corner of the screen when the bloodhound tool is active to activate Route mode. 

The *Route Mode* feature requires that a route planner (e.g., the planner that is bundled with the VNS Plug-in) has been installed and configured.   
]
#toolbox.side-by-side(columns:(2.5fr, 8fr, 3fr))[
#image("06_bloodhound_route_config.jpg", width: 90%)
][
Once the route planner has been configured, the Bloodhound’s Range & Bearing line will become a route. This will update periodically (the default is 1 second) to determine if the route from the start map item to the end map item needs to be calculated. This occurs when either the start or the end map item is a pre-configured distance away from the calculated route (the default is 150 meters). This setting can be changed in the Bloodhound preferences.  
][
#set align(right)
#image("06_bloodhound_route_mode.jpg", width: 90%)
]
== Navigation Mode with Route Mode
#toolbox.side-by-side(columns:(.75fr, 8fr, 4.5fr))[
#image("06_open_navigation_icon.svg", width: 90%)
][
When Route Mode is active, the Navigation Interface can be activated by selecting the *Open Navigation Interface* icon. The navigation interface provides voice and visual cues like those that are used when navigating traditional routes. 
][
#set align(right)
#image("06_route_mode_with_nav_interface.jpg", width: 90%)
]
]

#tak-slide[
== Multiple Bloodhounds
#toolbox.side-by-side(columns:(10fr, 3fr))[
To create multiple bloodhounds, open Range Tools, select the *R&B Line* icon, then select any two markers on the map. Once the R&B line is created between the two map items, select the line to open the radial, then select the *Bloodhound* icon from the radial.  The bloodhound information will be displayed on the R&B Line.

If either point moves, the Bloodhound information shown on the R&B Line will update. As the tracking object begins to navigate toward the target, the Estimated Time of Arrival (ETA) will update accordingly. 
][
#set align(right)
#image("06_multiple_bloodhounds.jpg", width: 90%)
]
]

#tak-slide[
= 7 CASEVAC
#toolbox.side-by-side(columns:(.75fr, 13fr))[
#image("07_casevac_icon.svg", width: 90%)
][
The casualty evacuation CASEVAC Tool is used to denote any casualties/injuries in the field. The CASEVAC tool follows Appendix G of the JFIRE 2016 publication and can be used for either CASEVAC or the more restrictive MEDEVAC.
]
#toolbox.side-by-side(columns:(3fr, 10fr, 3.5fr))[
#image("07_casevac_radial_menu.jpg", width: 90%)
][
To drop a CASEVAC marker, select the *CASEVAC* icon in the additional tools menu and select the desired location on the map. Sub-radial menus are available for additional fine adjustment functionality and a quick send feature. Long press the yellow arrows to access them.  

The nine lines of information can be completed in the CASEVAC details window which is opened by selecting the *Details* icon from the CASEVAC radial. The ZMIST (ZAP number, Mechanism of Injury, Injury Sustained, Symptoms and Signs, Treatment Given) report and/or a Helo Landing Zone (HLZ) brief can also be accessed in the Details. Select the *MEDEVAC/CASEVAC Brief* icon to display the brief which can then be copied and used elsewhere. Select the *Attachment* (paperclip) icon to attach files, including images to the marker. Once all applicable information has been added, the CASEVAC may be sent to available contacts by selecting *Send*. 

Multiple ZMIST reports can be associated with one CASEVAC by selecting *ADD* next to the initial ZMIST heading and section. A ZMIST report can be deleted by selecting the *Delete* icon. ZMIST submenus contain hotkeys for common entries and allow for text entries for nonstandard conditions.  
][
#set align(right)
#image("07_casevac_details.jpg", width: 90%)
]
]

#tak-slide[
= 8 Maps and Favorites
#toolbox.side-by-side(columns:(.75fr, 9fr, 2fr, 2fr))[
  #image("08_map_manager_icon.svg", width: 90%)
][
Select the *Maps & Favorites* icon to list the imagery loaded in the application. 

The following categories are shown: Imagery, Mobile and Favorites (FAVS). Select *Online/Local* on the Mobile tab to toggle between using an online map source or locally stored map layers over a desired area. Tap on the FAVS tab to review a previously saved view or to add the current view.
][
#set align(right)
#image("08_maps_layers_mil.jpg", width: 90%)
][
#set align(right)
#image("08_maps_select_area_mil.jpg", width: 90%)
]
== Saving a Map Layer
#toolbox.side-by-side(columns:(8fr, 3fr))[
To save a local copy of a map layer, choose the *Mobile* tab and toggle to *Online*.  Select the right arrow to expand the Map Source option, then tap *Select Area* to define a region of interest. A prompt will appear presenting four options for map area selection: Rectangle, Free Form, Lasso or Map Select. The Rectangle option uses the top left and lower right corners to denote the area to be downloaded. The Free Form option provides the ability to create a custom area to be downloaded by tapping different points on the map until the shape is complete or the end button is selected. Lasso provides the ability to draw a lasso around a specific area on the map to be downloaded.
][
#set align(right)
#image("08_download_lasso_tiles.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(2fr, 8fr, 2fr))[
#image("08_maps_save_layer.jpg", width: 90%)
][
Map Select allows for an already existing shape or route to be selected as the area intended for download. If a route is chosen, a prompt will be provided to enter a Route Download Range. This is the distance from the route that will be downloaded. 

Drag the map source slider end points to select the resolution for the tileset being downloaded. The number of tiles to be downloaded will be indicated, with the current download limit set at 850,000 tiles. Select the *Download* button to begin the download process. If the selected tiles are close to the threshold, a warning message will appear before the download begins.
][
#set align(right)
#image("08_map_layer_overlay_option.jpg", width: 90%)
]
]

#tak-slide[
== Saving a Map Layer (continued)
#toolbox.side-by-side(columns:(8fr, 3fr))[
A new tileset can be created, the tiles being downloaded can be added to an existing one, or the download can be created as an Overlay file to layer on top of an existing map layer. Enter the name to be applied to the selected layers and select *OK*. A status indicator will appear to show the download progress. A download-in-process can be canceled by selecting the *Cancel* button. Online and Local map layers can be toggled as needed. When *Local* is selected, a listing of the downloaded imagery layers in the current map interface appears. The local layers are listed in order beginning with the area closest to the map center. Select the *Outline* checkbox to toggle the outline layers on or off. When the user selects a layer from the list, map source data corresponding to that downloaded layer will be used as the source for map data.
][
#set align(right)
#image("08_download_route_tiles.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(2fr, 9fr, 2.5fr))[
#image("08_maps_tileset_saved.jpg", width: 90%)
][
Imagery can be imported via the Import Manager tool or placed directly into the atak folder structure for use by the application. For instructions on which subfolder a specific file type should be placed, refer to the information found in Settings > Support >ATAK Documents > ATAK Dataset Instructions. All imported imagery will be listed under the *Imagery* tab and function the same as the *Mobile* tab.

Imported imagery and downloaded map sets can be shared with other users or deleted from the *Imagery* tab. Selecting the *Imagery* tab displays the types of imagery available. Expanding the category lists the individual files.  From here, the user can *Send* the file to other users or delete it.
][
#set align(right)
#image("08_imagery_send_delete_option.jpg", width: 90%)
]
If *Show All* is checked, all available layers are listed. Otherwise, only layers that are visible in the current map view will be listed.  Imagery based map products (e.g., MrSID, GeoTIFF, NITF) that are placed in the atak/imagery folder will appear under the *Imagery* tab when ATAK is restarted. To view the list of supported products, select Settings > Support > ATAK Documents > ATAK Dataset Instructions.
]

#tak-slide[
== Bookmarking a Location
#toolbox.side-by-side(columns:(11fr, 2fr))[
To save the current view and displayed imagery, select the FAVS tab and tap *Add Current View*. A prompt will appear to name the view, which will be saved along with its coordinates and the map source being used. Selecting a view in the FAVS listing will pan the map to that location and transition to the map source used in the saved view. 

Saved Favorite views can be sent to other TAK users by selecting the *Send* option. 
][
#set align(right)
#image("08_maps_favorites.jpg", width:90%)
]
== Web Feature Service (WFS) Support 

WFS imagery is supported and can be ingested in several different ways. Existing WFS XML configuration files can be imported through the Import Manager. The files can also be placed into the atak/WFS directory manually.

Additionally, WFS Imagery can be added by selecting the right arrow at the bottom of the Maps & Favorites Mobile tab to expand the Map Source option, selecting *+* and then entering a WFS Imagery Service URL. After querying the service, a list of available imagery sets will be presented.  Select the required services for import and then select *OK*.
]

#tak-slide[
= 9 Overlay Manager
#toolbox.side-by-side(columns:(.75fr, 13fr))[
#image("09_overlay_icon.svg", width: 90%)
][
Overlay Manager sorts map objects, files and overlays into categories and subcategories. Select the *Overlay Manager* icon to bring up the list of categories. These include: Teams by color, Alerts, Markers, Data Packages, Navigation, Shapes, Hashtags, Elevation Manager, Map Controls, as well as other file types. 
]
#toolbox.side-by-side(columns:(2.5fr, 6fr, 3.5fr))[
#image("09_overlay_manager.jpg", width: 90%)  
][
Selecting a category will open a detailed listing of the items available in that category. The available items within each category are annotated on the menu entry, allowing the user to reference sub-menu content.

When a displayed item in a specific category is selected, the map view will pan to that item and its radial will display. If the *Show All* checkbox is checked, all map items in Overlay Manager are listed. If the *Show All* checkbox is unchecked, Overlay Manager filters map items based on the current map view.
][
#set align(right)
#image("09_overlay_markers.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(10fr, 2fr))[
Visibility of any category can be toggled on and off using the *Visibility* (eye) radio buttons.  When the eyeball icon appears white, the corresponding layer objects are visible. An outlined eyeball indicates that the subcategory has some, but not all, objects visible. An eyeball with a line through it corresponds to a layer that is not visible. 

The Markers category offers additional visibility controls for CoT markers allowing for groups of them to be toggled between various display states. 
][
#set align(right)
#image("09_om_visibility.jpg", width: 90%)
]
]

#tak-slide[
== Overlay Manager (continued)

After selecting the Markers category, select *Slicer*, which appears beside the *Multi-Select* button at the top of the window. CoT markers should now be displayed in a grid format. Tapping the *All* button will toggle all CoT markers on the map between the three visibility states; visible, points, and hidden. 
#toolbox.side-by-side(columns:(.75fr, 10fr, 3.5fr))[
#image("09_cot_grid.svg", width: 90%)
][
The left side of the grid breaks up the CoT markers by affiliation, using their first letter to denote the affiliation. Tapping *F* toggles the visibility states of Friendly markers, tapping *N* toggles the visibility states of Neutral markers, etc. The letters visible at the top of the grid denote the tracks that these markers belong to: Space (P), Air (A), Ground (G), Sea (S), and Subsurface (U). 
][
#set align(right)
#image("09_cot_states.jpg", width: 90%)
]
Tapping any letters at the top of the grid will toggle the visibility states of CoT markers that match that track regardless of their affiliation. A specific marker group from a specific affiliation and track can also be toggled by selecting the marker icon at the intersection of the two attributes.
\
#toolbox.side-by-side(columns:(.75fr, 13fr))[
#image("09_cot_list.svg", width: 90%)
][
To return to the Overlay Manager marker sub-categories, select the *List* icon. The objects in a category listing can be sorted alphabetically or by proximity to the Self-Marker. If the *Show All* checkbox is checked, all map items in Overlay Manager are listed. If the *Show All* checkbox is unchecked, Overlay Manager filters map items based on the current map view.
]
 #toolbox.side-by-side(columns:(2fr, 10fr, 3.5fr))[
#image("09_overlay_filters.jpg", width: 90%)
 ][
Additional filters can be applied to the Markers category. Available filters include Auto Send and Self Authored. The Auto Send filter will only show markers that have the Auto Send feature enabled. The Self-Authored filter will show only markers created on the local device and exclude all markers received from other TAK contacts. To apply, select *Filters* to open the filter dialog, select a filter to activate it, then *OK*. To deactivate a filter, select it a second time. 

Available overlays can be searched for by selecting the *Search* icon in the main Overlay Manager toolbar. Select specific categories of overlays to narrow the search results. 
 ][
#set align(right)
#image("09_other_file_overlays.jpg", width: 90%)
 ]
 ]

 #tak-slide[
== Common Operating Picture (COP) Refresh
#toolbox.side-by-side(columns:(2.5fr, 6fr, 2fr))[
#image("09_cop_icon.jpg", width: 90%)
][
The COP Refresh removes any temporary map items from the map. These include Self-Markers from other users, SPI's from other users and ADSB Aircraft Tracks.

Selecting COP Refresh will display a window to confirm the deleted items.
][
#set align(right)
#image("09_cop_delete.jpg", width: 90%)
]
== Hashtags and Sticky Tags
#toolbox.side-by-side(columns:(3fr, 8fr, 2fr))[
#image("09_hashtags_overlay_manager.jpg", width: 90%)
][
Hashtags and Sticky Tags can be added to map items to aid in categorizing and searching for items.

Hashtags can be added to map items in the Remarks field of the Details window or by selecting the *Hashtags* icon in Overlay Manager. Select *+* to open the New Hashtag dialog. Enter a Hashtag and select *Done*. 
][
#set align(right)
#image("09_hashtag_in_remarks.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(2fr, 7fr, 4fr))[
#image("09_new_hashtag_dialog.jpg", width: 90%)
 ][
After the new Hashtag has been created, the *Add to Hashtag* dialog box appears, and the hashtag can be added to items via Map Select, Lasso or through Overlays. Multiple items may be selected. Once all desired items have been chosen, select *Done*. 
 ][
#set align(right)
#image("09_hashtag_map_select.jpg", width: 90%)
 ]
 The Overlay Manager will list all created hashtags along with the tagged items. If one of the tagged items is selected, the map will pan to that item and its radial will display.
 ]

 #tak-slide[
== Hashtags and Sticky Tags (continued)
#toolbox.side-by-side(columns:(1.5fr, 10fr))[
#image("09_hashtag_add_to.jpg", width: 90%)
 ][
Select *Hashtag* to open the Sticky Tags dialog. Enter a name and select *+* to apply that sticky tag to all subsequently placed map items. More than one sticky tag may be added at a time. To discontinue adding a particular sticky tag to subsequent items, uncheck the checkbox beside the sticky tag. 
 ]
 #toolbox.side-by-side(columns:( 11fr, 2.5fr, 1.5fr))[
  Select *Delete* (the trashcan icon) to remove a sticky tag from the list. Select *Done* to confirm any changes made.To remove hashtags from existing map items, open the Hashtags category and tap *Multi-Select*. Choose the *Remove Hashtags* option and then choose the hashtag(s) or map item(s) and confirm the selection by tapping *OK*.
 ][
  #set align(right)
#image("09_new_sticky_tags_dialog.jpg", width: 90%)
  ][
  #set align(right)
  #image("09_overlay_hashtag_removal.jpg", width: 90%)
 ]
 == Multi-Select Export and Delete
#toolbox.side-by-side(columns:(.75fr, 13fr))[
#image("09_overlays_multi_select_icon.svg", width: 90%)
][
Existing overlays can be exported to a file or directly to other users for use in other applications. To export, select the *Multi-Select Action* icon, select *Export*, choose a file format, then select each category of overlays that should be included in the export file.  Select *Previous Exports* to send to other users.
]
#toolbox.side-by-side(columns:(2fr, 9fr, 1.5fr, 2fr))[
#image("09_overlays_multi_select.jpg", width: 90%)
][
After the selections have been made, tap *Export*, enter the desired file name and select *Export* to create the file.  A notification will appear when the file has been exported. Select *Done* to save the file or *Send* to send to a TAK Contact, TAK Server or an application.  
][
#image("09_overlays_export_format_mil.jpg", width: 90%)
][
  #set align(right)
  #image("09_overlays_export.jpg", width:90%)
]
]

 #tak-slide[
== Multi-Select Export and Delete (continued)
#toolbox.side-by-side(columns:(2fr,  10fr))[
#set align(right)
#image("09_overlays_select_protocol.jpg", width: 90%)
][
To delete existing overlay items, tap the *Multi-Select Action* icon, then the *Delete* icon. Items may be selected as a category (Markers, Shapes, File Overlays, etc.) or individually (specific neutral marker, for instance). When all selections have been made, select the *Delete* button to remove the specified items and confirm deletion.
]
== Elevation Manager
#toolbox.side-by-side(columns:(5fr, 8fr, 3fr))[
#image("09_elevation_data.jpg", width: 90%)
][
By default, ATAK will stream in elevation data (DTED, SRTM, etc.) if the device was not previously provisioned with its own data. To disable this default preference, navigate to Settings > Tool Preferences > Specific Tool Preferences > Elevation Overlays Preferences and uncheck *Stream Elevation Data*. 

To view the area of coverage, select *Overlay Manager*, then check the outline checkbox for the DTED layer. The yellow grids indicate where the DTED0 data has been downloaded. 

_*Note:*_ If multiple streaming sources of elevation data are available (i.e., DTED0, SRTM, Quantized Mesh, etc.), the highest resolution source for that area will take precedence when querying elevation data.
][
#set align(right)
#image("09_elevation_data_preferences.jpg", width: 90%)
]
 ]

 #tak-slide[
== Elevation Manager (continued)
#toolbox.side-by-side(columns:(4fr, 9fr))[
#image("09_elevation_manager_layers.jpg", width: 90%)
][
The Elevation Manager provides several sources to choose from when analyzing the 3D terrain of a point of interest on the map. While the items on the map will continue to utilize the elevation source that has been configured, the layer currently set to be displayed will change how the elevation is rendered. Like DTED, TAK Terrain and TAK Bathy will be streamed to the device and cached overtime. 

\
Layer display priority is based on the layer’s position in the list, starting from the top. A layer can be re-ordered by selecting it, highlighted in blue, and then using the arrow buttons located at the bottom of the window. The visibility of each layer can also be toggled off/on by using the visibility radio buttons. 

\
Layers can also be sent to others to initiate the download on their local device, if they don’t already have the layer in their Elevation Manager. Toggling the switch at the bottom of the window to Offline will only allow what has already been cached to be utilized.
]
 ]

 #tak-slide[
== File Editing
#toolbox.side-by-side(columns:(3fr, 8fr, 1.5fr))[
#image("09_file_edit.jpg", width: 90%)
][
The color and thickness of file overlays (Shapefiles, KMZ, DRW, etc.) can be edited by selecting *Edit* (pencil icon) from the listing. 
][
#set align(right)
#image("09_edit_options.jpg", width: 90%)
]
== Map Controls
#toolbox.side-by-side(columns:(8fr, 2fr))[
Different aspects of the map can be controlled by using Map Controls in the Overlay Manager. Options include:
#set list(indent: .5cm)
 
- Displaying shape labels for File Overlays shapes 
- The ability to enhance visualization of depth when looking straight down in 2D mode with the Enhanced Depth Perception option 
- Adjusting the scaling of map imagery (useful for using a different zoom level for imagery with multiple resolution layers) 
- The ability to use a slider to change the Map Imagery Transparency 
- The rendering of stars when zooming out to see the globe, and toggling Sun/Moon Illumination
][
#set align(right)
#image("09_map_controls.jpg", width: 90%)
]
 
== 3D Models - Metadata
#toolbox.side-by-side(columns:(10fr, 1.5fr))[
When ATAK receives a 3D model that has been edited by WinTAK, Overlay Manager displays the metadata associated with the model.  Metadata includes information such as the Callsign of the user who performed the editing, terrain model employed, etc. 
][
#set align(right)
#image("09_3d_model_metadata.jpg", width: 90%)
]
 ]

 #tak-slide[
== Other Overlays
#toolbox.side-by-side(columns:(4fr, 10fr))[
#image("09_overlay_eye_altitude.jpg", width: 90%)
][
Additional overlays are available under the Other Overlays category of Overlay Manager. Select *Overlay Manager*, then *Other Overlays* to access the menu. Toggle the Eye icon to Enable/Disable these overlays individually.

Center Designator can be used to pinpoint a location’s coordinates and elevation data. 
Altitude can be used to estimate the current altitude based on the zoom level over the map surface.
]
\
#toolbox.side-by-side(columns:(10fr, 4fr))[
Grid Lines controls the visibility of MGRS grid lines laid over the map surface. These grid lines will automatically adjust based on the current zoom level.
][
  #set align(right)
#image("09_overlay_grid_lines.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(10fr, 4fr))[
The Heatmap Overlay’s visibility can also be controlled from Other Overlays, it’ll function identically to how it does in Elevation Tools. Lastly, Off-Screen Indicator visibility can also be toggled on/off. Offscreen indicators appear for newly placed/received markers that are no longer in the current map view but are nearby (zoom level dependent).
][
#set align(right)
#image("09_overlay_offscreen.jpg", width:90%)
]
 ]

 #tak-slide[
= 10 Radio Controls
#toolbox.side-by-side(columns:(.75fr, 11fr, 2fr))[
#image("10_radio_controls_icon.svg", width: 90%)
][
Select the *Radio* icon to access controls for the currently supported ATAK radios. 

PRC-152-A is a Harris voice and data radio, this enables point-to-point and displays the coordinates of the 152-A SA Radio. Note the text will state whether the device is supported. Harris Soft KDU is a keypad display unit to access the Harris radio controls. 

ISRV (Rover) is capable of video downlink from aircraft to ground through a TacRover and TacRover-E.
][
#set align(right)
#image("10_radio_menu.jpg", width: 90%)
]
== PRC-152 Connection
#toolbox.side-by-side(columns:(8fr, 2fr, 1.5fr))[
Connect to a PRC-152 Harris Radio and select the configure icon on the Radio Controls menu. The Cable Configuration window will display to choose the appropriate cable configuration. 

A description of each cable and the radio’s hardware interface is provided.  Select *OK* to exit the cable configuration selection menu. 
][
#set align(right)
#image("10_radio_cable_config.jpg", width: 90%)
][
#set align(right)
#image("10_radio_select_cable.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(3fr, 9fr))[
#image("10_radio_map.jpg", width: 90%)
][
Slide the *OFF* button to *ON* in the PRC-152 section to begin connecting. 

Once a connection is established, point-to-point protocol communication is available and is indicated on the Radio Controls menu. Other radios on the same network will appear on the map once squelched.
]
 ]

 #tak-slide[
== Rover Controls
#toolbox.side-by-side(columns:(2fr, 9fr))[
#image("10_radio_rover_connected.jpg", width: 90%)
][
Slide the *OFF* button to *ON* in the ISRV (Rover) section to begin connecting through an Ethernet connection. ATAK scans through frequencies to establish an active feed. When the connection is established, the antenna icon will turn from gray to green and will indicate that it is connected.
]
#toolbox.side-by-side(columns:(10fr, 2fr))[
Select the *Rover* menu item to open the Rover Controls menu.  The Status field will indicate the device connection status and the current waveform. The waveform can be changed by accessing the Frequency Scan menu.  The Frequency or Module/Channel (DDL) can be changed from the Rover Controls menu, as well as entering the Settings menu, launching the web configuration page for the Rover device, and watch the video. The preset can be added through this interface as well. 

Setting the Module and Channel will automatically change the waveform to DDL.  The *Recently Used* and *+* buttons have been added to save hand jammed frequencies and DDL, for quicker viewing capability. Tap *+* to add a channel to the recently used list. Video frequencies and DDL information will not automatically be added to the list. 
][
#set align(right)
#image("10_radio_rover_controls.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(9fr, 3fr, 3fr))[
The frequency can be manually changed by tapping the current frequency entry or by opening the Scan Frequency Range Menu by tapping the *Scan* button.  Change the frequency by entering a *Start Freq Code* and *End Freq Code* and selecting *OK*. ATAK will scan all available frequencies within the established range. Select *Next* on the Rover Controls menu to advance to the next available stream in the range. 

Tap the *Waveform* drop-down to open the menu of available waveforms and select the desired waveform from the list.  Additionally, the user may choose between L-Band, S-Band, C-Band Low and C-Band high, Ku-Band Low, Ku-Band Low (2), and Ku-Band High.
][
#set align(right)
#image("10_radio_scan_frequency.jpg", width: 90%)
][
#set align(right)
#image("10_radio_waveform_dropdown.jpg", width: 90%)
]
 ]

#tak-slide[
== Rover Controls (continued)
#toolbox.side-by-side(columns:(.75fr, 8fr, 2fr, 2fr))[
#image("10_rover_settings_icon.svg", width: 90%)
][
Select the *Settings* icon to access the Rover Configuration settings. Options are available to initiate the Web based Config for the Rover device in a web browser, Ping Radio, Initiate Test Video, engage the Raw Video Recorder and advanced options.
][
#set align(right)
#image("10_radio_rover_settings.jpg", width: 90%)
][
#set align(right)
#image("10_radio_rover_web_settings.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(1fr, 9fr))[
#image("10_watch_icon.svg", width: 70%)
][
Select the *Watch* icon to view a video stream.
]
#set align(center)
#image("10_radio_view_video.jpg", width: 25%)
]

#tak-slide[
= 11 Data Package
#toolbox.side-by-side(columns:(.75fr, 10fr, 2.5fr))[
#image("11_mp_dp_icon.svg", width: 90%)
][
Select the *Data Package* icon to open the data package tool.  When opened, the tool will display any data packages that have been stored, as well as provide options for building new packages, downloading them from a server, deleting and exporting them. When preparing for an operation, a team leader may prepare a route, place markers, shapes and imagery on that map that pertains to operation objectives. All of these items can be included in a data package and sent to each person on the team. This allows everyone on the team to have the same operational information. In addition to Map Items (with or without attachments), external files (from the SD card) may be included in a package. The visibility of the package or its elements may be toggled on or off. 
][
#set align(right)
#image("11_dp_tool_civ.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(2fr, 9fr, 5fr))[
#image("11_new_package.jpg", width: 90%)
][
Select the *+* icon in the Data Package Tool to create a new data package. Choose the selection method: Map Select, File Select, Overlays or Lasso to add items to the data package.

The Map Select option allows for one or more items to be selected from the map and included in the data package. The File Select option displays the file browser which can be navigated to the desired files to be included in the data package. 
][
#set align(right)
#image("11_dp_map_select.jpg", width: 90%)
]
The Overlays option opens the Overlay Manager tool where categories and individual items can be selected to be included in the data package.
]

#tak-slide[
== Data Package (continued)
#toolbox.side-by-side(columns:(5fr, 8fr, 3fr))[
#image("11_lasso_map_items.jpg", width: 90%)
][
The Lasso option allows for a lasso to be drawn around items on the map to be included in the data package. External Native Data items (such as the map source) are also included when using Lasso. The items that have been selected with the Lasso can be modified to remove any unnecessary items.  Choose *Done* or *Select* with Lasso when finished, then choose to either create a new Data Package or add the items to an existing Data Package.
][
#set align(right)
#image("11_lasso_selections.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(.75fr, 13fr))[
#image("11_save_dp.svg", width: 90%)
][
When items are added to a data package, a red asterisk will appear on the data package name to indicate it needs to be saved. Select the *Save* icon to save the changes.
 
The information under the package name includes the callsign of the user who created the data package and the number of items in the data package. Select the name of the data package to view the included items. Toggle the visibility radio button to control data package content visibility on the map interface.
]
#toolbox.side-by-side(columns:(10fr, 2fr))[
When done with modifications, select the *Send* icon to open a list of options for sending the data package including TAK Contact, TAK Server or another application. If the package size is larger than the value set in the Preferences, the size shown in the package list will display red and the user will have an option to override the limit when they send. The threshold size may be changed in Settings > Tool Preferences > Specific Tool Preferences > Data Package Control Preferences.
][
#set align(right)
#image("11_send_package.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(10fr, 3fr))[
When sending a data package to a TAK contact, either Select All, Show All or toggle recipients by selecting or de-selecting their corresponding checkboxes. 

When the *Delete* icon is selected, a prompt will appear to remove or leave the contents of the data package on the map interface.  
][
#set align(right)
#image("11_data_package_summary.jpg", width: 90%)
]
]

#tak-slide[
== Data Package (continued)

#image("11_mp_buttons.jpg", width: 20%)
#toolbox.side-by-side(columns:(10fr, 3fr))[
Select the *Download* icon to access an existing data package from a TAK Server.

Select the *Transfer Log* icon on the Data Package Tool menu to view the file transfer log of imported and exported data packages. The Data Package Tool can be customized in Settings > Tool Preferences > Specific Tool Preferences > Data Package Control Preferences.

Select the *Multi-Select Action* icon to export or delete multiple data packages via the Overlay Manager.

Select the *Search* icon to locate the desired data package in the listing.
][
#set align(right)
#image("11_mp_download_tak_server.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(9fr, 2.5fr, 2.5fr))[
An existing data package can be modified by selecting the *+* icon and following the same steps as above. Select the data package listing name and then select the *Edit* icon to change the name or add remarks or hashtags. 
][
#set align(right)
#image("11_dp_edit.jpg", width: 90%)
][
#set align(right)
#image("11_edit_mp_mil.jpg", width: 90%)
]
]

#tak-slide[
= 12 Contacts
#toolbox.side-by-side(columns:(.75fr, 13fr))[
#image("12_contacts_icon.svg", width: 90%)
][
The Contacts list includes a variety of ways in which a user may communicate with other users on the local network or TAK Server. A default communication type (shown in the last column) may be selected and used until another type of communication is selected. 
]
#toolbox.side-by-side(columns:(10fr, 1.5fr))[
There are several ways within the Contact Tool to filter the contact list. To only see contacts that use a specific communication type, select the arrow next to the word “Contacts” at the top of the list window. A window will open showing all the types of communication, such as Data Packages, GeoChat (built-in chat capability), Email, Phone, SMS, VoIP and XMPP. Choose one of these to ensure all contacts without that type available are filtered out of the contacts list. 

_*Note:*_ If the type chosen is not the default for a user, the type can be accessed through their radial.
][
#set align(right)
#image("12_contacts_comm_types.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(2fr, 10fr, 1.5fr))[
#image("12_filter_contacts.jpg", width: 90%)
][
Options located at the top right of the contact’s window, available for all communication types, include Multi-Select, Favorites, Sorting and Searching. The multi-select option provides the ability to filter out contacts.  Any number of contacts can be filtered. Filtered contacts will be removed from the map and can be removed from the Contact’s window, if *Hide Filtered Contacts* is enabled. 

To add a contact to Favorites, select the *Star* icon, select the *+* icon, choose the contact(s) and then select *Add*.
][
#set align(right)
#image("12_favorites.jpg", width: 90%)
]
]

#tak-slide[
== Contacts (continued)
#toolbox.side-by-side(columns:(1fr, 11fr, 1.5fr))[
#image("12_sorting.svg", width: 90%)
][
Select the *Sort* icon to sort contacts alphabetically, by connection status, by unread messages or by distance from the Self-Marker. The sort type will change with each tap of the icon.

Select the *Search* icon to search for a specific contact. As the search criteria is entered, any contacts that meet the criteria will begin to appear. 
][
#set align(right)
#image("12_searching.jpg", width: 90%)  
]
#toolbox.side-by-side(columns:(2fr, 10fr, 2fr))[
#image("12_contact_list.jpg", width: 90%)
][
The Contacts list also has two filters available at the bottom of the screen. The *Unread Only* box, when checked, will display only contacts with whom an unread message is waiting. When unchecked (default), all available contacts are displayed. The *Show All* box, when checked (default), will display all contacts regardless of their location. When unchecked, only contacts that are visible within the current map view will be displayed. 
][
#set align(right)
#image("12_contacts_staling_out.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(7fr, 2fr, 2fr, 2fr))[
Profile cards (accessed by selecting the second to last column) are available for each contact and contain additional information about that contact including: 1) Role, software type and version installed, node type, default connector, last reported time, battery life, 2) Location information and 3) Available types of communication.
][
#set align(right)
#image("12_contacts_profile_pg_1.jpg", width: 80%)
][
#set align(right)
#image("12_contacts_profile_pg_2.jpg", width: 89%)
][
#set align(right)
#image("12_contacts_profile_pg_3.jpg", width: 86%)
]
]

#tak-slide[
== GeoChat Group Management
#toolbox.side-by-side(columns:(.75fr, 10fr, 1.5fr))[
#image("12_contacts_icon.svg", width: 90%)
][
GeoChat Group Management is initiated through Contacts. Select the *Contacts* icon, then select the *Groups* line (not the communications button). The local device can create, edit and delete chat groups, as well as sub-groups. To create a chat group, select the *Add Group* icon, enter a group name, select the contacts to add to the group and select *Create*.    
][
#set align(right)
#image("12_chat_users.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(.75fr, 10fr, 1.5fr))[
#image("12_add_contact_group.svg", width: 90%)  
][
If a parent group is being created, no contacts need to be added at this level. To add a nested group, select the parent group, select the *Add Group* icon, enter a group name for the sub-group, choose the contacts that will be in the group, and select *Create*. Groups may be managed using the add/delete contacts or add/delete GeoChat groups options.
][
#set align(right)
#image("12_contacts_group_config.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(1.5fr, 9fr, 2fr))[
#image("12_contacts_sub_groups.jpg", width: 90%) 
][
To add users to an existing group, select the *Groups* line (not the communications button), then select the name of the group. Select the *Add Users* icon. A window will open allowing the group creator to add users to the selected group. Select the *Add* button when all the users to be added are checked. 
][
#set align(right)
#image("12_contacts_add_delete_groups.svg", width: 90%)
]
]

#tak-slide[
== GeoChat Messaging

If the user specifies the GeoChat (built-in Chat capability) communication from the drop-down at the upper left, group and person-to-person messaging is filtered. In addition to the toolbar options that are available with other communication types: Favorites, Sorting and Searching, the GeoChat communication type also has a History option (*Clock* icon). If the History option is selected, chat history with the selected contact is displayed. 

To view messages from or send messages to an individual, select the desired contact’s *Communication* icon. 
#toolbox.side-by-side(columns:(10fr, 2fr))[
Select *All Chat Rooms* to view all messages from or send messages to all present on the network and TAK Server. Other groups available for viewing or sending messages are: Forward Observer, Groups (user made), HQ, K9, Medic, RTO, Sniper, Team Lead, and Teams. If the user’s current role is Forward Observer, HQ, K9, Medic, RTO, Sniper or Team Lead, that user can view or send messages to all other contacts with the same role. 

If a GeoChat message is sent from the top level of Teams, it will be sent to all contacts, similar to *All Chat Rooms*.
][
#set align(right)
#image("12_chat_contacts_list.jpg", width: 90%)  
]
When a sub-team is chosen, messages can only be sent to the user’s active (my team) team color. When a parent group is chosen, messages are sent to all members of the parent group, as well as all of the sub-groups. When a sub-group is chosen, messages are sent only to members of the sub-group. Individuals or groups listed within GeoChat may be removed from the contacts menu by toggling off their visibility in Overlay Manager.
]

#tak-slide[
== GeoChat Messaging (continued)
#toolbox.side-by-side(columns:(4fr, 9fr, 2.5fr))[
#image("12_chat_keyboard.jpg", width: 90%)  
][
Selecting in the *Free Text Entry* area will bring up an onscreen keyboard.

Selecting *Add Attachment* will display selection options for choosing items to send. The selection options presented are identical to those seen when creating a Data Package. Map items may be selected by using *Map Select* or *Lasso*. 
][
#set align(right)
#image("12_chat_messages_attachments.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(10fr, 1.9fr))[
Select *Overlays* to choose from previously imported files or map items listed in the Overlay Manager. Files on the device’s internal storage or SD card can be selected using *File Select*. After the attachment(s) have been selected, an entry will be generated in the chat indicating that a data package has been sent. Select *More Details* to see a breakdown of the sent attachments.

The items sent can be seen listed in the breakdown. In addition to the name of the item, options exist to *View Attachments* associated with a map item, *Display the Radial* of the map item, and *Pan To* the map item.
][
#set align(right)
#image("12_attachments_breakdown.jpg", width: 90%)  
]
#toolbox.side-by-side(columns:(2.5fr, 9fr, 2fr))[
#image("12_chat_canned_default_1.jpg", width: 90%)
#image("12_chat_canned_default_2.jpg", width: 90%)
][
At the bottom of the chat area are pre-defined messages that may be used to quickly create a message to send. Select the current menu button to scroll through the eight different menus of pre-defined messages, including: DFLT1, DFLT2, ASLT1, ASLT2, JM1, JM2, RECON1 and RECON2. These pre-defined messages present an easy way to transmit a brief message to other network members concerning position or other important communication. The messages may be changed by long pressing on the button and changing its label and corresponding text.
][
#set align(right)
#image("12_chat_messages.jpg", width: 90%)
]
]

#tak-slide[
  == GeoChat Messaging (continued)
#toolbox.side-by-side(columns:(.75fr, 13fr))[
#image("12_pan_to.svg", width: 90%)
][
Selecting the *Pan To* icon, located at the top right of the call sign in an individual chat, will pan the map interface to that user’s location.
]
#toolbox.side-by-side(columns:(2fr, 10fr))[
#image("12_chat_message_received.svg", width: 30%)
#image("12_chat_message_received_2.jpg", width: 90%)
][
A numbered red dot will appear on the *Contacts* icon when a message has been received successfully. The number denotes the number of unread messages that have been received. Select this icon to view the contact list. The username who sent the message will appear with a numbered red dot next to their name. Alternatively, the text of the message can be read by dragging down from the top to open the Android notifications drawer. This notification will only remain available for a short time.
]
#toolbox.side-by-side(columns:(1fr, 10fr))[
#image("12_chat_read_indicator.jpg", width: 90%)
#image("12_chat_unread_indicator.jpg", width: 90%)
][
When sending a chat message to a user, a “read” indicator will appear next to the sent message. When the indicator is filled (white background, dark checkmark), it indicates the recipient has read the message. 

When the indicator is an unfilled (dark background, white checkmark), it indicates the message has been delivered but the recipient has not opened their chat window to view it.

\
Newer TAK Servers can hold messages sent to disconnected users and deliver the messages the next time the offline user reconnects to the server.
]
]

#tak-slide[
= 13 Chat
#toolbox.side-by-side(columns:(.75fr, 13fr))[
#image("13_Chat_icon.svg", width: 90%)
][
The Chat tool logs, organizes and displays the most recent chat message that was sent from each chatroom associated with the local device.  This provides a quick resource to see the most current chat activity for all subscribed groups and conversations. 
]
#toolbox.side-by-side(columns:(10fr, 2fr))[
Each entry in the Chat tool will display who the message exchange is with, what the last message said and when the last message was sent. The list of entries is ordered by timestamp with the most recent appearing at the top.

Selecting a group or conversation will immediately open the GeoChat feature in Contacts for additional interaction. Select a chat message entry to open the chatroom associated with the message. Select *Search* to input text to search for a specific message or sender. Select *+* to open the Contacts tool. Messages that haven’t been read yet will display a red indicator, like the Contacts tool.
][
#set align(right)
#image("13_chat_overview.jpg", width: 90%)
]
]

#tak-slide[
= 14 Encrypted Mesh
#toolbox.side-by-side(columns:(9fr, 3fr))[
Enhanced security for all communications can be configured on a mesh network in ATAK.  An AES-256 encryption key is generated on one device and is then shared with other devices that require encrypted communications.  Once enabled, encrypted devices can securely communicate with one another and exchange SA, Chat, Data Packages, etc. Encrypted devices cannot communicate on the mesh network with non-encrypted devices and vice versa. This feature provides an additional level of security for advanced users.
][
#set align(right)
#image("14_encryption_key_options.jpg", width: 90%)  
]
To configure encryption, navigate to Settings > Network Preferences > Network Connection Preferences > Configure AES-256 Mesh Encryption. 
#toolbox.side-by-side(columns:(9fr, 3fr))[
Select the *Generate Key* button to create an encryption key, enter the desired file name and select *OK*. The encryption key is saved in the atak/config/prefs folder and can be added to a Data Package to be shared with other users (prior to enabling encryption on the device) or can be preloaded onto the devices. At the time the key is generated, the user has the option to load the key immediately.
][
#set align(right)
#image("14_generate_key.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(9fr, 3fr))[
To load an encryption key, select the *Load Key* button and navigate to the location of the key file. Select the *Forget Key* button to remove the key and revert to unencrypted traffic.  
][
#set align(right)
#image("14_load_key.jpg", width: 90%)
]
]

#tak-slide[
= 15 Video Player
#toolbox.side-by-side(columns:(.75fr, 10fr))[
#image("15_video_player_icon.svg", width: 90%)
][
Select the *Video Player* icon to open the Video player.  The Video Player supports playing video streams from IP cameras, Rover5 and H.264 encoders. The menu provides options to add, edit, delete, play or send videos to other network members. 
]
#toolbox.side-by-side(columns:(2fr, 8fr, 2fr))[
#image("15_video_player_menu.jpg", width: 90%)
][
The toolbar available at the top of the video player window can be used to view video snapshots, add new video alias, download an alias from the TAK Server, sort, multi-select, to either export or delete, or search for a specific alias.

Select the desired video alias or file name to begin playing the stored or streaming video. The video will display half the width of the screen. 

To view a video at full screen, slide the pull bar. To return to half screen, press the device’s back button. Select the *Back* button to return to the list of available videos. 
][
  #set align(right)
#image("15_video_player_screen.jpg", width: 90%)
]
\
To add a stored video file, select the *Import Manager* icon, select *Local SD*, and navigate to the video file and select *OK* to add the video to the list of available videos. The user can also manually place video files in atak/tools/videos/ to be listed after ATAK is restarted.

When a video is playing at half width, slide the pull bar to the right to hide the video but maintain the connection. Slide the pull bar to the left to unhide the video. The video player status is reflected in the Android notifications bar at the top of the screen. 

]

#tak-slide[
== Video Player (continued)
#toolbox.side-by-side(columns:(.75fr, 12fr))[
#image("15_snapshot_icon.svg", width: 90%)
][
Select the *Snapshot* icon to save the current frame of the video as a JPEG image file, the icon will flash green to indicate that the snapshot has occurred. The file will be saved in \atak\tools\videosnaps.   
]
#toolbox.side-by-side(columns:(.75fr, 6fr, 3fr, 3fr))[
#image("15_video_snapshots_icon.svg", width: 90%)
][
Select a snapshot to view it in a larger format. While viewing a snapshot, a caption can be added to it. Select *Send* to send the snapshot to another TAK user, TAK server, or send through a 3rd party app. Select *EXIF* to burn overlay metadata onto the snapshot. Select *Edit* to edit the image in Image Markup, Image Markup must be installed for this option to appear. Select *Pan To* to pan to the location of the video source. Select an *Arrow* to cycle to the next snapshot.   
][
#set align(right)
#image("15_video_snapshots_screen.jpg", width: 80%)
][
  #set align(right)
  #image("15_snapshot.jpg", width: 80%)
]
#toolbox.side-by-side(columns:(.75fr, 10fr, 2fr))[
 #image("15_video_red_record_icon.svg", width: 90%) 
 #image("15_video_green_record_icon.svg", width: 90%)
 
][
If a live UDP or an appropriately formatted RTSP stream is being viewed, it can be recorded by selecting the *Record* icon. The icon will change to a green square while recording. Select *Auto Record* to automatically start recording every time this video stream is opened.  The Video Recording button will be hidden once activated. Stopping the recording will deactivate Auto Record.

Select the *Record* icon again to end the recording. The recordings are saved in /atak/tools/videos/. 
][
#set align(right)
#image("15_auto_record.jpg", width: 80%)
]
#toolbox.side-by-side(columns:(.75fr, 12fr))[
  #image("15_video_close_player.svg", width: 90%)
][
 To close the video player, select the *X* located at the bottom right corner of the video player or select the *Back* button. 
]
]

#tak-slide[
== Adding a Video Stream
#toolbox.side-by-side(columns:(.75fr, 12fr))[
#image("15_add_video_alias_icon.svg", width: 90%)
][
To add a video alias, select *+* at the top of the Video Player screen.
]
#toolbox.side-by-side(columns:(3fr, 12fr))[
#image("15_video_add_alias.jpg", width: 90%)
][
 Enter the necessary information for the selected stream type:  Stream Type (UDP, RTSP, RTMP, RTMPS, TCP, RTP, HTTP, HTTPS, RAW, SRT) along with the necessary streaming information including, IP address (leave IP address blank to listen on the local IP), Port Number, Alias Name, Network Timeout, Buffering and Buffer Time. Selecting buffering along with a buffer time will provide a small amount of buffering of input video flow to help smooth video streams. Adding buffering will increase latency. When the necessary information is entered, select *Add*.
]
#toolbox.side-by-side(columns:(10fr, 6fr))[
A video alias can also be added to a sensor marker  from the Mission Specific iconset, or other marker types with the sensor option enabled. Open the details of the marker to configure the URL and FOV. After both the URL and the FOV are configured, the video alias can be viewed.  These markers can also be sent to other users to share the video alias associated with them.  
][
#set align(right)
#image("15_video_streaming.jpg", width: 90%)
]
]

#tak-slide[
== Downloading a Video Stream
#toolbox.side-by-side(columns:(.75fr, 13fr))[
#image("15_download_video_alias_icon.svg", width: 90%)
][
To download a video alias from a TAK Server, select the *Download* icon at the top of the Video Player screen, then select the TAK Server. A list of available Video aliases will be presented. Select the desired video alias, then choose *OK*. The selected video will then be added to the list of videos.   
]
\
*Search for Video*

To find a particular video alias from the list of available videos, select the *Search* icon at the top of the Video Player screen, then enter the name of the desired video.

\
*Individual Video Options*
#toolbox.side-by-side(columns:(.75fr, 10fr, 3fr))[
#image("15_video_options_icon.svg", width: 90%)
][
To view additional options for individual videos, select the *Additional Options* icon. The options include sending the video alias to other contacts, editing the alias or deleting the alias.


][
#set align(right)
#image("15_additional_video_options.jpg", width: 90%)
]
The *Send* button will prompt the user to choose to send to Contacts or a TAK Server.  If Contacts is selected, choose one or more contacts, then tap *Send*. Selecting *Broadcast* will send to all contacts.  

To edit an existing Video Alias, select the *Edit* icon to access the same options as shown for the Add Video Alias option. During editing, the video alias can be renamed or redirected to a new address and port combination.

To delete an existing video, select the *Delete* icon, then confirm the deletion.
]

#tak-slide[
== Viewing KLV
#toolbox.side-by-side(columns:(8fr, 5fr))[
If a video includes associated metadata, an option will be available to view a representative SPI or CoT Marker. These markers indicate the map location of the sensor at the corresponding time viewed within the video player. 

The SPI marker will indicate the center of view corresponding to that sensor as the video plays.  The user may zoom to the SPI or CoT Marker by selecting the *Zoom To* icon on the video controls or may lock to an SPI by selecting the SPI on the map and selecting the *Lock* icon on the radial. 

_*Note:*_ This functionality is only available for live streams in UDP format if the KLV data is available as well.
][
#set align(right)
#image("15_video_klv.jpg", width: 90%)
]
== Live Video Map Display
#toolbox.side-by-side(columns:(.75fr, 8fr, 4fr))[
#image("15_globe_icon.svg", width: 90%)
][
When the user has a video (video stream) with metadata for the four corners of the video, the user can view the video in the map interface. The user starts by opening the video and selecting the *Globe* icon in the upper right-hand corner of the video window, turning the globe green. 

When the window is minimized, the video can be viewed on the map interface. The video will overlay upon any current imagery displayed.
][
#set align(right)
#image("15_video_pic_in_pic.jpg", width: 90%)
]
]

#tak-slide[
= 16 Go To
#toolbox.side-by-side(columns:(1fr, 12fr, 3.5fr))[
#image("16_go_to_icon.svg", width: 90%)
][
Select the *Go To* icon to enter coordinates and navigate to a specific location on the map. 

Select from the *MGRS* (military grid reference system), *DD* (decimal degrees), *DM* (degrees - minutes), *DMS* (degrees-minutes-seconds), *UTM* (Universal Transverse Mercator) or *ADDR* (address) tabs on the Go To interface and enter the location data of interest. Select the *Cycle* button when using either the MGRS or UTM entry methods to reformat the text fields. The first cycle mode will combine the Easting/Northing into one field with the following cycle mode combining the entire coordinate entry into one large field.
][
  #set align(right)
  #image("16_go_to_mgrs.jpg", width: 80%)
]
#toolbox.side-by-side(columns:(2fr, 10fr))[
#image("16_reordered_goto.jpg", width: 90%)
][
The coordinate entry tabs can be reordered by long pressing and dragging the coordinate entry tab to the new desired location in GoTo.   
]
Enter the Latitude, Longitude and Elevation in the space provided for MGRS, DD, DM or DMS searches. If an elevation source is available, the elevation value can be automatically populated by tapping the *Pull Elevation* button. By default, the source with the highest resolution for the specified location will be used, but this can be configured in Settings > Tool Preferences > Specific Tool Preferences > Coordinate Entry Preferences.
#toolbox.side-by-side(columns:(9fr, 3fr))[
An address can be entered to drop a marker or pan the map to the desired location. After entering an address, select *Find*.  This will populate the *Add Address* box in the Go To window.  Check the Set the Address as the Title box to place the address on the map as a label.  If a marker is selected, the address will be the marker’s label.  If no marker is selected, the address will be placed alone on the map as a label.  
][
  #set align(right)
  #image("16_go_to_enter_address.jpg", width: 90%)
]
Additional address providers can be configured by placing an XML file with the provider information into atak/tools/address. The address lookup provider used for the ADDR tab can be configured in the Settings > Tool Preferences > Specific Tool Preferences > Address Lookup Preferences. The chosen address provider will be displayed in the Go To window on the *ADDR* tab.
]

#tak-slide[
= 17 Drawing Tools
#toolbox.side-by-side(columns:(.75fr, 8fr, 3fr))[
#image("17_drawing_tools_icon.svg", width: 90%)
][
Drawing Tools provides the ability to create different shapes and/or telestrate on the map. Available shapes include, circle, ellipse, rectangle, free form and telestrate. Create a shape to access the radial menu option associated with each shape type. 
][
#set align(right)
#image("17_drawing_tools_toolbar.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(10fr, 3fr))[
The Delete, Fine Adjust, R&B Line and Details radial options behave in the same manner as other map objects. 

The Labels option toggles on/off a label associated with the shape displaying the shape’s dimensions or name. The Free Rotate / 3D View (eye icon) radial option allows map rotation and to view in 3D mode. A yellow eye will appear in the upper left corner and as the map is rotated, the degrees of rotation are displayed and updated. Tapping the yellow eye again will turn the feature off. 

The Geo Fence Tool creates a virtual fence that triggers entry/exit notifications based on its assigned parameters. The Edit option can be used to re-adjust and make additional changes to the shape, as needed. In addition to the standard configuration options that are present in the Details window, the area of the shape is calculated and displayed here as well. 
][
#set align(right)
#image("17_drawing_circle_radial_menu.jpg", width: 90%)
]
\
#toolbox.side-by-side(columns:(3fr, 6fr, 5fr))[
#image("17_free_rotate_3d_view.jpg", width: 90%)
][
Free Rotate/3D View provides display options. All shape types with height can be rendered as 3-dimensional objects when the map is in 3D Mode.
][
#set align(right)
#image("17_atak_3d_shapes.jpg", width: 90%)
]
]

#tak-slide[
== Create a Shape - Circle
#toolbox.side-by-side(columns:(.75fr, 12fr))[
#image("17_circle_icon.svg", width: 90%)
][
To add a Circle, select the *Circle* icon, select a location to place the center point and tap another location for the radius. Select the circle and select *Details* on the radial menu to change the name, radius, number of rings, color, opacity, line thickness, line style, add a remark, add an attachment or to send/broadcast the circle information to others.  
]
#toolbox.side-by-side(columns:(9fr, 4fr))[
All dimensions (radius, area, circumference and height) can be manually adjusted in the Details allowing for precise shape placement on the map. Label visibility and Center point visibility can also be controlled from the Details. Selecting any point of the circle on the map, will open the circle radial menu. 

To edit the circle, select *Edit* from the Details window. Long press the center point to move the circle or long press the edge to resize. Select *Undo* to reverse changes or select *End Editing* to save the changes.
][
#set align(right)
#image("17_circle_height.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(9fr, 6fr))[
  Extrude Mode (circle only) can change a circle with a height to a Cone (Down), Cone (Up), Cylinder, Dome, or Sphere. To enable this feature, place the circle and then define a height for the circle in the details. Once the height has been defined, the Extrude Mode field becomes active.   Select from the list of shapes available. Placing the device into 3D mode, displays the circle with the selected extrusion. 
][
#set align(right)
#image("17_circle_extrusion.jpg", width: 70%)
]
#image("17_atak_3d_extruded_circles.jpg", width: 40%)
]

#tak-slide[
== Create a Shape - Ellipse
#toolbox.side-by-side(columns:(.75fr, 12fr))[
#image("17_ellipse_icon.svg", width: 90%)
][
To add an Ellipse, select the *Ellipse* icon, select a location to place the first corner, select another location for the second corner which sets the edge, and finally select a third location to establish depth.   
]
#toolbox.side-by-side(columns:(10fr, 4fr))[
Select the ellipse and select *Details* on the radial menu to change the name, display the label or center point, color, opacity, line thickness, line style, add a remark, add an attachment or to send/broadcast the circle information to others.  

All dimensions (width, length, heading, area, circumference and height) can be manually adjusted in the Details, allowing for precise shape placement on the map. Label visibility and center point visibility can also be controlled from the Details. To edit the ellipse, select *Edit* from the Details window. Drag or long press the markers to edit the ellipse on the map. Select *Undo* to reverse changes or select *End Editing* to save the changes.  
][
#set align(right)
#image("17_drawing_ellipse.jpg", width: 90%)
]
== Create a Shape - Rectangle
#toolbox.side-by-side(columns:(.75fr, 10fr, 2.5fr))[
#image("17_rectangle_icon.svg", width: 90%)
][
To add a Rectangle, select the *Rectangle* icon, then select a location to place the first corner, select another location to add a parallel corner and then select a third location to indicate the desired depth of the rectangle. 

Selecting any point of the rectangle on the map will open the rectangle radial. Note that Fine Adjust is not active for this shape. To edit the rectangle, select the *Edit* icon. Drag a corner or side of the rectangle, or long press a mid-point or vertex, to move the selected side. The rectangle can be rotated if one of the four mid-points is held and dragged. Select *Undo* to reverse changes or select *End Editing* to save the changes. 
][
#set align(right)
#image("17_drawing_rectangle_radial_menu.jpg", width: 90%)  
]
]

#tak-slide[
== Create a Shape - Rectangle (continued)
#toolbox.side-by-side(columns:(9fr, 5fr))[
Select the rectangle and select *Details* on the radial menu to change the name, color, opacity, line thickness, line style, show labels, show center point, show tactical overlays, add a remark or send/broadcast the rectangle information to others. All dimensions (width, length, area, perimeter and height) can be manually adjusted in the Details allowing for precise shape placement on the map. Label visibility and center point visibility can also be controlled from the Details.

\
Selecting the Tactical Overlay option will allow the user to add tactical color coding to a structure being outlined, establishing a common set of terms for operational coordination. The white side of the rectangle represents the front, while black represents the back of a structure. The green side of the structure appears clockwise from the front (white), while the red side appears counterclockwise. 

\
The first point during placement should be the left front corner of the structure (white/green corner), while the second point should be the right front corner of the structure (white/red corner) and the third point should be the back.  Create the rectangle and when the Details menu is displayed, select the *Tactical Overlay* checkbox to turn on the color coding. 
][
#set align(right)
#image("17_drawing_rectangle.jpg", width: 90%)
#image("17_tactical_overlay.jpg", width: 90%)
]
]

#tak-slide[
== Create a Shape - Free Form

*Free Form*
#toolbox.side-by-side(columns:(.75fr, 13fr))[
#image("17_free_form_icon.svg", width: 90%)
][
To add a Free Form shape (i.e., a polyline or polygon) on the map, select the *Free Form* icon and then select a location to place the first vertex for the shape; continue to tap to add vertices. Select the initial vertex to close the shape or select *End Shape* to form an open shape. Select the *Undo* button to remove the links in sequence. 
]
#toolbox.side-by-side(columns:( 9fr, 5fr))[
Select the free form on the map and select *Details* on the radial menu to change the name of the shape, coordinate center point, coordinate units, color, opacity, line thickness, line style, add height, add a remark or send/broadcast the shape information to others.  

\
Checking the *Show Labels* box will display the name of the shape on the perimeter. Checking the *Center* box will unhide/hide the center point and center point label. Checking the *Closed* box will display a closed shape versus an open shape on the map. An open shape’s center point cannot be changed.  On a free form line, the Details pane displays the start point and end point of the line.  Start Point and End Points can be precisely adjusted from the Details pane.

\
Selecting the shape on the map will open the free form radial. Note that Fine Adjust is not active for this shape. To edit the shape, select the *Edit* icon on the radial. Drag a vertex of the shape or long press a line to add a vertex. Select *Undo* to reverse changes or select *End Editing* to save the changes.
][
#set align(right)
#image("17_drawing_free_form.jpg", width: 90%)
#image("17_drawing_free_form_line.jpg", width: 90%)
]
]

#tak-slide[
== Telestrate
#toolbox.side-by-side(columns:(.75fr, 8fr, 5fr))[
#image("17_telestrate_icon.svg", width: 90%)  
][
Select the *Telestrate* icon to access the Telestrate toolbar. Selecting the *Telestrate* icon enables and disables map scrolling by turning telestration on or off. When Telestrate is toggled on, the user is able to free form draw manually or with a stylus. 

Select *Undo* to remove the most recent activity.  Selecting *End*, ends the current telestration session saving all activity as a single multi-polyline and returns the user to the main Drawing Tools menu. 

\
][
#set align(right)
#image("17_drawing_telestrate.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(2fr, 10fr))[
#image("17_drawing_telestrate_radial_menu.jpg", width: 90%)
][
Selecting a telestration on the map will open the radial menu. Select *Details* on the radial menu to change the name, add a height, change line colors, delete lines or add lines, adjust line thickness, select line style, add remarks or send/broadcast to others. In addition to the Details, Delete, Label and Free Rotate/3D View, the Clamping Tool is available. Select the Clamp radial option to bind the map item to the map's surface.  
]
#toolbox.side-by-side(columns:(10fr, 2fr))[
Selecting the *Color Selection* icon will open the Choose Telestration Color menu. Choose a provided color or select *Custom* to customize a color. 
][
#set align(right)
#image("17_drawing_color_selection.jpg")
]
]

#tak-slide[
== Briefing Graphics and Tactical Symbols 
#toolbox.side-by-side(columns:(.75fr, 9fr, 3fr))[
#image("17_milsym_icon.svg", width: 90%)
][
Depending on the specific symbol type selected, the procedure for placing a symbol or graphic shape will be the same as for an ordinary Drawing Tools shape (e.g., free form shape, rectangle, or circle etc.). The shape will draw on the map according to the selected type instead of as an ordinary free form shape. The symbol or graphic type is listed in the shape’s Details panel, where it can also be changed or cleared.
][
#set align(right)
#image("17_milsym_warm_front.jpg", width: 90%)
]
== Minimum Safe Distance
#toolbox.side-by-side(columns:(.75fr, 13fr))[
#image("17_msd_radial.svg", width: 90%)
][
Select the *Minimum Safe Distance* (MSD) radial option to create a zone around a shape that has been previously placed on the map.
]

\
#toolbox.side-by-side(columns:(8fr, 5fr, 2.5fr))[
Use the Range field to specify the distance from the shape’s border that the zone will be created from. MSD color can be adjusted by selecting the color swatch and the visibility for the zone can be enabled by checking the *Enabled* checkbox.

MSDs can also be created and saved to the Favorites section by selecting the *+*. These favorites are shared between all shape types that support this feature. Select an MSD from the Favorites section to quickly enable it.
][
  #set align(right)
  #image("17_minimum_safe_enabled.jpg", width: 90%)
][#set align(right)
  #image("17_msd_favorites.jpg", width: 90%)
]
]

#tak-slide[
  = 18 Geofencing
#toolbox.side-by-side(columns:(.75fr, 13fr))[
#image("18_geofence_icon.svg", width: 90%)
][
The Geo Fence Tool allows users to create a virtual fence that triggers entry/exit notifications if map items of interest cross the virtual boundary lines. Geo Fence options are added to the existing drawing tools. 
]
#toolbox.side-by-side(columns:(9fr, 3fr))[
After a Geo Fence has been added to a shape, it can be accessed by selecting the *Geo Fence* icon from the additional tools menu or by selecting it from the shape’s radial.  The Geo Fence radial option will be highlighted when a Geo Fence has been added to a shape.

\
The Enabled field slider will move to Tracking by default when a new Geo Fence is created. Toggle the slider between Tracking and Off to enable/disable the Geo Fence.  Use the Trigger field to define which types of Geo Fence breaches to monitor. Choose between Entry, Exit or Both. Use the Monitor field to define which entities the Geo Fence will track. 

\
Choose between TAK Users, Friendly, Hostile, Custom or All. Elevation boundaries for the entities being tracked can be defined using the shape’s characteristics. If a height value has been set for the shape, the Min value will use the shape’s elevation, and the Max will be the elevation plus the height. Select the *OK* button to finish creating the Geo Fence. Select the *Send* button to create the fence and send it to another user. Select *Delete* to close the Create Geo Fence window and discard changes. 

\
Geo Fence breaches will trigger an alert in the lower left corner of the map and will also be listed in the Overlay Manager.
][
#set align(right)
#image("18_geofence_edit.jpg", width: 60%)
#image("18_geofence_breach.jpg", width: 100%)
#image("18_alert_in_om.jpg", width: 50%)
]
]

#tak-slide[
= 19 Lasso Select
#toolbox.side-by-side(columns:(.75fr, 13fr))[
#image("19_lasso_tool.svg", width: 90%)
][
The Lasso Tool provides a way to quickly select items on the map. These items can then be exported and shared with others or deleted. When the tool is launched, onscreen directions are displayed in green, providing instructions how to use the tool.   
]
#image("19_user_instructions.jpg", width: 20%)
#toolbox.side-by-side(columns:(5fr, 4fr, 3fr))[
Draw a circle around the area of interest to display a list of all the map items in the area. 

Select the *External Native Data* box to include the underlying map data in the export. Tap *Select* when all individual items have been chosen. 
][
#set align(right)
#image("19_lasso_map_items.jpg", width: 90%)
][
#set align(right)
#image("19_select_items.jpg", width: 70%)
]
#toolbox.side-by-side(columns:(8fr, 3fr, 3fr))[
The Select Lasso Action interface provides the option to either *Export*, *Delete*, or *Send* the items.

Selecting *Export* displays the Select Export Format Screen. The available formats are Attachments, Data Package, GPX, KML, KMZ, Shapefile or Video. Choose the desired format or select *Previous Exports* to view and send previously exported files.

Select *Delete* and verify by selecting *OK* on the confirmation screen. This will remove the selected items from the map.
][
#set align(center)
#image("19_select_action.jpg", width: 70%)
][
#set align(right)
#image("19_select_export_format.jpg", width: 90%)
]
]

#tak-slide[
= 20 Quick Pic
#toolbox.side-by-side(columns:(.75fr, 13fr))[
#image("20_quick_pic_icon.svg", width: 90%)
][
Select the *Quick Pic* icon to access the Android device’s camera or another camera application. After taking a picture, it can be discarded by selecting *Retry* or saved by selecting *OK* or *Done*. Saving the picture returns to the map view and places a camera marker at the self-marker’s  location.  The image taken is attached to the marker.
]
#toolbox.side-by-side(columns:(10fr,3fr))[
The image can then be renamed, sent to other TAK users on the same server, marked-up (if the Image Markup application has been installed and loaded), have overlay metadata added to the image (if the TAK GeoCam application is installed and loaded) or panned-to the Quick Pic marker on the map. If the user selects the Rename Image icon a dialog box with different predefined options is displayed. Once confirmed, the file name is updated to reflect this change.
][
#set align(right)
#image("20_quick_pic_rename_image.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(2.5fr, 8fr, 5fr))[
#image("20_quick_pic_radial_menu.jpg", width: 90%)
][
Select the Quick Pic marker to activate its radial.  Options include: Delete, Polar Coordinate Entry, R&B line, Image View and Details (long press to access the sub-radial menu to quickly send the marker). Select *Image View* to view the image along with the marker and the approximate field of view of the still image. The image can also be accessed by selecting *Details*, the *Attachments* (paperclip) icon and then selecting the image thumbnail.
][
#set align(right)
#image("20_quick_pic_marker_and_attachment.jpg",width: 90%)
]
#toolbox.side-by-side(columns:(.75fr, 8fr, 4fr))[
#image("20_gallery_icon.svg", width:90%)
][
The integrated Gallery Tool can be used to view media attachments. The attachment image’s file name and the associated map item will be displayed. Captions can be added to the image file by opening the specific image in the gallery and tapping the line at the top of the image.
][
#set align(right)
#image("20_quick_pic_gallery.jpg", width: 92%)
]
]

#tak-slide[
= 21 Track History
#toolbox.side-by-side(columns:(.75fr, 13fr))[
#image("21_track_history_icon.svg", width: 90%)
][
The device’s GPS can be used to track movements with the Track History Tool. These tracked paths can be exported to a TAK Server, to a route or to a KML, KMZ, GPX, or CSV file. A GPS position must be established before tracking can begin.
]
#toolbox.side-by-side(columns:(2fr, 8fr, 2fr))[
#image("21_track_new.jpg", width: 90%)
][
Tracks can also be recorded for other users/markers by selecting the *Breadcrumb* option from the marker’s radial.

Selecting the *Track History* icon will open Track Details for the current active track.  The track title, color and style can be modified.  Initiate a new track by selecting the *Add Track* icon.  Accept or edit the default track name and select the *OK* button to begin the new track. User location data is recorded as breadcrumbs in a new track file.
][
#set align(right)
#image("21_track_details.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(1fr, 11fr))[
#image("21_track_history_toolbar.svg", width: 90%)
][
Use the Track Search function to view track information that has been previously saved locally or on a TAK Server. The tool searches the track database for matches against the specified time range and by user callsign. Matching tracks are displayed as a list and can be selected to view on the map interface.  
]
]

#tak-slide[
== Track History (continued)
#toolbox.side-by-side(columns:(10fr, 5fr))[
Select the *Track Search* icon to access the function.  Specify callsign and time frame, check the box for Server Search (if desired), then select *Search*.  The track list will appear.  The query results can be sorted by Track Name or Start Time. Select any of the query results to move to that track. Once selected, the name, color and style of a selected track can be modified, or the track can be cleared. Convert a track to a TAK route, publish to a server or export it as a KML, KMZ, GPX or CSV file by selecting the desired track and then *Export*. Enter a file name then select *Next* and choose the export format. Select *Done* or *Send* when the export completes.
][
#set align(right)
#image("21_track_list_with_map.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(2fr, 11fr))[
#image("21_track_search.jpg", width: 90%)
][
When viewing the track list, the Track History Toolbar will appear at the top of the Track History pane.  The options include *Add a Track*, *Multi-select*, *Track Search* and *Clear Tracks*. The Track History list allows a user to view and select the tracks of other users that they have saved locally. The Track Search – Local Device provides the ability to perform a tailored search for tracks meeting specific criteria. Use the searching option to retrieve all the tracks on the device.
]
#toolbox.side-by-side(columns:(2fr, 12fr))[
#image("21_track_list_toolbar.svg", width: 90%)
][
Track History and Bread Crumb options can be configured in the Settings > Tool Preferences > Specific Tool Preferences > Track History Preferences.
]
]

#tak-slide[
= 22 Digital Pointer Tools
#toolbox.side-by-side(columns:(.75fr, 13fr))[
#image("22_digital_pointer_icon.svg", width: 90%)
][
Select the *Digital Pointer* icon to open the Digital Pointer toolbar. The Digital Pointer toolset is primarily used to share pointers with team members. Additionally, GoTo MGRS is available for use.
]
#toolbox.side-by-side(columns:(.75fr, 13fr))[
#image("22_dp_icon.svg", width: 90%)
][
The Pointer button allows the user to place an indicator on the map. If other team members are on the same network, the pointer icon will automatically be sent to them. It will appear on their map and as a notification message. A line can also be seen connecting the pointer to the user that placed it.
]
#toolbox.side-by-side(columns:(9fr, 2.5fr, 2.5fr))[

Selecting the user’s own pointer opens a radial menu. This menu includes Fine adjust, Polar Coordinates, Pair to Self, Range and Bearing, Custom Threat Rings and Place a Marker.

Selecting another user’s pointer opens a radial menu with additional options, including the Lock to Self feature, the Bloodhound Tool, and the ability to Track Breadcrumbs. 
][
#set align(right)
#image("22_digital_pointer_user_radial_menu.jpg", width: 90%)
][
  #set align(right)
  #image("22_digital_pointer_team_member_radial_menu.jpg", width: 85%)
]
#toolbox.side-by-side(columns:(.75fr, 13fr))[
#image("22_mgrs_go_to_icon.svg", width: 90%)
][
Select the *GoTo MGRS* icon from the Digital Pointer toolbar to manually enter desired MGRS coordinates to place a local Pointer. This allows for fast entry of the 10-digit Easting and Northing and includes the corresponding grid zone for that map view. 
]
Digital Pointer Tools settings can be customized in Settings > Tool Preferences > Specific Tool Preferences > Digital Pointer Toolbar Preferences. The legacy toolbar mode can be enabled in preferences. Legacy mode will add the Dynamic R&B Line option and provide the option to configure between 1-3 DPs on the Digital Pointer toolbar.
]

#tak-slide[
= 23 Elevation Tools
#toolbox.side-by-side(columns:(.75fr, 13fr))[
#image("23_elevation_tools_icon.svg", width: 90%)
][
Select the *Elevation Tools* icon to open the Elevation Tool. The Elevation Tool includes Heatmap, Terrain Slope, Viewshed and Contour Lines functionality. 
]
#toolbox.side-by-side(columns:(1fr, 9fr, 6fr))[
#image("23_elevation_tools_scale.jpg", width: 90%)
][
The Heatmap Tool displays elevation data on a color scale with lower elevations represented by blue and higher elevations by red. The Intensity, Saturation, and Value can be modified for user preference. Elevation Data (such as DTED, SRTM, Quantized Mesh or other forms) is needed for this tool to work properly.
][
#set align(right)
#image("23_elevation_tools_heatmap.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(9fr, 5fr))[
Terrain Slope can be viewed by selecting the *Heatmap* box and changing the selection to Terrain Slope. The slope of the elevation data is a color scale with smaller slopes depicted as yellow and higher slope values as black. The intensity can be modified by the user. 
][
#set align(right)
#image("23_elevation_tools_terrain_slope.jpg", width: 93%)
]
]

#tak-slide[
== Viewshed
#toolbox.side-by-side(columns:(11fr, 2fr))[
The Viewshed tool determines the visibility from a selected map location. From the Viewshed tab, select the *Place Viewshed* (eye above peak) icon and then tap a location on the map or select a map marker. A Viewshed (eye) marker will appear on the map interface. 

_*Note:*_ If zoomed out too far, only the Eye View icon will be visible.  Zoom in to see the viewshed.

_*Note:*_ Viewsheds and contour lines do not persist upon ATAK quit/restart.

A circle with the specified radius will display with the viewshed marker as the center. Green parts of the circle represent areas visible from that location and red parts represent areas that are obstructed from view.  The Height Above Marker can be altered to reflect how far above ground level the viewshed should calculate. The radius of the viewshed can also be modified.

The elevation data source can be selected by toggling the Source button.  Terrain and Surface are supported sources. If Adjust Together is unchecked, the intensity of the visible and obscured areas can be adjusted separately using the seen/unseen sliders. Intensity can be increased or decreased by using the slide bar or entering a numeric value. Select *Remove Viewshed* to delete the viewshed from the map.


][
#set align(right)
#image("23_elevation_tools_viewshed.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(1.5fr, 11fr, 1.5fr))[
#image("23_viewshed_list.jpg", width: 90%)
][
Tap *Select Viewshed* to show a list of all created viewsheds. Select an individual viewshed name to pan to it on the map. Select the *i* icon (Details) to view or modify the current viewshed parameters. Viewsheds can also be removed through the multi-select tool. The viewshed radial will open by selecting the *Viewshed* (eye) icon. Available options are Delete, Fine Adjust/Enter Coordinate/MGRS Location, R&B Line, and Details.
][
#set align(right)
#image("23_viewshed_radial.jpg", width: 90%)
]
Settings for Elevation Tools can be changed by navigating to Settings > Tool Preferences > Specific Tool Preferences > Elevation Overlay Preferences.
]

#tak-slide[
== Contour Lines
#toolbox.side-by-side(columns:(8fr, 5fr))[
The Contour Lines Tool generates contour lines on the map in the area within the current window. 

To generate contour lines, select the *Contour* tab within the Elevation Tools screen. The *Generate* button becomes active when the map is zoomed to the correct scale (Scale varies based on screen resolution). Modify fields as desired and then select the *Generate* button. A progress bar and the percentage complete will appear to give feedback on the contour line generation. 

Major Lines and Minor Lines can be toggled on or off without having to regenerate the contour lines. Line color, Units (meters or feet) and Major Line Width can also be changed without regenerating the contour lines. If the Interval is modified, select the *Generate* button to regenerate the contour lines with the new value. 
][
#set align(right)
#image("23_contour_lines.jpg", width: 90%)
]
]

#tak-slide[
= 24 Resection Tool
#toolbox.side-by-side(columns:(.75fr, 12fr, 1.5fr))[
#image("24_resection_icon.svg", width: 90%)
#image("24_point_dropper.svg", width: 90%)
][
The Resection Tool provides a capability for users without a GPS signal to estimate their location from known points/landmarks on the map. Two or more reference points on the map are needed to provide a more accurate estimate of the user’s location. 

From their position, the user identifies and places a series of landmark points on the map.  As each landmark point is placed via the point dropper icon, a compass is displayed allowing the user to adjust the bearing from their position to the landmark.

After a landmark point has been placed, the location and bearing can be modified by tapping the corresponding field for that landmark entry on the Resection panel. When two or more landmark points have been placed, an intersection point is computed and displayed in the intersection field. 
][
#set align(right)
#image("24_resection_main_menu.jpg", width: 90%)
#image("24_compass_adjust_bearing.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(.75fr, 12fr, 3fr))[
#image("24_plot_intersection_icon.svg", width: 90%)
][
Select the *Plot Intersection* icon to place a marker on the map at the current intersection point. If additional landmark points are added, the intersection field will be updated and the *Plot Intersection* icon can be selected again to place a more up to date intersection point marker on the map. The marker will be labeled with the callsign of the device.

To move a landmark point, long press the point on the map and then select another location for the point. To remove an individual landmark point, select the landmark point on the map to bring up the radial and select the *Delete* radial option.
][
#set align(right)
#image("24_intersection_with_marker.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(.75fr, 12fr, 2fr))[
#image("24_clear_landmarks_icon.svg", width: 90%)
][
 Other landmark point radial options include: Bearing, Fine Adjust, R&B Line and Details. To delete all landmark points that have been placed on the map, select the *Clear Landmarks* icon and confirm the deletion of all landmark points.

When finished estimating a new location, select the *Back* button to exit the tool. A dialog will appear with the option to update the Self-Marker location. Select *Yes* to update the Self-Marker to the new resection estimate and have that location broadcast in a SA message to other users on the network. Select *No* to not update the Self-Marker location.
][
#set align(right)
#image("24_update_location.jpg", width: 90%)
]
]

#tak-slide[
= 25 Import
#toolbox.side-by-side(columns:(.75fr, 11fr, 2.5fr))[
#image("25_import_manager_icon.svg", width: 90%)
][
Select the *Import* icon to import supported files into the TAK application from an SD card or via the network.

From the *Select Import Type* interface window the following can be selected:  *Local SD*, *Gallery*, *KML Link*, *HTTP URL* or *Choose App*.
][
#set align(right)
#image("25_import_manager_import_type.jpg", width: 90%)
]
Select *LOCAL SD* to import from a folder residing on the internal or external SD card.   Navigate to the folder from where files are to be imported.  Various types of files can be imported via Import Manager including: ATAK configuration, Data package zip files, Elevation Data (such as DTED, SRTM, Quantized Mesh or other forms), imagery and overlay files. 

Imagery file types that are supported include XML, SQLite, MVT, GeoPackage with imagery, CADRG, CIB, ECRG, GeoTiff, JPG2000, KMZ with imagery, MrSid, NTIF, and PFPS.  Overlay file types that are supported include DRW, GPX, KML, KMZ, LPT and Shape. 
#toolbox.side-by-side(columns:(10fr, 2fr))[
\
Select *OK* to confirm the files to be imported,  then choose an import strategy. Select *Copy* to copy the chosen files to the ATAK directory. Select *Move* to move the file from its original location to the ATAK directory. Select *Use in Place* to import the file from its current location in the storage directory. 
][
#set align(right)
#image("25_import_strategy_prompt.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(8fr, 2fr, 2fr, 2fr))[
Depending on file type, most imported files will be accessible via Maps and Favorites, Data Package or Overlay Manager. Some file extensions, like GPX or ZIP files for example, may result in a prompt for the user to select which import method to use. Import Manager is extensible when plug-ins are installed.  Plug-ins that provide an alternate way to process a given file format will appear as an option for that file type.
][
#set align(right)
#image("25_tiff_import_options.jpg", width: 90%)
][
#set align(right)
#image("25_gpx_import_options.jpg", width: 90%)
][
#set align(right)
#image("25_zip_import_options.jpg", width: 90%)
]
]

#tak-slide[
== Import (continued)

Select *KML Link* to import a KML file via the network using HTTP or tap *HTTP URL* to import other file types via the network using HTTP. Enter a name for the link, a valid HTTP URL and a refresh interval (KML link only). Finally, indicate whether the local content should be removed when ATAK is shut down. Select *Add* to save the link. 

#toolbox.side-by-side(columns:(3fr, 10fr, 3fr))[
#image("25_import_manager_add_kml.jpg", width: 90%)
][
Once the KML Link or Remote File Resource has been added, it will be listed in the Remote Resources category in Overlay Manager. The red status indicator appears next to files that are available for download but have yet to be added. Selecting the *Download* icon initiates the download process after the user verifies the activity. The green status indicator lists files that have been successfully downloaded.
][
#set align(right)
#image("25_import_manager_remote_resources.jpg", width: 90%)
]
]

#tak-slide[
= 26 Alert
#toolbox.side-by-side(columns:(.75fr, 11fr, 1.5fr))[
#image("26_alert_icon.svg", width: 90%)
][
Select the *Alert* icon to open the Alert Tool in ATAK.  The Alert Tool provides the capability of indicating the need for assistance, the type of emergency and its location on the map.  

The type of emergency can be selected from the drop-down menu, before activation, and includes options for a 911 Alert, Ring the Bell, Geo Fence Breached or In Contact. 
][
#set align(right)
#image("26_emergency_types.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(11fr, 6fr))[
Once the emergency type has been selected and both switches have been enabled, the TAK Server broadcasts the announcement to all network contacts.  Even if the  device is turned off, the beacon will continue to broadcast. The beacon can be canceled and removed from the map by returning  to the Alert tool and toggling the switches off. 
][
#set align(right)
#image("26_emergency_911.jpg", width: 90%)
]
]

#tak-slide[
= 27 Rubber Sheet
#toolbox.side-by-side(columns:(.75fr, 10fr, 2fr))[
#image("27_rubber_sheet_icon.svg", width: 90%)  
][
The Rubber Sheet Tool allows the ability to add georeferencing to a 2D image or a non-rectified 3D model, as well as edit a 2D image overlay or 3D model that is already georeferenced. Select the *Rubber Sheet* icon to open the toolbar. Options available on the toolbar are: Import, Sort, Export and Search.  Imported Rubber Sheets can be sorted in either alphabetical order or by distance from the Self-Marker.
][
#set align(right)
#image("27_rubber_sheet_toolbar.svg", width: 90%)
]
== Working with 2D Imagery
#toolbox.side-by-side(columns:(10fr, 3fr))[
To import a 2D image, pan and zoom the map to the desired location for the imagery. Select the *+* icon, select the desired image file to import, then select *OK*. The image will be displayed on the current map view and listed in the Rubber Sheets section of Overlay Manager. 

After the image has been imported, selecting it will open the radial menu. Imported Rubber Sheets can be sorted in either alphabetical order or by distance from the Self-Marker.

Select *Edit* to adjust the size, rotation, and location of the imagery. The controls are the same as for editing Drawing Tools rectangles. Drag a corner to change the rectangle size, drag the midpoint of an edge to change size and rotation, and drag the center to move the entire rectangle. Select *Rotate* to use two fingers to rotate the rectangle without changing its size. When the image has been adjusted to the desired position on the map, select *End Editing*.
][
#set align(right)
#image("27_2d_rubber_sheet_radial_menu.jpg", width: 90%)
]
\
#toolbox.side-by-side(columns:(.75fr, 13fr))[
#image("27_export_icon.svg", width: 90%)
][
Select *Export* from the Rubber Sheets’ Details, or Rubber Sheet category in Overlay Manager to export the rubber sheet image as a KMZ file. The exported result is placed in the atak/tools/rubbersheet folder. For convenience, a prompt will appear to import the exported result as an image overlay.
]
]

#tak-slide[
== Working with 3D Imagery
#toolbox.side-by-side(columns:(10fr, 2fr))[
To import a 3D model, pan and zoom the map to the desired location for the model, unless the model is georeferenced. Select the *+* icon and navigate to the file location of the desired 3D model then select *OK*.

If the model is not geo-rectified, projection options are presented. Choose between ENU (East North Up), with the option to Flip Y/Z, or LLA (Lat, Lon, Alt), then select *Import*.
][
#set align(right)
#image("27_import_3d_model.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(3fr, 8fr, 5.5fr))[
#image("27_3d_rubber_sheet_radial_menu.jpg", width: 90%)
][
Once the model has been imported and placed on the map, the  user  can  select  the  model  to  open  the  radial. To edit the model, select the *Edit* icon from the radial. Once in edit mode, the model can be dragged on the map, rotated and/or raised or lowered.

After the model has been positioned as desired, select *End Editing*. To export it to a georeferenced OBJ 3D model, select Overlay Manager > Rubber Sheets, then select the *Export* icon. The file is placed in the /atak/export folder. 
][
#set align(right)
#image("27_rubber_sheet_editing.jpg", width: 90%)
]
\
#toolbox.side-by-side(columns:(10fr, 2fr, 3fr))[
When the export has finished, select whether to import the finished product into ATAK or send it to another user. If the model is imported, it will be placed on the map and will be listed in the Overlay Manager under 3D Models.
][
#set align(right)
#image("27_rubber_sheet_done_edit_highlight.jpg", width: 90%)
][
 #set align(right)
 #image("27_rubber_sheet_export.jpg", width: 90%)
]
]

#tak-slide[
= 28 TAK Package Management
#toolbox.side-by-side(columns:(.75fr,13fr))[
#image("28_plugin_icon.svg", width: 90%)
][
The primary user interface for managing products and product repositories in ATAK is the TAK Package Management Tool. This tool streamlines the process of obtaining plug-ins and provides a single location to manage TAK products available across all supported TAK product repositories. To install tools or plug-ins into Android OS and load them, select the *Plugins* icon from the additional tools and plug-ins menu.
]
#toolbox.side-by-side(columns:(10fr, 2.5fr))[
 Actions available from here include viewing the status, availability, and details of all available products.  Products can also be installed, updated or uninstalled from this menu. Select *Search* (magnifying glass) to enable a text search field that can also filter by status and origin.
][
#set align(right)
#image("28_tak_pkg_mgt_toolbar.svg", width: 90%)
]
\
Upon opening ATAK or after a product repository sync is completed, if any current products have available updates (e.g., a new version of an application or plug-in), a prompt will appear to open the TAK Package Management view for updates. Note that compatible products may still function when not up to date; however, incompatible products will not be loaded until they are updated. 

#toolbox.side-by-side(columns:(8fr, 3fr, 2.5fr))[
Upon opening ATAK or after a product repository sync is completed, if any current products have available updates (e.g., a new version of an application or plug-in), a prompt will appear to open the TAK Package Management view for updates. Note that compatible products may still function when not up to date; however, incompatible products will not be loaded until they are updated. 
][
#set align(right)
#image("28_tak_pkg_mgt_main.jpg", width: 90%)
][
#set align(right)
#image("28_tak_pkg_mgt_updates_available.jpg", width: 90%)
]
The lists of plug-ins will have a red or green shield associated with it. A green shield signifies an officially signed plug-in or application. A red shield signifies that the plug-in or application is not officially signed.
]

#tak-slide[
== TAK Package Management (continued)
#toolbox.side-by-side(columns:(10fr, 1.25fr))[
To enable or disable auto synchronization of the device with all configured product repositories, select the *Overflow Menu* icon (three vertical dots) from the TAK Package Management screen then select *Edit* and check the *Auto Sync* box.  _*Note:*_ The *Overflow Menu* might not be needed on a tablet device.

If Auto Sync is enabled, the sync occurs each time ATAK is started. If a newly installed plug-in is not on the list, select the *Sync* icon to update the local list. 
_*Note:*_ The status of installed but inactive plug-ins will appear as STATUS: Not Loaded.

Over-the-Air Update Server product repository is disabled by default but may be enabled to (1) view status of remote repository (status of last sync attempt), (2) view time of last successful sync with the remote repository and/or (3) switch to a custom or private remote repository (change the URL).

To enable Over-the-Air Server product repository, open TAK Package Management > select the *Overflow Menu* > select *Edit*, then check the box for *Update Server*. *Update Server URL* becomes available (empty by default). Select to enter the URL for the desired repository server and select *OK*. 
][
#set align(right)
#image("28_tak_pkg_mgt_details.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(10fr, 2.25fr))[
Select *Quick Configure Update Server URL from a TAK Server* to quickly configure the update server URL based on an existing TAK Server connection.   Return to the TAK Package Management screen and press *Sync* to populate server and local plug-ins. The server status information will appear near the top of the screen. 

If running *Update Server* provides multiple base versions of ATAK (e.g., 5.6.0, 5.7.0, and 5.8.0) and it is preferred to upgrade base versions, select the *Overflow Menu* > *Check Versions* > Choose the desired version.    If unselected, the repository chosen will be the one that aligns with the current ATAK version. 
ATAK automatically synchronizes with the configured product repositories the first time it runs after being upgraded to a new version. This allows ATAK to check for available updates and incompatible plug-ins. If a repository sync is manually initiated by the user, a sync operation will display a progress dialog. Auto sync operations which occur during startup do not display a progress dialog, minimizing interference of the user working in other tools.

][
#set align(right)
#image("28_tak_pkg_mgt_incompatible.jpg", width: 90%)
#image("28_tak_pkg_mgt_sync.jpg", width: 90%)
]
]

#tak-slide[
= 29 Toolbar Manager 

The ATAK toolbar is customizable allowing for multiple layouts to be available to quickly switch to or edit. The default toolbar contains five tools, a sixth tool can be added on a smartphone device with up to ten tools supported on a tablet device. 
#toolbox.side-by-side(columns:(10fr, 2fr))[
Select the *Additional Tools* button, beside the toolbar, to access the other available tools which are displayed in a grid format by default. 
][
#set align(right)
#image("29_default_toolbar.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(.75fr, 13fr, 1.5fr, 1.5fr))[
#image("29_list_view_icon.svg", width: 90%)
#image("29_grid_view_icon.svg", width: 90%)
][

\
Toggle between the *List View* and *Grid View* to change the layout of the additional tools interface.   To change the tools layout to list format, select the *List View* button. Selecting *Grid View* will return the layout to the grid format.  
][
#set align(right)
#image("29_tools_list_view.jpg", width: 93%)
][
#set align(right)
#image("29_tools_grid_view.jpg", width: 85%)
]
== Modifying a Toolbar
#toolbox.side-by-side(columns:(.75fr, 14fr))[
#image("29_edit_toolbar_icon.svg", width: 90%)
][
Select *Edit Toolbar* to launch the toolbar Edit Mode. Any user-created toolbar may be modified by selecting *Edit Toolbar* at any time. 
]
#toolbox.side-by-side(columns:(10fr, 3.5fr))[
_*Note:*_ When entering Edit Mode through the icon on the toolbar, the current toolbar will be presented to modify. If this is the default toolbar, a copy of it will be used as the starting point for the new toolbar since the default toolbar cannot be modified. To start with a “clean” toolbar, go to the toolbars list and choose to *Add Toolbar* instead.
][
#set align(right)
#image("29_create_toolbar.jpg", width: 90%)
]
]

#tak-slide[
== Modifying a Toolbar (continued)
#toolbox.side-by-side(columns:(9fr, 5fr, 3fr))[
The following actions can be done when in Create/Edit Mode:
#set list(indent: 2cm)

 - Name/rename the toolbar
 - Add and/or remove a tool on the toolbar
 - Change the visibility of a tool in the listing

To add a tool to the toolbar, long-press and drag the icon into the position desired. 
][
#set align(right)
#image("29_drag_tool.jpg", width: 90%)
][
#set align(right)
#image("29_delete_tool.jpg", width: 80%)
]
If an icon is already in the space, dragging the new tool into the space will replace it. Long-press and drag a tool from the toolbar to the delete area on the map to delete it from the toolbar and return it to the main tool list. The are several icons that appear on the tools when in Edit Mode that provide the ability to manage the tool icons both within the tool list and on the main toolbar. 
#toolbox.side-by-side(columns:(.75fr, 15fr))[
#image("29_visible_in_tools.svg", width: 90%)
][
The visibility icon indicates that a tool is displayed in the main toolbar list after the editing panel is closed. Select this icon to remove it from the tool list. It may be made visible again when in Edit Mode.
]

#toolbox.side-by-side(columns:(.75fr, 15fr))[
#image("29_no_visibility.svg", width: 90%)
][
The visibility icon with a slash through it indicates that the tool will not be visible at all after closing Edit Mode. Select it to return the tool icon to the tool list.
]

#toolbox.side-by-side(columns:(.75fr, 15fr))[
#image("29_remove_from toolbar.svg", width: 90%)
][
The delete icon gives the user another way to remove a tool from the main toolbar. Select it and the tool will be removed from the toolbar and moved back to the tool list. An icon with the trashcan also indicates that the tool will be visible on the toolbar but not visible in the list when Edit Mode is closed.
]

#toolbox.side-by-side(columns:(.75fr, 15fr))[
#image("29_save_icon.svg", width: 90%)
][
After completing all desired modifications to the toolbar, select *Save*. The toolbar will appear in ATAK’s available list of toolbars.
]
]

#tak-slide[
== Export Custom Toolbar
#toolbox.side-by-side(columns:(.75fr, 13fr))[
#image("29_export_icon.svg", width: 90%)
][
To save a toolbar outside of ATAK, select *Export Toolbar*. The exported file will be saved to /sdcard/atak/export/. After exporting, the toolbar can be sent to another TAK user. The toolbar will also be saved to the toolbar list. 

_*Note:*_ A toolbar can also be imported using ATAK Import Manager or selecting the *Import Toolbar* button in the Toolbar Manager panel.
]
== Toolbar Management
#toolbox.side-by-side(columns:(.75fr, 11fr, 2fr))[
#image("29_toolbar_manager_icon.svg", width: 90%)
][
Select the *Toolbar Manager* button to see a list of all the currently available toolbars. 

Select *Add Toolbar* at the top to enter Edit Mode with a new “clean” toolbar. No tools are initially present on the toolbar when a new toolbar is created through Toolbar Manager. Toolbar creation uses the same actions as when modifying an existing toolbar. These are described in the Modifying a Toolbar section. Select *Save* after the edits are complete and the new toolbar will appear in the toolbar list. 
][
#set align(right)
#image("29_toolbar_manager.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(.75fr, 13fr))[
#image("29_import_icon.svg", width: 90%)
][
Select *Import Toolbar* to import a previously saved toolbar from the Android file system. Note: Another TAK user can also send a toolbar in a Data Package that will be auto-imported into the receiver’s toolbar list.  
]
Toolbar selection is also done in the Toolbar Manager. Select the radio toggle button to choose which toolbar to use. Toolbars may be edited by selecting *Edit* on the line with its name. Remove a toolbar by selecting *Delete*.

A legacy toolbar option is available as well.  The legacy tool bar can be selected during TAK device setup by selecting *Action Bar Experience* and then the *Right Side (Legacy)* option.  The option to switch to the legacy toolbar can be accessed after device setup by navigating to Additional Tools and Plugins > Settings > Support > TAK Initial Device Configurations.  
]

#tak-slide[
= 30 Clear Content
#toolbox.side-by-side(columns:(.75fr, 10fr, 2.5fr))[
#image("30_clear_content_icon.svg", width: 90%)
][
Select the *Clear Content* icon to remove all ATAK content from the Android device. Note that this action will permanently erase all content. 

Select the *Clear maps & imagery* checkbox to clear map and imagery data as well. 
][
#set align(right)
#image("30_clear_content_screen.jpg", width: 90%) 
]
#toolbox.side-by-side(columns:(11fr, 2.5fr))[
Lock both switches by swiping them to the right to activate the Clear Now button.  Tap *Clear Now* to remove all content. ATAK will exit after this action has been completed. 

Select specific items to delete by tapping the *Select Items* button. This will navigate to the Overlay Manager multi-select tool to choose specific items to delete. Tap *Cancel* to return to the main ATAK interface. 

During the deletion process the file data is corrupted, making file recovery nearly impossible.
][
#set align(right)
#image("30_clear_content_clear_now.jpg", width: 90%)
]
]

#tak-slide[
= 31 Other Features

Plug-ins must be installed and then loaded to function in ATAK.
== Installing a Plug-in - Manual Installation

If an update server is not available, a manual install can be done. Acquire the plug-in .apk file, place it on the local device, browse to it, then select the apk to launch it. Android will prompt for permission to install. Allow the installation to run until notified that it is complete. 
#toolbox.side-by-side(columns:(.75fr, 11fr, 5fr))[
#image("31_plugins_icon.svg", width: 90%)  

\
#image("31_plugins_sync.svg", width: 90%)
][
Launch ATAK and open the TAK Package Management screen by selecting *Plugins* from the Additional Tools menu or by navigating to Settings > Tool Preferences > Package Management. 

\

A list of currently installed plug-ins will be displayed. Also included in the list are available stand-alone applications.

 If the newly installed plug-in is not on the list, select the *Sync* icon to update the local list.
][
#set align(right)
#image("31_package_mgmt_icon.jpg", width: 60%)
#image("31_list_of_plugins.jpg", width: 90%)

]

]

#tak-slide[
== Manual Installation (continued)
#toolbox.side-by-side(columns:(10fr, 4fr))[
The status of installed but inactive plug-ins will appear with a status of Not Loaded. Select the checkbox and then *Load* for the new plug-in to be loaded. The plug-in can be unloaded by unchecking the box.  When the plug-in has a status of Loaded, it is ready for use. The status of an installed plug-in that is no longer compatible with ATAK will appear with a status of Incompatible. 

The lists of plug-ins and applications will also have a red or green shield associated with it. A green shield signifies an officially signed plug-in or application. A red shield signifies that the plug-in or application is not officially signed. If an updated plug-in is not available, the plug-in should be uninstalled. To uninstall the plug-in, select the *Trashcan* icon.

The *Information* icon provides additional information about the plug-in.
][
#set align(right)
#image("31_plugin_details.jpg", width: 90%)
]
#toolbox.side-by-side(columns:(10fr, 4fr))[
To install a stand-alone application that has a status of Not Installed, select the application entry from the list. Once the selected application dialog appears, select the *Install* option. Once installed, the application is loaded.

\

The application can also be upgraded to a newer existing version if the status is Update Available. Select the application entry in the list and once the application dialog appears, select the *Update* option. The application can also be uninstalled from this dialog by selecting the *Uninstall* option.  
][
#set align(right)
#image("31_install_standalone.jpg", width: 60%)
#image("31_standalone_apps.jpg", width: 90%)
]
]

#tak-slide[
== Installing a Plug-in - Update Server Installation
#toolbox.side-by-side(columns:(10fr, 3fr))[
Plug-ins can also be installed via an Update Server. The Update Server can be configured by selecting Settings > Tool Preferences > Package Management > select the Additional Options drop-down > Edit. Check the box for *Update Server* and configure the three subsequent fields.

Launch ATAK and go to the TAK Package Management screen by navigating to Settings > Tool Preferences > Package Management. A list of currently installed applications and plug-ins will be displayed. 
][
#set align(right)
#image("31_subsequent_3_fields.jpg", width: 90%)
]
Select the *Sync* icon to update the list from the server. 

The plug-ins and applications listed can be installed, loaded, upgraded and uninstalled in the same manner that was described in the previous section.

== Installing a Plug-in - Automatic Provisioning (Link EUD)
#toolbox.side-by-side(columns:(10fr, 3fr))[
Using the Link EUD option when ATAK is initially configured, links the user device to an organization's server. The list of available plug-ins will be updated in TAK Package Management, based on the user’s role in the organization. To manually refresh the list, select the *Sync* icon.
][
#set align(right)
#image("31_link_eud.jpg", width: 80%)
]
]

#tak-slide[
== User Feedback
#toolbox.side-by-side(columns:(9fr, 2fr))[
ATAK users can initiate feedback and issue reporting.  To access the User Feedback Tool, navigate to Additional Tools and Settings > Settings > Support > User Feedback.  From the User Feedback dialog, select *+* and complete the text fields using text entry or the Android speech to text feature accessed by selecting the *Microphone* on the top row of the QWERTY keyboard.  Associated files, like log files and screenshots, can be added by selecting *Attachment* > *Edit* > and *+* to navigate within the Android internal file structure system to the desired file(s).  When the form is completed with the required information, select *Send* and the bug report will automatically be uploaded to a repository when a connection is available.   
][
  #set align(right)
  #image("01_user_feedback.jpg", width: 90%)
]
]
