package com.example.francois.indoornav;

import android.os.SystemClock;
import android.util.Log;

import com.ftdi.j2xx.interfaces.SpiMaster;

import java.util.Arrays;

class Dwm1000Master extends Dwm1000 {

    private final int           numberSlaves    = 1;
    private final UwbMessages[] messagesArray   = new UwbMessages[numberSlaves]; //FIXME
    private static final double correctivePol[] = { -0.0081, 0.0928, 0.6569, -0.0612};
    private              double distancemm[]    = new double[numberSlaves];
    enum State {
        POLL_INIT,
        WAIT_POLL_SEND,
        WAIT_RESPONSE,
        WAIT_FINAL_SEND,
        GET_TIMES,
        END,
    }





    Dwm1000Master(SpiMaster my_spimInterface) {
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
        //long start, stop;
        //start = SystemClock.elapsedRealtimeNanos();
        for (int i = 0; i < numberSlaves; ++i) {
            Log.d("Slave number", String.valueOf(i));
            System.arraycopy(ranging(messagesArray[i]),0, allClockTime, i * 6, 6);
        }
        //stop = SystemClock.elapsedRealtimeNanos();
        return compute_distances(allClockTime); //FIXME
        //return stop - start;
    }

    private long[] ranging(UwbMessages messages) {
        long[] clockTime = new long[6];
        State state = State.POLL_INIT;
        int caseImIn = 0;
        while (state != State.END) {
            Log.d("Current state: ", state.toString());
            switch (state) {
                case POLL_INIT:
                    caseImIn = 0;
                    sendFrameUwb(messages.masterPoll, (byte) messages.masterPoll.length);
                    state = State.WAIT_POLL_SEND;
                    break;
                case WAIT_POLL_SEND:
                    caseImIn = 0;
                    if (checkFrameSent()) {
                        clockTime[0] = byteArray5ToLong(readDataSpi(TX_TIME, (byte) 0x05));
                        state = State.WAIT_RESPONSE;
                        caseImIn = 1;
                    }
                    break;
                case WAIT_RESPONSE:
                    caseImIn = -1;
                    switch (checkForFrameUwb()) {
                        case 0:
                            caseImIn = 0;
                            state = State.POLL_INIT;
                            if (receiveFrameUwb()[0] == messages.slaveResponse[0]) {
                                caseImIn = 10;
                                clockTime[3] = byteArray5ToLong(readDataSpi(RX_TIME, (byte) 0x05));
                                sendFrameUwb(messages.masterFinal, (byte) messages.masterPoll.length);
                                state = State.WAIT_FINAL_SEND;
                            }
                            break;
                        case 1:
                            caseImIn = 1;
                            state = State.POLL_INIT;
                            break;
                        case 2:
                            caseImIn = 2;
                            state = State.POLL_INIT;
                            break;
                        case 3:
                            caseImIn = 3;
                            break;
                    }
                    break;
                case WAIT_FINAL_SEND:
                    caseImIn = 0;
                    if (checkFrameSent()) {
                        caseImIn = 1;
                        clockTime[4] = byteArray5ToLong(readDataSpi(TX_TIME, (byte) 0x05));
                        state = State.GET_TIMES;
                    }
                    break;
                case GET_TIMES:
                    switch (checkForFrameUwb()) {
                        case 0:
                            caseImIn = 0;
                            state = State.END;
                            byte[] data = receiveFrameUwb();
                            clockTime[1] = byteArray5ToLong(Arrays.copyOfRange(data, 0, 5));
                            clockTime[2] = byteArray5ToLong(Arrays.copyOfRange(data, 5, 10));
                            clockTime[5] = byteArray5ToLong(Arrays.copyOfRange(data, 10, 15));
                            if (clockTime[5] < clockTime[2] || clockTime[4] < clockTime[0]) {
                                caseImIn = 11;
                                state = State.POLL_INIT;
                            }
                            break;
                        case 1:
                            caseImIn = 1;
                            state = State.POLL_INIT;
                            break;
                        case 2:
                            caseImIn = 2;
                            state = State.POLL_INIT;
                            break;
                        case 3:
                            caseImIn = 3;
                            break;
                    }
                    break;
            }
            Log.d("Outputs: ", String.valueOf(caseImIn));
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

            //if (!(tReplySlave > tRoundMaster || tReplyMaster > tRoundSlave)) {
                tof = (tRoundMaster * tRoundSlave - tReplyMaster * tReplySlave) * TIME_UNIT /
                        (tRoundMaster + tRoundSlave + tReplyMaster + tReplySlave);
                double distanceMeasured = tof * 299792458;
                distance = distanceMeasured;
                /*distance = correctivePol[0];
                for(int j = 1; j < 4; ++j) {
                    distance = distance * distanceMeasured + correctivePol[j];
                }*/
                //if (distance < 100) {
                    distancemm[i] = distance * 1000;
            //}
            //TODO
        }
        return distancemm;
    }


}
