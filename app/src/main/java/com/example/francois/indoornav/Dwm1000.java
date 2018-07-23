package com.example.francois.indoornav;

import java.util.Arrays;
import static android.os.SystemClock.sleep;

public class Dwm1000 {

    public FT311SPIMasterInterface spimInterface;

    // Constructor
    public Dwm1000(FT311SPIMasterInterface my_spimInterface){
        spimInterface = my_spimInterface;
    }

    // Initialize DWM1000
    public boolean initDwm1000(){
        byte address;
        byte[] offset;
        byte dataLength;
        // Reset DWM1000
        resetDwm1000();
        // Check DWM1000 ID
        byte[] deviceId = readDeviceId();
        byte[] deviceIdTheor = {(byte)0x30,(byte)0x01,(byte)0xca,(byte)0xde};
        if(!Arrays.equals(deviceId,deviceIdTheor)){
            return false;
        }

        // SYS_CFG: system configuration (configure receiver)
        address    = (byte)0x04;
        offset     = new byte[1]; offset[0]  = (byte)0xff;
        dataLength = (byte)0x04;
        byte[] sysCfg = readDataSpi(address,offset,dataLength);
        sysCfg[0] = (byte)0x00; // leave as is by default
        sysCfg[1] = (byte)0x12; // leave as is by default
        sysCfg[2] = (byte)0x40; // set RXM110K bit
        sysCfg[3] = (byte)0x20; // set RXAUTR: Receiver auto-re-enable
        writeDataSpi(address, offset, sysCfg, dataLength);

        // CHAN_CTRL: Configure channel control
        address    = (byte)0x1f;
        offset     = new byte[1]; offset[0]  = (byte)0xff;
        dataLength = (byte)0x04;
        byte[] chanCtrl = readDataSpi(address,offset,dataLength);
        chanCtrl[0] = (byte)0x55;
        chanCtrl[2] &= 0xc5;
        chanCtrl[2] |= 0x04;
        writeDataSpi(address, offset, chanCtrl, dataLength);

        // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        // DEFAULT CONFIGURATIONS THAT SHOULD BE MODIFIED (SECTION 2.5.5 OF USER MANUAL)
        // AGC_TUNE1
        address    = (byte)0x23;
        offset     = new byte[1]; offset[0]  = (byte)0x04;
        dataLength = (byte)0x02;
        byte[] agc_tune1 = {(byte)0x70, (byte)0x88};
        writeDataSpi(address, offset, agc_tune1, dataLength);
        // AGC_TUNE2
        address    = (byte)0x23;
        offset     = new byte[1]; offset[0]  = (byte)0x0c;
        dataLength = (byte)0x04;
        byte[] agc_tune2 = {(byte)0x07, (byte)0xa9, (byte)0x02, (byte)0x25};
        writeDataSpi(address, offset, agc_tune2, dataLength);
        // DRX_TUNE2
        address    = (byte)0x27;
        offset     = new byte[1]; offset[0]  = (byte)0x08;
        dataLength = (byte)0x04;
        byte[] drx_tune2 = {(byte)0x2d, (byte)0x00, (byte)0x1a, (byte)0x31};
        writeDataSpi(address, offset, drx_tune2, dataLength);
        // LDE_CFG1: NTM
        address    = (byte)0x2e;
        offset     = new byte[2]; offset[0] = (byte)0x08; offset[1] =  (byte)0x06;
        dataLength = (byte)0x01;
        byte[] lde_cfg1 = {(byte)0x6d};
        writeDataSpi(address, offset, lde_cfg1, dataLength);
        // LDE_CFG2
        address    = (byte)0x2e;
        offset     = new byte[2]; offset[0] = (byte)0x18; offset[1] =  (byte)0x06;
        dataLength = (byte)0x02;
        byte[] lde_cfg2 = {(byte)0x07, (byte)0x16};
        writeDataSpi(address, offset, lde_cfg2, dataLength);
        // TX_POWER
        address    = (byte)0x1e;
        offset     = new byte[1]; offset[0]  = (byte)0x00;
        dataLength = (byte)0x04;
        byte[] tx_power = {(byte)0x48, (byte)0x28, (byte)0x08, (byte)0x0e};
        writeDataSpi(address, offset, tx_power, dataLength);
        // RF_TXCTRL
        address    = (byte)0x28;
        offset     = new byte[1]; offset[0]  = (byte)0x0c;
        dataLength = (byte)0x04;
        byte[] rf_txctrl = {(byte)0xe0, (byte)0x3f, (byte)0x1e, (byte)0x00};
        writeDataSpi(address, offset, rf_txctrl, dataLength);
        // TC_PGDELAY
        address    = (byte)0x2a;
        offset     = new byte[1]; offset[0]  = (byte)0x0b;
        dataLength = (byte)0x01;
        byte[] tc_pgdelay = {(byte)0xc0};
        writeDataSpi(address, offset, tc_pgdelay, dataLength);
        // FS_PLLTUNE
        address    = (byte)0x2b;
        offset     = new byte[1]; offset[0]  = (byte)0x0b;
        dataLength = (byte)0x01;
        byte[] fs_plltune = {(byte)0xbe};
        writeDataSpi(address, offset, fs_plltune, dataLength);
        // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

        // TX_FCTRL: Transmit frame control
        address    = (byte)0x08;
        offset     = new byte[1]; offset[0]  = (byte)0xff;
        dataLength = (byte)0x05;
        byte[] tx_fctrl = readDataSpi(address,offset,dataLength);
        tx_fctrl[0] = (byte)0x0c;
        tx_fctrl[1] = (byte)0x80;
        tx_fctrl[2] = (byte)0x15;
        tx_fctrl[3] = (byte)0x00;
        tx_fctrl[4] = (byte)0x00;
        writeDataSpi(address, offset, tx_fctrl, dataLength);

        // RX_FWTO: setup Rx Timeout 5ms and set RXWTOE bit
        address    = (byte)0x0c;
        offset     = new byte[1]; offset[0]  = (byte)0xff;
        dataLength = (byte)0x02;
        byte[] timeout = {(byte)0x88, (byte)0x13};
        writeDataSpi(address, offset, timeout, dataLength);
        address    = (byte)0x04;
        offset     = new byte[1]; offset[0]  = (byte)0xff;
        dataLength = (byte)0x04;
        sysCfg = readDataSpi(address,offset,dataLength);
        sysCfg[3] |= 0x01;
        writeDataSpi(address, offset, sysCfg, dataLength);

        // No setup of IRQ (unlike in Quentin's code)
        // ...

        // TX_ANTD: Set the Tx antenna delay
        address    = (byte)0x18;
        offset     = new byte[1]; offset[0]  = (byte)0xff;
        dataLength = (byte)0x02;
        byte[] antennaDelay = {(byte)0x08, (byte)0x00};
        writeDataSpi(address, offset, antennaDelay, dataLength);


        // End of initialization function
        return true;
    }

