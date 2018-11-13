package com.example.francois.indoornav;

import java.util.Arrays;

class Dwm1000Master extends Dwm1000 {

    private final int           numberSlaves    = 1;
    private final UwbMessages[] messagesArray   = new UwbMessages[numberSlaves]; //FIXME
    //private static final double correctivePol[] = { -0.0081, 0.0928, 0.6569, -0.0612};
    //private static final double correctivePol[] = {0.004396537699051796, 0.9195024228226539,
    //        0.23848199262062902}; // 4.30m calib, distance
    private static final double correctivePol[] = {-8.18957391e-07, -2.34689642e-04, -2.48275823e-02, -1.15859275e+00,
            -2.04203118e+01}; // 7.94m calib, Pr
    private static final double __estimated_power = -14.3+20*Math.log10(LIGHT_SPEED)-
            20*Math.log10(4*Math.PI*6489e6);

    private              double distancemm[]    = new double[numberSlaves];
    enum State {
        POLL_INIT,
        WAIT_POLL_SEND,
        WAIT_RESPONSE,
        WAIT_FINAL_SEND,
        GET_TIMES,
        END,
    }





    Dwm1000Master(FT311SPIMasterInterface my_spimInterface) {
        super(my_spimInterface);

        for(int i = 0; i < numberSlaves; ++i) {
            messagesArray[i] = new UwbMessages();
            messagesArray[i].masterPoll     = new byte[] {(byte)(0x11 + i)};
            messagesArray[i].masterFinal    = new byte[] {(byte)(0x21 + i)};
            messagesArray[i].slaveResponse  = new byte[] {(byte)(0x1A + (i<<4))};
            distancemm[i] = 0.0;
        }
    }

    double[] getDistances() {
        long[] allClockTime = new long[6 * messagesArray.length];
        for (int i = 0; i < numberSlaves; ++i) {
            System.arraycopy(ranging(messagesArray[i]),0, allClockTime, i * 6, 6);
        }
        return compute_distances(allClockTime); //FIXME
    }

    private long[] ranging(UwbMessages messages) {
        long[] clockTime = new long[6];
        State state = State.POLL_INIT;
        while (state != State.END) {
            switch (state) {
                case POLL_INIT:
                    sendFrameUwb(messages.masterPoll, (byte) messages.masterPoll.length);
                    state = State.WAIT_POLL_SEND;
                    break;
                case WAIT_POLL_SEND:
                    if (checkFrameSent()) {
                        clockTime[0] = byteArray5ToLong(readDataSpi(TX_TIME, (byte) 0x05));
                        state = State.WAIT_RESPONSE;
                    }
                    break;
                case WAIT_RESPONSE:
                    switch (checkForFrameUwb()) {
                        case 0:
                            state = State.POLL_INIT;
                            if (receiveFrameUwb()[0] == messages.slaveResponse[0]) {
                                clockTime[3] = byteArray5ToLong(readDataSpi(RX_TIME, (byte) 0x05));
                                sendFrameUwb(messages.masterFinal, (byte) messages.masterPoll.length);
                                state = State.WAIT_FINAL_SEND;
                            }
                            break;
                        case 1:
                            state = State.POLL_INIT;
                            break;
                        case 2:
                            state = State.POLL_INIT;
                            break;
                        case 3:
                            break;
                    }
                    break;
                case WAIT_FINAL_SEND:
                    if (checkFrameSent()) {
                        clockTime[4] = byteArray5ToLong(readDataSpi(TX_TIME, (byte) 0x05));
                        state = State.GET_TIMES;
                    }
                    break;
                case GET_TIMES:
                    switch (checkForFrameUwb()) {
                        case 0:
                            state = State.END;
                            byte[] data = receiveFrameUwb();
                            clockTime[1] = byteArray5ToLong(Arrays.copyOfRange(data, 0, 5));
                            clockTime[2] = byteArray5ToLong(Arrays.copyOfRange(data, 5, 10));
                            clockTime[5] = byteArray5ToLong(Arrays.copyOfRange(data, 10, 15));
                            if (clockTime[5] < clockTime[1] || clockTime[4] < clockTime[0]) {
                                state = State.POLL_INIT;
                            }
                            break;
                        case 1:
                            state = State.POLL_INIT;
                            break;
                        case 2:
                            state = State.POLL_INIT;
                            break;
                        case 3:
                            break;
                    }
                    break;
            }
        }
        return clockTime;
    }

    private double[] compute_distances(long[] allClockTime) {
        double tRoundMaster, tRoundSlave, tReplyMaster, tReplySlave;
        double tof;
        double distance;
        long[] clockTime = new long[6];
        for(int i = 0; i < numberSlaves; ++i) {
            System.arraycopy(allClockTime, i * 6, clockTime, 0, 6);
            tRoundMaster = clockTime[3] - clockTime[0];
            tReplyMaster = clockTime[4] - clockTime[3];
            tReplySlave = clockTime[2] - clockTime[1];
            tRoundSlave = clockTime[5] - clockTime[2];

            tof = (tRoundMaster * tRoundSlave - tReplyMaster * tReplySlave) * TIME_UNIT /
                    (tRoundMaster + tRoundSlave + tReplyMaster + tReplySlave);
            double distanceMeasured = tof * LIGHT_SPEED;
            double estimated_power = __estimated_power - 20 * Math.log10(distanceMeasured);
            if (estimated_power >= -50 || distanceMeasured <= 0) {
                estimated_power = -50;
            } else if (estimated_power<= -110) {
                estimated_power = -110;
            }
            distance = distanceMeasured - (Math.pow(estimated_power, 4) * correctivePol[0] +
                    Math.pow(estimated_power, 3) * correctivePol[1] +
                    Math.pow(estimated_power, 2) * correctivePol[2] +
                    estimated_power * correctivePol[3] +
                    correctivePol[4]);
            distancemm[i] = 100 * distance;
        }
        return distancemm;
    }


}
