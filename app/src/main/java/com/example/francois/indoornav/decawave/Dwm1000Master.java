package com.example.francois.indoornav.decawave;

import android.os.SystemClock;

import com.example.francois.indoornav.location.ILocationProvider;
import com.example.francois.indoornav.spi.FT311SPIMaster;
import com.example.francois.indoornav.util.PointD;
import com.example.francois.indoornav.util.UwbFrame;

import java.util.Arrays;

import static com.example.francois.indoornav.util.BytesUtils.byteArray4ToInt;
import static com.example.francois.indoornav.util.BytesUtils.byteArray5ToLong;

public class Dwm1000Master extends Dwm1000 implements ILocationProvider {

    private final int numberSlaves = 6;
    private final UwbFrame masterPoll = new UwbFrame();
    private final UwbFrame masterFinal = new UwbFrame();
    private final UwbFrame slaveMessage = new UwbFrame();
    //private static final double correctivePol[] = { -0.0081, 0.0928, 0.6569, -0.0612};
    //private static final double correctivePol[] = {0.004396537699051796, 0.9195024228226539,
    //        0.23848199262062902}; // 4.30m calib, distance
    private static final double correctivePol[] = {-8.18957391e-07, -2.34689642e-04, -2.48275823e-02, -1.15859275e+00,
            -2.04203118e+01}; // 7.94m calib, Pr
    private static final double __estimated_power = -14.3 + 20 * Math.log10(LIGHT_SPEED) -
            20 * Math.log10(4 * Math.PI * 6489e6);

    private double distancemm[] = new double[numberSlaves];
    private long allClockTime[] = new long[6*numberSlaves];
    private int beaconCoordinates[][] = new int[numberSlaves][3];
    private short beaconAddress[] = new short[numberSlaves];
    private int numberResponse;
    private int numberTwr;

    enum State {
        POLL_INIT,
        WAIT_POLL_SEND,
        WAIT_RESPONSE,
        WAIT_FINAL_SEND,
        GET_TIMES,
        END,
    }

    private PointD coordinates = new PointD();
    /*private static final int BEACONPOS1X = 60;
    private static final int BEACONPOS1Y = 321;
    private static final int BEACONPOS1Z = 295;
    private static final int BEACONPOS2X = 1220;
    private static final int BEACONPOS2Y = 46;
    private static final int BEACONPOS2Z = 225;
    private static final int BEACONPOS3X = 1220;
    private static final int BEACONPOS3Y = 699;
    private static final int BEACONPOS3Z = 155;*/

    private static final int TAGZ = 155;

    //private static final int[] deltah = {BEACONPOS1Z - TAGZ, BEACONPOS2Z - TAGZ, BEACONPOS3Z - TAGZ};

    public Dwm1000Master(FT311SPIMaster spi) {
        super(spi);


        masterPoll.uwbFrameShort.setFc((short) 0x8841);
        masterPoll.uwbFrameShort.setSeq((byte) 0);
        masterPoll.uwbFrameShort.setDstPanId(mPanId);
        masterPoll.uwbFrameShort.setDstAddr(mBroadcast);
        masterPoll.uwbFrameShort.setSrcAddr(mAddress);
        masterPoll.uwbFrameShort.setData(new byte[] {0});

        masterFinal.uwbFrameShort.setFc((short) 0x8841);
        masterFinal.uwbFrameShort.setSeq((byte) 0);
        masterFinal.uwbFrameShort.setDstPanId(mPanId);
        masterFinal.uwbFrameShort.setDstAddr(mBroadcast);
        masterFinal.uwbFrameShort.setSrcAddr(mAddress);
        masterFinal.uwbFrameShort.setData(new byte[] {2});
    }

    public double[] getDistances() {

        return compute_distances(ranging());
    }

