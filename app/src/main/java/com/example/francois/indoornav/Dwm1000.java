package com.example.francois.indoornav;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import static android.os.SystemClock.sleep;

abstract class Dwm1000 {

    static abstract class Define {
        static abstract class Channel{
            enum CHANNEL {
                _1((byte)1,3494.4e6,499.2e6),_2((byte)2,3993.6e6,499.2e6),
                _3((byte)3,4492.8e6,499.2e6),_4((byte)4,3993.6e6,900e6),
                _5((byte)5,6489.6e6,499.2e6),_7((byte)7,6489.6e6,900e6);
                final byte value;
                final double fc;
                final double bw;

                CHANNEL(byte numval, double fcarrier, double bandwidth) {
                    this.value = numval;
                    this.fc = fcarrier;
                    this.bw = bandwidth;
                }
            }
            enum PRF {
                _16MHZ((byte)0b01), _64MHZ((byte)0b10);
                final byte value;

                PRF(byte numval) {
                    this.value = numval;
                }
            }

            enum BITRATE {
                _110KBPS((byte)0b00), _850KBPS((byte)0b01), _6800KBPS((byte)0b10);
                final byte value;

                BITRATE(byte numval) {
                    this.value = numval;
                }
            }

            enum PE {
                _64((byte)0b0001), _128((byte)0b0101),_256((byte)0b1001), _512((byte)0b1101),
                _1024((byte)0b0010), _1536((byte)0b0110),_2048((byte)0b1010), _4096((byte)0b0011);
                final byte value;

                PE(byte numval) {
                    this.value = numval;
                }
            }

        }
        static abstract class Spi {
            enum DATAORDER {
                MSB((byte) 0), LSB((byte) 1);
                final byte value;

                DATAORDER(byte numVal) {
                    this.value = numVal;
                }
            }
        }
    }


    private Config config;

    Config getConfig() {
        return config;
    }

    class Config {
        final Spi spi;
        final Transmitter transmitter;
        final Receiver receiver;
        private Define.Channel.CHANNEL channel;

        Config() {
            spi = new Spi();
            resetIC();
            transmitter = new Transmitter();
            receiver = new Receiver();
            channel = Define.Channel.CHANNEL._5;
            updateChannel();
        }

        private void resetIC() {
            byte address    = PMSC;
            byte offset   = (byte)0x00;
            byte dataLength = (byte)0x01;
            byte[] pmsc_ctrl0 = new byte[1];
            // Set SYSCLKS bits to 01 (other 6 bits can be cleared)
            pmsc_ctrl0[0] = (byte) 0x01;

            writeDataSpi(address, offset, pmsc_ctrl0, dataLength);
            sleep(1);
            // Clear SOFTRESET bits
            offset = (byte) 0x03;

            pmsc_ctrl0[0] = (byte) 0x00;

            writeDataSpi(address, offset, pmsc_ctrl0, dataLength);
            sleep(1);
            // Reset SOFTRESET bits

            pmsc_ctrl0[0] = (byte) 0xF0;

            writeDataSpi(address, offset, pmsc_ctrl0, dataLength);
            sleep(5);

            // LOAD THE LDE ALGORITHM MICROCODE INTO LDE RAM
            // Write to PMSC_CTRL0 register: set LSBs to 0x0301
            address     = PMSC;
            offset   = (byte)0x00;
            dataLength  = (byte)0x02;
            pmsc_ctrl0 = new byte[] {(byte) 0x01, (byte) 0x03};
            writeDataSpi(address, offset, pmsc_ctrl0, dataLength);
            sleep(1);
            // OTP_CTRL: set LDELOAD bit to 1
            address     = OTP_IF;
            offset   = (byte)0x06;
            dataLength  = (byte)0x02;
            byte[] otp_ctrl = {(byte)0x00, (byte)0x80};
            writeDataSpi(address, offset, otp_ctrl, dataLength);
            sleep(10);
            // Reset SYSCLKS bits to 00
            address     = PMSC;
            offset   = (byte)0x00;
            dataLength  = (byte)0x02;
            pmsc_ctrl0[0] = (byte)0x00;
            pmsc_ctrl0[1] = (byte)0x02;
            writeDataSpi(address, offset, pmsc_ctrl0, dataLength);
            sleep(10);
            //maxSpeedFT311();
        }

        void setChannel(Define.Channel.CHANNEL channel) {
            this.channel = channel;
            transmitter.setChannel(channel);
            receiver.setChannel(channel);
            updateChannel();
        }

        private void updateChannel() {
            byte chan[] = new byte[]{(byte)((channel.value << 4)|channel.value)};
            writeDataSpi(CHAN_CTRL, chan, (byte)1);
            byte pllTune[] = new byte[1];
            ByteBuffer pllCfg = ByteBuffer.allocate(4);
            pllCfg.order(ByteOrder.LITTLE_ENDIAN);
            switch (this.channel) {
                case _1:
                    pllCfg.putInt(0x09000407);
                    pllTune[0] = (byte)0x1E;
                    break;
                case _2: case _4:
                    pllCfg.putInt(0x08400508);
                    pllTune[0] = (byte)0x26;
                    break;
                case _3:
                    pllCfg.putInt(0x08401009);
                    pllTune[0] = (byte)0x56;
                    break;
                case _5: case _7:
                    pllCfg.putInt(0x0800041D);
                    pllTune[0] = (byte)0xBE;
                    break;
            }
            writeDataSpi(FS_CTRL, (byte)0x07, pllCfg.array(), (byte)4);
            writeDataSpi(FS_CTRL, (byte)0x0B, pllTune, (byte)1);
        }

