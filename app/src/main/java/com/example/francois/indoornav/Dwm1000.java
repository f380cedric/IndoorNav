package com.example.francois.indoornav;

import java.util.Arrays;
import static android.os.SystemClock.sleep;

abstract class Dwm1000 {

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
            static final byte RX_FINFO   = (byte)0x10;
    private static final byte RX_BUFFER  = (byte)0x11;
            static final byte RX_FQUAL   = (byte)0x12;
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
    static final byte DRX_CONF   = (byte)0x27;
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


    private final FT311SPIMasterInterface spimInterface;

    // Constructor
    Dwm1000(FT311SPIMasterInterface my_spimInterface){
        spimInterface = my_spimInterface;
    }

    // Initialize DWM1000
    boolean initDwm1000(){
        // Reset DWM1000
        resetDwm1000();
        // Check DWM1000 ID
        byte[] deviceId = readDeviceId();
        if(!Arrays.equals(deviceId,DEV_ID_THEOR)){
            return false;
        }

        // SYS_CFG: system configuration (configure receiver)
        byte[] sysCfg;

        // CHAN_CTRL: Configure channel control
        byte[] chanCtrl = new byte[] {(byte) (1<<RXPRF)}; // Set RXPRF to 01
        writeDataSpi(CHAN_CTRL, (byte)0x02, chanCtrl, (byte)0x01);



        // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        // DEFAULT CONFIGURATIONS THAT SHOULD BE MODIFIED (SECTION 2.5.5 OF USER MANUAL)
        // AGC_TUNE1
        byte[] agc_tune1 = {(byte)0x70, (byte)0x88};
        writeDataSpi(AGC_CTRL, (byte)0x04, agc_tune1, (byte)0x02);

        // AGC_TUNE2
        byte[] agc_tune2 = {(byte)0x07, (byte)0xa9, (byte)0x02, (byte)0x25};
        writeDataSpi(AGC_CTRL, (byte)0x0c, agc_tune2, (byte)0x04);

        // DRX_TUNE2
        byte[] drx_tune2 = {(byte)0x2d, (byte)0x00, (byte)0x1a, (byte)0x31};
        writeDataSpi(DRX_CONF, (byte)0x08, drx_tune2, (byte)0x04);

        // LDE_CFG1: NTM
        byte[] lde_cfg1 = {(byte)0x6d};
        writeDataSpi(LDE_CTRL, (short)0x0806, lde_cfg1, (byte)0x01);

        // LDE_CFG2
        byte[] lde_cfg2 = {(byte)0x07, (byte)0x16};
        writeDataSpi(LDE_CTRL, (short) 0x1806, lde_cfg2, (byte)0x02);

        // TX_POWER
        byte[] tx_power = {(byte)0x48, (byte)0x28, (byte)0x08, (byte)0x0e};
        writeDataSpi(TX_POWER, tx_power, (byte)0x04);

        // RF_TXCTRL
        byte[] rf_txctrl = {(byte)0xe0, (byte)0x3f, (byte)0x1e};
        writeDataSpi(RF_CONF, (byte)0x0c, rf_txctrl, (byte)0x03);

        // TC_PGDELAY
        byte[] tc_pgdelay = {(byte)0xc0};
        writeDataSpi(TX_CAL, (byte)0x0b, tc_pgdelay, (byte)0x01);

        // FS_PLLTUNE
        byte[] fs_plltune = {(byte)0xbe};
        writeDataSpi(FS_CTRL, (byte)0x0b, fs_plltune, (byte)0x01);

        // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

        // TX_FCTRL: Transmit frame control
        byte[] tx_fctrl = new byte[] {(byte)0xC0};
        writeDataSpi(TX_FCTRL, (byte)0x01, tx_fctrl, (byte)0x01);

        // RX_FWTO: setup Rx Timeout 5ms and set RXWTOE bit
        byte[] timeout = {(byte)0x88, (byte)0x13};
        writeDataSpi(RX_FWTO, timeout, (byte)0x02);

        sysCfg = readDataSpi(SYS_CFG, (byte) 0x03, (byte)0x01);
        sysCfg[0] |= (1<<RXWTOE);
        writeDataSpi(SYS_CFG, (byte) 0x03, sysCfg, (byte)0x01);

        // No setup of IRQ (unlike in Quentin's code)
        // ...

        // TX_ANTD: Set the Tx antenna delay
        //byte[] antennaDelay = {(byte)ANTENNA_DELAY, (byte)(ANTENNA_DELAY>>8)};
        //writeDataSpi(TX_ANTD, antennaDelay, (byte)0x02);

        // End of initialization function
        return true;
    }

    // Read DWM1000 device ID
    byte[] readDeviceId(){
        byte dataLength = (byte)DEV_ID_THEOR.length;
        return readDataSpi(DEV_ID,dataLength);
    }

