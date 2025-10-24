
package com.atakmap.android.radiolibrary.rover;

public class Tre extends Radio {

    /**
     * Construct a TRE radio.
     */
    public Tre(final String nickname,
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
