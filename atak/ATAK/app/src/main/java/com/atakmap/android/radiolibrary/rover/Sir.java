
package com.atakmap.android.radiolibrary.rover;

public class Sir extends Radio {

    /**
     * Construct a SIR radio.
     */
    public Sir(final String nickname,
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
