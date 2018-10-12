package com.example.francois.indoornav;

import android.os.SystemClock;
import android.util.Log;

import java.util.Arrays;

class Dwm1000Master extends Dwm1000 {

    private final int           numberSlaves    = 1;
    private final UwbMessages[] messagesArray   = new UwbMessages[numberSlaves]; //FIXME
    private static final double correctivePol[] = { -0.0081, 0.0928, 0.6569, -0.0612};
    private              double distancemm[]    = new double[numberSlaves];
    private              double coordinates[]   = {0,0};


    private static final int BEACONPOS1X    = 0;
    private static final int BEACONPOS1Y    = 0;
    private static final int BEACONPOS2X    = 1;
    private static final int BEACONPOS2Y    = 0;
    private static final int BEACONPOS3X    = 0;
    private static final int BEACONPOS3Y    = 0;


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
        long tRoundMaster, tRoundSlave, tReplyMaster, tReplySlave;
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
                distance = correctivePol[0];
                for(int j = 1; j < 4; ++j) {
                    distance = distance * distanceMeasured + correctivePol[j];
                }
                if (distance < 100) {
                    distancemm[i] = distance * 1000;
                }
            //}
            //TODO
        }
        return distancemm;
    }

    private double[] computeCoordinates(double[] distances) {
        int p[][] = new int[3][2];
        p[0][0] = BEACONPOS1X;
        p[0][1] = BEACONPOS1Y;
        p[1][0] = BEACONPOS2X;
        p[1][1] = BEACONPOS2Y;
        p[2][0] = BEACONPOS3X;
        p[2][1] = BEACONPOS3Y;

        // computing a
        double a[] = {0,0};
        double ppTp[] = {0,0};
        double rp[] = {0,0};
        for (int i=0; i<numberSlaves;++i){
            ppTp[0] += Math.pow(p[i][0],3) + p[i][0] * Math.pow(p[i][1],2);
            ppTp[1] += Math.pow(p[i][1],3) + p[i][1] * Math.pow(p[i][0],2);
            rp[0] += p[i][0] * Math.pow(distances[i],2);
            rp[1] += p[i][1] * Math.pow(distances[i],2);
        }
        for (int i=0;i<2;++i){
            a[i] = (ppTp[i]-rp[i])/3.0;
        }

        // computing B
        double B[][] = {{0,0},{0,0}};
        double ppT[][] = {{0,0},{0,0}};
        double pTpI[][] = {{0,0},{0,0}};
        double rI[][] = {{0,0},{0,0}};
        for (int i=0;i<numberSlaves;++i){
            for(int j=0;j<2;j++){
                for(int k=0;k<2;k++){
                    ppT[j][k] += p[i][j] * p[i][k];
                }

                pTpI[j][j] += Math.pow(p[i][0],2) + Math.pow(p[i][1],2);
                rI[j][j] += Math.pow(distances[i],2);
            }
        }
        for (int i=0;i<2;i++){
            for (int j=0;j<2;j++){
                B[i][j] = (-2.0*ppT[i][j] - pTpI[i][j] + rI[i][j])/3;
            }
        }

        // computing c
        double c[] = {0,0};
        for (int i=0;i<3;i++){
            for(int j=0;j<2;j++){
                c[j] += p[i][j];
            }
        }
        for (int i=0;i<2;i++){
            c[i] = c[i]/3.0;
        }

        // computing f
        double f[] = {0,0};
        double ccTc[] = {0,0};
        double Bc[] = {0,0};
        ccTc[0] = 2*(Math.pow(c[0],3) + c[0] * Math.pow(c[1],2));
        ccTc[1] = 2*(Math.pow(c[1],3) + c[1] * Math.pow(c[0],2));
        Bc[0] = B[0][0]*c[0] + B[0][1]*c[1];
        Bc[1] = B[1][0]*c[0] + B[1][1]*c[1];
        for (int i=0;i<2;++i){
            f[i] = a[i] + Bc[i] + ccTc[i];
        }

        // H
        double H[][] = {{0,0},{0,0}};
        double ccT[][] = {{0,0},{0,0}};
        for(int i=0;i<2;++i){
            for(int j=0;j<2;++j){
                ccT[i][j] = c[i]*c[j];
                H[i][j] = -2.0/3.0 * ppT [i][j] + 2*ccT[i][j];
            }
        }
        // H-1
        // det(H)
        double detH = H[0][0]*H[1][1] - H[0][1]*H[1][0];
        double invH[][] = {{H[1][1]/detH, -H[0][1]/detH},{-H[1][0]/detH,H[0][0]/detH}};

        //q = -h-1*f
        double q[] = {-invH[0][0]*f[0] - invH[0][1]*f[1],-invH[1][0]*f[0] - invH[1][1]*f[1]};

        this.coordinates[0] = c[0] + q[0];
        this.coordinates[1] = c[1] + q[1];
        return coordinates;
    }

    double[] getCoordinates() {
        computeCoordinates(distancemm);
        return this.coordinates;
    }


}