        private class Channel extends Define.Channel {
            private CHANNEL channel;
            private byte preambleCode;
            private PE preambleSize;
            private BITRATE bitRate;
            private PRF prf;

            Channel() {
                this(CHANNEL._5, BITRATE._6800KBPS, (byte) 4, PE._128, PRF._16MHZ);
            }

            private Channel(CHANNEL channelNumber, BITRATE bitrate, byte preambleCode,
                    PE preambleSize, PRF prf) {
                this.channel = channelNumber;
                this.prf = prf;
                this.bitRate = bitrate;
                this.preambleCode = validPreambleCode(preambleCode);
                this.preambleSize = validPreambleSize(preambleSize);
//                this.preambleSize = preambleSize;
            }

            void setPreambleCode(byte preambleCode) {
                this.preambleCode = validPreambleCode(preambleCode);
            }

            void setPreambleSize(PE preambleSize) {
                this.preambleSize = validPreambleSize(preambleSize);
//                this.preambleSize = preambleSize;
            }

            private void setChannel(CHANNEL channel) {
                this.channel = channel;
                if (!isValidPreambleCode(this.preambleCode)) {
                    setPreambleCode(this.preambleCode);
                }
            }

            void setBitRate(BITRATE bitRate) {
                this.bitRate = bitRate;
                if (!isValidPreambleSize(preambleSize)){
                    setPreambleSize(preambleSize);
                }
            }

            void setPrf(PRF prf) {
                this.prf = prf;
                if (!isValidPreambleCode(this.preambleCode)) {
                    setPreambleCode(this.preambleCode);
                }
            }

            private byte validPreambleCode(byte preambleCode) {
                switch (this.prf) {
                    case _16MHZ:
                        switch (this.channel) {
                            case _1:
                                if (preambleCode != (byte) 1 && preambleCode != (byte) 2) {
                                    preambleCode = 1;
                                }
                                break;
                            case _2: case _5:
                                if (preambleCode != (byte) 3 && preambleCode != (byte) 4) {
                                    preambleCode = 3;
                                }
                                break;
                            case _3:
                                if (preambleCode != (byte) 5 && preambleCode != (byte) 6) {
                                    preambleCode = 5;
                                }
                                break;
                            case _4: case _7:
                                if (preambleCode != (byte) 7 && preambleCode != (byte) 8) {
                                    preambleCode = 7;
                                }
                                break;
                        }
                        break;
                    case _64MHZ:
                        switch (this.channel) {
                            case _1: case _2: case _3: case _5:
                                if (preambleCode < (byte) 9 || preambleCode > (byte) 12) {
                                    preambleCode = 9;
                                }
                                break;
                            case _4: case _7:
                                if (preambleCode < (byte) 17 || preambleCode > (byte) 20) {
                                    preambleCode = 17;
                                }
                                break;
                        }
                        break;
                }
                return preambleCode;
            }

            private boolean isValidPreambleCode(byte preambleCode) {
                return validPreambleCode(preambleCode) == preambleCode;
            }

            private PE validPreambleSize(PE preambleSize) {
                switch (preambleSize) {
                    case _1536: case _2048: case _4096:
                        if (bitRate != BITRATE._110KBPS){
                            preambleSize = PE._1024;
                        }
                        break;
                    case _128: case _256: case _512: case _1024:
                        if (bitRate == BITRATE._110KBPS){
                            preambleSize = PE._1536;
                        }
                        break;
                    case _64:
                        switch (bitRate){
                            case _850KBPS:
                                preambleSize = PE._128;
                                break;
                            case _110KBPS:
                                preambleSize = PE._1536;
                                break;
                        }
                        break;
                }
                return preambleSize;
            }

            private boolean isValidPreambleSize(PE preambleSize){
                return validPreambleSize(preambleSize) == preambleSize;
            }
            BITRATE getBitRate() {
                return bitRate;
            }

            byte getPreambleCode() {
                return preambleCode;
            }

            CHANNEL getChannel() {
                return channel;
            }

            PE getPreambleSize() {
                return preambleSize;
            }

            PRF getPrf() {
                return prf;
            }
        }


        class Transmitter extends Channel{

            Transmitter() {
                super();
                updateChannel();
                updateBitrate();
                updatePrf();
                updatePreambleCode();
                updatePreambleSize();
            }
            private void setChannel(CHANNEL channel) {
                super.setChannel(channel);
                updateChannel();
            }

            @Override
            void setBitRate(BITRATE bitRate) {
                super.setBitRate(bitRate);
                updateBitrate();
            }

            @Override
            void setPreambleCode(byte preambleCode) {
                super.setPreambleCode(preambleCode);
                updatePreambleCode();
            }

            @Override
            void setPreambleSize(PE preambleSize) {
                super.setPreambleSize(preambleSize);
                updatePreambleSize();
            }

