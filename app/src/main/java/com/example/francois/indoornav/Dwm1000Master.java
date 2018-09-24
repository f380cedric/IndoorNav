package com.example.francois.indoornav;

import java.util.Arrays;

class Dwm1000Master extends Dwm1000 {

    private final UwbMessages[] messagesArray = new UwbMessages[2]; //FIXME
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

        for(int i = 0; i < messagesArray.length; ++i) {
            messagesArray[i] = new UwbMessages();
            messagesArray[i].masterPoll     = new byte[] {(byte)(0x11 + i)};
            messagesArray[i].masterFinal    = new byte[] {(byte)(0x21 + i)};
            messagesArray[i].slaveResponse  = new byte[] {(byte)(0x1A + (i<<4))};
        }
    }

    double getDistance() {
        long[] allClockTime = new long[6 * messagesArray.length];
        for (int i = 0; i < messagesArray.length; ++i) {
            System.arraycopy(ranging(messagesArray[i]),0, allClockTime, i * 6, 6);
        }
        return compute_distance(allClockTime); //FIXME
    }

    private long[] ranging(UwbMessages messages) {
        long[] clockTime = new long[6];
        State state = State.POLL_INIT;
        while (state != State.END) {
            switch (state) {
                case POLL_INIT:
                    sendFrameUwb(messages.masterPoll, (byte) messages.masterPoll.length);
                    state = State.WAIT_POLL_SEND;
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
                                sendFrameUwb(messages.masterFinal, (byte) messages.masterPoll.length);
                                clockTime[3] = byteArray5ToLong(readDataSpi(RX_TIME, (byte) 0x05));
                                state = State.WAIT_FINAL_SEND;
                            }
                            break;
                        case 1:
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
                            if (clockTime[5] < clockTime[2] || clockTime[4] < clockTime[0]) {
                                state = State.POLL_INIT;
                            }
                            break;
                        case 1:
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

    private double compute_distance(long[] allClockTime) {
        long tRoundMaster, tRoundSlave, tReplyMaster, tReplySlave;
        double tof = 0.0;
        double distance = 0.0;
        long[] clockTime = new long[6];
        for(int i = 0; i < messagesArray.length; ++i) {
            System.arraycopy(allClockTime, i * 6, clockTime, 0, 6);
            tRoundMaster = clockTime[3] - clockTime[0];
            tReplyMaster = clockTime[4] - clockTime[3];
            tReplySlave = clockTime[2] - clockTime[1];
            tRoundSlave = clockTime[5] - clockTime[2];

            if (tReplySlave > tRoundMaster || tReplyMaster > tRoundSlave) {
                tof = 0.0;
                distance = 0.0;
            } else {
                tof = (tRoundMaster * tRoundSlave - tReplyMaster * tReplySlave) * TIME_UNIT /
                        (tRoundMaster + tRoundSlave + tReplyMaster + tReplySlave);
                distance = 0.0;
            }
            //TODO
        }
        return tof;
    }


}