    // Read DWM1000 device ID
    public byte[] readDeviceId(){
        byte address    = (byte)0x00;
        byte[] offset   = {(byte)0xff};
        byte dataLength = (byte)0x04;
        byte[] deviceId = readDataSpi(address,offset,dataLength);
        return deviceId;
    }

    // Send frame over UWB channel
    public void sendFrameUwb(byte[] frame, byte frameLength){
        // maximum payload length is 125 bytes
        byte address;
        byte[] offset;
        byte dataLength;
        // TX_BUFFER: write data into tx buffer
        address    = (byte)0x09;
        offset     = new byte[1]; offset[0]  = (byte)0xff;
        dataLength = frameLength;
        writeDataSpi(address, offset, frame, dataLength);
        // TX_FCTRL: set TFLEN bits to specify length of payload
        address    = (byte)0x08;
        offset     = new byte[1]; offset[0]  = (byte)0xff;
        dataLength = (byte)0x05;
        byte[] tx_fctrl = readDataSpi(address,offset,dataLength);
        tx_fctrl[0] = (byte)(frameLength+0x02); // +2 bytes for CRC
        writeDataSpi(address, offset, tx_fctrl, dataLength);
        // SYS_CTRL: Prepare transmission of a packet
        address    = (byte)0x0d;
        offset     = new byte[1]; offset[0]  = (byte)0xff;
        dataLength = (byte)0x04;
        byte[] sys_ctrl = readDataSpi(address,offset,dataLength);
        sys_ctrl[0] |= 0x02; // set TXSTRT bit to start transmission
        sys_ctrl[0] &= 0xbf; // clear TRXOFF bit to remove force idle mode
        sys_ctrl[0] |= 0x80; // set WAIT4RESP bit to wait for response
        writeDataSpi(address,offset,sys_ctrl,dataLength);
    }