            @Override
            void setPrf(PRF prf) {
                super.setPrf(prf);
                updatePrf();
            }

            private void updateChannel() {
                byte tc[] = new byte[1];
                ByteBuffer txctrl = ByteBuffer.allocate(4);
                txctrl.order(ByteOrder.LITTLE_ENDIAN);
                switch (getChannel()) {
                    case _1:
                        txctrl.putInt(0x00005C40);
                        tc[0] = (byte) 0xC9;
                        break;
                    case _2:
                        txctrl.putInt(0x00045CA0);
                        tc[0] = (byte) 0xC2;
                        break;
                    case _3:
                        txctrl.putInt(0x00086CC0);
                        tc[0] = (byte) 0xC5;
                        break;
                    case _4:
                        txctrl.putInt(0x00045C80);
                        tc[0] = (byte) 0x95;
                        break;
                    case _5:
                        txctrl.putInt(0x001E3FE0);
                        tc[0] = (byte) 0xC0;
                        break;
                    case _7:
                        txctrl.putInt(0x001E7DE0);
                        tc[0] = (byte) 0x93;
                        break;
                }
                writeDataSpi(RF_CONF, (byte) 0x0C, txctrl.array(), (byte) 3);
                writeDataSpi(TX_CAL, (byte) 0x0B, tc, (byte) 1);
                updateTxPower();
            }
            private void updateBitrate() {
                writeDataSpi(TX_FCTRL, (byte) 1,
                        new byte[]{(byte)((getBitRate().value << 5) | (1<<TR))}, (byte) 1);
                updateTxPower();
            }

            private void updatePreambleCode() {
                byte chan[] = readDataSpi(CHAN_CTRL, (byte)2, (byte)2);
                byte preambleCode = getPreambleCode();
                chan[0] &= 0x3F;
                chan[1] &= 0xF8;
                chan[0] |= preambleCode << 6;
                chan[1] |= preambleCode >>> 2;
                writeDataSpi(CHAN_CTRL, (byte)2, chan, (byte)2);
            }

            private void updatePreambleSize() {
                writeDataSpi(TX_FCTRL, (byte)2,new byte[]{(byte)(getPrf().value |
                        (getPreambleSize().value << 2))}, (byte)1);
            }

            private void updatePrf() {
                   writeDataSpi(TX_FCTRL, (byte)2, new byte[]{(byte)(getPrf().value |
                           (getPreambleSize().value << 2))}, (byte)1);
                updateTxPower();
            }

            private void updateTxPower() {
                ByteBuffer txPower = ByteBuffer.allocate(4);
                txPower.order(ByteOrder.LITTLE_ENDIAN);
                    switch (getBitRate()) {
                        case _6800KBPS:
                            switch (getPrf()) {
                                case _16MHZ:
                                    switch (getChannel()) {
                                        case _1: case _2:
                                            txPower.putInt(0x15355575);
                                            break;
                                        case _3:
                                            txPower.putInt(0x0F2F4F6F);
                                            break;
                                        case _4:
                                            txPower.putInt(0x1F1F3F5F);
                                            break;
                                        case _5:
                                            txPower.putInt(0x0E082848);
                                            break;
                                        case _7:
                                            txPower.putInt(0x32527292);
                                            break;
                                    }
                                    break;
                                case _64MHZ:
                                    switch (getChannel()) {
                                        case _1: case _2:
                                            txPower.putInt(0x07274767);
                                            break;
                                        case _3:
                                            txPower.putInt(0x2B4B6B8B);
                                            break;
                                        case _4:
                                            txPower.putInt(0x3A5A7A9A);
                                            break;
                                        case _5:
                                            txPower.putInt(0x25456585);
                                            break;
                                        case _7:
                                            txPower.putInt(0x5171B1D1);
                                            break;
                                    }
                                    break;
                            }
                            break;
                        default:
                            switch (getPrf()) {
                                case _16MHZ:
                                    switch (getChannel()) {
                                        case _1: case _2:
                                            txPower.putInt(0x75757575);
                                            break;
                                        case _3:
                                            txPower.putInt(0x6F6F6F6F);
                                            break;
                                        case _4:
                                            txPower.putInt(0x5F5F5F5F);
                                            break;
                                        case _5:
                                            txPower.putInt(0x48484848);
                                            break;
                                        case _7:
                                            txPower.putInt(0x92929292);
                                            break;
                                    }
                                    break;
                                case _64MHZ:
                                    switch (getChannel()) {
                                        case _1: case _2:
                                            txPower.putInt(0x67676767);
                                            break;
                                        case _3:
                                            txPower.putInt(0x8B8B8B8B);
                                            break;
                                        case _4:
                                            txPower.putInt(0x9A9A9A9A);
                                            break;
                                        case _5:
                                            txPower.putInt(0x85858585);
                                            break;
                                        case _7:
                                            txPower.putInt(0xD1D1D1D1);
                                            break;
                                    }
                                    break;
                            }
                            break;
                    }
                    writeDataSpi(TX_POWER, txPower.array(), (byte)4);
            }
        }

        class Receiver extends Channel{
            private short frameTimeoutDelay = 0;
            private int pac;
            private int sfd;

