
package com.atakmap.android.radiolibrary.rover;

public class Rover6 extends Radio {

    /**
    * Construct a Rover 6 radio.
    * @param nickname a user readable description of the radio.
    * @param radioAddress the ip address of the radio for use when launching the web configurator
    * @param mciOutputAddress the multicast output address, standard is 230.77.68.76
    * @param mciOutputPort the multicast output address, standard is 19024
    * @param mciInputAddress the multicast output address, standard is 230.77.68.76
    * @param mciInputPort the multicast output address, standard is 19025
    */
    public Rover6(final String nickname,
            final String radioAddress,
            final String mciInputAddress,
            final int mciInputPort,
            final String mciOutputAddress,
            final int mciOutputPort) {
        super(nickname, radioAddress,
                mciInputAddress, mciInputPort,
                mciOutputAddress, mciOutputPort);
    }

}