    // Check for received frame over UWB channel
    public boolean checkForFrameUwb(){
        byte address;
        byte[] offset;
        byte dataLength;
        // SYS_STATUS: check for RXDFR bit
        address    = (byte)0x0f;
        offset     = new byte[1]; offset[0]  = (byte)0xff;
        dataLength = (byte)0x05;
        byte[] sys_status = readDataSpi(address,offset,dataLength);
        byte rxdfr_bit = (byte)(sys_status[1] & 0x20);
        if (rxdfr_bit == (byte)0x20){
            return true;
        }
        else{
            return false;
        }

    }

    // Read frame received over UWB channel - user code should use checkForFrameUwb() before calling this function
    public byte[] receiveFrameUwb(){
        byte address;
        byte[] offset;
        byte dataLength;
        // RX_FINFO: Get frame length
        address    = (byte)0x10;
        offset     = new byte[1]; offset[0]  = (byte)0xff;
        dataLength = (byte)0x04;
        byte[] rx_finfo = readDataSpi(address,offset,dataLength);
        byte frameLength = (byte)(rx_finfo[0]-0x02); // -2 bytes for CRC
        // RX_BUFFER: Read received frame
        address    = (byte)0x11;
        offset     = new byte[1]; offset[0]  = (byte)0xff;
        dataLength = frameLength;
        byte[] frame = readDataSpi(address,offset,dataLength);
        return frame;
    }

    // Disable UWB Rx
    void disableUwbRx(){
        // SYS_CTRL: Prepare transmission of a packet
        byte address    = (byte)0x0d;
        byte[] offset   = new byte[1]; offset[0]  = (byte)0xff;
        byte dataLength = (byte)0x04;
        byte[] sys_ctrl = readDataSpi(address,offset,dataLength);
        sys_ctrl[1] &= 0xfe; // clear RXENAB bit to stop receiving
        writeDataSpi(address,offset,sys_ctrl,dataLength);
    }

    // Enable UWB Rx
    void enableUwbRx(){
        // SYS_CTRL: Prepare transmission of a packet
        byte address    = (byte)0x0d;
        byte[] offset   = new byte[1]; offset[0]  = (byte)0xff;
        byte dataLength = (byte)0x04;
        byte[] sys_ctrl = readDataSpi(address,offset,dataLength);
        sys_ctrl[1] |= 0x01; // set RXENAB bit to start receiving
        writeDataSpi(address,offset,sys_ctrl,dataLength);
    }

    // Read from SPI
    public byte[] readDataSpi(byte address, byte[] offset, byte dataLength){
        byte numBytes;
        byte[] readWriteBuffer = new byte[64];
        byte[] numReadWritten = new byte[1];

        // Prepare readWriteBuffer for SPI transaction
        if (offset[0] == (byte)0xff){
            readWriteBuffer[0] = (byte)(address | 0x00);
            numBytes = (byte)(dataLength+0x01);
        }
        else{
            readWriteBuffer[0] = (byte)(address | 0x40);
            if ((byte)(offset[0] & 0x80) == (byte)0x00){
                readWriteBuffer[1] = offset[0];
                numBytes = (byte)(dataLength+0x02);
            }
            else{
                readWriteBuffer[1] = (byte)(offset[0] | 0x80);
                readWriteBuffer[2] = offset[1];
                numBytes = (byte)(dataLength+0x03);
            }
        }
        numReadWritten[0] = (byte) 0x00;
        // Perform SPI transaction
        spimInterface.ReadData(numBytes, readWriteBuffer, numReadWritten);
        byte[] data = Arrays.copyOfRange(readWriteBuffer, numBytes-dataLength, numBytes);
        return data;
    }