            Receiver() {
                super();
                updateChannel();
                updateBitrate();
                updatePrf();
                updatePreambleCode();
                updatePreambleSize();
                updateFrameTimeoutDelay();
            }
            private void setChannel(CHANNEL channel) {
                super.setChannel(channel);
                updateChannel();
            }

            @Override
            void setBitRate(BITRATE bitRate) {
                super.setBitRate(bitRate);
                updateBitrate();
            }

            @Override
            void setPreambleCode(byte preambleCode) {
                super.setPreambleCode(preambleCode);
                updatePreambleCode();
            }

            @Override
            void setPreambleSize(PE preambleSize) {
                super.setPreambleSize(preambleSize);
                updatePreambleSize();
            }

            @Override
            void setPrf(PRF prf) {
                super.setPrf(prf);
                updatePrf();
            }

            void setFrameTimeoutDelay(short timeoutDelay) {
                this.frameTimeoutDelay = timeoutDelay;
                updateFrameTimeoutDelay();

            }

            private  void updateFrameTimeoutDelay() {
                byte sysCfg[] = readDataSpi(SYS_CFG, (byte) 0x03, (byte)0x01);
                sysCfg[0] |= (1<<RXWTOE);
                ByteBuffer rxfwto = ByteBuffer.allocate(2);
                rxfwto.order(ByteOrder.LITTLE_ENDIAN);
                rxfwto.putShort(frameTimeoutDelay);
                if (frameTimeoutDelay == 0){
                    sysCfg[0] &= ~(1<<RXWTOE);
                }

                writeDataSpi(RX_FWTO, rxfwto.array(), (byte)0x02);
                writeDataSpi(SYS_CFG, (byte) 0x03, sysCfg, (byte)0x01);
            }

            private void updateChannel() {
                byte rxCtrl[] = new byte[1];
                switch (getChannel()){
                    case _1: case _2: case _3: case _5:
                        rxCtrl[0] = (byte)0xD8;
                        break;
                    case _4: case _7:
                        rxCtrl[0] = (byte)0xBC;
                        break;
                }
                writeDataSpi(RF_CONF, (byte)0x0B, rxCtrl, (byte)1);
            }

            private void updateBitrate() {
                 byte sys[] = new byte[1];
                 ByteBuffer drxTune0b = ByteBuffer.allocate(2);
                 drxTune0b.order(ByteOrder.LITTLE_ENDIAN);
                 switch (getBitRate()){
                     case _110KBPS:
                         drxTune0b.putShort((short)0x000A);
                         sys[0] = 1<<RXM110K;
                         sfd = 64;
                         break;
                     case _850KBPS: case _6800KBPS:
                         drxTune0b.putShort((short)0x0001);
                         sfd = 8;
                         break;
                 }
                 writeDataSpi(SYS_CFG, (byte)2, sys, (byte)1);
                 writeDataSpi(DRX_CONF, (byte)0x02, drxTune0b.array(),(byte)2);
                 updateSFDTimeout();
            }

            private void updatePreambleCode() {
                byte chan[] = readDataSpi(CHAN_CTRL, (byte)3, (byte)1);
                byte preambleCode = getPreambleCode();
                chan[0] &= 0x07;
                chan[0] |= preambleCode << RX_PCODE;
                writeDataSpi(CHAN_CTRL, (byte)3, chan, (byte)1);
            }

            private void updatePreambleSize() {
                ByteBuffer drxTune1b = ByteBuffer.allocate(2);
                drxTune1b.order(ByteOrder.LITTLE_ENDIAN);
                switch (getPreambleSize()){
                    case _64:
                        drxTune1b.putShort((short)0x0064);
                        break;
                    case _128: case _256: case _512: case _1024:
                        drxTune1b.putShort((short)0x0020);
                        break;
                    case _1536: case _2048: case _4096:
                        drxTune1b.putShort((short)0x0010);
                        break;
                }
                writeDataSpi(DRX_CONF, (byte)0x06, drxTune1b.array(), (byte)2);
                updatePacSize();
                updateSFDTimeout();
            }

            private void updatePrf() {
                byte prf[] = readDataSpi(CHAN_CTRL, (byte)2, (byte)1);
                prf[0] &= 0xF3;
                prf[0] |= getPrf().value << RXPRF;
                writeDataSpi(CHAN_CTRL, (byte)2, prf, (byte)1);
                ByteBuffer drxTune1a = ByteBuffer.allocate(2);
                ByteBuffer agcTune1 = ByteBuffer.allocate(2);
                ByteBuffer ldeCfg2 = ByteBuffer.allocate(2);
                agcTune1.order(ByteOrder.LITTLE_ENDIAN);
                drxTune1a.order(ByteOrder.LITTLE_ENDIAN);
                ldeCfg2.order(ByteOrder.LITTLE_ENDIAN);
                switch (getPrf()){
                    case _16MHZ:
                        drxTune1a.putShort((short)0x0087);
                        agcTune1.putShort((short)0x8870);
                        ldeCfg2.putShort((short)0x1607);
                        break;
                    case _64MHZ:
                        drxTune1a.putShort((short)0x008D);
                        agcTune1.putShort((short)0x889B);
                        ldeCfg2.putShort((short)0x0607);
                        break;
                }
                writeDataSpi(DRX_CONF, (byte)0x04, drxTune1a.array(), (byte)2);
                writeDataSpi(AGC_CTRL,(byte)0x04, agcTune1.array(), (byte)2);
                writeDataSpi(LDE_CTRL, (short)0x1806, ldeCfg2.array(), (byte)2);
                updatePacSize();
            }

