package com.example.francois.indoornav.decawave;

import android.os.SystemClock;

import com.example.francois.indoornav.location.ILocationProvider;
import com.example.francois.indoornav.spi.FT311SPIMaster;
import com.example.francois.indoornav.util.PointD;
import com.example.francois.indoornav.util.UwbMessages;

import java.util.Arrays;

import static com.example.francois.indoornav.util.BytesUtils.byteArray5ToLong;

public class Dwm1000Master extends Dwm1000 implements ILocationProvider {

    private final int numberSlaves = 3;
    private final UwbMessages[] messagesArray = new UwbMessages[numberSlaves];
    //private static final double correctivePol[] = { -0.0081, 0.0928, 0.6569, -0.0612};
    //private static final double correctivePol[] = {0.004396537699051796, 0.9195024228226539,
    //        0.23848199262062902}; // 4.30m calib, distance
    private static final double correctivePol[] = {-8.18957391e-07, -2.34689642e-04, -2.48275823e-02, -1.15859275e+00,
            -2.04203118e+01}; // 7.94m calib, Pr
    private static final double __estimated_power = -14.3 + 20 * Math.log10(LIGHT_SPEED) -
            20 * Math.log10(4 * Math.PI * 6489e6);

    private double distancemm[] = new double[numberSlaves];

    enum State {
        POLL_INIT,
        WAIT_POLL_SEND,
        WAIT_RESPONSE,
        WAIT_FINAL_SEND,
        GET_TIMES,
        END,
    }

    private PointD coordinates = new PointD();
    private static final int BEACONPOS1X = 60;
    private static final int BEACONPOS1Y = 321;
    private static final int BEACONPOS1Z = 295;
    private static final int BEACONPOS2X = 1220;
    private static final int BEACONPOS2Y = 46;
    private static final int BEACONPOS2Z = 225;
    private static final int BEACONPOS3X = 1220;
    private static final int BEACONPOS3Y = 699;
    private static final int BEACONPOS3Z = 155;

    private static final int TAGZ = 115;

    private static final int[] deltah = {BEACONPOS1Z - TAGZ, BEACONPOS2Z - TAGZ, BEACONPOS3Z - TAGZ};

    public Dwm1000Master(FT311SPIMaster spi) {
        super(spi);

        for (int i = 0; i < numberSlaves; ++i) {
            messagesArray[i] = new UwbMessages();
            messagesArray[i].masterPoll = new byte[]{(byte) (0x11 + i)};
            messagesArray[i].masterFinal = new byte[]{(byte) (0x21 + i)};
            messagesArray[i].slaveResponse = new byte[]{(byte) (0x1A + (i << 4))};
            distancemm[i] = 0.0;
        }
    }

    public double[] getDistances() {
        long[] allClockTime = new long[6 * messagesArray.length];
        long[] clockTime;
        for (int i = 0; i < numberSlaves; ++i) {
            clockTime = ranging(messagesArray[i]);
            if (clockTime == null) {
                return null;
            }
            System.arraycopy(clockTime, 0, allClockTime, i * 6, 6);
        }
        return compute_distances(allClockTime);
    }

    private long[] ranging(UwbMessages messages) {
        long[] clockTime = new long[6];
        State state = State.POLL_INIT;
        long startTime = SystemClock.currentThreadTimeMillis();
        boolean timeOut = false;
        while (!(state == State.END ||
                (timeOut = (SystemClock.currentThreadTimeMillis() - startTime > 500)))) {
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
                                sendFrameUwb(messages.masterFinal, (byte) messages.masterFinal.length);
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
        if (timeOut) {
            idle();
            clockTime = null;
        }
        return clockTime;
    }

    private double[] compute_distances(long[] allClockTime) {
        double tRoundMaster, tRoundSlave, tReplyMaster, tReplySlave;
        double tof;
        double distance;
        long[] clockTime = new long[6];
        for (int i = 0; i < numberSlaves; ++i) {
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
            } else if (estimated_power <= -92) {
                estimated_power = -92;
            }
            distance = distanceMeasured - (Math.pow(estimated_power, 4) * correctivePol[0] +
                    Math.pow(estimated_power, 3) * correctivePol[1] +
                    Math.pow(estimated_power, 2) * correctivePol[2] +
                    estimated_power * correctivePol[3] +
                    correctivePol[4]);
            distance *= 100;
            this.distancemm[i] = Math.sqrt(distance * distance - deltah[i] * deltah[i]);
        }
        return distancemm;
    }