    // Write to SPI
    public void writeDataSpi(byte address, byte[] offset, byte[] data, byte dataLength){
        byte numBytes;
        byte[] readWriteBuffer = new byte[64];
        byte[] numReadWritten = new byte[1];

        // Prepare readWriteBuffer for SPI transaction
        if (offset[0] == (byte)0xff){
            readWriteBuffer[0] = (byte)(address | 0x80);
            numBytes = (byte)(dataLength+0x01);
        }
        else{
            readWriteBuffer[0] = (byte)(address | 0x80 | 0x40);
            if ((byte)(offset[0] & 0x80) == (byte)0x00){
                readWriteBuffer[1] = offset[0];
                numBytes = (byte)(dataLength+0x02);
            }
            else{
                readWriteBuffer[1] = (byte)(offset[0] | 0x80);
                readWriteBuffer[2] = offset[1];
                numBytes = (byte)(dataLength+0x03);
            }
        }
        for (int i = 0; i<dataLength; i++){
            readWriteBuffer[numBytes-dataLength+i] = data[i];
        }
        numReadWritten[0] = (byte) 0x00;
        // Perform SPI transaction
        spimInterface.SendData(numBytes, readWriteBuffer, numReadWritten);
    }


    // -------------- HELPER FUNCTIONS -------------- //
    // Reset DWM1000
    private void resetDwm1000(){
        // Reset FT311 chip
        resetFT311();
        // Read PMSC_CTRL0 register
        byte address    = (byte)0x36;
        byte[] offset   = {(byte)0x00};
        byte dataLength = (byte)0x04;
        byte[] pmsc_ctrl0 = readDataSpi(address, offset, dataLength);
        // Set SYSCLKS bits to 01
        pmsc_ctrl0[0] &= 0xfc;
        pmsc_ctrl0[0] |= 0x01;
        writeDataSpi(address, offset, pmsc_ctrl0, dataLength);
        sleep(1);
        // Clear SOFTRESET bits
        pmsc_ctrl0[3] &= 0x0f;
        writeDataSpi(address, offset, pmsc_ctrl0, dataLength);
        sleep(1);
        // Reset SOFTRESET bits and reset SYSCLKS bits to 00
        pmsc_ctrl0[3] |= 0xf0;
        pmsc_ctrl0[0] &= 0xfc;
        writeDataSpi(address, offset, pmsc_ctrl0, dataLength);
        sleep(5);

        // LOAD THE LDE ALGORITHM MICROCODE INTO LDE RAM
        // Read PMSC_CTRL0 register
        address     = (byte)0x36;
        offset[0]   = (byte)0x00;
        dataLength  = (byte)0x04;
        pmsc_ctrl0 = readDataSpi(address, offset, dataLength);
        // Set SYSCLKS bits to 01
        pmsc_ctrl0[0] = (byte)0x01;
        pmsc_ctrl0[1] = (byte)0x03;
        writeDataSpi(address, offset, pmsc_ctrl0, dataLength);
        sleep(1);
        // OTP_CTRL: set LDELOAD bit to 1
        address     = (byte)0x2d;
        offset[0]   = (byte)0x06;
        dataLength  = (byte)0x02;
        byte[] otp_ctrl = {(byte)0x00, (byte)0x80};
        writeDataSpi(address, offset, otp_ctrl, dataLength);
        sleep(1);
        // Reset SYSCLKS bits to 00
        address     = (byte)0x36;
        offset[0]   = (byte)0x00;
        dataLength  = (byte)0x04;
        pmsc_ctrl0[0] = (byte)0x00;
        pmsc_ctrl0[1] = (byte)0x02;
        writeDataSpi(address, offset, pmsc_ctrl0, dataLength);
    }

    // Reset FT311 parameters for SPI-M interface
    private void resetFT311(){
        spimInterface.Reset();
        // Set SPI interface parameters
        byte clockPhaseMode      = (byte) 0x00;
        byte dataOrderSelected   = (byte) 0x00;
        int clockFreq           = 3000000;
        spimInterface.SetConfig(clockPhaseMode,dataOrderSelected,clockFreq);
    }

}