            private void updatePacSize() {
                ByteBuffer drxTune2 = ByteBuffer.allocate(4);
                drxTune2.order(ByteOrder.LITTLE_ENDIAN);
                switch (getPrf()){
                    case _16MHZ:
                        switch (getPreambleSize()){
                            case _64: case _128:
                                drxTune2.putInt(0x311A002D);
                                pac = 8;
                                break;
                            case _256: case _512:
                                drxTune2.putInt(0x331A0052);
                                pac = 16;
                                break;
                            case _1024:
                                drxTune2.putInt(0x351A009A);
                                pac = 32;
                                break;
                            case _1536: case _2048: case _4096:
                                drxTune2.putInt(0x371A011D);
                                pac = 64;
                                break;
                        }
                        break;
                    case _64MHZ:
                        switch (getPreambleSize()){
                            case _64: case _128:
                                drxTune2.putInt(0x313B006B);
                                pac = 8;
                                break;
                            case _256: case _512:
                                drxTune2.putInt(0x333B00BE);
                                pac = 16;
                                break;
                            case _1024:
                                drxTune2.putInt(0x353B015E);
                                pac = 32;
                                break;
                            case _1536: case _2048: case _4096:
                                drxTune2.putInt(0x373B0296);
                                pac = 64;
                                break;
                        }
                        break;
                }
                writeDataSpi(DRX_CONF, (byte)0x08, drxTune2.array(), (byte)4);
            }

            private void updateSFDTimeout() {
                ByteBuffer timeout = ByteBuffer.allocate(2);
                timeout.order(ByteOrder.LITTLE_ENDIAN);
                timeout.putShort((short)(Integer.parseInt(getPreambleSize().toString().substring(1)) +
                        sfd + 1 - pac));
                writeDataSpi(DRX_CONF, (byte)0x20, timeout.array(),(byte)2);
            }
        }

        class Spi extends Define.Spi {
            private byte clockPhaseMode      = (byte) 0;
            private DATAORDER dataOrderSelected   = DATAORDER.MSB;
            private int clockFreq            = 3000000;

            Spi(){
                reset();
            }
            void update() {
                mSpi.SetConfig(clockPhaseMode,dataOrderSelected.value,clockFreq);
            }

            void set(byte clockPhaseMode, DATAORDER dataOrderSelected, int clockFreq) {
                setClockPhaseMode(clockPhaseMode);
                setDataOrderSelected(dataOrderSelected);
                setClockFreq(clockFreq);
                mSpi.SetConfig(this.clockPhaseMode, this.dataOrderSelected.value, this.clockFreq);
            }

            void reset() {
                mSpi.Reset();
                clockPhaseMode      = (byte) 0;
                dataOrderSelected   = DATAORDER.MSB;
                clockFreq           = 3000000;
                update();
            }

            void setClockPhaseMode(byte clockPhaseMode) {
                if (clockPhaseMode < 0 || clockPhaseMode > 3){
                    clockPhaseMode = 0;
                }
                this.clockPhaseMode = clockPhaseMode;
            }

            void setDataOrderSelected(DATAORDER dataOrderSelected) {
                this.dataOrderSelected = dataOrderSelected;
            }

            void setClockFreq(int clockFreq) {
                if (clockFreq < 3000000) {
                    clockFreq = 3000000;
                }
                else if (clockFreq > 18000000) {
                    clockFreq = 18000000;
                }
                this.clockFreq = clockFreq;
            }

            byte getClockPhaseMode() {
                return clockPhaseMode;
            }

            DATAORDER getDataOrderSelected() {
                return dataOrderSelected;
            }

            int getClockFreq(){
                return clockFreq;
            }

        }
    }

    private static final byte[] DEV_ID_THEOR    = {(byte)0x30,(byte)0x01,(byte)0xca,(byte)0xde};
            static final double TIME_UNIT       = 1 / (128 * 499.2 * 1000000);
            static final int    LIGHT_SPEED     = 299792458;
    private static final short  ANTENNA_DELAY   = (short)0x8000;

    /*                  REGISTERS MAP                */