    private long[] ranging() {
        State state = State.POLL_INIT;
        long startTime = 0;
        byte[] data;
        boolean waiting = false;
        int position;
        numberTwr = 0;
        for (int i = 0; i < beaconAddress.length; ++i) {
            beaconAddress[i] = (short) 0xFFFF;
        }
        numberResponse = 0;

        while (true) {
            switch (state) {
                case POLL_INIT:
                    sendFrameUwb(masterPoll.getFrame(),
                            (byte) masterPoll.uwbFrameShort.getFrameLength());
                    state = State.WAIT_POLL_SEND;
                    break;
                case WAIT_POLL_SEND:
                    if (checkFrameSent()) {
                        allClockTime[0] = byteArray5ToLong(readDataSpi(TX_TIME, (byte) 0x05));
                        startTime = SystemClock.currentThreadTimeMillis();
                        state = State.WAIT_RESPONSE;
                    }
                    break;
                case WAIT_RESPONSE:
                    switch (checkForFrameUwb()) {
                        case 0:
                            waiting = false;
                            receiveFrameUwb(slaveMessage);
                            if(slaveMessage.uwbFrameShort.getFc() == (short)0x8841 &&
                                    slaveMessage.uwbFrameShort.getDstAddr() == mAddress &&
                                    slaveMessage.uwbFrameShort.getDstPanId() == mPanId &&
                                    (data = slaveMessage.uwbFrameShort.getData())[0] == 0x01) {
                                beaconAddress[numberResponse]
                                        = slaveMessage.uwbFrameShort.getSrcAddr();
                                beaconCoordinates[numberResponse][0]
                                        = byteArray4ToInt(Arrays.copyOfRange(data, 1, 5));
                                beaconCoordinates[numberResponse][1]
                                        = byteArray4ToInt(Arrays.copyOfRange(data, 5, 9));
                                beaconCoordinates[numberResponse][2]
                                        = byteArray4ToInt(Arrays.copyOfRange(data, 9, 13));
                                allClockTime[2+numberResponse*2]
                                        = byteArray5ToLong(Arrays.copyOfRange(data, 13, 18));
                                allClockTime[3+numberResponse*2]
                                        = byteArray5ToLong(readDataSpi(RX_TIME, (byte) 0x05));
                                ++numberResponse;
                            }
                            break;
                        case 1:
                            waiting = false;
                            break;
                        case 2:
                            waiting = false;
                            break;
                        case 3:
                            waiting = true;
                            break;
                    }
                    if(SystemClock.currentThreadTimeMillis() - startTime < 120
                            && numberResponse < 6) {
                        if(!waiting) enableUwbRx();
                    } else {
                        if(waiting) idle();
                        if(numberResponse < 3) return allClockTime;
                        sendFrameUwb(masterFinal.getFrame(),
                                (byte) masterFinal.uwbFrameShort.getFrameLength());
                        state = State.WAIT_FINAL_SEND;
                    }
                    break;
                case WAIT_FINAL_SEND:
                    if (checkFrameSent()) {
                        allClockTime[1] = byteArray5ToLong(readDataSpi(TX_TIME, (byte) 0x05));
                        startTime = SystemClock.currentThreadTimeMillis();
                        state = State.GET_TIMES;
                    }
                    break;
                case GET_TIMES:
                    switch (checkForFrameUwb()) {
                        case 0:
                            waiting = false;
                            receiveFrameUwb(slaveMessage);
                            if(slaveMessage.uwbFrameShort.getFc() == (short)0x8841 &&
                                    slaveMessage.uwbFrameShort.getDstAddr() == mAddress &&
                                    slaveMessage.uwbFrameShort.getDstPanId() == mPanId &&
                                    (position = contains(beaconAddress, numberResponse,
                                            slaveMessage.uwbFrameShort.getSrcAddr())) != -1 &&
                                    (data = slaveMessage.uwbFrameShort.getData())[0] == 0x03) {
                                allClockTime[2+numberResponse*2+position*2]
                                        = byteArray5ToLong(Arrays.copyOfRange(data, 1, 6));
                                allClockTime[3+numberResponse*2+position*2]
                                        = byteArray5ToLong(Arrays.copyOfRange(data, 6, 12));
                                ++numberTwr;
                            }
                            break;
                        case 1:
                            waiting = false;
                            break;
                        case 2:
                            waiting = false;
                            break;
                        case 3:
                            waiting = true;
                            break;
                    }
                    if(SystemClock.currentThreadTimeMillis() - startTime < 120
                            && numberTwr < numberResponse) {
                        if(!waiting) enableUwbRx();
                    } else {
                        if(waiting) idle();
                        return allClockTime;
                    }
                    break;
            }
        }
    }