    // Send frame over UWB channel
    void sendFrameUwb(byte[] frame, byte frameLength){
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
    void idle(){
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
    private void enableUwbRx(){
        // SYS_CTRL: Prepare transmission of a packet
        byte[] sys_ctrl = new byte[] {(byte)0x01}; // set RXENAB bit to start receiving
        writeDataSpi(SYS_CTRL, (byte)0x01,sys_ctrl, (byte)0x01);
    }


    // Read from SPI
    //  1-octet
    byte[] readDataSpi(byte address, byte dataLength){
        byte[] readWriteBuffer = new byte[64];
        byte numBytes = dataLength;

        // Prepare readWriteBuffer for SPI transaction
        readWriteBuffer[0] = address;
        ++numBytes;
        // Perform SPI transaction
        spimInterface.ReadData(numBytes, readWriteBuffer);
        return Arrays.copyOfRange(readWriteBuffer, 1, numBytes);
    }
    //  2-octet
    private byte[] readDataSpi(byte address, byte offset, byte dataLength){
        byte[] readWriteBuffer = new byte[64];
        byte numBytes = dataLength;

        // Prepare readWriteBuffer for SPI transaction
        readWriteBuffer[0] = (byte)(address | 0x40);
        readWriteBuffer[1] = offset;
        numBytes += 2;
        // Perform SPI transaction
        spimInterface.ReadData(numBytes, readWriteBuffer);
        return Arrays.copyOfRange(readWriteBuffer, 2, numBytes);
    }
    //  3-octet
    byte[] readDataSpi(byte address, short offset, byte dataLength){
        byte[] readWriteBuffer = new byte[64];
        byte numBytes = dataLength;

        // Prepare readWriteBuffer for SPI transaction

        readWriteBuffer[0] = (byte)(address | 0x40);
        readWriteBuffer[1] = (byte)(offset | 0x80);
        readWriteBuffer[2] = (byte)(offset >> 7);
        numBytes += 3;

        // Perform SPI transaction
        spimInterface.ReadData(numBytes, readWriteBuffer);
        return Arrays.copyOfRange(readWriteBuffer, 3, numBytes);
    }

    // Write to SPI
    //  1-octet
    void writeDataSpi(byte address, byte[] data, byte dataLength){
        byte[] readWriteBuffer = new byte[64];
        byte numBytes = dataLength;

        // Prepare readWriteBuffer for SPI transaction
        readWriteBuffer[0] = (byte)(address | 0x80);
        ++numBytes;

        System.arraycopy(data, 0, readWriteBuffer, numBytes - dataLength, dataLength);
        // Perform SPI transaction
        spimInterface.SendData(numBytes, readWriteBuffer);
    }
    //  2-octet
    void writeDataSpi(byte address, byte offset, byte[] data, byte dataLength){
        byte[] readWriteBuffer = new byte[64];
        byte numBytes = dataLength;

        // Prepare readWriteBuffer for SPI transaction

        readWriteBuffer[0] = (byte)(address | 0x80 | 0x40);
        readWriteBuffer[1] = offset;
        numBytes += 2;

        System.arraycopy(data, 0, readWriteBuffer, numBytes - dataLength, dataLength);
        // Perform SPI transaction
        spimInterface.SendData(numBytes, readWriteBuffer);
    }
    //  3-octet
    void writeDataSpi(byte address, short offset, byte[] data, byte dataLength){
        byte[] readWriteBuffer = new byte[64];
        byte numBytes = dataLength;

        // Prepare readWriteBuffer for SPI transaction

        readWriteBuffer[0] = (byte)(address | 0x80 | 0x40);
        readWriteBuffer[1] = (byte)(offset | 0x80);
        readWriteBuffer[2] = (byte)(offset >> 7);
        numBytes += 3;

        System.arraycopy(data, 0, readWriteBuffer, numBytes - dataLength, dataLength);
        // Perform SPI transaction
        spimInterface.SendData(numBytes, readWriteBuffer);
    }


    // -------------- HELPER FUNCTIONS -------------- //
    // Reset DWM1000
    private void resetDwm1000(){
        // Reset FT311 chip
        resetFT311();
        // Write to PMSC_CTRL0 register
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

    private void resetRx(){
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
        spimInterface.Reset();
        // Set SPI interface parameters
        byte clockPhaseMode      = (byte) 0x00;
        byte dataOrderSelected   = (byte) 0x00;
        int clockFreq            = 3000000;
        spimInterface.SetConfig(clockPhaseMode,dataOrderSelected,clockFreq);
    }
    private void maxSpeedFT311(){
        byte clockPhaseMode      = (byte) 0x00;
        byte dataOrderSelected   = (byte) 0x00;
        int clockFreq            = 18000000;
        spimInterface.SetConfig(clockPhaseMode,dataOrderSelected,clockFreq);
    }

    private static int byteArray4ToInt(byte[] bytes){
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