    private static final byte DEV_ID     = (byte)0x00;
    private static final byte EUI        = (byte)0x01;
    private static final byte PANADR     = (byte)0x03;
    private static final byte SYS_CFG    = (byte)0x04;
    private static final byte SYS_TIME   = (byte)0x06;
    private static final byte TX_FCTRL   = (byte)0x08;
    private static final byte TX_BUFFER  = (byte)0x09;
    private static final byte DX_TIME    = (byte)0x0A;
    private static final byte RX_FWTO    = (byte)0X0C;
    private static final byte SYS_CTRL   = (byte)0x0D;
    private static final byte SYS_MASK   = (byte)0x0E;
    private static final byte SYS_STATUS = (byte)0x0F;
    private static final byte RX_FINFO   = (byte)0x10;
    private static final byte RX_BUFFER  = (byte)0x11;
    private static final byte RX_FQUAL   = (byte)0x12;
    private static final byte RX_TTCKI   = (byte)0x13;
    private static final byte RX_TTCKO   = (byte)0x14;
            static final byte RX_TIME    = (byte)0x15;
            static final byte TX_TIME    = (byte)0x17;
            static final byte TX_ANTD    = (byte)0x18;
    private static final byte SYS_STATE  = (byte)0x19;
    private static final byte ACK_RESP_T = (byte)0x1A;
    private static final byte RX_SNIFF   = (byte)0x1D;
    private static final byte TX_POWER   = (byte)0x1E;
    private static final byte CHAN_CTRL  = (byte)0x1F;
    private static final byte USR_SFD    = (byte)0x21;
    private static final byte AGC_CTRL   = (byte)0x23;
    private static final byte EXT_SYNC   = (byte)0x24;
    private static final byte ACC_MEM    = (byte)0x25;
    private static final byte GPIO_CTRL  = (byte)0x26;
    private static final byte DRX_CONF   = (byte)0x27;
    private static final byte RF_CONF    = (byte)0x28;
    private static final byte TX_CAL     = (byte)0x2A;
    private static final byte FS_CTRL    = (byte)0x2B;
    private static final byte AON        = (byte)0x2C;
    private static final byte OTP_IF     = (byte)0x2D;
    static final byte LDE_CTRL   = (byte)0x2E;
    private static final byte DIG_DIAG   = (byte)0x2F;
    private static final byte PMSC       = (byte)0x36;

    /*                REGISTERS BIT MAP              */
    private static final byte RXM110K   = (byte)6;
    private static final byte RXAUTR    = (byte)5;
    private static final byte RXPRF     = (byte)2;
    private static final byte RX_PCODE  = (byte)3;
    private static final byte TR        = (byte)7;
    private static final byte RXWTOE    = (byte)4;
    private static final byte TXSTRT    = (byte)1;
    private static final byte TRXOFF    = (byte)6;
    private static final byte WAIT4RESP = (byte)7;
    private static final byte RXDFR     = (byte)5;
    private static final byte TXFRS     = (byte)7;

    /*                 REGISTERS MASK                */

    private static final int RX_ERROR_MASK      = 0x4279000; //SYS_STATUS, length: 4
    private static final int RX_RESET_MASK      = 0x79000;
    private static final int RX_OK              = 0x6400;
    private static final int RX_MUST_CLEAR      = 0x60000;

    private byte[] readWriteBuffer = new byte[64];


    private final FT311SPIMaster mSpi;

    // Constructor
    Dwm1000(FT311SPIMaster spi){
        mSpi = spi;
    }

    // Initialize DWM1000
    boolean initDwm1000() {
        // Reset DWM1000
        config = new Config();
        // Check DWM1000 ID
        byte[] deviceId = readDeviceId();
        if(!Arrays.equals(deviceId,DEV_ID_THEOR)){
            return false;
        }

        // AGC_TUNE2
        byte[] agc_tune2 = {(byte)0x07, (byte)0xa9, (byte)0x02, (byte)0x25};
        writeDataSpi(AGC_CTRL, (byte)0x0c, agc_tune2, (byte)0x04);

        // LDE_CFG1: NTM
        byte[] lde_cfg1 = {(byte)0x6d};	// LOS CONFIGURATION
        //byte[] lde_cfg1 = {(byte)0x07};		// NLOS CONFIGURATION

        writeDataSpi(LDE_CTRL, (short)0x0806, lde_cfg1, (byte)0x01);

        // LDE_CFG2
        //byte[] lde_cfg2 = {(byte)0x07, (byte)0x16};
        // NLOS CONFIGURATION
        //byte[] lde_cfg2 = {(byte)0x03, (byte)0x00};

        config.receiver.setFrameTimeoutDelay((short)5000);

        // End of initialization function
        return true;
    }

    // Read DWM1000 device ID
    byte[] readDeviceId(){
        byte dataLength = (byte)DEV_ID_THEOR.length;
        return readDataSpi(DEV_ID,dataLength);
    }

    // Send frame over UWB channel
    void sendFrameUwb(byte[] frame, byte frameLength) {
        // maximum payload length is 125 bytes
        // TX_BUFFER: write data into tx buffer
        writeDataSpi(TX_BUFFER, frame, frameLength);

        // TX_FCTRL: set TFLEN bits to specify length of payload
        byte [] tx_fctrl = new byte[] {(byte)(frameLength+0x02)}; // +2 bytes for CRC
        writeDataSpi(TX_FCTRL, tx_fctrl, (byte)0x01);

        // SYS_CTRL: Prepare transmission of a packet
        byte[] sys_ctrl = readDataSpi(SYS_CTRL, (byte)0x01);

        sys_ctrl[0] |= (1<<TXSTRT); // set TXSTRT bit to start transmission
        //sys_ctrl[0] &= ~(1<<TRXOFF); // clear TRXOFF bit to remove force idle mode
        sys_ctrl[0] |= (1<<WAIT4RESP); // set WAIT4RESP bit to wait for response
        writeDataSpi(SYS_CTRL,sys_ctrl, (byte)0x01);
    }

    //