    @LocationUpdated
    private int computeCoordinates(double[] distances) {

        if (distances == null) {
            return FAILED;
        }
        int p[][] = new int[2][3];
        p[0][0] = BEACONPOS1X;
        p[1][0] = BEACONPOS1Y;
        p[0][1] = BEACONPOS2X;
        p[1][1] = BEACONPOS2Y;
        p[0][2] = BEACONPOS3X;
        p[1][2] = BEACONPOS3Y;

        double sumDistanceSquared = 0;
        int[][] ppT = {{0, 0}, {0, 0}};
        double[] c = {0, 0};

        for (int i = 0; i < numberSlaves; ++i) {
            sumDistanceSquared += distances[i] * distances[i];
            ppT[0][0] += p[0][i] * p[0][i];
            ppT[1][0] += p[1][i] * p[0][i];
            ppT[1][1] += p[1][i] * p[1][i];
            c[0] += p[0][i];
            c[1] += p[1][i];
        }

        c[0] /= numberSlaves;
        c[1] /= numberSlaves;

        ppT[0][1] = ppT[1][0];
        double[][] ccT = {{c[0] * c[0], c[0] * c[1]}, {c[0] * c[1], c[1] * c[1]}};

        int pTp;
        double temp;
        double[] a = {0, 0};
        double[][] B = {{sumDistanceSquared - 2 * ppT[0][0], -2 * ppT[0][1]},
                {-2 * ppT[1][0], sumDistanceSquared - 2 * ppT[1][1]}};

        for (int i = 0; i < numberSlaves; ++i) {
            pTp = p[0][i] * p[0][i] + p[1][i] * p[1][i];
            temp = pTp - distances[i] * distances[i];

            a[0] += temp * p[0][i];
            a[1] += temp * p[1][i];

            B[0][0] -= pTp;
            B[1][1] -= pTp;

        }

        a[0] /= numberSlaves;
        a[1] /= numberSlaves;
        B[0][0] /= numberSlaves;
        B[1][0] /= numberSlaves;
        B[0][1] /= numberSlaves;
        B[1][1] /= numberSlaves;

        double[] f = new double[2];

        f[0] = a[0] + B[0][0] * c[0] + B[0][1] * c[1] + 2 * (ccT[0][0] * c[0] + ccT[0][1] * c[1]);
        f[1] = a[1] + B[1][0] * c[0] + B[1][1] * c[1] + 2 * (ccT[1][0] * c[0] + ccT[1][1] * c[1]);

        double[][] H = {{2 * (-ppT[0][0] / (double) numberSlaves + ccT[0][0]),
                2 * (-ppT[0][1] / (double) numberSlaves + ccT[0][1])},
                {2 * (-ppT[1][0] / (double) numberSlaves + ccT[1][0]),
                        2 * (-ppT[1][1] / (double) numberSlaves + ccT[1][1])}};

        double detH = H[0][0] * H[1][1] - H[0][1] * H[1][0];
        double invH[][] = {{H[1][1] / detH, -H[0][1] / detH}, {-H[1][0] / detH, H[0][0] / detH}};
        double q[] = {-invH[0][0] * f[0] - invH[0][1] * f[1], -invH[1][0] * f[0] - invH[1][1] * f[1]};

        coordinates.set(c[0] + q[0], c[1] + q[1]);
        return SUCCESS;
    }

    @Override
    @LocationUpdated
    public int updateLocation() {
        return computeCoordinates(getDistances());
    }

    @Override
    public PointD getLastLocation() {
        return coordinates;
    }

}