    private double[] compute_distances(long[] allClockTime) {
        if(numberTwr < 3) return distancemm;
        double tRoundMaster, tRoundSlave, tReplyMaster, tReplySlave;
        double tof;
        double distance;
        for (int i = 0; i < numberTwr; ++i) {
            tRoundMaster = allClockTime[3+i*2] - allClockTime[0];
            tReplyMaster = allClockTime[1] - allClockTime[3+i*2];
            tReplySlave = allClockTime[2+numberResponse*2+i*2] - allClockTime[2+i*2];
            tRoundSlave = allClockTime[3+numberResponse*2+i*2] - allClockTime[2+numberResponse*2+i*2];

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
            int deltaH = beaconCoordinates[i][3] - TAGZ;
            this.distancemm[i] = Math.sqrt(distance * distance - deltaH * deltaH);
        }
        return distancemm;
    }

    @LocationUpdated
    private int computeCoordinates(double[] distances) {
        if(numberTwr < 3) return FAILED;
        int p[][] = new int[2][6];
        for(int i = 0; i < numberTwr; i++) {
            p[0][i] = beaconCoordinates[i][0];
            p[1][i] = beaconCoordinates[i][1];
        }

        double sumDistanceSquared = 0;
        int[][] ppT = {{0, 0}, {0, 0}};
        double[] c = {0, 0};

        for (int i = 0; i < numberTwr; ++i) {
            sumDistanceSquared += distances[i] * distances[i];
            ppT[0][0] += p[0][i] * p[0][i];
            ppT[1][0] += p[1][i] * p[0][i];
            ppT[1][1] += p[1][i] * p[1][i];
            c[0] += p[0][i];
            c[1] += p[1][i];
        }

        c[0] /= numberTwr;
        c[1] /= numberTwr;

        ppT[0][1] = ppT[1][0];
        double[][] ccT = {{c[0] * c[0], c[0] * c[1]}, {c[0] * c[1], c[1] * c[1]}};

        int pTp;
        double temp;
        double[] a = {0, 0};
        double[][] B = {{sumDistanceSquared - 2 * ppT[0][0], -2 * ppT[0][1]},
                {-2 * ppT[1][0], sumDistanceSquared - 2 * ppT[1][1]}};

        for (int i = 0; i < numberTwr; ++i) {
            pTp = p[0][i] * p[0][i] + p[1][i] * p[1][i];
            temp = pTp - distances[i] * distances[i];

            a[0] += temp * p[0][i];
            a[1] += temp * p[1][i];

            B[0][0] -= pTp;
            B[1][1] -= pTp;

        }

        a[0] /= numberTwr;
        a[1] /= numberTwr;
        B[0][0] /= numberTwr;
        B[1][0] /= numberTwr;
        B[0][1] /= numberTwr;
        B[1][1] /= numberTwr;

        double[] f = new double[2];

        f[0] = a[0] + B[0][0] * c[0] + B[0][1] * c[1] + 2 * (ccT[0][0] * c[0] + ccT[0][1] * c[1]);
        f[1] = a[1] + B[1][0] * c[0] + B[1][1] * c[1] + 2 * (ccT[1][0] * c[0] + ccT[1][1] * c[1]);

        double[][] H = {{2 * (-ppT[0][0] / (double) numberTwr + ccT[0][0]),
                2 * (-ppT[0][1] / (double) numberTwr + ccT[0][1])},
                {2 * (-ppT[1][0] / (double) numberTwr + ccT[1][0]),
                        2 * (-ppT[1][1] / (double) numberTwr + ccT[1][1])}};

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

    private int contains(short[] input, int maxidx, short value) {
        for (int i = 0; i < maxidx; ++i) {
            if (input[i] == value) return i;
        }
        return -1;
    }

}