    /**
     * Check for received frame over UWB channel
     * @return int, 0 if frame received, 1 if RX has been reset and re-enabled due to internal
     * error(s), 2 if frame timeout (RX reset but not re-enabled), 3 if still waiting
     */
    int checkForFrameUwb() {
        byte address;
        byte dataLength;
        int result = 3;
        // SYS_STATUS: check for RX error or timeout
        address = SYS_STATUS;
        dataLength = (byte) 0x04;
        int sys_status = byteArray4ToInt(readDataSpi(address, dataLength));
        if ((sys_status & RX_ERROR_MASK) != 0) {
            result = 2;
            if ((sys_status & RX_MUST_CLEAR) != 0) {
                writeDataSpi(address, (byte)0x02, new byte[] {(byte)0x06}, (byte)0x01);
            }
            if ((sys_status & RX_RESET_MASK) != 0) {
                resetRx();
            }
            if ((sys_status & 0x20000) == 0) {
                //enableUwbRx();
                result = 1;
            }
        } else if ((sys_status & RX_OK) == RX_OK) {
            result = 0;
        } else if ((sys_status & (1<<(RXDFR+8))) == 1<<(RXDFR+8)) {
            throw new java.lang.Error("LDE misconfiguration");
        }
        return result;
    }

    boolean checkFrameSent(){
        return (readDataSpi(SYS_STATUS, (byte) 0x01)[0] & (1 << TXFRS)) == (1 << TXFRS);
    }

    // Read frame received over UWB channel - user code should use checkForFrameUwb() before calling this function
    byte[] receiveFrameUwb(){
        // RX_FINFO: Get frame length
        byte[] rx_finfo = readDataSpi(RX_FINFO, (byte)0x01);
        byte frameLength = (byte)((rx_finfo[0]&~(1<<7))-0x02); // -2 bytes for CRC

        // RX_BUFFER: Read received frame
        return readDataSpi(RX_BUFFER, frameLength);
    }

    // Disable tranceiver (force idle)
    void idle() {
        byte[] sys_ctrl = new byte[] {(byte)(1<<TRXOFF)};
        writeDataSpi(SYS_CTRL,sys_ctrl,(byte)0x01);
    }
    // Disable UWB Rx
    /*void disableUwbRx(){
        // SYS_CTRL: Prepare transmission of a packet
        byte address    = SYS_CTRL;
        byte offset     = (byte)0x01;
        byte dataLength = (byte)0x01;
        byte[] sys_ctrl = readDataSpi(address, offset, dataLength);
        sys_ctrl[0] &= ~0x01; // clear RXENAB bit to stop receiving
        writeDataSpi(address, offset, sys_ctrl,dataLength);
    }*/

    // Enable UWB Rx
    private void enableUwbRx() {
        // SYS_CTRL: Prepare transmission of a packet
        byte[] sys_ctrl = new byte[] {(byte)0x01}; // set RXENAB bit to start receiving
        writeDataSpi(SYS_CTRL, (byte)0x01,sys_ctrl, (byte)0x01);
    }


    // Read from SPI
    //  1-octet
    synchronized byte[] readDataSpi(byte address, byte dataLength){
        byte numBytes = dataLength;

        // Prepare readWriteBuffer for SPI transaction
        readWriteBuffer[0] = address;
        ++numBytes;
        // Perform SPI transaction
        mSpi.ReadData(numBytes, readWriteBuffer);
        return Arrays.copyOfRange(readWriteBuffer, 1, numBytes);
    }
    //  2-octet
    synchronized private byte[] readDataSpi(byte address, byte offset, byte dataLength) {
        byte numBytes = dataLength;

        // Prepare readWriteBuffer for SPI transaction
        readWriteBuffer[0] = (byte)(address | 0x40);
        readWriteBuffer[1] = offset;
        numBytes += 2;
        // Perform SPI transaction
        mSpi.ReadData(numBytes, readWriteBuffer);
        return Arrays.copyOfRange(readWriteBuffer, 2, numBytes);
    }
    //  3-octet
    synchronized byte[] readDataSpi(byte address, short offset, byte dataLength) {
        byte numBytes = dataLength;

        // Prepare readWriteBuffer for SPI transaction

        readWriteBuffer[0] = (byte)(address | 0x40);
        readWriteBuffer[1] = (byte)(offset | 0x80);
        readWriteBuffer[2] = (byte)(offset >> 7);
        numBytes += 3;

        // Perform SPI transaction
        mSpi.ReadData(numBytes, readWriteBuffer);
        return Arrays.copyOfRange(readWriteBuffer, 3, numBytes);
    }

