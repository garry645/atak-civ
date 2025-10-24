package gov.tak.api.widgets;

public interface IJoystickWidget extends IMapWidget
{
    interface OnJoystickMotionListener
    {
        /**
         *
         * @param direction Degrees clockwise, starting from 12 o'clock
         * @param weight    Distance from _neutral_, where {@code 0f} is neutral and {@code 1f} is
         *                  maximum distance
         */
        void onJoystickMotion(IJoystickWidget joystick, float direction, float weight);
    }

    void setSize(float size);
    float getSize();
    void setColor(int color);
    int getColor();
    void addOnJoystickMotionListener(OnJoystickMotionListener l);
    void removeOnJoystickMotionListener(OnJoystickMotionListener l);
}
