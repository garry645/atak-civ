
package com.atakmap.android.metrics;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;

import android.widget.TextView;
import android.widget.EditText;

import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;

import gov.tak.api.annotation.DontObfuscate;
import gov.tak.api.annotation.NonNull;

@DontObfuscate
public class MetricsUtils {

    public static final String CATEGORY_MAPITEM = "category_mapitem";
    public static final String CATEGORY_CHAT = "category_chat";
    public static final String CATEGORY_ACTIVITY = "category_activity";
    public static final String CATEGORY_UNKNOWN = "category_unknown";
    public static final String CATEGORY_MAPWIDGET = "category_mapwidget";
    public static final String CATEGORY_PREFERENCE = "category_preference";
    public static final String CATEGORY_TOOL = "category_tool";

    public static final String EVENT_CHAT_SENT = "event_chat_sent";
    public static final String EVENT_CHAT_PREDEFINED_SELECTED = "event_chat_predefined_selected";
    public static final String EVENT_CHAT_MODE_CHANGED = "event_chat_mode_changed";
    public static final String EVENT_CHAT_MESSAGE = "event_chat_message";

    public static final String REASON_NO_MESSAGE = "event_chat_no_message";
    public static final String REASON_NO_RECIPIENT = "event_chat_no_recipient";

    public static final String EVENT_LOCATION_ENTRY = "event_location_entry";

    public static final String EVENT_MAPITEM_CHANGED = "event_mapitem_changed";
    public static final String EVENT_MAPITEM_REMOVED = "event_mapitem_removed";
    public static final String EVENT_MAPITEM_ADDED = "event_mapitem_added";
    public static final String EVENT_MAPITEM_MOVED = "event_mapitem_moved";
    public static final String EVENT_MAPITEM_ACTION = "event_mapitem_action";

    public static final String EVENT_PREFERENCE_CLICKED = "event_preference_selected";

    public static final String EVENT_WIDGET_STATE = "event_widget_state";
    public static final String EVENT_CLICKED = "event_clicked";
    public static final String EVENT_LONG_CLICKED = "event_long_clicked";

    public static final String EVENT_STATUS_STARTED = "started";
    public static final String EVENT_STATUS_SUCCESS = "success";
    public static final String EVENT_STATUS_FAILED = "failed";
    public static final String EVENT_STATUS_CANCELLED = "cancelled";

    public static final String FIELD_ACTION_TYPE = "action_type";
    public static final String FIELD_CALLSIGN = "callsign";
    public static final String FIELD_HAS_FOCUS = "has_focus";
    public static final String FIELD_KEYEVENT_ACTION = "keyevent_action";
    public static final String FIELD_KEYEVENT_KEYCODE = "keyevent_keycode";
    public static final String FIELD_KEYEVENT_KEYPRESSED = "keyevent_keypressed";
    public static final String FIELD_KEYEVENT = "keyevent";

    public static final String FIELD_ELEMENT_NAME = "element_name";
    public static final String FIELD_EVENT = "event";
    public static final String FIELD_MAPITEM_UID = "mapitem_uid";
    public static final String FIELD_MAPITEM_TYPE = "mapitem_type";

    public static final String FIELD_STATE = "state";
    public static final String FIELD_CLASS = "class";

    public static final String FIELD_ACTION_IDENTIFIER = "action_identifier";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_REASON = "reason";
    public static final String FIELD_SELECTED = "selected";
    public static final String FIELD_RESULTING_MESSAGE = "resulting_message";
    public static final String FIELD_INFO = "info";

    public static final String INTENT_EXTRA_CLASS = "metricapi_intent_extra_class";
    public static final String INTENT_EXTRA_CATEGORY = "metricapi_intent_extra_category";
    public static final String INTENT_EXTRA_ELEMENT = "metricapi_intent_extra_element";