    // Write to SPI
    //  1-octet
    synchronized void writeDataSpi(byte address, byte[] data, byte dataLength) {
        byte numBytes = dataLength;

        // Prepare readWriteBuffer for SPI transaction
        readWriteBuffer[0] = (byte)(address | 0x80);
        ++numBytes;

        System.arraycopy(data, 0, readWriteBuffer, numBytes - dataLength, dataLength);
        // Perform SPI transaction
        mSpi.SendData(numBytes, readWriteBuffer);
    }
    //  2-octet
    synchronized private void writeDataSpi(byte address, byte offset, byte[] data, byte dataLength) {
        byte numBytes = dataLength;

        // Prepare readWriteBuffer for SPI transaction

        readWriteBuffer[0] = (byte)(address | 0x80 | 0x40);
        readWriteBuffer[1] = offset;
        numBytes += 2;

        System.arraycopy(data, 0, readWriteBuffer, numBytes - dataLength, dataLength);
        // Perform SPI transaction
        mSpi.SendData(numBytes, readWriteBuffer);
    }
    //  3-octet
    synchronized void writeDataSpi(byte address, short offset, byte[] data, byte dataLength) {
        byte numBytes = dataLength;

        // Prepare readWriteBuffer for SPI transaction

        readWriteBuffer[0] = (byte)(address | 0x80 | 0x40);
        readWriteBuffer[1] = (byte)(offset | 0x80);
        readWriteBuffer[2] = (byte)(offset >> 7);
        numBytes += 3;

        System.arraycopy(data, 0, readWriteBuffer, numBytes - dataLength, dataLength);
        // Perform SPI transaction
        mSpi.SendData(numBytes, readWriteBuffer);
    }


    // -------------- HELPER FUNCTIONS -------------- //

    double RxPower() {
        double A = config.receiver.getPrf() == Define.Channel.PRF._16MHZ ? 113.77:121.74;
        int to_remove = config.receiver.getBitRate() == Define.Channel.BITRATE._110KBPS ? 64:5;
        byte[] cir_pwr = readDataSpi(RX_FQUAL, (byte)6, (byte)2);
        long C = (cir_pwr[0] & 0xFF) | (cir_pwr[1] & 0xFF) << 8;
        byte[] rxpacc = readDataSpi(RX_FINFO, (byte)2, (byte) 2);
        byte [] rxpacc_nosat = readDataSpi(DRX_CONF,(byte) 0x2C, (byte)2);
        long N = (rxpacc[0] & 0xF0)>>>4 | (rxpacc[1] & 0xFF)<< 4;
        long correct = (rxpacc_nosat[0] & 0xFF) | (rxpacc_nosat[1] & 0xFF)<<8;
        if (N == correct) {
            N -= to_remove;
        }
        return 10 * Math.log10(C * (1<<17) / (N * N)) - A;
    }

    double RxPowerFirstPath() {
        double A = config.receiver.getPrf() == Define.Channel.PRF._16MHZ ? 113.77:121.74;
        int to_remove = config.receiver.getBitRate() == Define.Channel.BITRATE._110KBPS ? 64:5;
        byte[] fp_ampl1 = readDataSpi(RX_TIME, (byte)7,(byte)2);
        long F1 = (fp_ampl1[0] & 0xFF) | (fp_ampl1[1] & 0xFF) <<8;
        byte[] fp_ampl23 = readDataSpi(RX_FQUAL, (byte)0x02, (byte)4);
        long F2 = (fp_ampl23[0] & 0xFF) | (fp_ampl23[1] & 0xFF) <<8;
        long F3 = (fp_ampl23[2] & 0xFF) | (fp_ampl23[3] & 0xFF) <<8;
        byte[] rxpacc = readDataSpi(RX_FINFO, (byte)2, (byte) 2);
        byte [] rxpacc_nosat = readDataSpi(DRX_CONF,(byte) 0x2C, (byte)2);
        long N = (rxpacc[0] & 0xF0)>>>4 | (rxpacc[1] & 0xFF)<< 4;
        long correct = (rxpacc_nosat[0] & 0xFF) | (rxpacc_nosat[1] & 0xFF)<<8;
        if (N == correct) {
            N -= to_remove;
        }
        return 10 * Math.log10((F1*F1 + F2*F2 + F3*F3) / (N * N)) - A;
    }

    private void resetRx() {
        byte address    = PMSC;
        byte offset   = (byte)0x03;
        byte dataLength = (byte)0x01;
        byte[] pmsc_ctrl0 = new byte[1];
        // Clear SOFTRESET bits

        pmsc_ctrl0[0] = (byte) 0xe0;

        writeDataSpi(address, offset, pmsc_ctrl0, dataLength);
        sleep(1);
        // Reset SOFTRESET bits

        pmsc_ctrl0[0] = (byte) 0xF0;

        writeDataSpi(address, offset, pmsc_ctrl0, dataLength);
    }

    // Reset FT311 parameters for SPI-M interface
    private void resetFT311(){
        config.spi.reset();
    }
    private void maxSpeedFT311(){
        byte clockPhaseMode      = (byte) 0x00;
        Define.Spi.DATAORDER dataOrderSelected   = Define.Spi.DATAORDER.MSB;
        int clockFreq            = 18000000;
        config.spi.set(clockPhaseMode,dataOrderSelected,clockFreq);
    }

    static int byteArray4ToInt(byte[] bytes){
        return (bytes[0] & 0xFF) |
                (bytes[1] & 0xFF) << 8 |
                (bytes[2] & 0xFF) << 16 |
                (bytes[3] & 0xFF) << 24;
    }

    static long byteArray5ToLong(byte[] bytes) {
        return (long)(bytes[0] & 0xFF) |
                (long)(bytes[1] & 0xFF) << 8 |
                (long)(bytes[2] & 0xFF) << 16 |
                (long)(bytes[3] & 0xFF) << 24 |
                (long)(bytes[4] & 0xFF) << 32;

    }
}