    /**
     * Listen for keystrokes on a edit text field.
     * @param category the category associated with the edit text field where the list of valid
     *                 categories is defined in MetricsUtils.
     * @param className provide the classname for the recording.   Do not use getClass().getName()
     *                  as this will generally be obfuscated on release builds.
     * @param elementName the element name to identify the specific edit text
     * @param field the field to be monitored - please note that this will override the
     *              View.OnKeyListener, EditText.OnEditorActionListener and View.OnFocusChangeListener.
     *              If you want to record the actions of a edittext, please see recordKeyStroke
     */
    public static void attachKeyStrokeListener(@NonNull String category,
            @NonNull String className,
            @NonNull String elementName,
            @NonNull EditText field) {
        field.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (MetricsApi.shouldRecordMetric()) {
                    Bundle bundle = new Bundle();
                    bundle.putString(FIELD_ACTION_TYPE, "onKey");
                    bundle.putString(FIELD_CALLSIGN,
                            MapView.getMapView().getDeviceCallsign());
                    bundle.putInt(FIELD_KEYEVENT_ACTION, event.getAction());
                    bundle.putString(FIELD_ELEMENT_NAME, elementName);
                    bundle.putInt(FIELD_KEYEVENT_KEYCODE, keyCode);
                    bundle.putString(FIELD_CLASS, className);
                    Bundle details = new Bundle();
                    details.putString(FIELD_RESULTING_MESSAGE,
                            field.getText().toString());
                    bundle.putAll(details);
                    MetricsApi.record(category, bundle);
                }
                return false;

            }
        });

        field.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId,
                    KeyEvent event) {
                if (MetricsApi.shouldRecordMetric()) {
                    final Bundle bundle = new Bundle();
                    bundle.putString(FIELD_ACTION_TYPE, "onEditorAction");
                    bundle.putString(FIELD_CALLSIGN,
                            MapView.getMapView().getDeviceCallsign());
                    if (event != null) {
                        bundle.putInt(FIELD_KEYEVENT_ACTION, event.getAction());
                        bundle.putParcelable(FIELD_KEYEVENT, event);
                    }
                    bundle.putString(FIELD_ELEMENT_NAME, elementName);
                    bundle.putString(FIELD_CLASS, className);
                    Bundle details = new Bundle();
                    details.putString(FIELD_RESULTING_MESSAGE,
                            field.getText().toString());
                    bundle.putAll(details);
                    MetricsApi.record(category, bundle);
                }
                return false;
            }
        });

        field.addTextChangedListener(new TextWatcher() {
            int lastSize = -1;

            public void afterTextChanged(Editable s) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
                lastSize = count;
            }

            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
                if (MetricsApi.shouldRecordMetric()) {

                    if (start >= s.length())
                        return;

                    char keyPressed;

                    if (count < lastSize) {
                        keyPressed = '\b';
                    } else {
                        keyPressed = s.charAt(start);
                    }

                    final Bundle bundle = new Bundle();
                    bundle.putString(FIELD_ACTION_TYPE, "onTextChanged");
                    bundle.putString(FIELD_CALLSIGN,
                            MapView.getMapView().getDeviceCallsign());
                    bundle.putString(FIELD_ELEMENT_NAME, elementName);
                    bundle.putInt(FIELD_KEYEVENT_KEYPRESSED, keyPressed);
                    bundle.putString(FIELD_CLASS, className);
                    Bundle details = new Bundle();
                    details.putString(FIELD_RESULTING_MESSAGE,
                            field.getText().toString());
                    bundle.putAll(details);
                    MetricsApi.record(category, bundle);
                }
            }
        });

        field.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (MetricsApi.shouldRecordMetric()) {
                    final Bundle bundle = new Bundle();
                    bundle.putString(FIELD_ACTION_TYPE, "onFocusChange");
                    bundle.putString(FIELD_CALLSIGN,
                            MapView.getMapView().getDeviceCallsign());
                    bundle.putString(FIELD_ELEMENT_NAME, elementName);
                    bundle.putBoolean(FIELD_HAS_FOCUS, hasFocus);
                    bundle.putString(FIELD_CLASS, className);
                    MetricsApi.record(category, bundle);
                }
            }
        });
    }

    /**
     * Record information relevant to a specific android widget
     * @param category the category associated with this android widget metric record.
     * @param event one of the events for the provided category
     * @param className provide the classname for the recording.   Do not use getClass().getName()
     *                  as this will generally be obfuscated on release builds.
     * @param elementName the element name / widget name to identify the specific ui element
     * @param details the bundle of information to be recorded
     */
    public static void record(@NonNull String category,
            @NonNull String event,
            @NonNull String className,
            @NonNull String elementName,
            @NonNull Bundle details) {
        if (MetricsApi.shouldRecordMetric()) {
            final Bundle bundle = new Bundle();
            bundle.putString(FIELD_CALLSIGN,
                    MapView.getMapView().getDeviceCallsign());
            bundle.putString(FIELD_ELEMENT_NAME, elementName);
            bundle.putString(FIELD_EVENT, event);
            bundle.putString(FIELD_CLASS, className);
            bundle.putAll(details);
            MetricsApi.record(category, bundle);
        }
    }

    /**
     * Record information relevant to a specific android widget
     * @param category the category associated with this android widget metric record.
     * @param event one of the events for the provided category
     * @param className provide the classname for the recording.   Do not use getClass().getName()
     *                  as this will generally be obfuscated on release builds.
     * @param elementName the element name / widget name to identify the specific ui element
     * @param info the bundle of information to be recorded
     */
    public static void record(@NonNull String category,
            @NonNull String event,
            @NonNull String className,
            @NonNull String elementName,
            @NonNull String info) {
        if (MetricsApi.shouldRecordMetric()) {
            final Bundle bundle = new Bundle();
            bundle.putString(FIELD_CALLSIGN,
                    MapView.getMapView().getDeviceCallsign());
            bundle.putString(FIELD_ELEMENT_NAME, elementName);
            bundle.putString(FIELD_EVENT, event);
            bundle.putString(FIELD_CLASS, className);
            bundle.putString(FIELD_INFO, info);
            MetricsApi.record(category, bundle);
        }
    }

    /**
     * Record information relevant to a specific android widget
     * @param category the category associated with this android widget metric record.
     * @param event one of the events for the provided category
     * @param className provide the classname for the recording.   Do not use getClass().getName()
     *                  as this will generally be obfuscated on release builds.
     * @param details the information to be recorded
     */
    public static void record(@NonNull String category,
            @NonNull String event,
            @NonNull String className,
            @NonNull Bundle details) {
        if (MetricsApi.shouldRecordMetric()) {
            final Bundle bundle = new Bundle();
            bundle.putString(FIELD_CALLSIGN,
                    MapView.getMapView().getDeviceCallsign());
            bundle.putString(FIELD_EVENT, event);
            bundle.putString(FIELD_CLASS, className);
            bundle.putAll(details);
            MetricsApi.record(category, bundle);
        }
    }

    /**
     * Record information relevant to a specific android widget,   This performs the
     * same action as passing in a bundle with a DETAIL_FIELD_INFO instead of the
     * info string parameter.
     * @param category the category associated with this android widget metric record.
     * @param event one of the events for the provided category
     * @param className provide the classname for the recording.   Do not use getClass().getName()
     *                  as this will generally be obfuscated on release builds.
     * @param info the simple string information associated with the event
     */
    public static void record(@NonNull String category,
            @NonNull String event,
            @NonNull String className,
            @NonNull String info) {
        if (MetricsApi.shouldRecordMetric()) {
            final Bundle bundle = new Bundle();
            bundle.putString(FIELD_CALLSIGN,
                    MapView.getMapView().getDeviceCallsign());
            bundle.putString(FIELD_EVENT, event);
            bundle.putString(FIELD_CLASS, className);
            Bundle details = new Bundle();
            details.putString(FIELD_INFO, info);
            bundle.putAll(details);
            MetricsApi.record(category, bundle);
        }
    }

    /**
     *
     * @param category the category associated map item event
     * @param event the action to be associated with the map item event
     * @param className provide the classname for the recording.   Do not use getClass().getName()
     *                  as this will generally be obfuscated on release builds.
     * @param mapItem the map item
     * @param details the bundle of information to be recorded
     */
    public static void record(@NonNull String category,
            @NonNull String event,
            @NonNull String className,
            @NonNull MapItem mapItem,
            @NonNull Bundle details) {
        if (MetricsApi.shouldRecordMetric()) {
            final Bundle bundle = new Bundle();
            bundle.putString(FIELD_EVENT, event);
            bundle.putString(FIELD_CALLSIGN,
                    MapView.getMapView().getDeviceCallsign());
            bundle.putString(FIELD_CLASS, className);
            bundle.putString(FIELD_MAPITEM_UID, mapItem.getUID());
            bundle.putString(FIELD_MAPITEM_TYPE, mapItem.getType());
            bundle.putAll(details);
            MetricsApi.record(category, bundle);
        }
    }

    /**
     * This performs the same action as passing in a bundle with a DETAIL_FIELD_INFO
     * instead of the info string parameter.
     * @param category the category associated map item event
     * @param event the action to be associated with the map item event
     * @param className provide the classname for the recording.   Do not use getClass().getName()
     *                  as this will generally be obfuscated on release builds.
     * @param mapItem the map item
     * @param info the info line for the action
     */
    public static void record(@NonNull String category,
            @NonNull String event,
            @NonNull String className,
            @NonNull MapItem mapItem,
            @NonNull String info) {
        if (MetricsApi.shouldRecordMetric()) {
            final Bundle bundle = new Bundle();
            bundle.putString(FIELD_EVENT, event);
            bundle.putString(FIELD_CLASS, className);
            bundle.putString(FIELD_CALLSIGN,
                    MapView.getMapView().getDeviceCallsign());
            bundle.putString(FIELD_MAPITEM_UID, mapItem.getUID());
            bundle.putString(FIELD_MAPITEM_TYPE, mapItem.getType());

            Bundle details = new Bundle();
            details.putString(FIELD_INFO, info);
            bundle.putAll(details);

            MetricsApi.record(category, bundle);
        }
    }
}
